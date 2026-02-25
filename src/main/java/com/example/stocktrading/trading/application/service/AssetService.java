package com.example.stocktrading.trading.application.service;

import com.example.stocktrading.user.application.port.out.UserPort;
import com.example.stocktrading.user.domain.User;
import com.example.stocktrading.common.CacheConfig;
import com.example.stocktrading.trading.application.port.in.AssetUseCase;
import com.example.stocktrading.trading.application.port.out.BrokerApiPort;
import com.example.stocktrading.trading.domain.Asset;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetService implements AssetUseCase {

    private final UserPort userPort;
    private final BrokerApiPort brokerApiPort;

    @Override
    @Cacheable(value = CacheConfig.USER_ASSET_CACHE, key = "'getAccountAsset-' + #userId")
    public Asset getAccountAsset(Long userId) {
        User user = userPort.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return brokerApiPort.getAccountAsset(user);
    }
}
