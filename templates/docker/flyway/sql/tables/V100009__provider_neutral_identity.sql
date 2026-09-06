UPDATE app_user SET google_id = CONCAT('google:', google_id);
ALTER TABLE app_user RENAME COLUMN google_id TO user_id;
ALTER TABLE app_user ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'google';

UPDATE audit_log SET actor_google_id = CONCAT('google:', actor_google_id);
ALTER TABLE audit_log RENAME COLUMN actor_google_id TO actor_user_id;