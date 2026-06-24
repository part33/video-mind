package com.example.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scene_segments")
public class SceneSegment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long mediaId;
    private Integer segmentIndex;
    private Integer startSecond;
    private Integer endSecond;
    private String title;
    private String sceneSummary;
    private String actions;
    private String dialogue;
    private String shotType;
    private String emotion;
    private String creativeHook;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
