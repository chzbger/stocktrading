# AI Trading System

AI 기반 자동매매 시스템 (미장)

## 기술 스택

- **Backend**: Java 21, Spring Boot 3.2, JPA, Flyway, H2
- **Frontend**: React + TypeScript + Vite + MUI
- **AI Module**: Python + FastAPI
- **Broker**: 한국투자증권(KIS) / LS증권 API
- **알림**: 텔레그램

## 프로젝트 구조

```
src/main/java/com/example/stocktrading/
├── common/                              # 공통 모듈
│   ├── error/                           # GlobalExceptionHandler
│   └── security/                        # JWT 인증, AuthContext
├── trading/                             # 매매 모듈
│   ├── adapter/
│   │   ├── in/web/                      # Controller (TradingTarget, AiModel, Asset, TradeLog)
│   │   ├── in/scheduler/                # TradingScheduler (1분 주기 자동매매)
│   │   └── out/
│   │       ├── persistence/             # JPA Entity, Repository, PersistenceAdapter
│   │       ├── broker/                  # KisBrokerClient, LsBrokerClient, RoutingBrokerAdapter
│   │       └── ai/                      # AiModelAdapter (Python 추론 서버 연동)
│   ├── application/
│   │   ├── port/in/                     # UseCase 인터페이스
│   │   ├── port/out/                    # Port 인터페이스 (BrokerApiPort, AiModelPort 등)
│   │   └── service/                     # 비즈니스 로직 (TradingService, AiModelService 등)
│   └── domain/                          # 도메인 객체 (TradingTarget, Asset, StockOrder 등)
├── user/                                # 사용자 모듈
│   ├── adapter/
│   │   ├── in/web/                      # AuthController, SettingsController
│   │   └── out/
│   │       ├── persistence/             # UserEntity, BrokerInfoEntity
│   │       ├── notification/            # TelegramNotificationAdapter
│   │       └── security/                # JwtTokenProvider
│   ├── application/
│   │   ├── port/in/                     # UserUseCase
│   │   ├── port/out/                    # UserPort, NotificationPort
│   │   └── service/                     # UserService
│   └── domain/                          # User, BrokerInfo, BrokerType
└── StockTradingApplication.java

frontend/src/
├── pages/                               # DashboardPage
├── components/                          # SettingsModal 등
└── types/                               # TypeScript 인터페이스

ai_module/
├── ....py                               # 학습, 추론 등
```

---

## 실행 방법

### Backend (port 8080)
```bash
./gradlew bootRun
```

### Frontend (port 5173)
```bash
cd frontend
npm install
npm run dev
```

### AI Module (port 8000)
```bash
cd ai_module
pip install -r requirements.txt
uvicorn inference_server:app --host 0.0.0.0 --port 8000
```

---

## AI Module

Python 기반 머신러닝 모듈. 데이터로 모델을 학습하고, 실시간 데이터로 매수매도를 예측.
- AI 상세 이력 문서: `ai_module/AI_README.md` (이 파일은 별도 관리)
- ai_module의 학습,추론 소스는 업로드 하지 않음

---

## Core Workflow

1. 회원가입 및 관리자 승인: `POST /api/auth/register` → `POST /api/auth/users/{id}/approve`
2. 로그인: `POST /api/auth/login` → JWT 토큰 발급
3. 증권사 등록: `POST /api/settings/brokers`
4. 매매 대상 추가: `POST /api/trading-target`
5. AI 학습: `POST /api/ai/{ticker}/train` → 상태 확인: `POST /api/ai/{ticker}/training-status`
6. 자동매매 ON: `PATCH /api/trading-target/{ticker}/trading?active=true`
7. 스케줄러가 1분 주기로 자동매매 실행 (손절 → 예측 → 주문)

---

## API 명세

[API_SPEC.md](API_SPEC.md) 참조

---


