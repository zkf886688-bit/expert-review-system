-- Phase 11: sys_config for notification templates
USE expert_review;

CREATE TABLE IF NOT EXISTS sys_config (
    config_key   VARCHAR(128) PRIMARY KEY,
    config_value TEXT         NOT NULL,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by   BIGINT       NULL
);
