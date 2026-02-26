package com.example.stocktrading.trading.adapter.out.broker;

import com.example.stocktrading.trading.application.port.out.BrokerApiPort;
import com.example.stocktrading.trading.application.port.out.BrokerClient;
import com.example.stocktrading.trading.application.port.out.ExchangeCodePort;
import com.example.stocktrading.trading.domain.*;
import com.example.stocktrading.trading.domain.StockOrder.OrderType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Component
public class KisBrokerClient implements BrokerClient {

    private final RestClient restClient;
    private final TokenManager tokenManager;
    private final ObjectMapper objectMapper;
    private final ExchangeCodePort exchangeCodePort;

    public KisBrokerClient(@Qualifier("kisRestClient") RestClient restClient,
            TokenManager tokenManager,
            ObjectMapper objectMapper,
            ExchangeCodePort exchangeCodePort) {
        this.restClient = restClient;
        this.tokenManager = tokenManager;
        this.objectMapper = objectMapper;
        this.exchangeCodePort = exchangeCodePort;
    }

    private String getPriceExchangeCode(String ticker) {
        return exchangeCodePort.findByTicker(ticker.toUpperCase())
                .map(ExchangeCodePort.ExchangeCode::kisPriceCode)
                .orElse("NAS");
    }

    private String getStockOrderExchangeCode(String ticker) {
        return exchangeCodePort.findByTicker(ticker.toUpperCase())
                .map(ExchangeCodePort.ExchangeCode::kisOrderCode)
                .orElse("NASD");
    }

