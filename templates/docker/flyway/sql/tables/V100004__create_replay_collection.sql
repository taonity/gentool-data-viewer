CREATE TABLE replay (
    id                   VARCHAR(36) PRIMARY KEY,
    source_url           VARCHAR(2000) NOT NULL UNIQUE,
    source_date          DATE NOT NULL,
    reporter_id          VARCHAR(64) NOT NULL,
    reporter_name        VARCHAR(255) NOT NULL,
    match_at             TIMESTAMP NOT NULL,
    gentool_version      VARCHAR(50),
    game_version         VARCHAR(255),
    install_type         VARCHAR(1000),
    map_name             VARCHAR(500),
    start_cash           INTEGER,
    match_type           VARCHAR(50),
    match_length_seconds BIGINT,
    match_mode           VARCHAR(50),
    system_info          VARCHAR(10000),
    cpu                  VARCHAR(500),
    replay_file_name     VARCHAR(1000),
    replay_size_bytes    BIGINT,
    collected_at         TIMESTAMP NOT NULL
);

CREATE INDEX idx_replay_source_date ON replay(source_date);
CREATE INDEX idx_replay_match_at ON replay(match_at);
CREATE INDEX idx_replay_reporter_id ON replay(reporter_id);

CREATE TABLE replay_player (
    id          VARCHAR(36) PRIMARY KEY,
    replay_id   VARCHAR(36) NOT NULL REFERENCES replay(id) ON DELETE CASCADE,
    team_number INTEGER NOT NULL,
    slot_number INTEGER NOT NULL,
    address     VARCHAR(32) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    army        VARCHAR(255)
);

CREATE INDEX idx_replay_player_replay_id ON replay_player(replay_id);
CREATE INDEX idx_replay_player_name ON replay_player(name);

CREATE TABLE player_hardware (
    player_id        VARCHAR(64) PRIMARY KEY,
    latest_name      VARCHAR(255) NOT NULL,
    cpu              VARCHAR(500),
    cpu_score        INTEGER,
    system_info      VARCHAR(10000),
    observed_at      TIMESTAMP NOT NULL,
    source_replay_id VARCHAR(36) NOT NULL REFERENCES replay(id)
);

CREATE INDEX idx_player_hardware_cpu_score ON player_hardware(cpu_score);

CREATE TABLE replay_collection_job (
    id                     VARCHAR(36) PRIMARY KEY,
    trigger_type           VARCHAR(20) NOT NULL,
    status                 VARCHAR(20) NOT NULL,
    start_date             DATE NOT NULL,
    end_date               DATE NOT NULL,
    requested_by           VARCHAR(320) NOT NULL,
    created_at             TIMESTAMP NOT NULL,
    started_at             TIMESTAMP,
    finished_at            TIMESTAMP,
    directories_discovered BIGINT NOT NULL DEFAULT 0,
    directories_scanned    BIGINT NOT NULL DEFAULT 0,
    files_discovered       BIGINT NOT NULL DEFAULT 0,
    files_imported         BIGINT NOT NULL DEFAULT 0,
    files_skipped          BIGINT NOT NULL DEFAULT 0,
    failures               BIGINT NOT NULL DEFAULT 0,
    error_message          VARCHAR(2000)
);

CREATE INDEX idx_replay_collection_job_created_at ON replay_collection_job(created_at);