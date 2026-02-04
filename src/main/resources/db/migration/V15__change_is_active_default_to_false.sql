ALTER TABLE watchlist ALTER COLUMN is_active SET DEFAULT FALSE;
UPDATE watchlist SET is_active = FALSE;
