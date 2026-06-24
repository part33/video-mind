package com.example.server.util;

import com.example.server.entity.SceneSegment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class SegmentBuilder {

    private SegmentBuilder() {
    }

    public static List<SceneSegment> build(Long mediaId, String transcript, String summary) {
        List<String> sentences = extractSentences(transcript, summary);
        List<SceneSegment> segments = new ArrayList<>();
        int chunkSize = Math.max(1, (int) Math.ceil(sentences.size() / 4.0));
        int cursor = 0;

        for (int i = 0; i < sentences.size(); i += chunkSize) {
            int end = Math.min(sentences.size(), i + chunkSize);
            List<String> chunk = sentences.subList(i, end);
            String merged = String.join(" ", chunk).trim();

            SceneSegment segment = new SceneSegment();
            segment.setMediaId(mediaId);
            segment.setSegmentIndex(segments.size() + 1);
            segment.setStartSecond(cursor);
            cursor += 8 + Math.min(4, chunk.size());
            segment.setEndSecond(cursor);
            segment.setTitle(buildTitle(chunk.get(0), segments.size() + 1));
            segment.setSceneSummary(merged);
            segment.setActions(extractActionHint(merged));
            segment.setDialogue(merged);
            segment.setShotType(defaultShotType(segments.size()));
            segment.setEmotion(defaultEmotion(segments.size()));
            segment.setCreativeHook(buildHook(summary, merged));
            segments.add(segment);
        }

        if (segments.isEmpty()) {
            SceneSegment fallback = new SceneSegment();
            fallback.setMediaId(mediaId);
            fallback.setSegmentIndex(1);
            fallback.setStartSecond(0);
            fallback.setEndSecond(8);
            fallback.setTitle("Core Segment");
            fallback.setSceneSummary(defaultText(summary));
            fallback.setActions("Keep the main action beat and visual transition.");
            fallback.setDialogue(defaultText(transcript));
            fallback.setShotType("medium shot");
            fallback.setEmotion("neutral");
            fallback.setCreativeHook("Extract the strongest reusable opening beat.");
            segments.add(fallback);
        }

        return segments;
    }

    private static List<String> extractSentences(String transcript, String summary) {
        String source = defaultText(transcript);
        if (source.isBlank()) {
            source = defaultText(summary);
        }
        if (source.isBlank()) {
            return List.of();
        }

        String normalized = source.replace("\r", "\n")
                .replaceAll("(?<=[。！？!?])", "\n")
                .replaceAll("(?<=[.])\\s+", ".\n");
        String[] parts = normalized.split("\\n+");

        List<String> sentences = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    private static String buildTitle(String seed, int index) {
        String clean = seed == null ? "" : seed.replaceAll("\\s+", " ").trim();
        if (clean.length() > 18) {
            clean = clean.substring(0, 18) + "...";
        }
        return "Scene " + index + " - " + (clean.isEmpty() ? "Story Beat" : clean);
    }

    private static String extractActionHint(String content) {
        String lowerCase = defaultText(content).toLowerCase(Locale.ROOT);
        if (containsAny(lowerCase, "run", "walk", "move", "enter", "leave")) {
            return "Use body movement and camera follow-through to drive the beat.";
        }
        if (containsAny(lowerCase, "say", "ask", "answer", "talk", "speak")) {
            return "Focus on delivery, pauses, and facial reaction changes.";
        }
        if (containsAny(lowerCase, "show", "product", "demo", "display")) {
            return "Use hand actions and close framing to highlight the focal object.";
        }
        return "Rebuild the moment around action rhythm, shot changes, and a clear focal point.";
    }

    private static String defaultShotType(int index) {
        String[] presets = {"close up", "medium shot", "wide shot", "over shoulder", "tracking shot"};
        return presets[index % presets.length];
    }

    private static String defaultEmotion(int index) {
        String[] presets = {"hook", "informative", "dramatic", "light", "persuasive"};
        return presets[index % presets.length];
    }

    private static String buildHook(String summary, String chunk) {
        String base = !defaultText(summary).isBlank() ? summary : chunk;
        if (base.length() > 48) {
            base = base.substring(0, 48) + "...";
        }
        return "Reusable hook: " + base;
    }

    private static String defaultText(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean containsAny(String source, String... values) {
        return Arrays.stream(values).anyMatch(source::contains);
    }
}
