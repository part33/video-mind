package com.example.server.dto;

import lombok.Data;

@Data
public class RewriteDraftRequest {
    private String targetPlatform;
    private String persona;
    private String tone;
    private String audience;
    private Integer durationSeconds;
}
