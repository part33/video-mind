package com.example.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rewrite_drafts")
public class RewriteDraft {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long mediaId;
    private String targetPlatform;
    private String persona;
    private String tone;
    private String audience;
    private Integer durationSeconds;
    private String scriptMarkdown;
    private String storyboardMarkdown;
    private String promptBundle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
