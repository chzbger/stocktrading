CREATE TABLE pending_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ticker VARCHAR(20) NOT NULL,
    order_id VARCHAR(100) NOT NULL,
    order_type VARCHAR(10) NOT NULL,
    order_time TIMESTAMP NOT NULL
);

CREATE INDEX idx_pending_orders_user_id ON pending_orders(user_id);
CREATE INDEX idx_pending_orders_ticker ON pending_orders(ticker);
