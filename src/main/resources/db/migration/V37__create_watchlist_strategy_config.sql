CREATE TABLE watchlist_strategy_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    watchlist_id BIGINT NOT NULL,
    strategy VARCHAR(20) NOT NULL,
    training_period_years INT DEFAULT 4,
    profit_atr DOUBLE DEFAULT 0.6,
    stop_atr DOUBLE DEFAULT 0.4,
    max_holding INT DEFAULT 5,
    min_threshold DOUBLE DEFAULT 0.2,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE(watchlist_id, strategy)
);
