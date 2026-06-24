package com.example.server.dto;

import com.example.server.entity.MediaFile;
import com.example.server.entity.RewriteDraft;
import com.example.server.entity.SceneSegment;
import lombok.Data;

import java.util.List;

@Data
public class ProjectDetailResponse {
    private MediaFile project;
    private List<SceneSegment> segments;
    private List<RewriteDraft> rewriteDrafts;
}
