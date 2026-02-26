-- 1. trade_logs에 order_id 추가
ALTER TABLE trade_logs ADD COLUMN order_id VARCHAR(100);

-- 2. 기존 상태값 마이그레이션
UPDATE trade_logs SET status = 'FILLED' WHERE status = 'SUCCESS';
UPDATE trade_logs SET status = 'FAILED' WHERE status IN ('INSUFFICIENT_BALANCE', 'INSUFFICIENT_STOCK');

-- 3. 인덱스 추가
CREATE INDEX idx_trade_logs_status_ts ON trade_logs(status, timestamp);
CREATE INDEX idx_trade_logs_user_ticker_action_status ON trade_logs(user_id, ticker, action, status);

-- 4. trading_target에서 holding_quantity 제거
ALTER TABLE trading_target DROP COLUMN IF EXISTS holding_quantity;

-- 5. pending_orders 테이블 삭제
DROP TABLE IF EXISTS pending_orders;
