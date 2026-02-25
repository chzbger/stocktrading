package com.example.stocktrading.trading.application.port.in;

import com.example.stocktrading.trading.domain.Asset;

public interface AssetUseCase {

    Asset getAccountAsset(Long userId);
}
