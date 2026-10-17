-- Phase 13: expert qualification materials
USE expert_review;

CREATE TABLE IF NOT EXISTS expert_material (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    expert_id       BIGINT       NOT NULL,
    file_name       VARCHAR(256) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    material_type   VARCHAR(64)  NULL COMMENT '简历/职称证明/身份证/保密协议等',
    uploaded_by     BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_expert_material (expert_id)
);
