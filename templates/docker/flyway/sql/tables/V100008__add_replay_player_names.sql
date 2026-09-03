ALTER TABLE replay ADD COLUMN player_names VARCHAR(2000) NOT NULL DEFAULT '';
UPDATE replay SET player_names = reporter_name WHERE player_names = '';