package com.example.server.controller;

import com.example.server.dto.AnalysisTaskMsg;
import com.example.server.entity.MediaFile;
import com.example.server.mapper.MediaFileMapper;
import com.example.server.service.AiService;
import com.example.server.strategy.AiAnalysisStrategy;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/debug")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "false", matchIfMissing = true)
public class DebugController {

    @Autowired
    private MediaFileMapper mediaFileMapper;

    @Autowired
    @Qualifier("defaultAiStrategy")
    private AiAnalysisStrategy aiAnalysisStrategy;

    @Autowired
    private AiService aiService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @GetMapping("/ai")
    public String aiAnalyze(@RequestParam Long id) {
        String lockKey = "lock:analyze:" + id;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(0, -1, TimeUnit.SECONDS)) {
                return "Task is already being submitted";
            }

            RRateLimiter rateLimiter = redissonClient.getRateLimiter("limit:ai:global");
            rateLimiter.trySetRate(RateType.OVERALL, 10, 1, RateIntervalUnit.MINUTES);
            if (!rateLimiter.tryAcquire(1)) {
                return "System busy, try again later";
            }

            MediaFile file = mediaFileMapper.selectById(id);
            if (file == null) {
                return "File not found";
            }
            if (file.getAiSummary() != null && file.getAiSummary().contains("running")) {
                return "Task is already running";
            }

            file.setAiSummary("[MQ] Analysis task queued.");
            mediaFileMapper.updateById(file);
            String userIdKey = file.getUserId() == null ? "anon" : String.valueOf(file.getUserId());
            redisTemplate.delete("media:list:user:" + userIdKey);

            rocketMQTemplate.convertAndSend("video-analysis-topic", new AnalysisTaskMsg(id, "START_ANALYSIS"));
            return "Task sent to RocketMQ";
        } catch (Exception e) {
            return "Submit failed: " + e.getMessage();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @GetMapping("/transcribe")
    public String transcribe(@RequestParam Long id) {
        MediaFile mediaFile = mediaFileMapper.selectById(id);
        if (mediaFile == null) {
            return "File not found";
        }

        aiService.asyncTranscribe(id);
        return "Transcribe task started";
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam Long id) throws IOException {
        MediaFile mediaFile = mediaFileMapper.selectById(id);
        if (mediaFile == null) {
            return ResponseEntity.notFound().build();
        }

        String inputPath = mediaFile.getFilePath();
        if (!inputPath.startsWith("http") && !new File(inputPath).exists()) {
            return ResponseEntity.notFound().build();
        }

        String outputMp3Path = System.getProperty("java.io.tmpdir") + File.separator + "download_" + UUID.randomUUID() + ".mp3";
        boolean success = runFfmpeg(inputPath, outputMp3Path);
        if (!success) {
            return ResponseEntity.internalServerError().build();
        }

        File mp3File = new File(outputMp3Path);
        Resource resource = new FileSystemResource(mp3File);

        String fileName = "audio.mp3";
        if (mediaFile.getFilename() != null) {
            fileName = mediaFile.getFilename().replaceAll("\\.[^.]+$", "") + ".mp3";
        }
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(resource);
    }

    private boolean runFfmpeg(String inputPath, String outputPath) {
        try {
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-y");
            command.add("-i");
            command.add(inputPath);
            command.add("-vn");
            command.add("-acodec");
            command.add("libmp3lame");
            command.add("-q:a");
            command.add("2");
            command.add(outputPath);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            Process process = pb.start();
            return process.waitFor(15, TimeUnit.MINUTES) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
