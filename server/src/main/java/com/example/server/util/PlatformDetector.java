package com.example.server.util;

public final class PlatformDetector {

    private PlatformDetector() {
    }

    public static String detect(String source) {
        if (source == null || source.isBlank()) {
            return "GENERIC";
        }
        String value = source.toLowerCase();
        if (value.contains("douyin") || value.contains("tiktok")) {
            return "DOUYIN_TIKTOK";
        }
        if (value.contains("xiaohongshu") || value.contains("xhs")) {
            return "XIAOHONGSHU";
        }
        if (value.contains("bilibili") || value.contains("b23.tv")) {
            return "BILIBILI";
        }
        if (value.contains("youtube") || value.contains("youtu.be")) {
            return "YOUTUBE";
        }
        if (value.contains("instagram")) {
            return "INSTAGRAM";
        }
        return "GENERIC";
    }
}
