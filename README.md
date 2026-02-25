# AI Trading System

AI 기반 자동매매 시스템 (미장)

## 기술 스택

- **Backend**: Java 21, Spring Boot 3.2, JPA, Flyway, H2
- **Frontend**: React + TypeScript + Vite + MUI
- **AI Module**: Python + FastAPI (LightGBM + XGBoost + CatBoost 앙상블)
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

## 아키텍처 설계 결정

### 헥사고날 아키텍처 채택
- 외부 의존성(브로커 API, DB, AI 서버)이 도메인 로직에 침투하지 않도록 Port/Adapter 패턴 적용
- `BrokerApiPort` → `RoutingBrokerAdapter` → `KisBrokerClient`/`LsBrokerClient`로 브로커 교체가 자유로움
- 도메인 객체(`TradingTarget`, `Asset`)는 JPA/Spring 의존성 없는 순수 Java 클래스

### Rich Domain Model
- `TradingTarget`에 비즈니스 판단 로직 집중: 손절 판단, 트레일링 스톱 판단, 인버스 적용
- `TradingService`는 도메인 로직을 호출하는 오케스트레이터 역할만 수행
- 기본값 관리를 `@Builder.Default` 한 곳으로 통일하여 불일치 방지

### 멀티 브로커 라우팅
- `RoutingBrokerAdapter`가 사용자의 활성 브로커 설정에 따라 KIS/LS 클라이언트를 자동 선택
- 매매 대상별로 다른 브로커 지정 가능 (`brokerId`)
- 물리적 FK를 제거하고 논리적 참조로 전환하여 유연성 확보

### 스케줄러 기반 자동매매
- `@Scheduled` 단일 스레드로 매매 사이클 실행 → 동시성 이슈 원천 차단
- 1분 주기: 손절 → 트레일링 스톱 → AI 예측 → 주문 실행
- 2분 주기: 미체결 주문 자동 취소
- 5분 주기: 브로커 실제 보유수량과 DB 동기화

---

## 주요 개선 이력

### 전략 단일화
- 초기에는 scalping/momentum/swing 3개 전략 지원
- 실사용에서 scalping만 유효 → 나머지 전략 코드, DB 스키마, UI 모두 제거
- 불필요한 선택 분기 제거로 코드 복잡도 대폭 감소

### 프론트엔드 폴링 → 스케줄러 기반 예측
- 초기: 프론트엔드에서 주기적으로 예측 API 폴링
- 개선: 백엔드 스케줄러가 자동으로 예측 + 주문 실행 → 프론트엔드 부하 제거

### 네이밍 정규화
- `strategy` 패키지 → `trading` (실제 역할과 일치)
- `WatchlistItem` → `TradingTarget` (관심목록이 아닌 매매 대상)
- `/api/stocks` → `/api/trading-target` (REST 리소스명과 도메인 일치)

### 미체결 주문 관리 안정화
- 주문 후 `PendingOrder`로 추적 → 2분 후 자동 취소 시도
- 취소 성공/실패 무관하게 `PendingOrder` 항상 삭제하여 무한 재시도 방지

### Flyway 마이그레이션 전략
- 42개 마이그레이션으로 점진적 스키마 진화
- 파괴적 변경(테이블 리네임, 컬럼 삭제)도 마이그레이션으로 관리

---

## Changelog

[CHANGELOG.md](CHANGELOG.md) 참조

