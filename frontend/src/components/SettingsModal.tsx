import { useState, useEffect } from 'react'
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    TextField,
    Box,
    Typography,
    Divider,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    Alert,
    List,
    ListItem,
    ListItemText,
    ListItemButton,
    Radio,
    IconButton,
    ListItemIcon,
    Chip,
    AlertColor,
    SelectChangeEvent
} from '@mui/material'
import { Key, Save, Delete, Add, CheckCircle, People, AccessTime } from '@mui/icons-material'
import axios, { AxiosError } from 'axios'
import { useNavigate } from 'react-router-dom'
import type { ApiResponse, UserSettings, BrokerInfo } from '../types'

interface SettingsModalProps {
    open: boolean;
    onClose: () => void;
}

interface Message {
    type: AlertColor | '';
    text: string;
}

const getAuthHeaders = () => ({
    Authorization: `Bearer ${localStorage.getItem('authToken')}`
})

function SettingsModal({ open, onClose }: SettingsModalProps) {
    const navigate = useNavigate()
    const [brokerInfos, setBrokerInfos] = useState<BrokerInfo[]>([])
    const [activeBrokerId, setActiveBrokerId] = useState<number | null>(null)
    const [tradingStartTime, setTradingStartTime] = useState<string>('22:30')
    const [tradingEndTime, setTradingEndTime] = useState<string>('05:00')
    const [loading, setLoading] = useState<boolean>(false)
    const [message, setMessage] = useState<Message>({ type: '', text: '' })

    // Form State for Adding/Editing
    const [isAdding, setIsAdding] = useState<boolean>(false)
    const [newBrokerType, setNewBrokerType] = useState<'KIS' | 'LS'>('KIS')
    const [newAppKey, setNewAppKey] = useState<string>('')
    const [newAppSecret, setNewAppSecret] = useState<string>('')
    const [newAccountNumber, setNewAccountNumber] = useState<string>('')

    useEffect(() => {
        if (open) {
            fetchSettings()
            resetForm()
        }
    }, [open])

    const fetchSettings = async (): Promise<void> => {
        try {
            const response = await axios.get<ApiResponse<UserSettings>>('/api/settings', {
                headers: getAuthHeaders(),
            })
            const payload = response.data.data
            setBrokerInfos(payload.brokerInfos || [])
            setActiveBrokerId(payload.activeBrokerId)
            if (payload.tradingStartTime) setTradingStartTime(String(payload.tradingStartTime).substring(0, 5))
            if (payload.tradingEndTime) setTradingEndTime(String(payload.tradingEndTime).substring(0, 5))
        } catch (err) {
            console.error("Failed to fetch settings", err)
        }
    }

    const handleSaveTradingHours = async (): Promise<void> => {
        try {
            await axios.post('/api/settings/trading-hours', {
                startTime: tradingStartTime,
                endTime: tradingEndTime
            }, {
                headers: getAuthHeaders(),
            })
            setMessage({ type: 'success', text: '매매 시간이 저장되었습니다.' })
        } catch (err) {
            const axiosError = err as AxiosError<ApiResponse<null>>
            setMessage({ type: 'error', text: '저장 실패: ' + (axiosError.response?.data?.message || axiosError.message) })
        }
    }

    const handleSetActive = async (id: number): Promise<void> => {
        try {
            await axios.post('/api/settings/active-broker', { brokerInfoId: id }, {
                headers: getAuthHeaders(),
            })
            setActiveBrokerId(id)
            setMessage({ type: 'success', text: '활성 증권사가 변경되었습니다.' })
        } catch (err) {
            setMessage({ type: 'error', text: '변경 실패' })
        }
    }

    const handleGoToUserManagement = (): void => {
        onClose()
        navigate('/users')
    }

    const resetForm = (): void => {
        setIsAdding(false)
        setNewBrokerType('KIS')
        setNewAppKey('')
        setNewAppSecret('')
        setNewAccountNumber('')
        setMessage({ type: '', text: '' })
    }

    const handleAddBroker = async (): Promise<void> => {
        setLoading(true)
        try {
            await axios.post('/api/settings/brokers', {
                brokerType: newBrokerType,
                appKey: newAppKey,
                appSecret: newAppSecret,
                accountNumber: newAccountNumber
            }, {
                headers: getAuthHeaders(),
            })
            await fetchSettings()
            resetForm()
            setMessage({ type: 'success', text: '증권사 정보가 추가되었습니다.' })
        } catch (err) {
            const axiosError = err as AxiosError<ApiResponse<null>>
            const errorMsg = axiosError.response?.data?.message || axiosError.message
            setMessage({ type: 'error', text: '추가 실패: ' + errorMsg })
        } finally {
            setLoading(false)
        }
    }

    const handleDeleteBroker = async (id: number): Promise<void> => {
        if (!window.confirm('정말 삭제하시겠습니까?')) return
        try {
            await axios.delete(`/api/settings/brokers/${id}`, {
                headers: getAuthHeaders(),
            })
            await fetchSettings()
            setMessage({ type: 'success', text: '삭제되었습니다.' })
        } catch (err) {
            setMessage({ type: 'error', text: '삭제 실패' })
        }
    }

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Key color="primary" />
                설정
            </DialogTitle>
            <DialogContent>
                {localStorage.getItem('userRole') === 'ROLE_ADMIN' && (
                    <>
                        <List sx={{ mb: 2 }}>
                            <ListItem disablePadding>
                                <ListItemButton onClick={handleGoToUserManagement}>
                                    <ListItemIcon><People color="secondary" /></ListItemIcon>
                                    <ListItemText primary="회원 관리" secondary="회원 가입 승인 및 삭제" />
                                </ListItemButton>
                            </ListItem>
                        </List>
                        <Divider sx={{ mb: 2 }} />
                    </>
                )}

                {message.text && message.type && (
                    <Alert severity={message.type} sx={{ mb: 2 }}>{message.text}</Alert>
                )}

                {!isAdding ? (
                    <Box>
                        {/* Trading Hours Section */}
                        <Box sx={{ mb: 4, p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                                <AccessTime color="primary" />
                                <Typography variant="subtitle1" fontWeight="bold">자동매매 가동 시간</Typography>
                            </Box>
                            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                                설정한 시간 범위 내에서만 매매가 수행됩니다.<br />
                                (기본값: 미국 주식 시장 22:30 ~ 05:00)
                            </Typography>
                            <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
                                <TextField
                                    label="시작 시간"
                                    type="time"
                                    value={tradingStartTime}
                                    onChange={(e) => setTradingStartTime(e.target.value)}
                                    InputLabelProps={{ shrink: true }}
                                    size="small"
                                />
                                <Typography>~</Typography>
                                <TextField
                                    label="종료 시간"
                                    type="time"
                                    value={tradingEndTime}
                                    onChange={(e) => setTradingEndTime(e.target.value)}
                                    InputLabelProps={{ shrink: true }}
                                    size="small"
                                />
                                <Button
                                    variant="contained"
                                    size="small"
                                    startIcon={<Save />}
                                    onClick={handleSaveTradingHours}
                                    sx={{ ml: 'auto' }}
                                >
                                    저장
                                </Button>
                            </Box>
                        </Box>

                        <Divider sx={{ mb: 2 }} />

                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                            <Typography variant="h6">연결된 증권사</Typography>
                            <Button startIcon={<Add />} variant="outlined" size="small" onClick={() => setIsAdding(true)}>
                                추가
                            </Button>
                        </Box>

                        <List>
                            {brokerInfos.length === 0 && (
                                <Typography color="text.secondary" align="center" sx={{ py: 3 }}>
                                    연결된 증권사가 없습니다. 추가해주세요.
                                </Typography>
                            )}
                            {brokerInfos.map((info) => (
                                <ListItem
                                    key={info.id}
                                    secondaryAction={
                                        <IconButton edge="end" aria-label="delete" onClick={() => handleDeleteBroker(info.id)}>
                                            <Delete />
                                        </IconButton>
                                    }
                                >
                                    <ListItemIcon>
                                        <Radio
                                            checked={activeBrokerId === info.id}
                                            onChange={() => handleSetActive(info.id)}
                                            value={info.id}
                                        />
                                    </ListItemIcon>
                                    <ListItemText
                                        primary={
                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                {info.brokerType}
                                                {activeBrokerId === info.id && <Chip label="Active" color="success" size="small" icon={<CheckCircle />} />}
                                            </Box>
                                        }
                                        secondary={`계좌: ${info.accountNumber || '미설정'}`}
                                    />
                                </ListItem>
                            ))}
                        </List>
                    </Box>
                ) : (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
                        <Typography variant="subtitle1" fontWeight="bold">새 증권사 추가</Typography>

                        <FormControl fullWidth>
                            <InputLabel>증권사</InputLabel>
                            <Select
                                value={newBrokerType}
                                label="증권사"
                                onChange={(e: SelectChangeEvent<'KIS' | 'LS'>) => setNewBrokerType(e.target.value as 'KIS' | 'LS')}
                            >
                                <MenuItem value="KIS">한국투자증권 (KIS)</MenuItem>
                                <MenuItem value="LS">LS증권 (LS)</MenuItem>
                            </Select>
                        </FormControl>

                        <TextField
                            fullWidth
                            label="App Key"
                            value={newAppKey}
                            onChange={(e) => setNewAppKey(e.target.value)}
                        />
                        <TextField
                            fullWidth
                            label="App Secret"
                            type="password"
                            value={newAppSecret}
                            onChange={(e) => setNewAppSecret(e.target.value)}
                        />
                        <TextField
                            fullWidth
                            label="계좌번호"
                            value={newAccountNumber}
                            onChange={(e) => setNewAccountNumber(e.target.value)}
                            placeholder="12345678-01"
                            helperText="계좌번호-상품코드"
                        />

                        <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end', mt: 1 }}>
                            <Button onClick={() => setIsAdding(false)}>취소</Button>
                            <Button variant="contained" onClick={handleAddBroker} disabled={loading}>
                                {loading ? '추가 중...' : '추가하기'}
                            </Button>
                        </Box>
                    </Box>
                )}
            </DialogContent>
            <DialogActions>
                {!isAdding && <Button onClick={onClose}>닫기</Button>}
            </DialogActions>
        </Dialog>
    )
}

export default SettingsModal
