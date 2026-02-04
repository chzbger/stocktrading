UPDATE trade_logs SET profit_rate = profit_rate * 100 WHERE profit_rate <= 2 AND profit_rate >= -2;

ALTER TABLE trade_logs ALTER COLUMN profit_rate INT;
