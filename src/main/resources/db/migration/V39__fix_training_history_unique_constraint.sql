DROP TABLE IF EXISTS ai_training_history;

CREATE TABLE ai_training_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    train_date VARCHAR(8) NOT NULL,
    user_id BIGINT,
    status VARCHAR(20) NOT NULL,
    message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_ticker_date_user UNIQUE (ticker, train_date, user_id)
);
