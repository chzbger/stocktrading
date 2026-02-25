package com.example.stocktrading.user.adapter.in.web;

import com.example.stocktrading.user.application.port.in.UserUseCase;
import com.example.stocktrading.user.domain.BrokerInfo;
import com.example.stocktrading.user.domain.BrokerType;
import com.example.stocktrading.user.domain.User;
import com.example.stocktrading.common.ApiResponse;
import com.example.stocktrading.common.security.AuthContext;
import com.example.stocktrading.common.security.RequireAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@RequireAuth
public class SettingsController {

    private final UserUseCase userUseCase;

    @GetMapping
    public ApiResponse<SettingsResponse> getSettings() {
        Long userId = AuthContext.getUserId();
        User user = userUseCase.getUser(userId);

        return ApiResponse.success(new SettingsResponse(
                user.getActiveBrokerId(),
                user.getBrokerInfos(),
                user.getTradingStartTime(),
                user.getTradingEndTime(),
                user.getNotificationEnabled(),
                user.getTelegramBotToken(),
                user.getTelegramChatId()));
    }

    @PostMapping("/brokers")
    public ApiResponse<ApiResponse.MsgData> addBroker(@RequestBody AddBrokerRequest request) {
        Long userId = AuthContext.getUserId();
        userUseCase.addBrokerInfo(userId, request.brokerType(), request.appKey(), request.appSecret(),
                request.accountNumber());
        return ApiResponse.successOnlyMsg("증권사 정보가 추가되었습니다.");
    }

    @DeleteMapping("/brokers/{id}")
    public ApiResponse<ApiResponse.MsgData> deleteBroker(@PathVariable Long id) {
        Long userId = AuthContext.getUserId();
        userUseCase.deleteBrokerInfo(userId, id);
        return ApiResponse.successOnlyMsg("증권사 정보가 삭제되었습니다.");
    }

    @PostMapping("/active-broker")
    public ApiResponse<ApiResponse.MsgData> setActiveBroker(@RequestBody SetActiveBrokerRequest request) {
        Long userId = AuthContext.getUserId();
        userUseCase.setActiveBroker(userId, request.brokerInfoId());
        return ApiResponse.successOnlyMsg("활성 증권사가 변경되었습니다.");
    }

    @PostMapping("/trading-hours")
    public ApiResponse<ApiResponse.MsgData> updateTradingHours(@RequestBody UpdateTradingHoursRequest request) {
        Long userId = AuthContext.getUserId();
        userUseCase.updateTradingHours(userId, request.startTime(), request.endTime());
        return ApiResponse.successOnlyMsg("매매 시간이 설정되었습니다.");
    }

    @PostMapping("/notification")
    public ApiResponse<ApiResponse.MsgData> updateNotification(@RequestBody UpdateNotificationRequest request) {
        Long userId = AuthContext.getUserId();
        userUseCase.updateNotificationSettings(userId, request.enabled(), request.botToken(), request.chatId());
        return ApiResponse.successOnlyMsg("알림 설정이 저장되었습니다.");
    }

    public record SettingsResponse(
            Long activeBrokerId,
            List<BrokerInfo> brokerInfos,
            LocalTime tradingStartTime,
            LocalTime tradingEndTime,
            Boolean notificationEnabled,
            String telegramBotToken,
            String telegramChatId) {
    }

    public record AddBrokerRequest(
            BrokerType brokerType,
            String appKey,
            String appSecret,
            String accountNumber) {
    }

    public record SetActiveBrokerRequest(Long brokerInfoId) {
    }

    public record UpdateTradingHoursRequest(
            LocalTime startTime,
            LocalTime endTime) {
    }

    public record UpdateNotificationRequest(
            boolean enabled,
            String botToken,
            String chatId) {
    }
}
