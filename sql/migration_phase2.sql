-- Phase 2 升级脚本（在 Phase 1 已建库基础上执行）
USE expert_review;

ALTER TABLE review_task ADD COLUMN blind_review TINYINT NOT NULL DEFAULT 0 COMMENT '盲审模式';
ALTER TABLE review_task ADD COLUMN avoid_same_org TINYINT NOT NULL DEFAULT 1 COMMENT '同单位回避';
ALTER TABLE review_assignment ADD COLUMN reject_reason VARCHAR(512) NULL COMMENT '拒绝/回避原因';

CREATE TABLE IF NOT EXISTS sys_notification (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(256) NOT NULL,
    content     TEXT,
    type        VARCHAR(32)  COMMENT 'INVITE/DEADLINE/OVERDUE/SYSTEM',
    related_id  BIGINT,
    read_flag   TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notify_user (user_id, read_flag)
);
