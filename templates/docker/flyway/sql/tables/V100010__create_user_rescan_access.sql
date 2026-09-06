CREATE TABLE gentool_user_link (
    user_id            VARCHAR PRIMARY KEY REFERENCES app_user(user_id) ON DELETE CASCADE,
    player_id          VARCHAR(12) NOT NULL UNIQUE,
    status             VARCHAR(20) NOT NULL,
    requested_at       TIMESTAMP NOT NULL,
    decided_at         TIMESTAMP,
    decided_by_user_id VARCHAR REFERENCES app_user(user_id)
);

CREATE INDEX idx_gentool_user_link_status ON gentool_user_link(status);

ALTER TABLE replay_collection_job ADD COLUMN target_player_id VARCHAR(12);

CREATE TABLE replay_rescan_request (
    id                   VARCHAR(36) PRIMARY KEY,
    requested_by_user_id VARCHAR NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE,
    target_player_id     VARCHAR(12) NOT NULL,
    own_target           BOOLEAN NOT NULL,
    job_id               VARCHAR(36) NOT NULL REFERENCES replay_collection_job(id),
    requested_at         TIMESTAMP NOT NULL
);

CREATE INDEX idx_replay_rescan_request_user_time
    ON replay_rescan_request(requested_by_user_id, requested_at);
CREATE INDEX idx_replay_rescan_request_user_target_time
    ON replay_rescan_request(requested_by_user_id, target_player_id, requested_at);