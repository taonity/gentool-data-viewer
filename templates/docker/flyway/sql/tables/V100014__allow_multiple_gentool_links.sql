CREATE TABLE gentool_user_link_multi (
    user_id            VARCHAR NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE,
    player_id          VARCHAR(12) NOT NULL UNIQUE,
    status             VARCHAR(20) NOT NULL,
    requested_at       TIMESTAMP NOT NULL,
    decided_at         TIMESTAMP,
    decided_by_user_id VARCHAR REFERENCES app_user(user_id),
    PRIMARY KEY (user_id, player_id)
);

INSERT INTO gentool_user_link_multi (
    user_id,
    player_id,
    status,
    requested_at,
    decided_at,
    decided_by_user_id
)
SELECT
    user_id,
    player_id,
    status,
    requested_at,
    decided_at,
    decided_by_user_id
FROM gentool_user_link;

DROP TABLE gentool_user_link;
ALTER TABLE gentool_user_link_multi RENAME TO gentool_user_link;

CREATE INDEX idx_gentool_user_link_status ON gentool_user_link(status);
CREATE INDEX idx_gentool_user_link_user ON gentool_user_link(user_id);