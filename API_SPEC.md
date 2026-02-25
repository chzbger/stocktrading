# API 명세

공통 응답 형식:
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "2026-02-25T10:00:00",
    "data": { ... }
}
```

---

### 인증

## 회원가입
#### 회원가입을 요청한다. 관리자 승인 후 로그인 가능.

### Request
**[POST]** `/api/auth/register`
#### Request Body
* `username` (String): 사용자명 (필수)
* `password` (String): 비밀번호 (필수)
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "message": "가입 신청이 완료되었습니다. 관리자 승인 후 로그인할 수 있습니다."
    }
}
```
* `message`: 결과 메시지 (String)

---

## 로그인
#### JWT 토큰을 발급받는다

### Request
**[POST]** `/api/auth/login`
#### Request Body
* `username` (String): 사용자명 (필수)
* `password` (String): 비밀번호 (필수)
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "token": "eyJhbGciOi...",
        "role": "USER"
    }
}
```
* `token`: JWT 토큰 (String)
* `role`: 사용자 역할 - `USER`, `ADMIN` (String)

---

## 사용자 목록 조회
#### 전체 사용자 목록을 조회한다 (관리자 전용)

### Request
**[GET]** `/api/auth/users`
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": [
        {
            "id": 1,
            "username": "user1",
            "status": "ACTIVE"
        }
    ]
}
```
* `id`: 사용자 ID (Number)
* `username`: 사용자명 (String)
* `status`: 사용자 상태 - `PENDING`, `ACTIVE` (String)

---

## 사용자 승인
#### 가입 대기 중인 사용자를 승인한다 (관리자 전용)

### Request
**[POST]** `/api/auth/users/{userId}/approve`
#### Path Parameter
* `userId` (Number): 사용자 ID
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "message": "사용자가 승인되었습니다."
    }
}
```

---

## 사용자 삭제
#### 사용자를 삭제한다 (관리자 전용)

### Request
**[DELETE]** `/api/auth/users/{userId}`
#### Path Parameter
* `userId` (Number): 사용자 ID
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "message": "사용자가 삭제되었습니다."
    }
}
```

---

### 설정

## 설정 조회
#### 사용자의 브로커, 매매시간, 알림 설정을 조회한다

### Request
**[GET]** `/api/settings`
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "activeBrokerId": 1,
        "brokerInfos": [
            {
                "id": 1,
                "brokerType": "KIS",
                "appKey": "...",
                "appSecret": "...",
                "accountNumber": "..."
            }
        ],
        "tradingStartTime": "22:30:00",
        "tradingEndTime": "05:00:00",
        "notificationEnabled": true,
        "telegramBotToken": "...",
        "telegramChatId": "..."
    }
}
```
* `activeBrokerId`: 활성 브로커 ID (Number)
* `brokerInfos`: 등록된 브로커 목록 (Array)
* `tradingStartTime`: 매매 시작 시각 (String, HH:mm:ss)
* `tradingEndTime`: 매매 종료 시각 (String, HH:mm:ss)
* `notificationEnabled`: 텔레그램 알림 활성화 여부 (Boolean)
* `telegramBotToken`: 텔레그램 봇 토큰 (String)
* `telegramChatId`: 텔레그램 채팅 ID (String)

---

## 브로커 추가
#### 증권사 정보를 추가한다

### Request
**[POST]** `/api/settings/brokers`
#### Request Body
* `brokerType` (String): 증권사 유형 - `KIS`, `LS` (필수)
* `appKey` (String): API 앱 키 (필수)
* `appSecret` (String): API 앱 시크릿 (필수)
* `accountNumber` (String): 계좌번호 (필수)
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "message": "증권사 정보가 추가되었습니다."
    }
}
```

---

## 브로커 삭제
#### 등록된 증권사 정보를 삭제한다

### Request
**[DELETE]** `/api/settings/brokers/{id}`
#### Path Parameter
* `id` (Number): 브로커 정보 ID
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "message": "증권사 정보가 삭제되었습니다."
    }
}
```

---

## 활성 브로커 변경
#### 매매에 사용할 활성 브로커를 변경한다

### Request
**[POST]** `/api/settings/active-broker`
#### Request Body
* `brokerInfoId` (Number): 브로커 정보 ID (필수)
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "message": "활성 증권사가 변경되었습니다."
    }
}
```

---

## 매매 시간 설정
#### 자동매매 시간대를 설정한다

