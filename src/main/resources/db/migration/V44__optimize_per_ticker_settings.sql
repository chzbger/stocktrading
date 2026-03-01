-- buyThreshold/sellThreshold: 10 → 20 (precision 향상)
UPDATE trading_target SET buy_threshold = 20, sell_threshold = 20;

-- 종목별 stopLoss/trailingStop/trailingWindow 최적화
-- 저변동 (SPY, QQQ)
UPDATE trading_target SET stop_loss_percentage = '2.0', trailing_stop_percentage = '1.5', trailing_window_minutes = 15 WHERE ticker IN ('SPY', 'QQQ');
-- 중변동 (GOOGL, QCOM, NFLX, SOXX)
UPDATE trading_target SET stop_loss_percentage = '3.0', trailing_stop_percentage = '2.0', trailing_window_minutes = 10 WHERE ticker IN ('GOOGL', 'QCOM', 'NFLX', 'SOXX');
-- 고변동 (TSLA, PLTR, NVDA)
UPDATE trading_target SET stop_loss_percentage = '3.5', trailing_stop_percentage = '2.5', trailing_window_minutes = 8 WHERE ticker IN ('TSLA', 'PLTR', 'NVDA');
