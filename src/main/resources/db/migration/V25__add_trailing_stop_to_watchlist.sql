ALTER TABLE watchlist ADD COLUMN IF NOT EXISTS trailing_stop_percentage VARCHAR(20) DEFAULT '2.0';
ALTER TABLE watchlist ADD COLUMN IF NOT EXISTS trailing_stop_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE watchlist ADD COLUMN IF NOT EXISTS trailing_window_minutes INT DEFAULT 10;

UPDATE watchlist SET trailing_stop_percentage = '3.0', trailing_window_minutes = 5 WHERE ticker IN ('TSLA', 'PLTR');
UPDATE watchlist SET trailing_stop_percentage = '2.5', trailing_window_minutes = 5 WHERE ticker = 'NVDA';

UPDATE watchlist SET trailing_stop_percentage = '1.0', trailing_window_minutes = 15 WHERE ticker = 'SPY';
UPDATE watchlist SET trailing_stop_percentage = '1.5', trailing_window_minutes = 15 WHERE ticker IN ('QQQ', 'SOXX');
