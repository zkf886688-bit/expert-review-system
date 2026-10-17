-- Phase 3+ 升级脚本
USE expert_review;

CREATE TABLE IF NOT EXISTS expert_credit_record (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    expert_id       BIGINT       NOT NULL,
    assignment_id   BIGINT,
    submission_id   BIGINT,
    record_type     VARCHAR(32)  NOT NULL COMMENT 'QUALITY/ON_TIME/REJECT/OVERDUE',
    score           DECIMAL(4,2) COMMENT '质量评分1-5',
    comment         TEXT,
    created_by      BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_credit_expert (expert_id)
);

ALTER TABLE expert ADD COLUMN avg_quality_score DECIMAL(4,2) NULL COMMENT '平均质量评分';
