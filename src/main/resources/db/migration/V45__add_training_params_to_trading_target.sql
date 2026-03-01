ALTER TABLE trading_target ADD COLUMN profit_atr DOUBLE DEFAULT 0.6;
ALTER TABLE trading_target ADD COLUMN stop_atr DOUBLE DEFAULT 0.4;
ALTER TABLE trading_target ADD COLUMN max_holding INT DEFAULT 5;
ALTER TABLE trading_target ADD COLUMN min_threshold DOUBLE DEFAULT 0.2;
ALTER TABLE trading_target ADD COLUMN training_period_years INT DEFAULT 4;
ALTER TABLE trading_target ADD COLUMN tuning_trials INT DEFAULT 30;

-- 종목별 최적값 세팅 (V38 기반 복원 + 조정)
-- 저변동 (SPY, QQQ)
UPDATE trading_target SET profit_atr = 0.5, stop_atr = 0.3, max_holding = 10, min_threshold = 0.15, training_period_years = 4 WHERE ticker IN ('SPY', 'QQQ');
-- 중변동 (GOOGL, QCOM, NFLX)
UPDATE trading_target SET profit_atr = 0.7, stop_atr = 0.45, max_holding = 6, min_threshold = 0.20, training_period_years = 4 WHERE ticker IN ('GOOGL', 'QCOM', 'NFLX');
-- 중~고변동 (SOXX)
UPDATE trading_target SET profit_atr = 0.7, stop_atr = 0.45, max_holding = 6, min_threshold = 0.22, training_period_years = 3 WHERE ticker IN ('SOXX');
-- 고변동 (PLTR, TSLA, NVDA)
UPDATE trading_target SET profit_atr = 0.8, stop_atr = 0.5, max_holding = 5, min_threshold = 0.25, training_period_years = 3 WHERE ticker IN ('PLTR', 'TSLA', 'NVDA');