### Request
**[POST]** `/api/settings/trading-hours`
#### Request Body
* `startTime` (String): 매매 시작 시각, HH:mm:ss (필수)
* `endTime` (String): 매매 종료 시각, HH:mm:ss (필수)
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "message": "매매 시간이 설정되었습니다."
    }
}
```

---

## 알림 설정
#### 텔레그램 알림 설정을 변경한다

### Request
**[POST]** `/api/settings/notification`
#### Request Body
* `enabled` (Boolean): 알림 활성화 여부 (필수)
* `botToken` (String): 텔레그램 봇 토큰
* `chatId` (String): 텔레그램 채팅 ID
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "message": "알림 설정이 저장되었습니다."
    }
}
```

---

### 매매 대상

## 매매 대상 목록 조회
#### 사용자의 매매 대상 목록을 조회한다

### Request
**[GET]** `/api/trading-target`
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": [
        {
            "id": 1,
            "ticker": "NVDA",
            "isTrading": true,
            "buyThreshold": 10,
            "sellThreshold": 10,
            "stopLossPercentage": "3.0",
            "baseTicker": null,
            "isInverse": false,
            "trailingStopPercentage": "2.0",
            "trailingStopEnabled": true,
            "trailingWindowMinutes": 10,
            "brokerId": 1,
            "holdingQuantity": 5
        }
    ]
}
```
* `id`: 매매 대상 ID (Number)
* `ticker`: 종목 티커 (String)
* `isTrading`: 자동매매 활성화 여부 (Boolean)
* `buyThreshold`: 매수 신뢰도 임계값 (Number)
* `sellThreshold`: 매도 신뢰도 임계값 (Number)
* `stopLossPercentage`: 손절 비율 % (String)
* `baseTicker`: 예측 기준 티커, null이면 ticker 사용 (String)
* `isInverse`: 인버스 매매 여부 (Boolean)
* `trailingStopPercentage`: 트레일링 스톱 비율 % (String)
* `trailingStopEnabled`: 트레일링 스톱 활성화 여부 (Boolean)
* `trailingWindowMinutes`: 트레일링 스톱 윈도우 (분) (Number)
* `brokerId`: 브로커 ID (Number)
* `holdingQuantity`: 보유 수량 (Number)

---

## 매매 대상 추가
#### 매매 대상 종목을 추가한다

### Request
**[POST]** `/api/trading-target`
#### Request Body
* `ticker` (String): 종목 티커 (필수)
* `brokerId` (Number): 브로커 ID (필수)
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "id": 1,
        "ticker": "NVDA",
        "isTrading": false,
        "buyThreshold": 10,
        "sellThreshold": 10,
        "stopLossPercentage": "3.0",
        "baseTicker": null,
        "isInverse": false,
        "trailingStopPercentage": "2.0",
        "trailingStopEnabled": true,
        "trailingWindowMinutes": 10,
        "brokerId": 1,
        "holdingQuantity": 0
    }
}
```

---

## 매매 대상 삭제
#### 매매 대상 종목을 삭제한다

### Request
**[DELETE]** `/api/trading-target/{id}`
#### Path Parameter
* `id` (Number): 매매 대상 ID
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": null
}
```

---

## 매매 대상 설정 수정
#### 매매 대상의 임계값, 손절, 트레일링 스톱 등 설정을 수정한다

### Request
**[PUT]** `/api/trading-target/{id}`
#### Path Parameter
* `id` (Number): 매매 대상 ID
#### Request Body
* `buyThreshold` (Number): 매수 신뢰도 임계값 (필수)
* `sellThreshold` (Number): 매도 신뢰도 임계값 (필수)
* `stopLossPercentage` (String): 손절 비율 % (필수)
* `baseTicker` (String): 예측 기준 티커
* `isInverse` (Boolean): 인버스 매매 여부 (필수)
* `trailingStopPercentage` (String): 트레일링 스톱 비율 % (필수)
* `trailingStopEnabled` (Boolean): 트레일링 스톱 활성화 여부 (필수)
* `trailingWindowMinutes` (Number): 트레일링 스톱 윈도우 (분) (필수)
* `brokerId` (Number): 브로커 ID (필수)
* `holdingQuantity` (Number): 보유 수량 (필수)
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "id": 1,
        "ticker": "NVDA",
        "isTrading": true,
        "buyThreshold": 15,
        "sellThreshold": 12,
        "stopLossPercentage": "3.0",
        "baseTicker": "QQQ",
        "isInverse": false,
        "trailingStopPercentage": "2.0",
        "trailingStopEnabled": true,
        "trailingWindowMinutes": 10,
        "brokerId": 1,
        "holdingQuantity": 5
    }
}
```

---

