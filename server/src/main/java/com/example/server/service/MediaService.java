package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.server.constant.AnalysisStatus;
import com.example.server.entity.MediaFile;
import com.example.server.mapper.MediaFileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class MediaService {

    private static final String CHUNK_UPLOAD_KEY_PREFIX = "upload:chunked:";

    @Autowired
    private MediaFileMapper mediaFileMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CreatorWorkspaceService creatorWorkspaceService;

    @Autowired
    private com.example.server.utils.MinioUtils minioUtils;

    @Value("${app.storage.local-root:./demo-storage}")
    private String localStorageRoot;

    public Map<String, Object> initChunkedUpload(String filename, Integer totalChunks, Long fileSize, String fileMd5, Long userId) {
        String uploadId = UUID.randomUUID().toString();
        Map<String, Object> response = new HashMap<>();
        response.put("uploadId", uploadId);
        response.put("uploadedChunks", List.of());
        response.put("completed", false);

        response.put("duplicateCandidate", findReusableMedia(fileMd5) != null);

        if (redisTemplate != null) {
            String redisKey = CHUNK_UPLOAD_KEY_PREFIX + uploadId;
            Map<String, String> session = new HashMap<>();
            session.put("filename", defaultValue(filename, "upload.mp4"));
            session.put("totalChunks", String.valueOf(totalChunks == null ? 0 : totalChunks));
            session.put("fileSize", String.valueOf(fileSize == null ? 0 : fileSize));
            session.put("fileMd5", defaultValue(fileMd5, ""));
            session.put("userId", userId == null ? "" : String.valueOf(userId));
            session.put("createdAt", String.valueOf(System.currentTimeMillis()));
            redisTemplate.opsForHash().putAll(redisKey, session);
            redisTemplate.expire(redisKey, 1, TimeUnit.DAYS);
        }
        return response;
    }

    public Map<String, Object> saveChunk(String uploadId, Integer chunkIndex, MultipartFile file) throws IOException {
        if (uploadId == null || uploadId.isBlank()) {
            throw new IllegalArgumentException("uploadId is required");
        }
        if (chunkIndex == null || chunkIndex < 0) {
            throw new IllegalArgumentException("chunkIndex is invalid");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("chunk file is empty");
        }

        Path uploadDir = ensureChunkDir(uploadId);
        Path chunkFile = uploadDir.resolve(chunkIndex + ".part");
        file.transferTo(chunkFile);

        if (redisTemplate != null) {
            String redisKey = CHUNK_UPLOAD_KEY_PREFIX + uploadId;
            redisTemplate.opsForHash().put(redisKey, "chunk:" + chunkIndex, "1");
            redisTemplate.expire(redisKey, 1, TimeUnit.DAYS);
        }

        return getUploadProgress(uploadId);
    }

    public Map<String, Object> getUploadProgress(String uploadId) {
        List<Integer> uploadedChunks = listUploadedChunks(uploadId);
        Map<String, Object> response = new HashMap<>();
        response.put("uploadId", uploadId);
        response.put("uploadedChunks", uploadedChunks);
        response.put("uploadedCount", uploadedChunks.size());
        return response;
    }

    public MediaFile mergeChunkedUpload(String uploadId, String filename, Integer totalChunks, String fileMd5, Long userId) throws Exception {
        if (uploadId == null || uploadId.isBlank()) {
            throw new IllegalArgumentException("uploadId is required");
        }
        if (totalChunks == null || totalChunks <= 0) {
            totalChunks = readTotalChunks(uploadId);
        }
        if (totalChunks == null || totalChunks <= 0) {
            throw new IllegalArgumentException("totalChunks is required");
        }

        String expectedMd5 = defaultValue(fileMd5, readSessionValue(uploadId, "fileMd5"));
        MediaFile duplicate = findReusableMedia(expectedMd5);
        if (duplicate != null) {
            return createDuplicateProject(duplicate, filename, userId, expectedMd5);
        }

        Path uploadDir = ensureChunkDir(uploadId);
        Path mergedFile = ensureUploadDir().toPath().resolve(uploadId + "_" + sanitizeFilename(defaultValue(filename, "upload.mp4")));
        try (OutputStream outputStream = Files.newOutputStream(mergedFile)) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunkFile = uploadDir.resolve(i + ".part");
                if (!Files.exists(chunkFile)) {
                    throw new IllegalStateException("Missing chunk: " + i);
                }
                Files.copy(chunkFile, outputStream);
            }
        }

        String actualMd5 = calculateMd5(mergedFile);
        if (expectedMd5 != null && !expectedMd5.isBlank() && !expectedMd5.equalsIgnoreCase(actualMd5)) {
            Files.deleteIfExists(mergedFile);
            throw new IllegalStateException("File checksum mismatch");
        }

        MediaFile reusable = findReusableMedia(actualMd5);
        if (reusable != null) {
            Files.deleteIfExists(mergedFile);
            deleteChunkDir(uploadDir);
            clearUploadSession(uploadId);
            return createDuplicateProject(reusable, filename, userId, actualMd5);
        }

        String storedPath = minioUtils.uploadLocalFile(mergedFile.toFile());
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFilename(defaultValue(filename, mergedFile.getFileName().toString()));
        mediaFile.setFilePath(storedPath);
        mediaFile.setFileMd5(actualMd5);
        mediaFile.setObjectKey(Path.of(storedPath).getFileName().toString());
        mediaFile.setFileSize(Files.size(mergedFile));
        mediaFile.setUploadStatus("COMPLETED");
        mediaFile.setStatus("COMPLETED");
        mediaFile.setUploadTime(LocalDateTime.now());
        if (userId != null) {
            mediaFile.setUserId(userId);
        }
        creatorWorkspaceService.markProjectSource(mediaFile, mediaFile.getFilename(), "LOCAL_UPLOAD");
        mediaFileMapper.insert(mediaFile);

        Files.deleteIfExists(mergedFile);
        deleteChunkDir(uploadDir);
        clearUploadSession(uploadId);
        return mediaFile;
    }

    public String convertVideoToAudio(MultipartFile file) throws IOException, InterruptedException {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFilename(file.getOriginalFilename());
        mediaFile.setStatus("PROCESSING");
        mediaFile.setUploadTime(LocalDateTime.now());
        mediaFile.setFilePath("");
        mediaFileMapper.insert(mediaFile);

        File uploadDir = ensureUploadDir();
        String fileId = UUID.randomUUID().toString();
        File inputFile = new File(uploadDir, fileId + "_input.mp4");
        File outputFile = new File(uploadDir, fileId + "_output.mp3");
        file.transferTo(inputFile);

        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(inputFile.getAbsolutePath());
        command.add("-vn");
        command.add("-acodec");
        command.add("libmp3lame");
        command.add("-q:a");
        command.add("2");
        command.add(outputFile.getAbsolutePath());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        if (process.waitFor() == 0) {
            inputFile.delete();
            mediaFile.setStatus("COMPLETED");
            mediaFile.setFilePath(outputFile.getAbsolutePath());
            mediaFileMapper.updateById(mediaFile);
            return outputFile.getAbsolutePath();
        }

        mediaFile.setStatus("FAILED");
        mediaFileMapper.updateById(mediaFile);
        throw new RuntimeException("FFmpeg conversion failed");
    }

    private File ensureUploadDir() {
        File uploadDir = new File(localStorageRoot, "uploads");
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new IllegalStateException("Failed to create upload directory: " + uploadDir.getAbsolutePath());
        }
        return uploadDir;
    }

    private Path ensureChunkDir(String uploadId) throws IOException {
        Path chunkDir = Path.of(localStorageRoot, "chunks", sanitizeFilename(uploadId)).toAbsolutePath().normalize();
        Files.createDirectories(chunkDir);
        return chunkDir;
    }

    private List<Integer> listUploadedChunks(String uploadId) {
        if (redisTemplate != null) {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(CHUNK_UPLOAD_KEY_PREFIX + uploadId);
            return entries.keySet().stream()
                    .map(String::valueOf)
                    .filter((key) -> key.startsWith("chunk:"))
                    .map((key) -> Integer.parseInt(key.substring("chunk:".length())))
                    .sorted()
                    .toList();
        }

        Path uploadDir = Path.of(localStorageRoot, "chunks", sanitizeFilename(uploadId));
        if (!Files.exists(uploadDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(uploadDir)) {
            return files
                    .map((path) -> path.getFileName().toString())
                    .filter((name) -> name.endsWith(".part"))
                    .map((name) -> Integer.parseInt(name.replace(".part", "")))
                    .sorted()
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private Integer readTotalChunks(String uploadId) {
        String value = readSessionValue(uploadId, "totalChunks");
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private String readSessionValue(String uploadId, String key) {
        if (redisTemplate == null) {
            return null;
        }
        Object value = redisTemplate.opsForHash().get(CHUNK_UPLOAD_KEY_PREFIX + uploadId, key);
        return value == null ? null : String.valueOf(value);
    }

    private void clearUploadSession(String uploadId) {
        if (redisTemplate != null) {
            redisTemplate.delete(CHUNK_UPLOAD_KEY_PREFIX + uploadId);
        }
    }

    private MediaFile findReusableMedia(String fileMd5) {
        if (fileMd5 == null || fileMd5.isBlank()) {
            return null;
        }
        QueryWrapper<MediaFile> query = new QueryWrapper<>();
        query.eq("file_md5", fileMd5)
                .eq("upload_status", "COMPLETED")
                .isNotNull("file_path")
                .orderByDesc("id")
                .last("LIMIT 1");
        return mediaFileMapper.selectOne(query);
    }

    private MediaFile createDuplicateProject(MediaFile source, String filename, Long userId, String fileMd5) {
        MediaFile copy = new MediaFile();
        copy.setUserId(userId);
        copy.setFilename(defaultValue(filename, source.getFilename()));
        copy.setStatus("COMPLETED");
        copy.setFilePath(source.getFilePath());
        copy.setFileMd5(defaultValue(fileMd5, source.getFileMd5()));
        copy.setObjectKey(source.getObjectKey());
        copy.setFileSize(source.getFileSize());
        copy.setUploadStatus("COMPLETED");
        copy.setAiSummary(source.getAiSummary());
        copy.setTranscriptText(source.getTranscriptText());
        copy.setCoverUrl(source.getCoverUrl());
        copy.setAnalysisStatus(defaultValue(source.getAnalysisStatus(), AnalysisStatus.PENDING));
        copy.setErrorMessage(null);
        copy.setDurationSeconds(source.getDurationSeconds());
        copy.setUploadTime(LocalDateTime.now());
        creatorWorkspaceService.markProjectSource(copy, copy.getFilename(), "LOCAL_UPLOAD");
        if (source.getTranscriptText() != null || source.getAiSummary() != null) {
            copy.setAnalysisStatus(defaultValue(source.getAnalysisStatus(), AnalysisStatus.SEGMENTED));
        }
        mediaFileMapper.insert(copy);
        creatorWorkspaceService.ensureSegments(copy);
        return copy;
    }

    private String calculateMd5(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (InputStream inputStream = new FileInputStream(file.toFile())) {
                byte[] buffer = new byte[1024 * 1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder builder = new StringBuilder();
            for (byte item : digest.digest()) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm is not available", e);
        }
    }

    private void deleteChunkDir(Path uploadDir) {
        if (!Files.exists(uploadDir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(uploadDir)) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception e) {
            System.err.println("Chunk cleanup failed: " + e.getMessage());
        }
    }

    private String sanitizeFilename(String value) {
        return defaultValue(value, "upload").replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