    @Override
    public BrokerApiPort.OrderResult sendOrder(BrokerContext ctx, StockOrder stockOrder) {
        log.info("[KIS] Sending stockOrder: {}", stockOrder);

        try {
            String token = tokenManager.getAccessToken(ctx.getAppKey(), ctx.getAppSecret());
            String trId;
            String sllType = null;
            if (stockOrder.getType() == OrderType.BUY) {
                trId = "TTTT1002U"; // 미국 매수 주문
            } else {
                trId = "TTTT1006U"; // 미국 매도 주문
                sllType = "00";
            }
            String exchCd = getStockOrderExchangeCode(stockOrder.getTicker());

            Map<String, String> body = new HashMap<>();
            body.put("CANO", ctx.getCano());
            body.put("ACNT_PRDT_CD", ctx.getAcntPrdtCd());
            body.put("OVRS_EXCG_CD", exchCd);
            body.put("PDNO", stockOrder.getTicker());
            body.put("ORD_QTY", String.valueOf(stockOrder.getQuantity()));
            BigDecimal roundedPrice = stockOrder.getPrice().setScale(2, RoundingMode.HALF_UP);
            body.put("OVRS_ORD_UNPR", roundedPrice.toPlainString());
            body.put("ORD_SVR_DVSN_CD", "0");
            body.put("ORD_DVSN", "00");
            if (sllType != null) {
                body.put("SLL_TYPE", sllType);
            }

            String jsonBody = objectMapper.writeValueAsString(body);
            String hashKey = getHashKey(ctx.getAppKey(), ctx.getAppSecret(), jsonBody);

            log.info("[KIS] StockOrder request - ticker={}, exchCd={}, trId={}", stockOrder.getTicker(), exchCd, trId);

            String response = restClient.post()
                    .uri("/uapi/overseas-stock/v1/trading/order")
                    .headers(defaultHeaders(token, ctx))
                    .header("tr_id", trId)
                    .header("custtype", "P")
                    .header("hashkey", hashKey != null ? hashKey : "")
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                String rtCd = root.path("rt_cd").asText();
                String msgCd = root.path("msg_cd").asText();
                String msg1 = root.path("msg1").asText();

                if ("0".equals(rtCd)) {
                    String odno = root.path("output").path("ODNO").asText(null);
                    log.info("[KIS] StockOrder success, orderId={}", odno);
                    return new BrokerApiPort.OrderResult(true, TradeLog.OrderStatus.SUCCESS, null, odno);
                }

                log.error("[KIS] StockOrder failed: rt_cd={}, msg_cd={}, msg={}", rtCd, msgCd, msg1);
                if (msgCd.contains("4033") || msg1.contains("잔고")) {
                    return new BrokerApiPort.OrderResult(false, TradeLog.OrderStatus.INSUFFICIENT_BALANCE, msg1);
                } else if (msgCd.contains("4035") || msg1.contains("보유")) {
                    return new BrokerApiPort.OrderResult(false, TradeLog.OrderStatus.INSUFFICIENT_STOCK, msg1);
                }
            }

        } catch (Exception e) {
            log.error("[KIS] StockOrder failed", e);
        }
        return new BrokerApiPort.OrderResult(false, TradeLog.OrderStatus.FAILED, "StockOrder failed");
    }

    @Override
    public BigDecimal getCurrentPrice(BrokerContext ctx, String ticker) {
        // 해외주식 현재체결가
        // https://apiportal.koreainvestment.com/apiservice-apiservice?/uapi/overseas-price/v1/quotations/price
        try {
            String token = tokenManager.getAccessToken(ctx.getAppKey(), ctx.getAppSecret());
            String exchCd = getPriceExchangeCode(ticker);

            String response = restClient.get()
                    .uri("/uapi/overseas-price/v1/quotations/price" +
                            "?AUTH=&EXCD={excd}&SYMB={symb}",
                            exchCd, ticker)
                    .headers(defaultHeaders(token, ctx))
                    .header("tr_id", "HHDFS00000300")
                    .header("custtype", "P")
                    .retrieve()
                    .body(String.class);

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                String priceStr = root.path("output").path("last").asText("0");
                BigDecimal price = new BigDecimal(priceStr);

                log.debug("[KIS] Price - ticker={}, 거래소={}, 현재가={}", ticker, exchCd, price);
                return price;
            }
        } catch (Exception e) {
            log.warn("[KIS] Price fetch failed for {}", ticker);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public List<StockCandle> getRecentCandles(BrokerContext ctx, String ticker, int limit) {
        return fetchStockCandles(ctx, ticker, limit, 1);
    }

    @Override
    public List<StockCandle> getRecentCandles5Min(BrokerContext ctx, String ticker, int limit) {
        return fetchStockCandles(ctx, ticker, limit, 5);
    }

    private List<StockCandle> fetchStockCandles(BrokerContext ctx, String ticker, int limit, int nmin) {
        // 해외주식분봉조회
        // https://apiportal.koreainvestment.com/apiservice-apiservice?/uapi/overseas-price/v1/quotations/inquire-time-itemchartprice
        try {
            String token = tokenManager.getAccessToken(ctx.getAppKey(), ctx.getAppSecret());
            String exchCd = getPriceExchangeCode(ticker);

            String response = restClient.get()
                    .uri("/uapi/overseas-price/v1/quotations/inquire-time-itemchartprice" +
                            "?AUTH=&EXCD={excd}&SYMB={symb}&NMIN={nmin}&NREC={limit}" +
                            "&PINC=1&NEXT=&FILL=&KEYB=",
                            exchCd, ticker, nmin, limit)
                    .headers(defaultHeaders(token, ctx))
                    .header("tr_id", "HHDFS76950200")
                    .header("custtype", "P")
                    .retrieve()
                    .body(String.class);

            if (response != null) {
                List<StockCandle> stockCandles = new ArrayList<>();
                JsonNode root = objectMapper.readTree(response);
                JsonNode output2 = root.path("output2");
                if (output2.isArray()) {
                    for (JsonNode node : output2) {
                        String ymd = node.path("kymd").asText();
                        String hms = node.path("khms").asText();
                        if (ymd.isEmpty()) {
                            ymd = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                        }

                        LocalDateTime localTs = LocalDateTime.parse(ymd + hms,
                                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                        ZonedDateTime ts = ZonedDateTime.of(localTs, ZoneId.of("Asia/Seoul"));

                        stockCandles.add(0, StockCandle.builder()
                                .timestamp(ts)
                                .open(new BigDecimal(node.path("open").asText("0")))
                                .high(new BigDecimal(node.path("high").asText("0")))
                                .low(new BigDecimal(node.path("low").asText("0")))
                                .close(new BigDecimal(node.path("last").asText("0")))
                                .volume(new BigDecimal(node.path("evol").asText("0")))
                                .build());
                    }
                }
                return stockCandles;
            }
        } catch (Exception e) {
            log.error("[KIS] StockCandle fetch failed for " + ticker + " (" + nmin + "min)", e);
        }
        return new ArrayList<>();
    }

    @Override
    public Asset getAccountAsset(BrokerContext ctx) {
        List<Asset.OwnedStock> ownedStocks = new ArrayList<>();

        try {
            String token = tokenManager.getAccessToken(ctx.getAppKey(), ctx.getAppSecret());

            // 해외주식 체결기준현재잔고
            // https://apiportal.koreainvestment.com/apiservice-apiservice?/uapi/overseas-stock/v1/trading/inquire-present-balance
            String resBalance = restClient.get()
                    .uri("/uapi/overseas-stock/v1/trading/inquire-present-balance" +
                            "?CANO={cano}&ACNT_PRDT_CD={acnt}" +
                            "&WCRC_FRCR_DVSN_CD=02" + // 외화
                            "&NATN_CD=000" + // 전체
                            "&TR_MKET_CD=00" + // 전체
                            "&INQR_DVSN_CD=00", // 전체
                            ctx.getCano(), ctx.getAcntPrdtCd())
                    .headers(defaultHeaders(token, ctx))
                    .header("tr_id", "CTRP6504R")
                    .header("custtype", "P")
                    .retrieve()
                    .body(String.class);

            if (resBalance != null) {
                JsonNode root = objectMapper.readTree(resBalance);
                JsonNode output1 = root.path("output1");
                if (output1.isArray()) {
                    for (JsonNode node : output1) {
                        int qty = (int) Double.parseDouble(node.path("ccld_qty_smtl1").asText("0"));
                        if (qty > 0) {
                            String stockCode = node.path("pdno").asText();
                            BigDecimal avgPrice = new BigDecimal(node.path("avg_unpr3").asText("0"));
                            BigDecimal curPrice = new BigDecimal(node.path("ovrs_now_pric1").asText("0"));

                            log.info("[KIS] Asset - ticker={}, 수량={}, 매수평단={}, 현재가={}", stockCode, qty, avgPrice,
                                    curPrice);

                            ownedStocks.add(Asset.OwnedStock.builder()
                                    .stockCode(stockCode)
                                    .stockName(node.path("ovrs_item_name").asText())
                                    .quantity(qty)
                                    .averagePrice(avgPrice)
                                    .currentPrice(curPrice)
                                    .profitRate(new BigDecimal(node.path("evlu_pfls_rt").asText("0")))
                                    .build());
                        }
                    }
                }
            }

            // 해외주식 매수가능금액조회
            // https://apiportal.koreainvestment.com/apiservice-apiservice?/uapi/overseas-stock/v1/trading/inquire-psamount
            BigDecimal usdDeposit = BigDecimal.ZERO;
            String resPsamount = restClient.get()
                    .uri("/uapi/overseas-stock/v1/trading/inquire-psamount" +
                            "?CANO={cano}&ACNT_PRDT_CD={acnt}" +
                            "&OVRS_EXCG_CD=NASD&OVRS_ORD_UNPR=23.8&ITEM_CD=AAPL",
                            ctx.getCano(), ctx.getAcntPrdtCd())
                    .headers(defaultHeaders(token, ctx))
                    .header("tr_id", "TTTS3007R")
                    .header("custtype", "P")
                    .retrieve()
                    .body(String.class);

            if (resPsamount != null) {
                JsonNode root = objectMapper.readTree(resPsamount);
                log.info("[KIS] Asset2 - 주문가능외화금액={}, 해외주문가능금액={}, 거래통화코드={}",
                        root.path("output").path("ord_psbl_frcr_amt").asText(),
                        root.path("output").path("ovrs_ord_psbl_amt").asText(),
                        root.path("output").path("tr_crcy_cd").asText());
                String usdBal = root.path("output").path("ord_psbl_frcr_amt").asText(); // 주문가능외화금액
                if (usdBal == null || usdBal.isEmpty() || "0.00".equals(usdBal)) {
                    usdBal = root.path("output").path("ovrs_ord_psbl_amt").asText(); // 해외주문가능금액
                }
                if (usdBal != null && !usdBal.isEmpty()) {
                    usdDeposit = new BigDecimal(usdBal);
                }
            }

            BigDecimal stockValKrw = BigDecimal.ZERO;
            for (Asset.OwnedStock s : ownedStocks) {
                stockValKrw = stockValKrw.add(
                        s.getCurrentPrice()
                                .multiply(new BigDecimal(s.getQuantity()))
                // .multiply(exchangeRate)
                );
            }

            BigDecimal totalAssetVal = usdDeposit
                    // .multiply(exchangeRate)
                    .add(stockValKrw);

            return Asset.builder()
                    .accountNo(ctx.getAccountNo())
                    .totalAsset(totalAssetVal)
                    .usdDeposit(usdDeposit)
                    .ownedStocks(ownedStocks)
                    .build();

        } catch (Exception e) {
            log.error("[KIS] Asset fetch failed", e);
            return Asset.builder().totalAsset(BigDecimal.ZERO).ownedStocks(new ArrayList<>()).build();
        }
    }

    private Consumer<HttpHeaders> defaultHeaders(String token, BrokerContext ctx) {
        return headers -> {
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authorization", "Bearer " + token);
            headers.set("appkey", ctx.getAppKey());
            headers.set("appsecret", ctx.getAppSecret());
        };
    }

    private String getHashKey(String appKey, String appSecret, String jsonBody) {
        try {
            String response = restClient.post()
                    .uri("/uapi/hashkey")
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);

            if (response != null) {
                JsonNode node = objectMapper.readTree(response);
                return node.path("HASH").asText();
            }
        } catch (Exception e) {
            log.error("[KIS] HashKey generation failed", e);
        }
        return null;
    }

    @Override
    public BrokerApiPort.CancelResult cancelOrder(BrokerContext ctx, String orderId) {
        // 해외주식 주문정정취소
        // https://apiportal.koreainvestment.com/apiservice-apiservice?/uapi/overseas-stock/v1/trading/order-rvsecncl
        try {
            String token = tokenManager.getAccessToken(ctx.getAppKey(), ctx.getAppSecret());

            Map<String, String> body = new HashMap<>();
            body.put("CANO", ctx.getCano());
            body.put("ACNT_PRDT_CD", ctx.getAcntPrdtCd());
            body.put("OVRS_EXCG_CD", "NASD");  // 기본 NASDAQ, 필요 시 주문 조회에서 거래소 받아와야 함
            body.put("PDNO", "");               // 종목코드 (취소 시 필수 아님)
            body.put("ORGN_ODNO", orderId);     // 원주문번호
            body.put("RVSE_CNCL_DVSN_CD", "02"); // 정정취소구분: 02=취소
            body.put("ORD_QTY", "0");           // 0 = 전량 취소
            body.put("OVRS_ORD_UNPR", "0");     // 취소 시 0
            body.put("ORD_SVR_DVSN_CD", "0");

            String jsonBody = objectMapper.writeValueAsString(body);
            String hashKey = getHashKey(ctx.getAppKey(), ctx.getAppSecret(), jsonBody);

            log.info("[KIS] cancelOrder request - orderId={}", orderId);

            String response = restClient.post()
                    .uri("/uapi/overseas-stock/v1/trading/order-rvsecncl")
                    .headers(defaultHeaders(token, ctx))
                    .header("tr_id", "TTTT1004U")
                    .header("custtype", "P")
                    .header("hashkey", hashKey != null ? hashKey : "")
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                String rtCd = root.path("rt_cd").asText();
                String msg1 = root.path("msg1").asText();

                if ("0".equals(rtCd)) {
                    log.info("[KIS] cancelOrder success: orderId={}", orderId);
                    return new BrokerApiPort.CancelResult(true, "Cancelled successfully");
                }

                log.warn("[KIS] cancelOrder failed: orderId={}, rt_cd={}, msg={}", orderId, rtCd, msg1);
                return new BrokerApiPort.CancelResult(false, msg1);
            }
        } catch (Exception e) {
            log.error("[KIS] cancelOrder failed: orderId={}", orderId, e);
        }
        return new BrokerApiPort.CancelResult(false, "Cancel request failed");
    }
}
