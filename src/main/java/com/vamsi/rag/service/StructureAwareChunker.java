
package com.vamsi.rag.service;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class StructureAwareChunker {

    private static final int MAX_CHUNK_CHARS = 2500;
    private static final int OVERLAP_CHARS = 250;
    private static final int MIN_CHUNK_CHARS = 100;

    private static final Pattern MARKDOWN_HEADING =
            Pattern.compile("^#{1,6}\\s+.+$");

    private static final Pattern NUMBERED_HEADING =
            Pattern.compile(
                    "^\\d+(?:\\.\\d+)*[.)]?\\s+[A-Z].{1,110}$"
            );

    private static final Pattern SENTENCE_BOUNDARY =
            Pattern.compile("(?<=[.!?])\\s+");

    public List<Document> chunk(List<Document> documents) {

        List<Document> result = new ArrayList<>();

        if (documents == null || documents.isEmpty()) {
            return result;
        }

        // Keep the active heading across PDF pages.
        String currentHeading = "General";
        int globalChunkIndex = 0;

        for (Document document : documents) {

            if (document == null ||
                    document.getText() == null ||
                    document.getText().isBlank()) {
                continue;
            }

            PageResult pageResult =
                    processPage(document, currentHeading);

            currentHeading = pageResult.lastHeading();

            for (SectionBlock section : pageResult.sections()) {

                List<String> sectionChunks =
                        createChunks(section.paragraphs());

                for (int i = 0; i < sectionChunks.size(); i++) {

                    String text = sectionChunks.get(i);

                    if (text.isBlank()) {
                        continue;
                    }

                    Map<String, Object> metadata =
                            new HashMap<>(document.getMetadata());

                    metadata.put("section", section.heading());
                    metadata.put("chunk_index", globalChunkIndex++);
                    metadata.put("chunk_part", i);
                    metadata.put(
                            "chunking_strategy",
                            "structure-aware-v1"
                    );

                    result.add(new Document(
                            "Section: " + section.heading()
                                    + "\n\n" + text,
                            metadata
                    ));
                }
            }
        }

        return result;
    }

    private PageResult processPage(
            Document document,
            String initialHeading) {

        List<SectionBlock> sections = new ArrayList<>();

        String heading = initialHeading;

        List<String> paragraphs = new ArrayList<>();

        StringBuilder paragraph = new StringBuilder();

        for (String rawLine :
                document.getText().split("\\R")) {

            String line = rawLine.trim();

            if (line.isEmpty()) {
                flushParagraph(paragraph, paragraphs);
                continue;
            }

            if (isHeading(line)) {

                flushParagraph(paragraph, paragraphs);

                if (!paragraphs.isEmpty()) {
                    sections.add(
                            new SectionBlock(
                                    heading,
                                    new ArrayList<>(paragraphs)
                            )
                    );
                    paragraphs.clear();
                }

                heading = cleanHeading(line);

            } else {

                if (!paragraph.isEmpty()) {
                    paragraph.append(" ");
                }

                paragraph.append(line);
            }
        }

        flushParagraph(paragraph, paragraphs);

        if (!paragraphs.isEmpty()) {
            sections.add(
                    new SectionBlock(
                            heading,
                            new ArrayList<>(paragraphs)
                    )
            );
        }

        return new PageResult(sections, heading);
    }

    private void flushParagraph(
            StringBuilder paragraph,
            List<String> paragraphs) {

        String value = paragraph.toString().trim();

        if (!value.isEmpty()) {
            paragraphs.add(value);
        }

        paragraph.setLength(0);
    }

    private boolean isHeading(String line) {

        if (line.length() > 120) {
            return false;
        }

        if (MARKDOWN_HEADING.matcher(line).matches()) {
            return true;
        }

        if (NUMBERED_HEADING.matcher(line).matches()) {
            return true;
        }

        // Fallback for uppercase headings extracted from PDFs.
        if (line.length() >= 4 &&
                !line.endsWith(".") &&
                !line.endsWith("?") &&
                !line.endsWith("!")) {

            long letters = line.chars()
                    .filter(Character::isLetter)
                    .count();

            long uppercase = line.chars()
                    .filter(Character::isUpperCase)
                    .count();

            return letters >= 4 &&
                    (double) uppercase / letters >= 0.85;
        }

        return false;
    }

    private String cleanHeading(String heading) {

        return heading
                .replaceFirst("^#{1,6}\\s+", "")
                .trim();
    }

    private List<String> createChunks(
            List<String> paragraphs) {

        List<String> chunks = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {

            List<String> pieces =
                    splitLargeParagraph(paragraph);

            for (String piece : pieces) {

                if (piece.isBlank()) {
                    continue;
                }

                if (!current.isEmpty() &&
                        current.length() + 2 + piece.length()
                                > MAX_CHUNK_CHARS) {

                    addChunk(chunks, current.toString());

                    String overlap =
                            getOverlap(current.toString());

                    current.setLength(0);

                    if (!overlap.isEmpty()) {
                        current.append(overlap);
                    }
                }

                // Ensure overlap never causes the size limit
                // to be exceeded.
                int available =
                        MAX_CHUNK_CHARS - current.length();

                if (piece.length() > available) {

                    if (!current.isEmpty()) {
                        addChunk(chunks, current.toString());
                        current.setLength(0);
                    }

                    // splitLargeParagraph normally prevents this,
                    // but retain a safe fallback.
                    current.append(piece, 0,
                            Math.min(
                                    piece.length(),
                                    MAX_CHUNK_CHARS
                            ));

                } else {

                    if (!current.isEmpty()) {
                        current.append("\n\n");
                    }

                    current.append(piece);
                }
            }
        }

        if (!current.isEmpty()) {
            addChunk(chunks, current.toString());
        }

        return chunks;
    }

    private List<String> splitLargeParagraph(
            String paragraph) {

        if (paragraph.length() <= MAX_CHUNK_CHARS) {
            return List.of(paragraph);
        }

        List<String> result = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        String[] sentences =
                SENTENCE_BOUNDARY.split(paragraph);

        for (String sentence : sentences) {

            sentence = sentence.trim();

            if (sentence.isEmpty()) {
                continue;
            }

            if (sentence.length() > MAX_CHUNK_CHARS) {

                flushLargeSentence(current, result);

                splitByWords(sentence, result);
                continue;
            }

            if (!current.isEmpty() &&
                    current.length() + 1 + sentence.length()
                            > MAX_CHUNK_CHARS) {

                result.add(current.toString());
                current.setLength(0);
            }

            if (!current.isEmpty()) {
                current.append(" ");
            }

            current.append(sentence);
        }

        flushLargeSentence(current, result);

        return result;
    }

    private void flushLargeSentence(
            StringBuilder current,
            List<String> result) {

        if (!current.isEmpty()) {
            result.add(current.toString());
            current.setLength(0);
        }
    }

    private void splitByWords(
            String text,
            List<String> result) {

        StringBuilder current = new StringBuilder();

        for (String word : text.split("\\s+")) {

            if (!current.isEmpty() &&
                    current.length() + 1 + word.length()
                            > MAX_CHUNK_CHARS) {

                result.add(current.toString());
                current.setLength(0);
            }

            if (!current.isEmpty()) {
                current.append(" ");
            }

            current.append(word);
        }

        if (!current.isEmpty()) {
            result.add(current.toString());
        }
    }

    private String getOverlap(String previousChunk) {

        if (OVERLAP_CHARS <= 0 ||
                previousChunk.length() <= MIN_CHUNK_CHARS) {
            return "";
        }

        int start = Math.max(
                0,
                previousChunk.length() - OVERLAP_CHARS
        );

        // Avoid starting in the middle of a word.
        int wordBoundary =
                previousChunk.indexOf(' ', start);

        if (wordBoundary >= 0 &&
                wordBoundary < previousChunk.length() - 1) {
            start = wordBoundary + 1;
        }

        return previousChunk.substring(start).trim();
    }

    private void addChunk(
            List<String> chunks,
            String text) {

        String value = text.trim();

        if (value.length() >= MIN_CHUNK_CHARS) {
            chunks.add(value);
        }
    }

    private record SectionBlock(
            String heading,
            List<String> paragraphs) {
    }

    private record PageResult(
            List<SectionBlock> sections,
            String lastHeading) {
    }
}