## 자동매매 ON/OFF
#### 매매 대상의 자동매매를 활성화/비활성화한다

### Request
**[PATCH]** `/api/trading-target/{ticker}/trading?active={active}`
#### Path Parameter
* `ticker` (String): 종목 티커
#### Query Parameter
* `active` (Boolean): 활성화 여부 (필수)
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "ticker": "NVDA",
        "isTrading": true
    }
}
```
* `ticker`: 종목 티커 (String)
* `isTrading`: 변경된 자동매매 상태 (Boolean)

---

### AI 모델

## AI 학습 시작
#### 종목의 AI 모델 학습을 시작한다

### Request
**[POST]** `/api/ai/{ticker}/train`
#### Path Parameter
* `ticker` (String): 종목 티커
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": null
}
```

---

## AI 학습 초기화
#### 종목의 학습 이력을 삭제한다

### Request
**[DELETE]** `/api/ai/{ticker}/train`
#### Path Parameter
* `ticker` (String): 종목 티커
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": null
}
```

---

## AI 학습 로그 조회
#### 종목의 학습 로그를 조회한다

### Request
**[GET]** `/api/ai/{ticker}/logs`
#### Path Parameter
* `ticker` (String): 종목 티커
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": "Training started...\nEpoch 1/10..."
}
```
* `data`: 학습 로그 텍스트 (String)

---

## AI 학습 상태 확인 및 갱신
#### Python 서버의 실제 학습 상태를 확인하고 DB에 반영한다

### Request
**[POST]** `/api/ai/{ticker}/training-status`
#### Path Parameter
* `ticker` (String): 종목 티커
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "id": 1,
        "ticker": "NVDA",
        "trainDate": "2026-02-25",
        "userId": 1,
        "status": "COMPLETED",
        "message": null
    }
}
```
* `id`: 학습 이력 ID (Number)
* `ticker`: 종목 티커 (String)
* `trainDate`: 학습 일자 (String)
* `userId`: 사용자 ID (Number)
* `status`: 학습 상태 - `PENDING`, `TRAINING`, `COMPLETED`, `FAILED` (String)
* `message`: 상태 메시지 (String)

---

### 자산

## 계좌 자산 조회
#### 브로커 계좌의 자산 현황을 조회한다

### Request
**[GET]** `/api/asset`
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "accountNo": "12345678",
        "totalAsset": 50000.00,
        "usdDeposit": 10000.00,
        "ownedStocks": [
            {
                "stockCode": "NVDA",
                "stockName": "NVIDIA Corp",
                "quantity": 5,
                "averagePrice": 120.50,
                "currentPrice": 135.20,
                "profitRate": 12.20
            }
        ]
    }
}
```
* `accountNo`: 계좌번호 (String)
* `totalAsset`: 총 자산 (Number)
* `usdDeposit`: USD 예수금 (Number)
* `ownedStocks`: 보유 종목 목록 (Array)
  * `stockCode`: 종목 코드 (String)
  * `stockName`: 종목명 (String)
  * `quantity`: 보유 수량 (Number)
  * `averagePrice`: 평균 매수가 (Number)
  * `currentPrice`: 현재가 (Number)
  * `profitRate`: 수익률 % (Number)

---

### 거래 로그

## 최근 거래 로그 조회
#### 최근 거래 로그를 조회한다

### Request
**[GET]** `/api/trade-log/recent`
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": [
        {
            "id": 1,
            "userId": 1,
            "ticker": "NVDA",
            "action": "BUY",
            "price": 135.20,
            "profitRate": null,
            "timestamp": "2026-02-25T10:30:00+09:00",
            "status": "SUCCESS"
        }
    ]
}
```
* `id`: 거래 로그 ID (Number)
* `userId`: 사용자 ID (Number)
* `ticker`: 종목 티커 (String)
* `action`: 주문 유형 - `BUY`, `SELL` (String)
* `price`: 주문 가격 (Number)
* `profitRate`: 수익률 (Number, 매도 시에만)
* `timestamp`: 거래 시각 (String)
* `status`: 주문 상태 - `SUCCESS`, `INSUFFICIENT_BALANCE`, `INSUFFICIENT_STOCK`, `FAILED` (String)

---

## 수익 통계 조회
#### 실현 손익 통계를 조회한다

### Request
**[GET]** `/api/trade-log/stats`
### Response
```json
{
    "status": 200,
    "message": "Success",
    "timestamp": "...",
    "data": {
        "realizedProfit": 1250.50
    }
}
```
* `realizedProfit`: 실현 손익 (Number)
