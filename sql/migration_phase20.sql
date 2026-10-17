USE expert_review;

ALTER TABLE review_material
    ADD COLUMN material_type VARCHAR(64) NULL COMMENT '申请书/中期报告/结题报告/附件/查重报告/预算材料' AFTER file_size;

ALTER TABLE review_submission
    ADD COLUMN attachment_path VARCHAR(512) NULL AFTER conclusion,
    ADD COLUMN attachment_name VARCHAR(256) NULL AFTER attachment_path;

CREATE TABLE IF NOT EXISTS expert_avoidance (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    expert_id    BIGINT       NOT NULL,
    avoid_type   VARCHAR(32)  NOT NULL COMMENT 'ORGANIZATION/EXPERT/PROJECT',
    avoid_value  VARCHAR(256) NOT NULL,
    reason       VARCHAR(512) NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_avoid_expert (expert_id)
);
