ALTER TABLE watchlist RENAME TO trading_target;

ALTER TABLE trading_target DROP CONSTRAINT IF EXISTS uk_watchlist_user_ticker;
ALTER TABLE trading_target ADD CONSTRAINT uk_trading_target_user_ticker UNIQUE (user_id, ticker);
