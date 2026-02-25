INSERT INTO watchlist_strategy_config (watchlist_id, strategy, training_period_years, profit_atr, stop_atr, max_holding, min_threshold) VALUES
(34,  'scalping', 3, 0.7, 0.45, 5,  0.2),
(65,  'scalping', 3, 0.8, 0.5,  5,  0.25),
(129, 'scalping', 3, 0.8, 0.5,  5,  0.25),
(193, 'scalping', 4, 0.8, 0.5,  5,  0.25),
(225, 'scalping', 3, 0.8, 0.5,  5,  0.25),
(226, 'scalping', 3, 0.7, 0.45, 7,  0.2),
(227, 'scalping', 4, 0.5, 0.3,  10, 0.15),
(228, 'scalping', 4, 0.5, 0.25, 12, 0.15),
(229, 'scalping', 4, 0.7, 0.45, 6,  0.22);

INSERT INTO watchlist_strategy_config (watchlist_id, strategy, training_period_years, profit_atr, stop_atr, max_holding, min_threshold) VALUES
(34,  'momentum', 5, 1.2, 0.8, 24, 0.3),
(65,  'momentum', 5, 1.5, 1.0, 20, 0.35),
(129, 'momentum', 5, 1.5, 1.0, 20, 0.35),
(193, 'momentum', 6, 1.5, 1.0, 20, 0.35),
(225, 'momentum', 5, 1.5, 1.0, 18, 0.35),
(226, 'momentum', 5, 1.2, 0.8, 24, 0.3),
(227, 'momentum', 7, 0.8, 0.5, 30, 0.25),
(228, 'momentum', 7, 0.8, 0.5, 30, 0.25),
(229, 'momentum', 6, 1.2, 0.8, 24, 0.3);

INSERT INTO watchlist_strategy_config (watchlist_id, strategy, training_period_years, profit_atr, stop_atr, max_holding, min_threshold) VALUES
(34,  'swing', 7,  2.0, 1.5, 3, 0.5),
(65,  'swing', 7,  2.5, 2.0, 3, 0.55),
(129, 'swing', 7,  2.5, 2.0, 3, 0.55),
(193, 'swing', 8,  2.5, 2.0, 3, 0.55),
(225, 'swing', 7,  2.5, 2.0, 3, 0.55),
(226, 'swing', 7,  2.0, 1.5, 3, 0.5),
(227, 'swing', 10, 1.5, 1.0, 5, 0.4),
(228, 'swing', 10, 1.5, 1.0, 5, 0.4),
(229, 'swing', 8,  2.0, 1.5, 4, 0.5);
