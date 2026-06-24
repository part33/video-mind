CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    nickname VARCHAR(64),
    avatar VARCHAR(255),
    role VARCHAR(32) DEFAULT 'USER'
);

CREATE TABLE IF NOT EXISTS media_files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    filename VARCHAR(255) NOT NULL,
    status VARCHAR(32) DEFAULT 'COMPLETED',
    file_path VARCHAR(1024),
    file_md5 VARCHAR(64),
    object_key VARCHAR(512),
    file_size BIGINT DEFAULT 0,
    upload_status VARCHAR(32) DEFAULT 'COMPLETED',
    ai_summary LONGTEXT,
    transcript_text LONGTEXT,
    cover_url VARCHAR(1024),
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    project_title VARCHAR(255),
    source_type VARCHAR(32) DEFAULT 'LOCAL_UPLOAD',
    platform VARCHAR(64) DEFAULT 'GENERIC',
    analysis_status VARCHAR(32) DEFAULT 'PENDING',
    error_message VARCHAR(1024),
    duration_seconds INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_media_files_md5 (file_md5),
    INDEX idx_media_files_user_updated (user_id, updated_at)
);

ALTER TABLE media_files ADD COLUMN file_md5 VARCHAR(64);
ALTER TABLE media_files ADD COLUMN object_key VARCHAR(512);
ALTER TABLE media_files ADD COLUMN file_size BIGINT DEFAULT 0;
ALTER TABLE media_files ADD COLUMN upload_status VARCHAR(32) DEFAULT 'COMPLETED';
CREATE INDEX idx_media_files_md5 ON media_files (file_md5);
CREATE INDEX idx_media_files_user_updated ON media_files (user_id, updated_at);

CREATE TABLE IF NOT EXISTS scene_segments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    media_id BIGINT NOT NULL,
    segment_index INT NOT NULL,
    start_second INT DEFAULT 0,
    end_second INT DEFAULT 0,
    title VARCHAR(255),
    scene_summary TEXT,
    actions TEXT,
    dialogue TEXT,
    shot_type VARCHAR(64),
    emotion VARCHAR(64),
    creative_hook TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_scene_segments_media_id (media_id)
);

CREATE TABLE IF NOT EXISTS rewrite_drafts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    media_id BIGINT NOT NULL,
    target_platform VARCHAR(64) NOT NULL,
    persona VARCHAR(255),
    tone VARCHAR(128),
    audience VARCHAR(255),
    duration_seconds INT DEFAULT 30,
    script_markdown LONGTEXT,
    storyboard_markdown LONGTEXT,
    prompt_bundle LONGTEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rewrite_drafts_media_id (media_id)
);
