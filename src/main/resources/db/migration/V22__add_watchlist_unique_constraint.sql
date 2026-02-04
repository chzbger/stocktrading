ALTER TABLE watchlist ADD CONSTRAINT uk_watchlist_user_ticker UNIQUE (user_id, ticker);
