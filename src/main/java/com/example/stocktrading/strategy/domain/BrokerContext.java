package com.example.stocktrading.strategy.domain;

import com.example.stocktrading.auth.domain.BrokerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerContext {

    private String appKey;
    private String appSecret;
    private String accountNo;
    private String cano;
    private String acntPrdtCd;
    private BrokerType brokerType;
}
