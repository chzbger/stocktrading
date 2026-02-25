import { useState, useEffect, useRef, useCallback, KeyboardEvent, ChangeEvent } from 'react'
import {
    Box,
    AppBar,
    Toolbar,
    Typography,
    IconButton,
    Container,
    Grid,
    Card,
    CardContent,
    TextField,
    Button,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    Chip,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Tooltip,
    Alert,
    FormControlLabel,
    Switch,
    Divider,
} from '@mui/material'
import {
    Settings,
    Logout,
    Add,
    PlayArrow,
    Psychology,
    TrendingUp,
    TrendingDown,
    WarningAmber,
    Article,
    Delete,
    Stop,
} from '@mui/icons-material'
import axios, { AxiosInstance, AxiosError } from 'axios'
import SettingsModal from '../components/SettingsModal'
import type { ApiResponse, Stock, TradingLog, ProfitStats, Asset, AiStatus, BrokerInfo, UserSettings } from '../types'

// Token expiry time (24 hours - common standard)
const TOKEN_EXPIRY_MS = 24 * 60 * 60 * 1000

interface DashboardPageProps {
    onLogout: () => void;
}

interface Thresholds {
    buy: number;
    sell: number;
    stopLoss: number;
    baseTicker: string;
    isInverse: boolean;
    trailingStop: number;
    trailingStopEnabled: boolean;
    trailingWindowMinutes: number;
}

interface RealizedProfitCardProps {
    value: number;
}

interface BalanceCardProps {
    label: string;
    value: number;
    isUSD: boolean;
}

/**
 * Check if token is expired based on stored timestamp
 */
const isTokenExpired = (): boolean => {
    const tokenTime = localStorage.getItem('tokenTime')
    if (!tokenTime) return true
    const elapsed = Date.now() - parseInt(tokenTime, 10)
    return elapsed > TOKEN_EXPIRY_MS
}

/**
 * Get auth headers for API requests
 */
const getAuthHeaders = () => ({
    Authorization: `Bearer ${localStorage.getItem('authToken')}`
})

