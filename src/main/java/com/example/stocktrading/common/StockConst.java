package com.example.stocktrading.common;

import java.util.function.Consumer;

public class StockConst {
    public static final Consumer<String> VALIDATE_TICKER = ticker -> {
        if (ticker == null || ticker.isBlank() ||
            ticker.contains("/") || ticker.contains("\\") || ticker.contains(".")) {
            throw new IllegalArgumentException("Invalid ticker: " + ticker);
        }
    };
}
