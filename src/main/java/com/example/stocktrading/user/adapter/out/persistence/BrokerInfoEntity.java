package com.example.stocktrading.user.adapter.out.persistence;

import com.example.stocktrading.user.domain.BrokerType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "broker_infos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "broker_type", nullable = false)
    private BrokerType brokerType;

    @Column(name = "app_key")
    private String appKey;

    @Column(name = "app_secret")
    private String appSecret;

    @Column(name = "account_number")
    private String accountNumber;
}
