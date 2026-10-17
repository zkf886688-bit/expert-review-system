-- Phase 5: applicant role + data domain isolation
-- Re-run safe: skip ALTER if columns already exist
USE expert_review;

ALTER TABLE sys_user ADD COLUMN data_domain VARCHAR(64) NULL DEFAULT 'DEFAULT' COMMENT 'data domain scope';
ALTER TABLE review_task ADD COLUMN data_domain VARCHAR(64) NULL DEFAULT 'DEFAULT' COMMENT 'task data domain';
ALTER TABLE review_project ADD COLUMN applicant_user_id BIGINT NULL COMMENT 'linked applicant user';

INSERT INTO sys_role (code, name)
SELECT 'APPLICANT', 'Applicant' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'APPLICANT');

UPDATE sys_user SET data_domain = 'DEFAULT' WHERE data_domain IS NULL;
UPDATE review_task SET data_domain = 'DEFAULT' WHERE data_domain IS NULL;
