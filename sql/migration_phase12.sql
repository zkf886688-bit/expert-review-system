-- Phase 12: avoid duplicate expert across projects in same task
USE expert_review;

ALTER TABLE review_task ADD COLUMN avoid_duplicate_expert TINYINT(1) NOT NULL DEFAULT 1
    COMMENT '同任务内同一专家不可评审多个项目';
