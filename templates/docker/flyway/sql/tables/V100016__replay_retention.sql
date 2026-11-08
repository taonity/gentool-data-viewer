-- Serialize replay imports and short cleanup batches, including across application instances.
CREATE TABLE replay_retention_lock (
    id INTEGER PRIMARY KEY CHECK (id = 1)
);
INSERT INTO replay_retention_lock (id) VALUES (1);

CREATE INDEX idx_player_hardware_observed_at ON player_hardware(observed_at, player_id);
CREATE INDEX idx_player_hardware_source_replay_id ON player_hardware(source_replay_id);
