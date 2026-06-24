package com.example.server.strategy.impl;

import com.example.server.strategy.AiAnalysisStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;

@Component("defaultAiStrategy")
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "demo")
public class DemoAiStrategy implements AiAnalysisStrategy {

    @Override
    public String transcribe(String videoPath) {
        String fileName = videoPath == null ? "reference-video" : new File(videoPath).getName();
        return """
                This is a demo transcript generated locally for %s.
                The reference video opens with a fast hook, introduces one clear character setup, then moves through three short visual beats before ending with a CTA.
                Reusable elements include action rhythm, camera progression, and speaking cadence.
                """.formatted(fileName);
    }

    @Override
    public String generateSummary(String videoPath) {
        return generateSummaryFromTranscript(transcribe(videoPath));
    }

    @Override
    public String generateSummaryFromTranscript(String transcript) {
        return """
                ## Core Summary
                This reference uses a short-form creator structure with a direct opening hook, compact action beats, and a closing CTA.

                ## Key Insights
                ### 1. Hook first
                The content establishes the premise quickly and earns attention in the first seconds.
                ### 2. Beat-driven pacing
                The middle section advances through compact visual and spoken beats rather than long exposition.
                ### 3. Easy to rewrite
                The original person can be replaced while preserving rhythm, staging, and narrative structure.

                ## Reusable Moments
                - Fast opening statement
                - Three short progression beats
                - Clear CTA ending

                ## Tags
                #shortvideo #creator #rewrite
                """;
    }
}
