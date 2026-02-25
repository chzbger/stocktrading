ALTER TABLE users ADD COLUMN notification_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN telegram_bot_token VARCHAR(255);
ALTER TABLE users ADD COLUMN telegram_chat_id VARCHAR(255);
