ALTER TABLE users
ADD COLUMN trading_start_time TIME DEFAULT '22:30:00',
ADD COLUMN trading_end_time TIME DEFAULT '05:00:00';
