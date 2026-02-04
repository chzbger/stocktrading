# Stock Trading System

미국 주식 자동매매 시스템. AI 기반 매매 신호 예측 및 한국투자증권 API 연동.

## 기술 스택

- **Backend**: Spring Boot 3.2, Java 21, H2 Database, Flyway
- **Frontend**: React, TypeScript, Vite
- **AI Module**: Python, 머신러닝, FastAPI

## 실행 방법

```bash
# Backend (port 8080)
./gradlew bootRun

# Frontend (port 5173)
cd frontend && npm run dev

```

## AI Module

Python 기반 머신러닝 모듈. 과거 데이터로 모델을 학습하고, 실시간 데이터로 매수매도를 예측.

---

## 화면 (Frontend)

| Path | 설명 |
|------|------|
| `/login` | 로그인 |
| `/signup` | 회원가입 (관리자 승인 필요) |
| `/` | 대시보드 - 종목 관리, 자산 현황, 거래 내역 |
| `/users` | 사용자 관리 (관리자 전용) |

---

## API Reference

모든 API는 JSON 형식. 인증이 필요한 API는 `Authorization: Bearer {token}` 헤더 필요.

---

### 인증 (`/api/auth`)

#### 회원가입
신규 사용자 등록. 관리자 승인 후 로그인 가능.

`POST /api/auth/register`

**Request Body**
```json
{
  "username": "user1",
  "password": "password123"
}
```

**Response**
```json
{
  "success": true,
  "data": {
    "message": "가입 신청이 완료되었습니다. 관리자 승인 후 로그인할 수 있습니다."
  }
}
```

---

#### 로그인
사용자 인증 후 JWT 토큰 발급.

`POST /api/auth/login`

**Request Body**
```json
{
  "username": "user1",
  "password": "password123"
}
```

**Response**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "role": "USER"
  }
}
```

---

#### 사용자 목록 조회
전체 사용자 목록 조회. 관리자 전용.

`GET /api/auth/users`

**Response**
```json
{
  "success": true,
  "data": [
    { "id": 1, "username": "admin", "status": "APPROVED" },
    { "id": 2, "username": "user1", "status": "PENDING" }
  ]
}
```

---

#### 사용자 승인
가입 대기 중인 사용자 승인. 관리자 전용.

`POST /api/auth/users/{userId}/approve`

**Response**
```json
{
  "success": true,
  "data": {
    "message": "사용자가 승인되었습니다."
  }
}
```

---

#### 사용자 삭제
사용자 계정 삭제. 관리자 전용.

`DELETE /api/auth/users/{userId}`

**Response**
```json
{
  "success": true,
  "data": {
    "message": "사용자가 삭제되었습니다."
  }
}
```

---

### 설정 (`/api/settings`)

#### 설정 조회
현재 사용자의 증권사 정보 및 매매 시간 설정 조회.

`GET /api/settings`

**Response**
```json
{
  "success": true,
  "data": {
    "activeBrokerId": 1,
    "brokerInfos": [
      {
        "id": 1,
        "brokerType": "KIS",
        "appKey": "PSxx...",
        "accountNumber": "12345678-01"
      }
    ],
    "tradingStartTime": "22:30:00",
    "tradingEndTime": "05:00:00"
  }
}
```

---

#### 증권사 추가
새 증권사 API 정보 등록.

`POST /api/settings/brokers`

**Request Body**
```json
{
  "brokerType": "KIS",
  "appKey": "PSxxxxxxxx",
  "appSecret": "xxxxxxxxxxxxxxxx",
  "accountNumber": "12345678-01"
}
```

**Response**
```json
{
  "success": true,
  "data": {
    "message": "증권사 정보가 추가되었습니다."
  }
}
```

---

#### 증권사 삭제
등록된 증권사 정보 삭제.

`DELETE /api/settings/brokers/{id}`

**Response**
```json
{
  "success": true,
  "data": {
    "message": "증권사 정보가 삭제되었습니다."
  }
}
```

---

#### 활성 증권사 변경
매매에 사용할 증권사 선택.

`POST /api/settings/active-broker`

**Request Body**
```json
{
  "brokerInfoId": 1
}
```

**Response**
```json
{
  "success": true,
  "data": {
    "message": "활성 증권사가 변경되었습니다."
  }
}
```

---

#### 매매 시간 설정
자동매매 허용 시간대 설정. 미국 장 시간 기준.

`POST /api/settings/trading-hours`

**Request Body**
```json
{
  "startTime": "22:30:00",
  "endTime": "05:00:00"
}
```

**Response**
```json
{
  "success": true,
  "data": {
    "message": "매매 시간이 설정되었습니다."
  }
}
```

---

### 종목 관리 (`/api/stocks`)

#### 종목 목록 조회
워치리스트에 등록된 종목과 보유 현황 조회.

`GET /api/stocks`

**Response**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "ticker": "NVDA",
      "currentPrice": 142.50,
      "quantity": 10,
      "profitRate": 5.23,
      "isTrading": true,
      "buyThreshold": 60,
      "sellThreshold": 60,
      "stopLossPercentage": "3.0",
      "baseTicker": null,
      "isInverse": false,
      "trainingPeriodYears": 4
    }
  ]
}
```

---

#### 종목 추가
워치리스트에 새 종목 추가.

`POST /api/stocks`

**Request Body**
```json
{
  "ticker": "AAPL"
}
```

