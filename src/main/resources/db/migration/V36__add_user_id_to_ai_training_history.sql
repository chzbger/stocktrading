ALTER TABLE ai_training_history ADD COLUMN user_id BIGINT;

ALTER TABLE ai_training_history DROP CONSTRAINT IF EXISTS uk_ticker_date;
ALTER TABLE ai_training_history ADD CONSTRAINT uk_ticker_date_user UNIQUE (ticker, train_date, user_id);
