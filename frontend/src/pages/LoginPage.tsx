import { useState, FormEvent } from 'react'
import { Link } from 'react-router-dom'
import {
    Box,
    Card,
    CardContent,
    TextField,
    Button,
    Typography,
    Container,
    Alert,
    InputAdornment,
    IconButton,
} from '@mui/material'
import { Visibility, VisibilityOff, TrendingUp } from '@mui/icons-material'
import axios, { AxiosError } from 'axios'
import type { ApiResponse, LoginResponse } from '../types'

interface LoginPageProps {
    onLogin: (token: string) => void;
}

function LoginPage({ onLogin }: LoginPageProps) {
    const [username, setUsername] = useState<string>('')
    const [password, setPassword] = useState<string>('')
    const [showPassword, setShowPassword] = useState<boolean>(false)
    const [error, setError] = useState<string>('')
    const [loading, setLoading] = useState<boolean>(false)

    const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
        e.preventDefault()
        setError('')
        setLoading(true)

        try {
            const response = await axios.post<ApiResponse<LoginResponse>>('/api/auth/login', { username, password })
            const { role, token } = response.data.data
            localStorage.setItem('userRole', role)
            // Store token timestamp for expiry checking
            localStorage.setItem('tokenTime', Date.now().toString())
            onLogin(token)
        } catch (err) {
            const axiosError = err as AxiosError<ApiResponse<null>>
            setError(axiosError.response?.data?.message || '오류가 발생했습니다.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <Box
            sx={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'linear-gradient(135deg, #0a1929 0%, #1a237e 100%)',
            }}
        >
            <Container maxWidth="xs">
                <Card
                    sx={{
                        backdropFilter: 'blur(10px)',
                        backgroundColor: 'rgba(19, 47, 76, 0.9)',
                        borderRadius: 3,
                        boxShadow: '0 8px 32px rgba(0, 0, 0, 0.3)',
                    }}
                >
                    <CardContent sx={{ p: 4 }}>
                        <Box sx={{ textAlign: 'center', mb: 4 }}>
                            <TrendingUp sx={{ fontSize: 48, color: 'primary.main', mb: 1 }} />
                            <Typography variant="h4" fontWeight="bold" color="primary.main">
                                AI Stock Trading
                            </Typography>
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                로그인하세요
                            </Typography>
                        </Box>

                        {error && (
                            <Alert severity="error" sx={{ mb: 2 }}>
                                {error}
                            </Alert>
                        )}

                        <form onSubmit={handleSubmit}>
                            <TextField
                                fullWidth
                                label="아이디"
                                variant="outlined"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                sx={{ mb: 2 }}
                                required
                            />
                            <TextField
                                fullWidth
                                label="비밀번호"
                                type={showPassword ? 'text' : 'password'}
                                variant="outlined"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                sx={{ mb: 2 }}
                                required
                                InputProps={{
                                    endAdornment: (
                                        <InputAdornment position="end">
                                            <IconButton
                                                onClick={() => setShowPassword(!showPassword)}
                                                edge="end"
                                            >
                                                {showPassword ? <VisibilityOff /> : <Visibility />}
                                            </IconButton>
                                        </InputAdornment>
                                    ),
                                }}
                            />
                            <Button
                                fullWidth
                                type="submit"
                                variant="contained"
                                size="large"
                                disabled={loading}
                                sx={{
                                    py: 1.5,
                                    fontWeight: 'bold',
                                    background: 'linear-gradient(45deg, #2196F3 30%, #21CBF3 90%)',
                                }}
                            >
                                {loading ? '처리 중...' : '로그인'}
                            </Button>
                        </form>

                        <Box sx={{ textAlign: 'center', mt: 3 }}>
                            <Button
                                color="secondary"
                                component={Link}
                                to="/signup"
                            >
                                계정이 없으신가요? 회원가입
                            </Button>
                        </Box>
                    </CardContent>
                </Card>
            </Container>
        </Box>
    )
}

export default LoginPage
