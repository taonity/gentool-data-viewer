ALTER TABLE replay ADD COLUMN windows_compat VARCHAR(255);
ALTER TABLE replay ADD COLUMN rep_info_in_use VARCHAR(50);
ALTER TABLE replay ADD COLUMN fields_json VARCHAR(30000) NOT NULL DEFAULT '{}';
ALTER TABLE replay ADD COLUMN raw_text VARCHAR(100000) NOT NULL DEFAULT '';

CREATE TABLE replay_associated_file (
    id         VARCHAR(36) PRIMARY KEY,
    replay_id  VARCHAR(36) NOT NULL REFERENCES replay(id) ON DELETE CASCADE,
    file_name  VARCHAR(1000) NOT NULL,
    size_bytes BIGINT NOT NULL
);

CREATE INDEX idx_replay_associated_file_replay_id ON replay_associated_file(replay_id);

ALTER TABLE replay_collection_job ADD COLUMN user_limit INTEGER;