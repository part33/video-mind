package com.example.server.strategy;

public interface AiAnalysisStrategy {

    String transcribe(String videoPath);

    String generateSummary(String videoPath);

    String generateSummaryFromTranscript(String transcript);
}
