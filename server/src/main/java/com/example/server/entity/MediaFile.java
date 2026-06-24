package com.example.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("media_files")
public class MediaFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String filename;
    private String status;
    private String filePath;
    private String fileMd5;
    private String objectKey;
    private Long fileSize;
    private String uploadStatus;
    private String aiSummary;
    private String transcriptText;
    private String coverUrl;
    private String projectTitle;
    private String sourceType;
    private String platform;
    private String analysisStatus;
    private String errorMessage;
    private Integer durationSeconds;
    private LocalDateTime uploadTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
