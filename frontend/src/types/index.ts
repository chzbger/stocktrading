// API Response Types
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  code?: number;
}

// User & Auth Types
export interface User {
  id: number;
  username: string;
  status: 'PENDING' | 'ACTIVE' | 'SUSPENDED';
  role: 'ROLE_USER' | 'ROLE_ADMIN';
}

export interface LoginResponse {
  token: string;
  role: string;
}

export interface BrokerInfo {
  id: number;
  brokerType: 'KIS' | 'LS';
  accountNumber?: string;
}

export interface UserSettings {
  brokerInfos: BrokerInfo[];
  activeBrokerId: number | null;
  tradingStartTime?: string;
  tradingEndTime?: string;
  notificationEnabled?: boolean;
  telegramBotToken?: string;
  telegramChatId?: string;
}

// Stock & Trading Types
export interface Stock {
  id: number;
  ticker: string;
  isTrading: boolean;
  buyThreshold: number;
  sellThreshold: number;
  stopLossPercentage: string;
  baseTicker: string | null;
  isInverse: boolean;
  trailingStopPercentage: string;
  trailingStopEnabled: boolean;
  trailingWindowMinutes: number;
  brokerId: number | null;
  profitAtr: number;
  stopAtr: number;
  maxHolding: number;
  minThreshold: number;
  trainingPeriodYears: number;
  tuningTrials: number;
  // AI status (fetched separately)
  trainingStatus?: 'PENDING' | 'TRAINING' | 'COMPLETED' | 'FAILED';
}

export interface PredictionResult {
  prediction: 'BUY' | 'SELL' | 'HOLD';
  confidence: number;
  probabilities: number[];
  executed?: boolean;
  orderMessage?: string;
}

export interface PredictionItem {
  tradingTargetId: number;
  ticker: string;
  prediction: 'BUY' | 'SELL' | 'HOLD';
  confidence: number;
  probabilities: number[];
  executed: boolean;
  orderMessage: string | null;
}

export interface AiStatus {
  id?: number;
  ticker: string;
  trainDate?: string;
  status: string;
  message?: string;
}

export interface TradingLog {
  id: number;
  ticker: string;
  action: 'BUY' | 'SELL' | 'HOLD';
  price: number;
  timestamp: string;
  orderId?: string;
  status?: 'PENDING' | 'FILLED' | 'CLOSED' | 'CANCELLED' | 'FAILED';
}

// Alias for backwards compatibility
export type TradeLog = TradingLog;

export interface ProfitStats {
  realizedProfit: number;
}

export interface Asset {
  accountNo?: string;
  totalAsset: number;
  usdDeposit: number;
  ownedStocks: OwnedStock[];
}

export interface OwnedStock {
  stockCode: string;
  stockName: string;
  quantity: number;
  averagePrice: number;
  currentPrice: number;
  profitRate: number;
}

// Form Types
export interface ThresholdConfig {
  buy: number;
  sell: number;
}
