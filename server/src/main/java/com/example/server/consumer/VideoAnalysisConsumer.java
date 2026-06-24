package com.example.server.consumer;

import com.example.server.constant.AnalysisStatus;
import com.example.server.dto.AnalysisTaskMsg;
import com.example.server.entity.MediaFile;
import com.example.server.mapper.MediaFileMapper;
import com.example.server.service.AiService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
@ConditionalOnProperty(name = "app.analysis.mode", havingValue = "mq", matchIfMissing = true)
@RocketMQMessageListener(topic = "video-analysis-topic", consumerGroup = "video-group")
public class VideoAnalysisConsumer implements RocketMQListener<AnalysisTaskMsg> {

    @Autowired
    private AiService aiService;

    @Autowired
    private MediaFileMapper mediaFileMapper;

    @Autowired
    private Executor aiTaskExecutor;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    public void onMessage(AnalysisTaskMsg msg) {
        Long mediaId = msg.getMediaId();
        CompletableFuture.runAsync(() -> {
            try {
                aiService.asyncAnalyze(mediaId);
            } catch (Exception e) {
                markAsFailed(mediaId, e.getMessage());
            }
        }, aiTaskExecutor);
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

        if (redisTemplate != null) {
            String userIdKey = file.getUserId() == null ? "anon" : String.valueOf(file.getUserId());
            redisTemplate.delete("media:list:user:" + userIdKey);
        }
    }
}
