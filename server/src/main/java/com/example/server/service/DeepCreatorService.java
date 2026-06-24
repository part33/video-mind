package com.example.server.service;

import com.example.server.dto.RewriteDraftRequest;
import com.example.server.entity.MediaFile;
import com.example.server.entity.RewriteDraft;
import com.example.server.entity.SceneSegment;
import com.example.server.util.SegmentBuilder;
import com.example.server.utils.DeepSeekUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class DeepCreatorService {

    @Autowired
    private DeepSeekUtils deepSeekUtils;

    public List<SceneSegment> buildSegments(MediaFile project) {
        return SegmentBuilder.build(project.getId(), project.getTranscriptText(), project.getAiSummary());
    }

    public RewriteDraft buildRewriteDraft(MediaFile project, RewriteDraftRequest request) {
        RewriteDraft draft = new RewriteDraft();
        draft.setMediaId(project.getId());
        draft.setTargetPlatform(defaultValue(request.getTargetPlatform(), "multi-platform short video"));
        draft.setPersona(defaultValue(request.getPersona(), "an original AI character"));
        draft.setTone(defaultValue(request.getTone(), "punchy and conversational"));
        draft.setAudience(defaultValue(request.getAudience(), "short-form content viewers"));
        draft.setDurationSeconds(request.getDurationSeconds() == null ? 30 : request.getDurationSeconds());
        draft.setScriptMarkdown(buildScript(project, draft));
        draft.setStoryboardMarkdown(buildStoryboard(draft));
        draft.setPromptBundle(buildPromptBundle(draft));
        draft.setCreatedAt(LocalDateTime.now());
        draft.setUpdatedAt(LocalDateTime.now());
        return draft;
    }

    private String buildScript(MediaFile project, RewriteDraft draft) {
        String systemPrompt = """
                You are a short-video writer for creators.
                Rebuild the reference into a fresh script for a new AI character.

                Requirements:
                1. Output markdown.
                2. Keep the action rhythm, pacing, and story structure from the reference.
                3. Do not copy the original person's identity or appearance.
                4. Use the user's new character setup.
                5. Include: Hook, Beat-by-beat Script, Dialogue Notes, and CTA.
                6. Write the result in Simplified Chinese.
                """;

        String userPrompt = """
                Reference title: %s
                Reference platform: %s
                Reference summary:
                %s

                Transcript:
                %s

                New character setup: %s
                Target platform: %s
                Tone: %s
                Audience: %s
                Target duration: %d seconds
                """.formatted(
                defaultValue(project.getProjectTitle(), project.getFilename()),
                defaultValue(project.getPlatform(), "GENERIC"),
                defaultValue(project.getAiSummary(), "No summary yet."),
                defaultValue(project.getTranscriptText(), "No transcript yet."),
                draft.getPersona(),
                draft.getTargetPlatform(),
                draft.getTone(),
                draft.getAudience(),
                draft.getDurationSeconds()
        );

        String aiResult = deepSeekUtils.chat(systemPrompt, userPrompt);
        if (isFailureResult(aiResult)) {
            return """
                    ## Hook
                    - Use the first 3 seconds to explain why this recreated scene is worth watching.

                    ## Beat-by-Beat Script
                    1. Introduce the new AI character: %s
                    2. Reuse the reference action rhythm, but move it into a fresh scene.
                    3. Make each beat land one clear information point or emotional turn.

                    ## Dialogue Notes
                    - Keep the lines short, punchy, and easy to perform for short video.

                    ## CTA
                    - End with a comment, follow, or save prompt tied to the new character series.
                    """.formatted(draft.getPersona());
        }
        return aiResult;
    }

    private String buildStoryboard(RewriteDraft draft) {
        return """
                ## Storyboard Draft
                1. Opening shot: use a strong hook frame to establish `%s`.
                2. Middle shots: preserve the motion beats and pacing of the reference while matching the `%s` tone.
                3. Ending shot: land a clear CTA adapted for `%s`.

                ## Visual Constraints
                - Do not replicate the original person's appearance.
                - Keep the reusable action logic, camera rhythm, and information layering.
                - Maintain one consistent character setup, wardrobe direction, and scene language.
                """.formatted(draft.getPersona(), draft.getTone(), draft.getTargetPlatform());
    }

    private String buildPromptBundle(RewriteDraft draft) {
        return """
                [Character]
                %s

                [Master Video Prompt]
                Create a %d-second short video for %s. Preserve the action rhythm and narrative beats from the reference video, but replace the original person with a new character: %s. Tone: %s. Audience: %s.

                [Shot Prompts]
                - Strong opening hook
                - Clear action beats
                - Dialogue-aligned visual transitions
                - End with memorable CTA shot
                """.formatted(
                draft.getPersona(),
                draft.getDurationSeconds(),
                draft.getTargetPlatform(),
                draft.getPersona(),
                draft.getTone(),
                draft.getAudience()
        );
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isFailureResult(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String lowerCase = value.toLowerCase(Locale.ROOT);
        return lowerCase.startsWith("ai request failed")
                || lowerCase.startsWith("network error");
    }
}
