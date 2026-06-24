package com.example.server.controller;

import com.example.server.dto.ProjectDetailResponse;
import com.example.server.dto.RewriteDraftRequest;
import com.example.server.entity.MediaFile;
import com.example.server.entity.RewriteDraft;
import com.example.server.service.AnalysisTaskOrchestrator;
import com.example.server.service.CreatorWorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/projects")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ProjectController {

    @Autowired
    private CreatorWorkspaceService creatorWorkspaceService;

    @Autowired
    private AnalysisTaskOrchestrator analysisTaskOrchestrator;

    @GetMapping
    public List<MediaFile> listProjects(@RequestParam Long userId) {
        return creatorWorkspaceService.listProjects(userId);
    }

    @GetMapping("/{projectId}")
    public ProjectDetailResponse getProjectDetail(@PathVariable Long projectId,
                                                  @RequestParam(required = false) Long userId) {
        return creatorWorkspaceService.getProjectDetail(projectId, userId);
    }

    @PostMapping("/{projectId}/analyze")
    public ResponseEntity<String> analyzeProject(@PathVariable Long projectId,
                                                 @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(analysisTaskOrchestrator.triggerAnalysis(projectId, userId));
    }

    @PostMapping("/{projectId}/rewrite-drafts")
    public RewriteDraft createRewriteDraft(@PathVariable Long projectId,
                                           @RequestParam Long userId,
                                           @RequestBody RewriteDraftRequest request) {
        return creatorWorkspaceService.createRewriteDraft(projectId, userId, request);
    }
}
