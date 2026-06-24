CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    nickname VARCHAR(64),
    avatar VARCHAR(255),
    role VARCHAR(32) DEFAULT 'USER'
);

CREATE TABLE IF NOT EXISTS media_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    filename VARCHAR(255) NOT NULL,
    status VARCHAR(32) DEFAULT 'COMPLETED',
    file_path VARCHAR(1024),
    file_md5 VARCHAR(64),
    object_key VARCHAR(512),
    file_size BIGINT DEFAULT 0,
    upload_status VARCHAR(32) DEFAULT 'COMPLETED',
    ai_summary CLOB,
    transcript_text CLOB,
    cover_url VARCHAR(1024),
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    project_title VARCHAR(255),
    source_type VARCHAR(32) DEFAULT 'LOCAL_UPLOAD',
    platform VARCHAR(64) DEFAULT 'GENERIC',
    analysis_status VARCHAR(32) DEFAULT 'PENDING',
    error_message VARCHAR(1024),
    duration_seconds INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE media_files ADD COLUMN IF NOT EXISTS file_md5 VARCHAR(64);
ALTER TABLE media_files ADD COLUMN IF NOT EXISTS object_key VARCHAR(512);
ALTER TABLE media_files ADD COLUMN IF NOT EXISTS file_size BIGINT DEFAULT 0;
ALTER TABLE media_files ADD COLUMN IF NOT EXISTS upload_status VARCHAR(32) DEFAULT 'COMPLETED';

CREATE INDEX IF NOT EXISTS idx_media_files_md5 ON media_files (file_md5);
CREATE INDEX IF NOT EXISTS idx_media_files_user_updated ON media_files (user_id, updated_at);

CREATE TABLE IF NOT EXISTS scene_segments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    media_id BIGINT NOT NULL,
    segment_index INT NOT NULL,
    start_second INT DEFAULT 0,
    end_second INT DEFAULT 0,
    title VARCHAR(255),
    scene_summary CLOB,
    actions CLOB,
    dialogue CLOB,
    shot_type VARCHAR(64),
    emotion VARCHAR(64),
    creative_hook CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scene_segments_media_id ON scene_segments (media_id);

CREATE TABLE IF NOT EXISTS rewrite_drafts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    media_id BIGINT NOT NULL,
    target_platform VARCHAR(64) NOT NULL,
    persona VARCHAR(255),
    tone VARCHAR(128),
    audience VARCHAR(255),
    duration_seconds INT DEFAULT 30,
    script_markdown CLOB,
    storyboard_markdown CLOB,
    prompt_bundle CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rewrite_drafts_media_id ON rewrite_drafts (media_id);
