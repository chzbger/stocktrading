ALTER TABLE watchlist ADD COLUMN strategy VARCHAR(20) DEFAULT NULL;

CREATE TABLE strategy_params (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    watchlist_id BIGINT NOT NULL,
    param_key VARCHAR(50) NOT NULL,
    param_value VARCHAR(100) NOT NULL,
    CONSTRAINT fk_strategy_params_watchlist FOREIGN KEY (watchlist_id) REFERENCES watchlist(id) ON DELETE CASCADE,
    CONSTRAINT uq_strategy_params UNIQUE (watchlist_id, param_key)
);
