package com.example.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.server.entity.MediaFile;
import com.example.server.mapper.MediaFileMapper;
import com.example.server.service.CreatorWorkspaceService;
import com.example.server.service.MediaService;
import com.example.server.utils.MinioUtils;
import com.example.server.utils.YtDlpUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/media")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class MediaController {

    @Autowired(required = false)
    private MediaFileMapper mediaFileMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MinioUtils minioUtils;

    @Autowired
    private YtDlpUtils ytDlpUtils;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private CreatorWorkspaceService creatorWorkspaceService;

    @PostMapping("/init-upload")
    public ResponseEntity<Map<String, Object>> initUpload(@RequestParam(value = "filename", required = false) String filename,
                                                          @RequestParam(value = "totalChunks", required = false) Integer totalChunks,
                                                          @RequestParam(value = "fileSize", required = false) Long fileSize,
                                                          @RequestParam(value = "fileMd5", required = false) String fileMd5,
                                                          @RequestParam(value = "userId", required = false) Long userId) {
        return ResponseEntity.ok(mediaService.initChunkedUpload(filename, totalChunks, fileSize, fileMd5, userId));
    }

    @PostMapping("/upload-chunk")
    public ResponseEntity<Map<String, Object>> uploadChunk(@RequestParam("uploadId") String uploadId,
                                                           @RequestParam("chunkIndex") Integer chunkIndex,
                                                           @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(mediaService.saveChunk(uploadId, chunkIndex, file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chunk upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/upload-progress")
    public ResponseEntity<Map<String, Object>> uploadProgress(@RequestParam("uploadId") String uploadId) {
        return ResponseEntity.ok(mediaService.getUploadProgress(uploadId));
    }

    @PostMapping("/merge-upload")
    public ResponseEntity<?> mergeUpload(@RequestParam("uploadId") String uploadId,
                                         @RequestParam("filename") String filename,
                                         @RequestParam("totalChunks") Integer totalChunks,
                                         @RequestParam(value = "fileMd5", required = false) String fileMd5,
                                         @RequestParam(value = "userId", required = false) Long userId) {
        try {
            return ResponseEntity.ok(mediaService.mergeChunkedUpload(uploadId, filename, totalChunks, fileMd5, userId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Merge upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "userId", required = false) Long userId) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Upload failed: file is empty");
        }
        if (mediaFileMapper == null) {
            return ResponseEntity.status(500).body("Upload failed: database not ready");
        }

        try {
            String fileUrl = minioUtils.uploadFile(file);

            MediaFile mediaFile = new MediaFile();
            mediaFile.setFilename(file.getOriginalFilename());
            mediaFile.setFilePath(fileUrl);
            mediaFile.setStatus("COMPLETED");
            mediaFile.setUploadTime(LocalDateTime.now());
            if (userId != null) {
                mediaFile.setUserId(userId);
            }
            creatorWorkspaceService.markProjectSource(mediaFile, file.getOriginalFilename(), "LOCAL_UPLOAD");

            mediaFileMapper.insert(mediaFile);
            clearListCache(userId);
            return ResponseEntity.ok("Upload success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/upload-url")
    public ResponseEntity<String> uploadUrl(@RequestParam("url") String url,
                                            @RequestParam(value = "userId", required = false) Long userId) {
        File tempFile = null;
        try {
            if (url == null || url.isBlank()) {
                return ResponseEntity.badRequest().body("Upload failed: url is empty");
            }
            if (mediaFileMapper == null) {
                return ResponseEntity.status(500).body("Upload failed: database not ready");
            }

            tempFile = ytDlpUtils.downloadVideo(url);
            String fileUrl = minioUtils.uploadLocalFile(tempFile);

            MediaFile mediaFile = new MediaFile();
            mediaFile.setFilename("WEB_" + tempFile.getName());
            mediaFile.setFilePath(fileUrl);
            mediaFile.setStatus("COMPLETED");
            mediaFile.setUploadTime(LocalDateTime.now());
            if (userId != null) {
                mediaFile.setUserId(userId);
            }
            creatorWorkspaceService.markProjectSource(mediaFile, url, "REMOTE_URL");

            mediaFileMapper.insert(mediaFile);
            clearListCache(userId);
            return ResponseEntity.ok("Upload success");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @GetMapping("/list")
    public List<MediaFile> getList(@RequestParam(value = "userId", required = false) Long userId) {
        String cacheKey = "media:list:user:" + (userId == null ? "anon" : userId);

        if (redisTemplate != null) {
            try {
                String json = redisTemplate.opsForValue().get(cacheKey);
                if (json != null) {
                    return objectMapper.readValue(json, new TypeReference<List<MediaFile>>() {});
                }
            } catch (Exception e) {
                System.err.println("Redis read failed: " + e.getMessage());
            }
        }

        if (userId == null || mediaFileMapper == null) {
            return List.of();
        }

        QueryWrapper<MediaFile> query = new QueryWrapper<>();
        query.eq("user_id", userId).orderByDesc("id");
        List<MediaFile> list = mediaFileMapper.selectList(query);

        if (redisTemplate != null) {
            try {
                String jsonToWrite = objectMapper.writeValueAsString(list);
                redisTemplate.opsForValue().set(cacheKey, jsonToWrite, 30, TimeUnit.MINUTES);
            } catch (Exception e) {
                System.err.println("Redis write failed: " + e.getMessage());
            }
        }

        return list;
    }

    @DeleteMapping("/delete")
    public String delete(@RequestParam("id") Long id,
                         @RequestParam(value = "userId", required = false) Long userId) {
        if (mediaFileMapper == null) {
            return "Database not ready";
        }

        MediaFile media = mediaFileMapper.selectById(id);
        if (media == null) {
            return "File not found";
        }
        if (userId != null && media.getUserId() != null && !media.getUserId().equals(userId)) {
            return "No permission";
        }

        if (media.getFilePath() != null && !hasSharedStorageReference(media)) {
            minioUtils.removeFile(media.getFilePath());
        }

        mediaFileMapper.deleteById(id);
        clearListCache(media.getUserId());
        return "Delete success";
    }

    private boolean hasSharedStorageReference(MediaFile media) {
        QueryWrapper<MediaFile> query = new QueryWrapper<>();
        query.ne("id", media.getId());

        if (media.getFileMd5() != null && !media.getFileMd5().isBlank()) {
            query.eq("file_md5", media.getFileMd5());
        } else {
            query.eq("file_path", media.getFilePath());
        }

        return mediaFileMapper.selectCount(query) > 0;
    }

    private void clearListCache(Long userId) {
        if (redisTemplate == null) {
            return;
        }
        String cacheKey = "media:list:user:" + (userId == null ? "anon" : userId);
        redisTemplate.delete(cacheKey);
    }
}
