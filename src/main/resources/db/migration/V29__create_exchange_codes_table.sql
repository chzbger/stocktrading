CREATE TABLE exchange_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(10) NOT NULL,
    exchange_mic VARCHAR(10) NOT NULL,
    kis_price_code VARCHAR(10) NOT NULL,
    kis_order_code VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX idx_exchange_codes_ticker ON exchange_codes(ticker);

INSERT INTO exchange_codes (ticker, exchange_mic, kis_price_code, kis_order_code) VALUES
('SPY', 'XASE', 'AMS', 'AMEX'), ('VOO', 'XASE', 'AMS', 'AMEX'),
('IVV', 'XASE', 'AMS', 'AMEX'), ('UPRO', 'XASE', 'AMS', 'AMEX'),
('SPXU', 'XASE', 'AMS', 'AMEX'), ('SOXL', 'XASE', 'AMS', 'AMEX'),
('SOXS', 'XASE', 'AMS', 'AMEX'), ('TSLZ', 'XASE', 'AMS', 'AMEX'),
('TSM', 'XNYS', 'NYS', 'NYSE'), ('JPM', 'XNYS', 'NYS', 'NYSE'),
('V', 'XNYS', 'NYS', 'NYSE'), ('DIS', 'XNYS', 'NYS', 'NYSE');