function DashboardPage({ onLogout }: DashboardPageProps) {
    const [stocks, setStocks] = useState<Stock[]>([])
    const [logs, setLogs] = useState<TradingLog[]>([])
    const [profitStats, setProfitStats] = useState<ProfitStats>({ realizedProfit: 0 })
    const [asset, setAsset] = useState<Asset>({ totalAsset: 0, usdDeposit: 0, ownedStocks: [] })
    const [tickerInput, setTickerInput] = useState<string>('')
    const [brokerIdInput, setBrokerIdInput] = useState<string>('')
    const [brokerInfos, setBrokerInfos] = useState<BrokerInfo[]>([])
    const [settingsOpen, setSettingsOpen] = useState<boolean>(false)
    const [isBrokerConnected, setIsBrokerConnected] = useState<boolean>(true)

    // Log Modal State
    const [logModalOpen, setLogModalOpen] = useState<boolean>(false)
    const [logContent, setLogContent] = useState<string>('')
    const [selectedTicker, setSelectedTicker] = useState<string>('')

    // Threshold Modal State
    const [thresholdModalOpen, setThresholdModalOpen] = useState<boolean>(false)
    const [editingStock, setEditingStock] = useState<Stock | null>(null)
    const [thresholds, setThresholds] = useState<Thresholds>({ buy: 10, sell: 10, stopLoss: 3, baseTicker: '', isInverse: false, trailingStop: 2, trailingStopEnabled: true, trailingWindowMinutes: 10 })

    // Create axios instance with interceptor for token expiry
    const apiClient: AxiosInstance = axios.create()

    // Response interceptor for handling 401 errors (token expiry)
    useEffect(() => {
        const interceptor = apiClient.interceptors.response.use(
            (response) => response,
            (error: AxiosError) => {
                if (error.response?.status === 401) {
                    console.warn('Token expired or invalid, logging out...')
                    localStorage.removeItem('authToken')
                    localStorage.removeItem('tokenTime')
                    localStorage.removeItem('userRole')
                    onLogout()
                }
                return Promise.reject(error)
            }
        )
        return () => { apiClient.interceptors.response.eject(interceptor) }
    }, [onLogout])

    useEffect(() => {
        // Check token expiry on mount
        if (isTokenExpired()) {
            console.warn('Token expired on mount, logging out...')
            onLogout()
            return
        }

        checkBrokerConnection()
        fetchStocks()
        fetchProfitStats()
        fetchAsset()
        fetchLogs()

        const interval = setInterval(() => {
            // Check token expiry periodically
            if (isTokenExpired()) {
                onLogout()
                return
            }
            fetchLogs()
            fetchStocks()
            fetchAsset()
        }, 20000)
        return () => clearInterval(interval)
    }, [onLogout])

    const checkBrokerConnection = async (): Promise<void> => {
        try {
            const response = await apiClient.get<ApiResponse<UserSettings>>('/api/settings', {
                headers: getAuthHeaders(),
            })
            const { activeBrokerId, brokerInfos: infos } = response.data.data
            setIsBrokerConnected(!!activeBrokerId)
            setBrokerInfos(infos || [])
            if (infos && infos.length > 0 && !brokerIdInput) {
                setBrokerIdInput(String(activeBrokerId || infos[0].id))
            }
        } catch (err) {
            setIsBrokerConnected(false)
        }
    }

    const fetchLogs = async (): Promise<void> => {
        try {
            const response = await apiClient.get<ApiResponse<TradingLog[]>>('/api/trade-log/recent', {
                headers: getAuthHeaders(),
            })
            setLogs(response.data.data)
        } catch (err) {
            console.error('Failed to fetch logs', err)
            setLogs([])
        }
    }

    const fetchStocks = async (): Promise<void> => {
        try {
            const response = await apiClient.get<ApiResponse<Stock[]>>('/api/trading-target', {
                headers: getAuthHeaders(),
            })
            const stockList = response.data.data

            // Fetch AI training status for each ticker in parallel
            const aiDataPromises = stockList.map(async (stock) => {
                try {
                    const statusRes = await apiClient.post<ApiResponse<AiStatus>>(`/api/ai/${stock.ticker}/training-status`, {}, { headers: getAuthHeaders() })
                    return {
                        ticker: stock.ticker,
                        status: statusRes.data.data,
                    }
                } catch {
                    return { ticker: stock.ticker, status: null }
                }
            })

            const aiData = await Promise.all(aiDataPromises)
            const aiDataMap = new Map(aiData.map(d => [d.ticker, d]))

            // Merge AI status into stocks
            const stocksWithAi = stockList.map(stock => {
                const ai = aiDataMap.get(stock.ticker)
                return {
                    ...stock,
                    trainingStatus: (ai?.status?.status as Stock['trainingStatus']) || 'PENDING',
                }
            })

            setStocks(stocksWithAi)
        } catch (err) {
            console.error('Failed to fetch stocks', err)
            setStocks([])
        }
    }

    // Ref to track training tickers for polling (avoids useCallback dependency issues)
    const trainingTickersRef = useRef<string[]>([])
    useEffect(() => {
        trainingTickersRef.current = stocks.filter(s => s.trainingStatus === 'TRAINING').map(s => s.ticker)
    }, [stocks])

    // Check and update training status for TRAINING tickers (calls Python to check actual status)
    const checkTrainingStatus = useCallback(async (): Promise<void> => {
        const tickers = trainingTickersRef.current
        if (tickers.length === 0) return

        const promises = tickers.map(async (ticker) => {
            try {
                const statusRes = await apiClient.post<ApiResponse<AiStatus>>(
                    `/api/ai/${ticker}/training-status`,
                    {},
                    { headers: getAuthHeaders() }
                )
                return { ticker, status: statusRes.data.data }
            } catch {
                return { ticker, status: null }
            }
        })

        const results = await Promise.all(promises)

        // Update stocks with changed statuses
        results.forEach(({ ticker, status }) => {
            if (status && status.status !== 'TRAINING') {
                setStocks(prev => prev.map(s =>
                    s.ticker === ticker
                        ? { ...s, trainingStatus: status.status as Stock['trainingStatus'] }
                        : s
                ))
            }
        })
    }, [apiClient])

    // Training status polling (every 10 seconds for TRAINING tickers)
    const trainingPollRef = useRef<ReturnType<typeof setInterval> | null>(null)
    const hasTrainingTickers = stocks.some(s => s.trainingStatus === 'TRAINING')

    useEffect(() => {
        if (hasTrainingTickers) {
            // Start polling if not already polling
            if (!trainingPollRef.current) {
                trainingPollRef.current = setInterval(() => {
                    checkTrainingStatus()
                }, 10000) // 10 seconds
            }
        } else {
            // Stop polling if no training tickers
            if (trainingPollRef.current) {
                clearInterval(trainingPollRef.current)
                trainingPollRef.current = null
            }
        }

        return () => {
            if (trainingPollRef.current) {
                clearInterval(trainingPollRef.current)
                trainingPollRef.current = null
            }
        }
    }, [hasTrainingTickers, checkTrainingStatus])

    const fetchProfitStats = async (): Promise<void> => {
        try {
            const response = await apiClient.get<ApiResponse<ProfitStats>>('/api/trade-log/stats', {
                headers: getAuthHeaders(),
            })
            setProfitStats(response.data.data)
        } catch (err) {
            setProfitStats({ realizedProfit: 0 })
        }
    }

    const fetchAsset = async (): Promise<void> => {
        try {
            const response = await apiClient.get<ApiResponse<Asset>>('/api/asset', {
                headers: getAuthHeaders(),
            })
            setAsset(response.data.data)
        } catch (err) {
            setAsset({ totalAsset: 0, usdDeposit: 0, ownedStocks: [] })
        }
    }

    const handleAddStock = async (): Promise<void> => {
        if (!tickerInput.trim()) return
        try {
            const brokerId = brokerIdInput ? Number(brokerIdInput) : null
            await apiClient.post('/api/trading-target', { ticker: tickerInput, brokerId }, {
                headers: getAuthHeaders(),
            })
            setTickerInput('')
            fetchStocks()
        } catch (err) {
            console.error('Failed to add stock', err)
            setTickerInput('')
        }
    }

    const handleOpenThresholdModal = async (stock: Stock): Promise<void> => {
        setEditingStock(stock)
        setThresholds({
            buy: stock.buyThreshold || 10,
            sell: stock.sellThreshold || 10,
            stopLoss: parseFloat(stock.stopLossPercentage) || 3,
            baseTicker: stock.baseTicker || '',
            isInverse: stock.isInverse || false,
            trailingStop: parseFloat(stock.trailingStopPercentage) || 2,
            trailingStopEnabled: stock.trailingStopEnabled ?? true,
            trailingWindowMinutes: stock.trailingWindowMinutes || 10,
        })
        setThresholdModalOpen(true)
    }

    const handleSaveThresholds = async (): Promise<void> => {
        if (!editingStock) return
        try {
            await apiClient.put(`/api/trading-target/${editingStock.id}`, {
                buyThreshold: thresholds.buy,
                sellThreshold: thresholds.sell,
                stopLossPercentage: String(thresholds.stopLoss),
                baseTicker: thresholds.baseTicker,
                isInverse: thresholds.isInverse,
                trailingStopPercentage: String(thresholds.trailingStop),
                trailingStopEnabled: thresholds.trailingStopEnabled,
                trailingWindowMinutes: thresholds.trailingWindowMinutes,
                brokerId: editingStock.brokerId,
                holdingQuantity: editingStock.holdingQuantity,
            }, {
                headers: getAuthHeaders(),
            })

            setThresholdModalOpen(false)
            fetchStocks()
        } catch (err) {
            const axiosError = err as AxiosError<ApiResponse<null>>
            alert('설정 저장 실패: ' + (axiosError.response?.data?.message || axiosError.message))
        }
    }

    const handleToggleTrading = async (stock: Stock): Promise<void> => {
        try {
            await apiClient.patch(`/api/trading-target/${stock.ticker}/trading`, null, {
                params: { active: !stock.isTrading },
                headers: getAuthHeaders(),
            })
            fetchStocks()
        } catch (err) {
            const axiosError = err as AxiosError<ApiResponse<null>>
            alert('매매 상태 변경 실패: ' + (axiosError.response?.data?.message || axiosError.message))
        }
    }

    const handleViewLogs = async (ticker: string): Promise<void> => {
        setSelectedTicker(ticker)
        try {
            const response = await apiClient.get<ApiResponse<string>>(`/api/ai/${ticker}/logs`, {
                headers: getAuthHeaders(),
            })
            setLogContent(response.data.data)
            setLogModalOpen(true)
        } catch (err) {
            setLogContent('로그를 불러오는데 실패했습니다.')
            setLogModalOpen(true)
        }
    }

    const handleResetTraining = async (ticker: string): Promise<void> => {
        if (!window.confirm(`${ticker} 학습 기록을 초기화하시겠습니까?`)) return
        try {
            await apiClient.delete(`/api/ai/${ticker}/train`, {
                headers: getAuthHeaders(),
            })
            alert('학습 기록이 초기화되었습니다.')
            fetchStocks()
        } catch (err) {
            const axiosError = err as AxiosError<ApiResponse<null>>
            alert('초기화 실패: ' + (axiosError.response?.data?.message || axiosError.message))
        }
    }

    const handleDeleteStock = async (id: number, ticker: string): Promise<void> => {
        if (!window.confirm(`${ticker} 종목을 관심 목록에서 삭제하시겠습니까?\n(매매가 진행 중이라면 먼저 중지됩니다)`)) return
        try {
            await apiClient.delete(`/api/trading-target/${id}`, {
                headers: getAuthHeaders(),
            })
            fetchStocks()
        } catch (err) {
            const axiosError = err as AxiosError<ApiResponse<null>>
            alert('삭제 실패: ' + (axiosError.response?.data?.message || axiosError.message))
        }
    }

    const handleTrainAI = async (_stockId: number, ticker: string): Promise<void> => {
        try {
            await apiClient.post(`/api/ai/${ticker}/train`, {}, {
                headers: getAuthHeaders(),
            })
            alert(`${ticker} 모델 학습을 시작했습니다. 완료까지 시간이 걸릴 수 있습니다.`)
            fetchStocks()
        } catch (err) {
            console.error(err)
            const axiosError = err as AxiosError<ApiResponse<null>>
            alert(`${ticker} 학습 요청 실패: ` + (axiosError.response?.data?.message || axiosError.message))
        }
    }

    const formatUSD = (value: number): string => {
        return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
    }

    const formatCurrency = (value: number): string => {
        return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(value)
    }

    const formatTime = (isoString: string): string => {
        return new Date(isoString).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    }

    const RealizedProfitCard = ({ value }: RealizedProfitCardProps) => (
        <Card sx={{ minWidth: 180, textAlign: 'center', bgcolor: 'background.paper' }}>
            <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">실현 손익</Typography>
                <Typography
                    variant="h6"
                    fontWeight="bold"
                    color={value >= 0 ? 'success.main' : 'error.main'}
                >
                    {value >= 0 ? '+' : ''}{formatUSD(value)}
                </Typography>
            </CardContent>
        </Card>
    )

    const BalanceCard = ({ label, value, isUSD }: BalanceCardProps) => (
        <Card sx={{ minWidth: 140, textAlign: 'center', bgcolor: 'primary.dark' }}>
            <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography variant="caption" color="text.secondary">{label}</Typography>
                <Typography variant="h6" fontWeight="bold" color="white">
                    {isUSD ? formatUSD(value) : formatCurrency(value)}
                </Typography>
            </CardContent>
        </Card>
    )

    const handleKeyPress = (e: KeyboardEvent<HTMLDivElement>): void => {
        if (e.key === 'Enter') {
            handleAddStock()
        }
    }

    return (
        <Box sx={{ flexGrow: 1, minHeight: '100vh', bgcolor: 'background.default' }}>
            <AppBar position="static" sx={{ bgcolor: 'background.paper' }}>
                <Toolbar>
                    <TrendingUp sx={{ mr: 2, color: 'primary.main' }} />
                    <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
                        AI Stock Trading
                    </Typography>
                    <Tooltip title="설정">
                        <IconButton color="inherit" onClick={() => setSettingsOpen(true)}>
                            <Settings />
                        </IconButton>
                    </Tooltip>
                    <Tooltip title="로그아웃">
                        <IconButton color="inherit" onClick={onLogout}>
                            <Logout />
                        </IconButton>
                    </Tooltip>
                </Toolbar>
            </AppBar>

            <Container maxWidth="lg" sx={{ mt: 4 }}>
                {/* Connection Warning */}
                {!isBrokerConnected && (
                    <Alert
                        severity="warning"
                        variant="filled"
                        icon={<WarningAmber fontSize="inherit" />}
                        sx={{ mb: 4, borderRadius: 2 }}
                        action={
                            <Button color="inherit" size="small" onClick={() => setSettingsOpen(true)}>
                                설정하러 가기
                            </Button>
                        }
                    >
                        증권사 계정 정보가 설정되지 않았습니다. 매매 엔진을 가동하려면 설정을 완료해주세요.
                    </Alert>
                )}

                {/* Profit Stats */}
                <Grid container spacing={2} sx={{ mb: 2 }}>
                    <Grid item><RealizedProfitCard value={profitStats.realizedProfit} /></Grid>
                </Grid>

                {/* Balance Info */}
                <Grid container spacing={2} sx={{ mb: 4 }}>
                    <Grid item><BalanceCard label="달러 잔고" value={asset.usdDeposit} isUSD={true} /></Grid>
                    <Grid item><BalanceCard label="총 자산" value={asset.totalAsset} isUSD={false} /></Grid>
                </Grid>

                {/* Add Stock */}
                <Card sx={{ mb: 4 }}>
                    <CardContent sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
                        <TextField
                            label="티커 입력 (예: NVDA)"
                            variant="outlined"
                            size="small"
                            value={tickerInput}
                            onChange={(e: ChangeEvent<HTMLInputElement>) => setTickerInput(e.target.value)}
                            sx={{ flexGrow: 1 }}
                            onKeyPress={handleKeyPress}
                        />
                        <TextField
                            label="증권사"
                            select
                            size="small"
                            value={brokerIdInput}
                            onChange={(e) => setBrokerIdInput(e.target.value)}
                            SelectProps={{ native: true }}
                            sx={{ minWidth: 160 }}
                        >
                            <option value="">기본 (활성 증권사)</option>
                            {brokerInfos.map(b => (
                                <option key={b.id} value={b.id}>
                                    {b.brokerType} ({b.accountNumber || '미설정'})
                                </option>
                            ))}
                        </TextField>
                        <Button
                            variant="contained"
                            startIcon={<Add />}
                            onClick={handleAddStock}
                        >
                            관심 등록
                        </Button>
                    </CardContent>
                </Card>

                {/* Global Controls */}
                <Box sx={{ mb: 2, display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
                    <Button
                        variant="contained"
                        color="error"
                        startIcon={<Stop />}
                        onClick={async () => {
                            if (!window.confirm('모든 종목의 자동매매를 정지하시겠습니까?')) return
                            const promises = stocks
                                .filter(s => s.isTrading)
                                .map(s => apiClient.patch(`/api/trading-target/${s.ticker}/trading`, null, {
                                    params: { active: false },
                                    headers: getAuthHeaders(),
                                }))
                            await Promise.all(promises)
                            fetchStocks()
                        }}
                    >
                        전체 중지
                    </Button>
                    <Button
                        variant="outlined"
                        color="error"
                        startIcon={<Delete />}
                        onClick={async () => {
                            const trainedStocks = stocks.filter(s => s.trainingStatus === 'COMPLETED' || s.trainingStatus === 'FAILED')
                            if (trainedStocks.length === 0) {
                                alert('삭제할 학습 데이터가 없습니다.')
                                return
                            }
                            if (!window.confirm(`${trainedStocks.length}개 티커의 학습 데이터를 모두 삭제하시겠습니까?\n(${trainedStocks.map(s => s.ticker).join(', ')})`)) return
                            const results = await Promise.allSettled(
                                trainedStocks.map(s => apiClient.delete(`/api/ai/${s.ticker}/train`, { headers: getAuthHeaders() }))
                            )
                            const failed = results.filter(r => r.status === 'rejected').length
                            if (failed > 0) {
                                alert(`${trainedStocks.length - failed}개 삭제 완료, ${failed}개 실패`)
                            }
                            fetchStocks()
                        }}
                    >
                        전체 학습 데이터 삭제
                    </Button>
                </Box>

                {/* Stock List */}
                <TableContainer component={Paper} sx={{ mb: 4 }}>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>티커</TableCell>
                                <TableCell align="right">현재가</TableCell>
                                <TableCell align="right">보유수량</TableCell>
                                <TableCell align="right">수익률</TableCell>
                                <TableCell align="center">AI 상태</TableCell>
                                <TableCell align="center">매매</TableCell>
                                <TableCell align="center">삭제</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {stocks.map((stock) => {
                                const owned = asset.ownedStocks.find(s => s.stockCode === stock.ticker);
                                const currentPrice = owned?.currentPrice ?? 0;
                                const profitRate = owned?.profitRate ?? 0;
                                return (
                                <TableRow key={stock.id}>
                                    <TableCell>{stock.ticker}</TableCell>
                                    <TableCell align="right">{formatUSD(currentPrice)}</TableCell>
                                    <TableCell align="right">{stock.holdingQuantity}</TableCell>
                                    <TableCell align="right">
                                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 0.5 }}>
                                            {profitRate >= 0 ? (
                                                <TrendingUp color="success" fontSize="small" />
                                            ) : (
                                                <TrendingDown color="error" fontSize="small" />
                                            )}
                                            <Typography color={profitRate >= 0 ? 'success.main' : 'error.main'}>
                                                {profitRate >= 0 ? '+' : ''}{profitRate.toFixed(2)}%
                                            </Typography>
                                        </Box>
                                    </TableCell>
                                    <TableCell align="center">
                                        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 0.5 }}>
                                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1 }}>
                                                {(() => {
                                                    const hasBaseTicker = !!stock.baseTicker
                                                    const status = stock.trainingStatus || 'PENDING'

                                                    if (hasBaseTicker) {
                                                        return (
                                                            <Tooltip title={`${stock.baseTicker} 모델 사용 중 (자체 학습 불필요)`}>
                                                                <Chip
                                                                    label={`${stock.baseTicker} 모델`}
                                                                    size="small"
                                                                    color="info"
                                                                    variant="outlined"
                                                                    icon={<Psychology />}
                                                                />
                                                            </Tooltip>
                                                        )
                                                    }

                                                    if (status === 'COMPLETED') {
                                                        return <Chip label="학습완료" color="success" size="small" icon={<Psychology />} />
                                                    } else if (status === 'TRAINING') {
                                                        return (
                                                            <Button size="small" variant="outlined" disabled startIcon={<Psychology />}>
                                                                학습중...
                                                            </Button>
                                                        )
                                                    } else if (status === 'FAILED') {
                                                        return (
                                                            <Button
                                                                size="small"
                                                                variant="outlined"
                                                                color="error"
                                                                startIcon={<WarningAmber />}
                                                                onClick={() => handleTrainAI(stock.id, stock.ticker)}
                                                            >
                                                                재시도
                                                            </Button>
                                                        )
                                                    } else {
                                                        return (
                                                            <Button
                                                                size="small"
                                                                variant="outlined"
                                                                startIcon={<Psychology />}
                                                                onClick={() => handleTrainAI(stock.id, stock.ticker)}
                                                            >
                                                                학습
                                                            </Button>
                                                        )
                                                    }
                                                })()}

                                                <Tooltip title="로그 보기">
                                                    <IconButton size="small" onClick={() => handleViewLogs(stock.ticker)}>
                                                        <Article fontSize="small" />
                                                    </IconButton>
                                                </Tooltip>

                                                {!stock.baseTicker && (stock.trainingStatus === 'COMPLETED' || stock.trainingStatus === 'FAILED') && (
                                                    <Tooltip title="학습 초기화">
                                                        <IconButton size="small" color="error" onClick={() => handleResetTraining(stock.ticker)}>
                                                            <Delete fontSize="small" />
                                                        </IconButton>
                                                    </Tooltip>
                                                )}

                                                <Tooltip title="매매 설정 (확률 기준)">
                                                    <IconButton size="small" onClick={() => handleOpenThresholdModal(stock)}>
                                                        <Settings fontSize="small" />
                                                    </IconButton>
                                                </Tooltip>
                                            </Box>

                                            {/* Threshold Display */}
                                            <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.7rem' }}>
                                                B:{stock.buyThreshold}% / S:{stock.sellThreshold}% / SL:{stock.stopLossPercentage}%
                                                {stock.trailingStopEnabled && (
                                                    <span style={{ marginLeft: 4, color: '#81c784' }}>
                                                        TS:{stock.trailingStopPercentage}%/{stock.trailingWindowMinutes}m
                                                    </span>
                                                )}
                                                {stock.baseTicker && (
                                                    <span style={{ marginLeft: 4, color: stock.isInverse ? '#f48fb1' : '#90caf9' }}>
                                                        ({stock.isInverse ? '↓' : '→'}{stock.baseTicker})
                                                    </span>
                                                )}
                                            </Typography>
                                        </Box>
                                    </TableCell>
                                    <TableCell align="center">
                                        <IconButton
                                            color={stock.isTrading ? 'error' : 'primary'}
                                            onClick={() => handleToggleTrading(stock)}
                                        >
                                            {stock.isTrading ? <Stop /> : <PlayArrow />}
                                        </IconButton>
                                    </TableCell>
                                    <TableCell align="center">
                                        <Tooltip title="관심 종목 삭제">
                                            <IconButton
                                                color="error"
                                                size="small"
                                                onClick={() => handleDeleteStock(stock.id, stock.ticker)}
                                            >
                                                <Delete fontSize="small" />
                                            </IconButton>
                                        </Tooltip>
                                    </TableCell>
                                </TableRow>
                                );
                            })}
                            {stocks.length === 0 && (
                                <TableRow>
                                    <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                                        <Typography color="text.secondary">
                                            관심 종목이 없습니다. 티커를 입력하여 추가하세요.
                                        </Typography>
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>

                {/* Live Activity Logs */}
                <Typography variant="h6" gutterBottom color="primary">
                    Live Activity
                </Typography>
                <TableContainer component={Paper} sx={{ maxHeight: 300 }}>
                    <Table stickyHeader size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell>시간</TableCell>
                                <TableCell>티커</TableCell>
                                <TableCell>Action</TableCell>
                                <TableCell align="right">가격</TableCell>
                                <TableCell>상태</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {logs.map((log) => (
                                <TableRow key={log.id}>
                                    <TableCell>{formatTime(log.timestamp)}</TableCell>
                                    <TableCell>{log.ticker}</TableCell>
                                    <TableCell>
                                        <Chip
                                            label={log.action}
                                            size="small"
                                            color={log.action === 'BUY' ? 'error' : log.action === 'SELL' ? 'primary' : 'default'}
                                            variant="outlined"
                                        />
                                    </TableCell>
                                    <TableCell align="right">{formatUSD(log.price)}</TableCell>
                                    <TableCell>
                                        <Chip
                                            label={
                                                log.status === 'SUCCESS' ? '성공' :
                                                log.status === 'INSUFFICIENT_BALANCE' ? '잔고부족' :
                                                log.status === 'INSUFFICIENT_STOCK' ? '보유부족' :
                                                log.status === 'FAILED' ? '실패' : '성공'
                                            }
                                            size="small"
                                            color={log.status === 'SUCCESS' || !log.status ? 'success' : 'warning'}
                                            variant="outlined"
                                        />
                                    </TableCell>
                                </TableRow>
                            ))}
                            {logs.length === 0 && (
                                <TableRow>
                                    <TableCell colSpan={5} align="center" sx={{ py: 2 }}>
                                        <Typography variant="body2" color="text.secondary">
                                            최근 매매 기록이 없습니다.
                                        </Typography>
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Container>

            {/* Threshold Settings Modal */}
            <Dialog open={thresholdModalOpen} onClose={() => setThresholdModalOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>{editingStock?.ticker} 매매 기준 설정</DialogTitle>
                <DialogContent>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                        AI가 매매를 결심하기 위한 최소 확률을 설정합니다. (0 ~ 100)<br />
                        높을수록 신중하게 거래합니다. (권장: 10)
                    </Typography>
                    <Grid container spacing={2}>
                        <Grid item xs={6}>
                            <TextField
                                label="매수 기준 (Buy)"
                                type="number"
                                inputProps={{ step: 1, min: 0, max: 100 }}
                                fullWidth
                                value={thresholds.buy}
                                onChange={(e: ChangeEvent<HTMLInputElement>) => setThresholds({ ...thresholds, buy: parseInt(e.target.value) || 0 })}
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <TextField
                                label="매도 기준 (Sell)"
                                type="number"
                                inputProps={{ step: 1, min: 0, max: 100 }}
                                fullWidth
                                value={thresholds.sell}
                                onChange={(e: ChangeEvent<HTMLInputElement>) => setThresholds({ ...thresholds, sell: parseInt(e.target.value) || 0 })}
                            />
                        </Grid>
                    </Grid>

                    <Typography variant="body2" color="text.secondary" sx={{ mt: 3, mb: 2 }}>
                        손절 기준을 설정합니다. 보유 종목의 손실이 이 비율을 초과하면 자동 매도됩니다.<br />
                        <strong>권장:</strong> 일반 주식 3%, 2배 레버리지 5~6%, 3배 레버리지 7~9%
                    </Typography>
                    <Grid container spacing={2}>
                        <Grid item xs={12}>
                            <TextField
                                label="손절 기준 (Stop Loss %)"
                                type="number"
                                inputProps={{ step: 0.5, min: 0.5, max: 50 }}
                                fullWidth
                                value={thresholds.stopLoss}
                                onChange={(e: ChangeEvent<HTMLInputElement>) => setThresholds({ ...thresholds, stopLoss: parseFloat(e.target.value) || 3 })}
                                helperText={`현재 가격이 매수가 대비 -${thresholds.stopLoss}% 이하가 되면 자동 매도`}
                            />
                        </Grid>
                    </Grid>

                    <Typography variant="body2" color="text.secondary" sx={{ mt: 3, mb: 2 }}>
                        <strong>트레일링 스탑</strong><br />
                        수익 구간에서 최근 N분 캔들 고점 대비 하락 시 자동 매도합니다.<br />
                        <strong>권장:</strong> 고변동성(TSLA,PLTR) 3%/5분, 중변동성(NVDA) 2.5%/5분, ETF 1~1.5%/15분
                    </Typography>
                    <Grid container spacing={2} alignItems="center">
                        <Grid item xs={4}>
                            <TextField
                                label="하락률 (%)"
                                type="number"
                                inputProps={{ step: 0.5, min: 0.5, max: 10 }}
                                fullWidth
                                value={thresholds.trailingStop}
                                onChange={(e: ChangeEvent<HTMLInputElement>) => setThresholds({ ...thresholds, trailingStop: parseFloat(e.target.value) || 2 })}
                                disabled={!thresholds.trailingStopEnabled}
                            />
                        </Grid>
                        <Grid item xs={4}>
                            <TextField
                                label="윈도우 (분)"
                                select
                                fullWidth
                                value={thresholds.trailingWindowMinutes}
                                onChange={(e) => setThresholds({ ...thresholds, trailingWindowMinutes: parseInt(e.target.value) || 10 })}
                                SelectProps={{ native: true }}
                                disabled={!thresholds.trailingStopEnabled}
                            >
                                <option value={5}>5분</option>
                                <option value={10}>10분</option>
                                <option value={15}>15분</option>
                                <option value={20}>20분</option>
                                <option value={30}>30분</option>
                            </TextField>
                        </Grid>
                        <Grid item xs={4}>
                            <FormControlLabel
                                control={
                                    <Switch
                                        checked={thresholds.trailingStopEnabled}
                                        onChange={(e) => setThresholds({ ...thresholds, trailingStopEnabled: e.target.checked })}
                                        color="success"
                                    />
                                }
                                label={
                                    <Box>
                                        <Typography variant="body2">활성화</Typography>
                                    </Box>
                                }
                            />
                        </Grid>
                    </Grid>
                    {thresholds.trailingStopEnabled && (
                        <Alert severity="success" sx={{ mt: 2 }}>
                            수익 중일 때, 최근 {thresholds.trailingWindowMinutes}분 고점 대비 {thresholds.trailingStop}% 하락하면 자동 매도합니다.
                        </Alert>
                    )}

                    <Divider sx={{ my: 3 }} />

                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                        <strong>기준 티커 연동</strong><br />
                        다른 티커의 AI 모델을 사용하여 매매 신호를 받습니다.<br />
                        예: TQQQ/SQQQ는 QQQ 모델을 사용하면 더 정확한 예측이 가능합니다.
                    </Typography>
                    <Grid container spacing={2} alignItems="center">
                        <Grid item xs={6}>
                            <TextField
                                label="기준 티커 (Base Ticker)"
                                fullWidth
                                value={thresholds.baseTicker}
                                onChange={(e: ChangeEvent<HTMLInputElement>) => setThresholds({ ...thresholds, baseTicker: e.target.value.toUpperCase() })}
                                placeholder="예: QQQ"
                                helperText={thresholds.baseTicker ? `${thresholds.baseTicker} 모델로 예측` : '비워두면 자체 모델 사용'}
                            />
                        </Grid>
                        <Grid item xs={6}>
                            <FormControlLabel
                                control={
                                    <Switch
                                        checked={thresholds.isInverse}
                                        onChange={(e) => setThresholds({ ...thresholds, isInverse: e.target.checked })}
                                        color="secondary"
                                        disabled={!thresholds.baseTicker}
                                    />
                                }
                                label={
                                    <Box>
                                        <Typography variant="body2">역방향 (Inverse)</Typography>
                                        <Typography variant="caption" color="text.secondary">
                                            {thresholds.isInverse ? '매수↔매도 반전' : '같은 방향'}
                                        </Typography>
                                    </Box>
                                }
                            />
                        </Grid>
                    </Grid>
                    {thresholds.baseTicker && (
                        <Alert severity="info" sx={{ mt: 2 }}>
                            {thresholds.isInverse
                                ? `${thresholds.baseTicker} 모델이 "매수"하면 → ${editingStock?.ticker}는 "매도" (역방향 ETF용)`
                                : `${thresholds.baseTicker} 모델이 "매수"하면 → ${editingStock?.ticker}도 "매수" (레버리지 ETF용)`
                            }
                        </Alert>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setThresholdModalOpen(false)}>취소</Button>
                    <Button onClick={handleSaveThresholds} variant="contained">저장</Button>
                </DialogActions>
            </Dialog>

            {/* Log Viewer Modal */}
            <Dialog
                open={logModalOpen}
                onClose={() => setLogModalOpen(false)}
                maxWidth="md"
                fullWidth
            >
                <DialogTitle>
                    {selectedTicker} 학습 로그
                    <IconButton
                        aria-label="close"
                        onClick={() => setLogModalOpen(false)}
                        sx={{ position: 'absolute', right: 8, top: 8 }}
                    >
                        <Logout sx={{ transform: 'rotate(180deg)' }} />
                    </IconButton>
                </DialogTitle>
                <DialogContent dividers>
                    <Box
                        sx={{
                            bgcolor: '#1e1e1e',
                            color: '#d4d4d4',
                            p: 2,
                            borderRadius: 1,
                            fontFamily: 'monospace',
                            whiteSpace: 'pre-wrap',
                            maxHeight: '60vh',
                            overflow: 'auto',
                            fontSize: '0.875rem'
                        }}
                    >
                        {logContent || '로그 데이터가 없습니다.'}
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => handleViewLogs(selectedTicker)}>새로고침</Button>
                    <Button onClick={() => setLogModalOpen(false)}>닫기</Button>
                </DialogActions>
            </Dialog>

            <SettingsModal
                open={settingsOpen}
                onClose={() => {
                    setSettingsOpen(false)
                    checkBrokerConnection()
                }}
            />
        </Box>
    )
}

export default DashboardPage
