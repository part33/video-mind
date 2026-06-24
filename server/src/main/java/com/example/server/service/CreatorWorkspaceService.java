package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.server.constant.AnalysisStatus;
import com.example.server.dto.ProjectDetailResponse;
import com.example.server.dto.RewriteDraftRequest;
import com.example.server.entity.MediaFile;
import com.example.server.entity.RewriteDraft;
import com.example.server.entity.SceneSegment;
import com.example.server.mapper.MediaFileMapper;
import com.example.server.mapper.RewriteDraftMapper;
import com.example.server.mapper.SceneSegmentMapper;
import com.example.server.util.PlatformDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CreatorWorkspaceService {

    @Autowired
    private MediaFileMapper mediaFileMapper;

    @Autowired
    private SceneSegmentMapper sceneSegmentMapper;

    @Autowired
    private RewriteDraftMapper rewriteDraftMapper;

    @Autowired
    private DeepCreatorService deepCreatorService;

    public List<MediaFile> listProjects(Long userId) {
        QueryWrapper<MediaFile> query = new QueryWrapper<>();
        query.eq("user_id", userId).orderByDesc("updated_at").orderByDesc("id");
        return mediaFileMapper.selectList(query);
    }

    public ProjectDetailResponse getProjectDetail(Long projectId, Long userId) {
        MediaFile project = getOwnedProject(projectId, userId);
        ensureSegments(project);

        QueryWrapper<SceneSegment> sceneQuery = new QueryWrapper<>();
        sceneQuery.eq("media_id", projectId).orderByAsc("segment_index");

        QueryWrapper<RewriteDraft> draftQuery = new QueryWrapper<>();
        draftQuery.eq("media_id", projectId).orderByDesc("id");

        ProjectDetailResponse response = new ProjectDetailResponse();
        response.setProject(project);
        response.setSegments(sceneSegmentMapper.selectList(sceneQuery));
        response.setRewriteDrafts(rewriteDraftMapper.selectList(draftQuery));
        return response;
    }

    public RewriteDraft createRewriteDraft(Long projectId, Long userId, RewriteDraftRequest request) {
        MediaFile project = getOwnedProject(projectId, userId);
        ensureSegments(project);

        RewriteDraft draft = deepCreatorService.buildRewriteDraft(project, request);
        rewriteDraftMapper.insert(draft);

        project.setAnalysisStatus(AnalysisStatus.REWRITTEN);
        project.setUpdatedAt(LocalDateTime.now());
        mediaFileMapper.updateById(project);
        return draft;
    }

    public void ensureSegments(MediaFile project) {
        if (!hasAnalysisMaterial(project)) {
            return;
        }

        QueryWrapper<SceneSegment> query = new QueryWrapper<>();
        query.eq("media_id", project.getId());
        if (sceneSegmentMapper.selectCount(query) > 0) {
            return;
        }

        saveSegments(project, deepCreatorService.buildSegments(project));
        if (!AnalysisStatus.REWRITTEN.equals(project.getAnalysisStatus())) {
            project.setAnalysisStatus(AnalysisStatus.SEGMENTED);
            project.setUpdatedAt(LocalDateTime.now());
            mediaFileMapper.updateById(project);
        }
    }

    public void refreshSegments(MediaFile project) {
        QueryWrapper<SceneSegment> query = new QueryWrapper<>();
        query.eq("media_id", project.getId());
        sceneSegmentMapper.delete(query);

        if (!hasAnalysisMaterial(project)) {
            return;
        }

        saveSegments(project, deepCreatorService.buildSegments(project));
        if (!AnalysisStatus.REWRITTEN.equals(project.getAnalysisStatus())) {
            project.setAnalysisStatus(AnalysisStatus.SEGMENTED);
        }
        project.setUpdatedAt(LocalDateTime.now());
        mediaFileMapper.updateById(project);
    }

    public void markProjectSource(MediaFile project, String sourceValue, String sourceType) {
        LocalDateTime now = LocalDateTime.now();
        project.setProjectTitle(defaultProjectTitle(project.getFilename()));
        project.setSourceType(defaultValue(sourceType, "LOCAL_UPLOAD"));
        project.setPlatform(PlatformDetector.detect(sourceValue));
        project.setAnalysisStatus(AnalysisStatus.PENDING);
        project.setErrorMessage(null);
        project.setDurationSeconds(0);
        if (project.getCreatedAt() == null) {
            project.setCreatedAt(now);
        }
        project.setUpdatedAt(now);
    }

    private MediaFile getOwnedProject(Long projectId, Long userId) {
        MediaFile project = mediaFileMapper.selectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("Project not found");
        }
        if (userId != null && project.getUserId() != null && !project.getUserId().equals(userId)) {
            throw new IllegalArgumentException("No permission to access this project");
        }
        return project;
    }

    private void saveSegments(MediaFile project, List<SceneSegment> segments) {
        LocalDateTime now = LocalDateTime.now();
        for (SceneSegment segment : segments) {
            if (segment.getMediaId() == null) {
                segment.setMediaId(project.getId());
            }
            segment.setCreatedAt(now);
            segment.setUpdatedAt(now);
            sceneSegmentMapper.insert(segment);
        }
    }

    private boolean hasAnalysisMaterial(MediaFile project) {
        return hasText(project.getTranscriptText()) || hasText(project.getAiSummary());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String defaultProjectTitle(String filename) {
        if (filename == null || filename.isBlank()) {
            return "Untitled Project";
        }
        return filename.replaceFirst("\\.[^.]+$", "");
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
