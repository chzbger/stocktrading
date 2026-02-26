package com.example.stocktrading.trading.adapter.out.broker;

import com.example.stocktrading.common.CacheConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class KisTokenManager implements TokenManager {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public KisTokenManager(@Qualifier("kisRestClient") RestClient restClient,
                           ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Cacheable(value = CacheConfig.KIS_TOKEN_CACHE, key = "'getAccessToken-'+ #appKey + '-' + #appSecret")
    public String getAccessToken(String appKey, String appSecret) {
        if (appKey == null || appSecret == null) {
            throw new RuntimeException("[KIS Token] invalid key/secret");
        }

        log.info("[KIS Token] Fetching new token for key: {}", appKey);
        String accessToken = null;
        try {
            Map<String, String> body = Map.of(
                    "grant_type", "client_credentials",
                    "appkey", appKey,
                    "appsecret", appSecret);
            String jsonBody = objectMapper.writeValueAsString(body);

            String response = restClient.post()
                    .uri("/oauth2/tokenP")
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            accessToken = jsonNode.get("access_token").asText();
            log.info("[KIS Token] Token obtained for key: {}", appKey);
        } catch (Exception e) {
            log.error("[KIS Token] Error obtaining token: {}", e.getMessage());
        }
        return accessToken;
    }
}
