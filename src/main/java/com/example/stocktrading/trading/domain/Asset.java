package com.example.stocktrading.trading.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {
    private String accountNo;
    private BigDecimal totalAsset;
    private BigDecimal usdDeposit;
    private List<OwnedStock> ownedStocks;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnedStock {
        private String stockCode;
        private String stockName;
        private int quantity;
        private BigDecimal averagePrice;
        private BigDecimal currentPrice;
        private BigDecimal profitRate;
    }
}
