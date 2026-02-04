package com.example.stocktrading.auth.domain;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerInfo {
    private Long id;
    private Long userId;
    private BrokerType brokerType;
    private String appKey;
    private String appSecret;
    private String accountNumber;
}