**Response**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "ticker": "AAPL",
    "currentPrice": 0,
    "quantity": 0,
    "profitRate": 0,
    "isTrading": false,
    "buyThreshold": 60,
    "sellThreshold": 60,
    "stopLossPercentage": "3.0",
    "baseTicker": null,
    "isInverse": false,
    "trainingPeriodYears": 4
  }
}
```

---

#### 종목 삭제
워치리스트에서 종목 제거.

`DELETE /api/stocks/{id}`

**Response**
```json
{
  "success": true,
  "data": null
}
```

---

#### 종목 설정 수정
매매 임계값, 손절률, 학습 기간 등 설정 변경. 변경할 필드만 전송.

`PATCH /api/stocks/{id}/thresholds`

**Request Body**
```json
{
  "buyThreshold": 70,
  "sellThreshold": 65,
  "stopLossPercentage": "5.0",
  "baseTicker": "SOXX",
  "isInverse": false,
  "trainingPeriodYears": 3
}
```

**Response**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "ticker": "SOXL",
    "buyThreshold": 70,
    "sellThreshold": 65,
    "stopLossPercentage": "5.0",
    "baseTicker": "SOXX",
    "isInverse": false,
    "trainingPeriodYears": 3
  }
}
```

---

#### 자동매매 ON/OFF
특정 종목의 자동매매 활성화/비활성화.

`PATCH /api/stocks/{ticker}/trading`

**Query Parameters**

| Name | Type | Description |
|------|------|-------------|
| active | boolean | true: 활성화, false: 비활성화 |

**Response**
```json
{
  "success": true,
  "data": {
    "ticker": "NVDA",
    "isTrading": true
  }
}
```

---

### AI 학습 (`/api/ai`)

#### 모델 학습 시작
해당 종목의 AI 모델 학습 시작. 백그라운드 실행.

`POST /api/ai/{ticker}/train`

**Response**
```json
{
  "success": true,
  "data": null
}
```

---

#### 학습 상태 초기화
학습 상태 및 모델 파일 삭제.

`DELETE /api/ai/{ticker}/train`

**Response**
```json
{
  "success": true,
  "data": null
}
```

---

#### 학습 로그 조회
학습 진행 중 또는 완료된 로그 조회.

`GET /api/ai/{ticker}/logs`

**Response**
```json
{
  "success": true,
  "data": "2024-01-15 10:30:00 - Training started...\n2024-01-15 10:35:00 - Epoch 1/10 completed..."
}
```

---

#### 학습 상태 조회
현재 학습 상태 및 정보 조회.

`GET /api/ai/{ticker}/status`

**Response**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "ticker": "NVDA",
    "trainDate": "2024-01-15",
    "status": "COMPLETED",
    "message": "Training completed successfully"
  }
}
```

---

### 거래 기록 (`/api/trade-log`)

#### 최근 거래 내역 조회
최근 거래 로그 목록 조회.

`GET /api/trade-log/recent`

**Response**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 1,
      "ticker": "NVDA",
      "action": "BUY",
      "price": 140.50,
      "profitRate": null,
      "timestamp": "2024-01-15T10:30:00+09:00",
      "status": "SUCCESS"
    },
    {
      "id": 2,
      "userId": 1,
      "ticker": "NVDA",
      "action": "SELL",
      "price": 145.00,
      "profitRate": 3,
      "timestamp": "2024-01-15T14:00:00+09:00",
      "status": "SUCCESS"
    }
  ]
}
```

---

#### 수익 통계 조회
실현 손익 합계 조회.

`GET /api/trade-log/stats`

**Response**
```json
{
  "success": true,
  "data": {
    "realizedProfit": 1250.50
  }
}
```

---

### 자산 (`/api/asset`)

#### 자산 현황 조회
계좌 총 자산, 예수금, 보유 종목 조회.

`GET /api/asset`

**Response**
```json
{
  "success": true,
  "data": {
    "accountNo": "12345678-01",
    "totalAsset": 15000.00,
    "usdDeposit": 5000.00,
    "ownedStocks": [
      {
        "stockCode": "NVDA",
        "stockName": "NVIDIA Corporation",
        "quantity": 10,
        "averagePrice": 135.00,
        "currentPrice": 142.50,
        "profitRate": 5.56
      }
    ]
  }
}
```

---

### 패키지 구조

```
com.example.stocktrading
├── auth/                          # 인증 모듈
│   ├── adapter/
│   │   ├── in/web/                # AuthController, SettingsController
│   │   └── out/
│   │       ├── persistence/       # UserRepository, BrokerInfoRepository
│   │       └── security/          # JwtTokenProvider
│   ├── application/
│   │   ├── port/in/               # UserUseCase
│   │   ├── port/out/              # UserPort, TokenPort
│   │   └── service/               # UserService
│   └── domain/                    # User, BrokerInfo
│
├── strategy/                      # 매매 전략 모듈
│   ├── adapter/
│   │   ├── in/
│   │   │   ├── web/               # StockController, AiModelController, ...
│   │   │   └── scheduler/         # TradingScheduler
│   │   └── out/
│   │       ├── persistence/       # Watchlist, TradeLog, ... Repository
│   │       ├── broker/            # KisBrokerClient, RoutingBrokerAdapter
│   │       └── ai/                # AiModelAdapter (→ Python)
│   ├── application/
│   │   ├── port/in/               # WatchlistUseCase, TradingUseCase, ...
│   │   ├── port/out/              # WatchlistPort, BrokerApiPort, AiModelPort, ...
│   │   └── service/               # WatchlistService, TradingService, ...
│   └── domain/                    # Asset, TradeLog, StockOrder
│
└── common/                        # 공통 모듈
    ├── ApiResponse                # 통일된 응답 형식
    ├── error/                     # GlobalExceptionHandler
    └── security/                  # AuthInterceptor, RequireAuth
```
