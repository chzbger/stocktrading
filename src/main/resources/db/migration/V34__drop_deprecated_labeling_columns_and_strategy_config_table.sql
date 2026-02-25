ALTER TABLE watchlist DROP COLUMN IF EXISTS profit_atr;
ALTER TABLE watchlist DROP COLUMN IF EXISTS stop_atr;
ALTER TABLE watchlist DROP COLUMN IF EXISTS max_holding;
ALTER TABLE watchlist DROP COLUMN IF EXISTS min_threshold;

DROP TABLE IF EXISTS watchlist_strategy_config;
