import { useState, FormEvent } from 'react'
import { useNavigate, Link } from 'react-router-dom'
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
import { Visibility, VisibilityOff, PersonAdd } from '@mui/icons-material'
import axios, { AxiosError } from 'axios'
import type { ApiResponse } from '../types'

function SignupPage() {
    const navigate = useNavigate()
    const [username, setUsername] = useState<string>('')
    const [password, setPassword] = useState<string>('')
    const [confirmPassword, setConfirmPassword] = useState<string>('')
    const [showPassword, setShowPassword] = useState<boolean>(false)
    const [error, setError] = useState<string>('')
    const [success, setSuccess] = useState<string>('')
    const [loading, setLoading] = useState<boolean>(false)

    const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
        e.preventDefault()
        setError('')
        setSuccess('')
        setLoading(true)

        // Client-side validation
        if (username.length < 4) {
            setError('아이디는 4자리 이상이어야 합니다.')
            setLoading(false)
            return
        }
        if (password.length < 4) {
            setError('비밀번호는 4자리 이상이어야 합니다.')
            setLoading(false)
            return
        }
        if (password !== confirmPassword) {
            setError('비밀번호가 일치하지 않습니다.')
            setLoading(false)
            return
        }

        try {
            const response = await axios.post<ApiResponse<null>>('/api/auth/register', { username, password })
            setSuccess(response.data.message || '가입이 완료되었습니다.')
            setTimeout(() => navigate('/login'), 2000)
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
                            <PersonAdd sx={{ fontSize: 48, color: 'secondary.main', mb: 1 }} />
                            <Typography variant="h4" fontWeight="bold" color="primary.main">
                                회원가입
                            </Typography>
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                새 계정을 만드세요
                            </Typography>
                        </Box>

                        {error && (
                            <Alert severity="error" sx={{ mb: 2 }}>
                                {error}
                            </Alert>
                        )}

                        {success && (
                            <Alert severity="success" sx={{ mb: 2 }}>
                                {success}
                            </Alert>
                        )}

                        <form onSubmit={handleSubmit}>
                            <TextField
                                fullWidth
                                label="아이디 (4자리 이상)"
                                variant="outlined"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                sx={{ mb: 2 }}
                                required
                                inputProps={{ minLength: 4 }}
                            />
                            <TextField
                                fullWidth
                                label="비밀번호 (4자리 이상)"
                                type={showPassword ? 'text' : 'password'}
                                variant="outlined"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                sx={{ mb: 2 }}
                                required
                                inputProps={{ minLength: 4 }}
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
                            <TextField
                                fullWidth
                                label="비밀번호 확인"
                                type="password"
                                variant="outlined"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                sx={{ mb: 2 }}
                                required
                            />
                            <Button
                                fullWidth
                                type="submit"
                                variant="contained"
                                size="large"
                                disabled={loading || !!success}
                                sx={{
                                    py: 1.5,
                                    fontWeight: 'bold',
                                    background: 'linear-gradient(45deg, #f48fb1 30%, #ce93d8 90%)',
                                }}
                            >
                                {loading ? '처리 중...' : '가입 신청'}
                            </Button>
                        </form>

                        <Box sx={{ textAlign: 'center', mt: 3 }}>
                            <Button
                                color="primary"
                                component={Link}
                                to="/login"
                            >
                                이미 계정이 있으신가요? 로그인
                            </Button>
                        </Box>
                    </CardContent>
                </Card>
            </Container>
        </Box>
    )
}

export default SignupPage
