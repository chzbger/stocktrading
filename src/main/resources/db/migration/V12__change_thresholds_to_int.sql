UPDATE watchlist SET buy_threshold = buy_threshold * 100 WHERE buy_threshold <= 2;
UPDATE watchlist SET sell_threshold = sell_threshold * 100 WHERE sell_threshold <= 2;

ALTER TABLE watchlist ALTER COLUMN buy_threshold INT;
ALTER TABLE watchlist ALTER COLUMN sell_threshold INT;

ALTER TABLE watchlist ALTER COLUMN buy_threshold SET DEFAULT 60;
ALTER TABLE watchlist ALTER COLUMN sell_threshold SET DEFAULT 60;
