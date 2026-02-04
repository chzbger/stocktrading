package com.example.stocktrading.auth.domain;

import lombok.*;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long userId;
    private String username;
    private String passwordHash;
    private UserRole role;
    private UserStatus status;
    private Long activeBrokerId;
    private LocalTime tradingStartTime;
    private LocalTime tradingEndTime;
    @Builder.Default
    private List<BrokerInfo> brokerInfos = new ArrayList<>();
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public enum UserStatus {
        PENDING,
        ACTIVE
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
