package com.example.stocktrading.trading.adapter.in.web;

import com.example.stocktrading.common.ApiResponse;
import com.example.stocktrading.common.security.AuthContext;
import com.example.stocktrading.common.security.RequireAuth;
import com.example.stocktrading.trading.application.port.in.AssetUseCase;
import com.example.stocktrading.trading.domain.Asset;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/asset")
@RequiredArgsConstructor
@RequireAuth
public class AssetController {

    private final AssetUseCase assetUseCase;

    @GetMapping
    public ApiResponse<Asset> getAsset() {
        Long userId = AuthContext.getUserId();
        Asset asset = assetUseCase.getAccountAsset(userId);
        return ApiResponse.success(asset);
    }
}
