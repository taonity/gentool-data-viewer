UPDATE config_override SET updated_by = 'legacy-user' WHERE updated_by LIKE '%@%';
UPDATE replay_collection_job SET requested_by = 'legacy-user' WHERE requested_by LIKE '%@%';

ALTER TABLE audit_log DROP COLUMN actor_email;
ALTER TABLE app_user DROP COLUMN email;