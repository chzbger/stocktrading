ALTER TABLE watchlist ADD COLUMN profit_atr VARCHAR(10) DEFAULT '0.6';
ALTER TABLE watchlist ADD COLUMN stop_atr VARCHAR(10) DEFAULT '0.4';
ALTER TABLE watchlist ADD COLUMN max_holding INT DEFAULT 5;
ALTER TABLE watchlist ADD COLUMN min_threshold VARCHAR(10) DEFAULT '0.2';

UPDATE watchlist SET profit_atr = '0.4', stop_atr = '0.25', max_holding = 8, min_threshold = '0.15' WHERE ticker IN ('SPY', 'QQQ');
UPDATE watchlist SET profit_atr = '0.8', stop_atr = '0.5', max_holding = 5, min_threshold = '0.25' WHERE ticker IN ('PLTR', 'TSLA', 'NVDA');
UPDATE watchlist SET profit_atr = '0.7', stop_atr = '0.45', max_holding = 6, min_threshold = '0.22' WHERE ticker IN ('SOXX');
UPDATE watchlist SET profit_atr = '0.6', stop_atr = '0.4', max_holding = 5, min_threshold = '0.2' WHERE ticker IN ('GOOGL', 'QCOM', 'NFLX');
