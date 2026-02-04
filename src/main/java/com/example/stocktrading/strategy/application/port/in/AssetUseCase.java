package com.example.stocktrading.strategy.application.port.in;

import com.example.stocktrading.strategy.domain.Asset;

public interface AssetUseCase {

    Asset getAccountAsset(Long userId);
}
