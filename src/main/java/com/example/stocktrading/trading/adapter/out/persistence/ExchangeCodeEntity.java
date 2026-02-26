package com.example.stocktrading.trading.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_codes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(name = "exchange_mic", nullable = false, length = 10)
    private String exchangeMic;

    @Column(name = "kis_price_code", nullable = false, length = 10)
    private String kisPriceCode;

    @Column(name = "kis_order_code", nullable = false, length = 10)
    private String kisOrderCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
