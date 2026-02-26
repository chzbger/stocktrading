package com.example.stocktrading.user.adapter.out.persistence;

import com.example.stocktrading.user.domain.User.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.ROLE_USER;

    @Column(name = "active_broker_id")
    private Long activeBrokerId;

    @Column(name = "trading_start_time")
    private LocalTime tradingStartTime;

    @Column(name = "trading_end_time")
    private LocalTime tradingEndTime;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BrokerInfoEntity> brokerInfos = new ArrayList<>();

    @Column(name = "notification_enabled")
    @Builder.Default
    private Boolean notificationEnabled = false;

    @Column(name = "telegram_bot_token")
    private String telegramBotToken;

    @Column(name = "telegram_chat_id")
    private String telegramChatId;

    private ZonedDateTime createdAt;

    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ZonedDateTime.now();
        updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    public enum UserStatus {
        PENDING, // 가입신청
        ACTIVE // 정상
    }
}
