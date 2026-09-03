CREATE TABLE cpu_benchmark (
    source_id           VARCHAR(32) PRIMARY KEY,
    model_name          VARCHAR(500) NOT NULL,
    normalized_name     VARCHAR(500) NOT NULL,
    normalized_model    VARCHAR(500) NOT NULL,
    single_thread_score INTEGER NOT NULL,
    source_url          VARCHAR(1000) NOT NULL,
    fetched_at          TIMESTAMP NOT NULL
);

CREATE INDEX idx_cpu_benchmark_normalized_name ON cpu_benchmark(normalized_name);
CREATE INDEX idx_cpu_benchmark_normalized_model ON cpu_benchmark(normalized_model);

ALTER TABLE player_hardware ADD COLUMN cpu_benchmark_id VARCHAR(32);
ALTER TABLE player_hardware ADD COLUMN cpu_benchmark_name VARCHAR(500);
ALTER TABLE player_hardware ADD COLUMN cpu_benchmark_url VARCHAR(1000);
ALTER TABLE player_hardware ADD COLUMN cpu_match_status VARCHAR(20) NOT NULL DEFAULT 'UNMATCHED';
ALTER TABLE player_hardware ADD COLUMN cpu_score_updated_at TIMESTAMP;