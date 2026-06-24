package com.example.server.service;

import com.example.server.constant.AnalysisStatus;
import com.example.server.entity.MediaFile;
import com.example.server.mapper.MediaFileMapper;
import com.example.server.strategy.AiAnalysisStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AiService {

    @Autowired
    private MediaFileMapper mediaFileMapper;

    @Autowired
    @Qualifier("defaultAiStrategy")
    private AiAnalysisStrategy aiAnalysisStrategy;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CreatorWorkspaceService creatorWorkspaceService;

    public void asyncAnalyze(Long mediaId) {
        MediaFile mediaFile = mediaFileMapper.selectById(mediaId);
        if (mediaFile == null) {
            return;
        }

        try {
            updateAnalysisStatus(mediaFile, AnalysisStatus.TRANSCRIBING, null);

            String transcript = aiAnalysisStrategy.transcribe(mediaFile.getFilePath());
            if (isFailureResult(transcript)) {
                throw new IllegalStateException(transcript);
            }

            mediaFile.setTranscriptText(transcript);
            mediaFile.setUpdatedAt(LocalDateTime.now());
            mediaFileMapper.updateById(mediaFile);

            updateAnalysisStatus(mediaFile, AnalysisStatus.ANALYZING, null);

            String summary = aiAnalysisStrategy.generateSummaryFromTranscript(transcript);
            if (isFailureResult(summary)) {
                throw new IllegalStateException(summary);
            }

            mediaFile.setAiSummary(summary);
            mediaFile.setErrorMessage(null);
            mediaFile.setUpdatedAt(LocalDateTime.now());
            mediaFileMapper.updateById(mediaFile);

            creatorWorkspaceService.refreshSegments(mediaFile);
            clearListCache(mediaFile);
        } catch (Exception e) {
            mediaFile.setAnalysisStatus(AnalysisStatus.FAILED);
            mediaFile.setErrorMessage(e.getMessage());
            mediaFile.setAiSummary("Analysis failed: " + e.getMessage());
            mediaFile.setUpdatedAt(LocalDateTime.now());
            mediaFileMapper.updateById(mediaFile);
            clearListCache(mediaFile);
        }
    }

    @Async("aiTaskExecutor")
    public void asyncTranscribe(Long mediaId) {
        MediaFile mediaFile = mediaFileMapper.selectById(mediaId);
        if (mediaFile == null) {
            return;
        }

        try {
            updateAnalysisStatus(mediaFile, AnalysisStatus.TRANSCRIBING, null);

            String transcript = aiAnalysisStrategy.transcribe(mediaFile.getFilePath());
            if (isFailureResult(transcript)) {
                throw new IllegalStateException(transcript);
            }

            mediaFile.setTranscriptText(transcript);
            mediaFile.setUpdatedAt(LocalDateTime.now());
            mediaFileMapper.updateById(mediaFile);
            clearListCache(mediaFile);
        } catch (Exception e) {
            mediaFile.setAnalysisStatus(AnalysisStatus.FAILED);
            mediaFile.setErrorMessage(e.getMessage());
            mediaFile.setUpdatedAt(LocalDateTime.now());
            mediaFileMapper.updateById(mediaFile);
            clearListCache(mediaFile);
        }
    }

    private void updateAnalysisStatus(MediaFile mediaFile, String status, String errorMessage) {
        mediaFile.setAnalysisStatus(status);
        mediaFile.setErrorMessage(errorMessage);
        mediaFile.setUpdatedAt(LocalDateTime.now());
        mediaFileMapper.updateById(mediaFile);
    }

    private void clearListCache(MediaFile mediaFile) {
        if (redisTemplate == null) {
            return;
        }
        String userIdStr = mediaFile.getUserId() == null ? "anon" : String.valueOf(mediaFile.getUserId());
        redisTemplate.delete("media:list:user:" + userIdStr);
    }

    private boolean isFailureResult(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String lowerCase = value.toLowerCase(Locale.ROOT);
        return lowerCase.startsWith("ai request failed")
                || lowerCase.startsWith("network error")
                || lowerCase.startsWith("path is empty")
                || lowerCase.startsWith("local file not found")
                || lowerCase.startsWith("ffmpeg conversion failed")
                || lowerCase.startsWith("processing error");
    }
}
