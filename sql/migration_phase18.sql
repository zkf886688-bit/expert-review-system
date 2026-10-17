-- Phase 18: 多轮评审归档表（启动时 SchemaAutoFix 也会自动创建）
CREATE TABLE IF NOT EXISTS review_submission_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    review_round INT NOT NULL,
    assignment_id BIGINT NULL,
    expert_id BIGINT NOT NULL,
    expert_name VARCHAR(64) NULL,
    scores JSON NULL,
    total_score DECIMAL(8,2) NULL,
    opinion TEXT NULL,
    conclusion VARCHAR(64) NULL,
    submitted_at DATETIME NULL,
    archived_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_archive_project_round (project_id, review_round)
);
