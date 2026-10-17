-- Phase 5 data-only patch (safe to re-run)
USE expert_review;

INSERT INTO sys_role (code, name)
SELECT 'APPLICANT', '项目申报人' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'APPLICANT');

UPDATE sys_user SET data_domain = 'DEFAULT' WHERE data_domain IS NULL;
UPDATE review_task SET data_domain = 'DEFAULT' WHERE data_domain IS NULL;
