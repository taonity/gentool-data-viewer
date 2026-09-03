ALTER TABLE player_hardware ADD COLUMN main_name VARCHAR(255);
UPDATE player_hardware SET main_name = latest_name;
ALTER TABLE player_hardware ALTER COLUMN main_name SET NOT NULL;
ALTER TABLE player_hardware ADD COLUMN aliases_json VARCHAR(5000) NOT NULL DEFAULT '[]';
ALTER TABLE player_hardware ADD COLUMN replay_count BIGINT NOT NULL DEFAULT 0;