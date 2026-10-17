-- Phase 16: 多轮评审 / 申报人整改
ALTER TABLE review_project
    ADD COLUMN review_round INT NOT NULL DEFAULT 1 COMMENT '评审轮次',
    ADD COLUMN rectification_status VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/REQUESTED/SUBMITTED/RE_REVIEWING',
    ADD COLUMN rectification_note TEXT NULL COMMENT '管理员整改要求',
    ADD COLUMN rectification_reply TEXT NULL COMMENT '申报人整改说明',
    ADD COLUMN rectification_requested_at DATETIME NULL,
    ADD COLUMN rectification_submitted_at DATETIME NULL;

CREATE TABLE IF NOT EXISTS project_rectification_material (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id      BIGINT       NOT NULL,
    file_name       VARCHAR(256) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    uploaded_by     BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rect_material_project (project_id)
);
