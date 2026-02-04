package com.example.stocktrading.strategy.application.service;

import com.example.stocktrading.auth.application.port.out.UserPort;
import com.example.stocktrading.auth.domain.User;
import com.example.stocktrading.common.CacheConfig;
import com.example.stocktrading.strategy.application.port.in.AssetUseCase;
import com.example.stocktrading.strategy.application.port.out.BrokerApiPort;
import com.example.stocktrading.strategy.domain.Asset;
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
