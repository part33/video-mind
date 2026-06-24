package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.server.constant.AnalysisStatus;
import com.example.server.dto.AnalysisTaskMsg;
import com.example.server.entity.MediaFile;
import com.example.server.mapper.MediaFileMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
public class AnalysisTaskOrchestrator {

    @Autowired
    private MediaFileMapper mediaFileMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    @Autowired
    private AiService aiService;

    @Autowired
    private CreatorWorkspaceService creatorWorkspaceService;

    @Autowired
    @Qualifier("aiTaskExecutor")
    private Executor aiTaskExecutor;

    @Value("${app.analysis.mode:mq}")
    private String analysisMode;

    public String triggerAnalysis(Long id, Long userId) {
        if (isLocalMode() || redissonClient == null || rocketMQTemplate == null) {
            return triggerLocalAnalysis(id, userId);
        }

        MediaFile initialFile = mediaFileMapper.selectById(id);
        String lockKey = "lock:analyze:" + analysisLockToken(initialFile, id);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(0, -1, TimeUnit.SECONDS)) {
                return "Analysis already being submitted";
            }

            RRateLimiter rateLimiter = redissonClient.getRateLimiter("limit:ai:global");
            rateLimiter.trySetRate(RateType.OVERALL, 10, 1, RateIntervalUnit.MINUTES);
            if (!rateLimiter.tryAcquire(1)) {
                return "System busy, try again later";
            }

            MediaFile file = validateProject(id, userId);
            if (reuseFinishedAnalysis(file)) {
                clearListCache(file);
                return "Analysis result reused by file fingerprint";
            }
            file.setAnalysisStatus(AnalysisStatus.TRANSCRIBING);
            file.setErrorMessage(null);
            file.setAiSummary("Analysis task queued.");
            file.setUpdatedAt(LocalDateTime.now());
            mediaFileMapper.updateById(file);

            clearListCache(file);
            rocketMQTemplate.convertAndSend("video-analysis-topic", new AnalysisTaskMsg(id, "START_ANALYSIS"));
            return "Analysis task submitted";
        } catch (Exception e) {
            return "Submit failed: " + e.getMessage();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String triggerLocalAnalysis(Long id, Long userId) {
        try {
            MediaFile file = validateProject(id, userId);
            if (reuseFinishedAnalysis(file)) {
                clearListCache(file);
                return "Analysis result reused by file fingerprint";
            }
            if (AnalysisStatus.TRANSCRIBING.equals(file.getAnalysisStatus())
                    || AnalysisStatus.ANALYZING.equals(file.getAnalysisStatus())) {
                return "Analysis is already running";
            }

            file.setAnalysisStatus(AnalysisStatus.TRANSCRIBING);
            file.setErrorMessage(null);
            file.setAiSummary("Local demo analysis started.");
            file.setUpdatedAt(LocalDateTime.now());
            mediaFileMapper.updateById(file);
            clearListCache(file);

            CompletableFuture.runAsync(() -> {
                try {
                    aiService.asyncAnalyze(id);
                } catch (Exception e) {
                    markAsFailed(id, e.getMessage());
                }
            }, aiTaskExecutor);
            return "Analysis started in local demo mode";
        } catch (Exception e) {
            return "Submit failed: " + e.getMessage();
        }
    }

    private MediaFile validateProject(Long id, Long userId) {
        MediaFile file = mediaFileMapper.selectById(id);
        if (file == null) {
            throw new IllegalArgumentException("Project not found");
        }
        if (userId != null && file.getUserId() != null && !userId.equals(file.getUserId())) {
            throw new IllegalArgumentException("No permission to analyze this project");
        }
        return file;
    }

    private void markAsFailed(Long id, String error) {
        MediaFile file = mediaFileMapper.selectById(id);
        if (file == null) {
            return;
        }
        file.setAnalysisStatus(AnalysisStatus.FAILED);
        file.setErrorMessage(error);
        file.setAiSummary("Analysis failed: " + error);
        file.setUpdatedAt(LocalDateTime.now());
        mediaFileMapper.updateById(file);
        clearListCache(file);
    }

    private void clearListCache(MediaFile file) {
        if (redisTemplate == null) {
            return;
        }
        String userIdKey = file.getUserId() == null ? "anon" : String.valueOf(file.getUserId());
        redisTemplate.delete("media:list:user:" + userIdKey);
    }

    private boolean isLocalMode() {
        return "local".equalsIgnoreCase(analysisMode);
    }

    private String analysisLockToken(MediaFile file, Long id) {
        if (file != null && file.getFileMd5() != null && !file.getFileMd5().isBlank()) {
            return "md5:" + file.getFileMd5();
        }
        return "id:" + id;
    }

    private boolean reuseFinishedAnalysis(MediaFile target) {
        if (target.getFileMd5() == null || target.getFileMd5().isBlank()) {
            return false;
        }
        QueryWrapper<MediaFile> query = new QueryWrapper<>();
        query.eq("file_md5", target.getFileMd5())
                .ne("id", target.getId())
                .isNotNull("transcript_text")
                .isNotNull("ai_summary")
                .orderByDesc("updated_at")
                .last("LIMIT 1");
        MediaFile source = mediaFileMapper.selectOne(query);
        if (source == null) {
            return false;
        }

        target.setTranscriptText(source.getTranscriptText());
        target.setAiSummary(source.getAiSummary());
        target.setAnalysisStatus(source.getAnalysisStatus() == null ? AnalysisStatus.SEGMENTED : source.getAnalysisStatus());
        target.setErrorMessage(null);
        target.setUpdatedAt(LocalDateTime.now());
        mediaFileMapper.updateById(target);
        creatorWorkspaceService.refreshSegments(target);
        return true;
    }
}
