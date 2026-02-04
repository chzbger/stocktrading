import { useState, useEffect } from 'react'
import {
    Box,
    Typography,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    Chip,
    IconButton,
    Tooltip,
    Button,
    Alert,
    AlertColor,
} from '@mui/material'
import { Delete, CheckCircle, HourglassEmpty, ArrowBack } from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import axios, { AxiosError } from 'axios'
import type { ApiResponse, User } from '../types'

interface Message {
    type: AlertColor | '';
    text: string;
}

function UserManagementPage() {
    const navigate = useNavigate()
    const [users, setUsers] = useState<User[]>([])
    const [message, setMessage] = useState<Message>({ type: '', text: '' })

    const getAuthHeaders = () => ({
        Authorization: `Bearer ${localStorage.getItem('authToken')}`
    })

    useEffect(() => {
        fetchUsers()
    }, [])

    const fetchUsers = async (): Promise<void> => {
        try {
            const response = await axios.get<ApiResponse<User[]>>('/api/auth/users', {
                headers: getAuthHeaders(),
            })
            setUsers(response.data.data)
        } catch (err) {
            console.error('Failed to fetch users', err)
            setUsers([])
        }
    }

    const handleApprove = async (userId: number): Promise<void> => {
        try {
            await axios.post(`/api/auth/users/${userId}/approve`, {}, {
                headers: getAuthHeaders(),
            })
            setMessage({ type: 'success', text: '사용자가 승인되었습니다.' })
            fetchUsers()
        } catch (err) {
            const axiosError = err as AxiosError<ApiResponse<null>>
            setMessage({ type: 'error', text: axiosError.response?.data?.message || '승인에 실패했습니다.' })
        }
    }

    const handleDelete = async (userId: number): Promise<void> => {
        if (!window.confirm('정말로 이 사용자를 삭제하시겠습니까?')) return
        try {
            await axios.delete(`/api/auth/users/${userId}`, {
                headers: getAuthHeaders(),
            })
            setMessage({ type: 'success', text: '사용자가 삭제되었습니다.' })
            fetchUsers()
        } catch (err) {
            const axiosError = err as AxiosError<ApiResponse<null>>
            setMessage({ type: 'error', text: axiosError.response?.data?.message || '삭제에 실패했습니다.' })
        }
    }

    return (
        <Box sx={{ p: 4, minHeight: '100vh', bgcolor: 'background.default' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                <IconButton onClick={() => navigate('/')} sx={{ mr: 2 }}>
                    <ArrowBack />
                </IconButton>
                <Typography variant="h4" fontWeight="bold">
                    회원 관리
                </Typography>
            </Box>

            {message.text && message.type && (
                <Alert severity={message.type} sx={{ mb: 2 }} onClose={() => setMessage({ type: '', text: '' })}>
                    {message.text}
                </Alert>
            )}

            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>아이디</TableCell>
                            <TableCell align="center">가입 상태</TableCell>
                            <TableCell align="center">관리</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {users.map((user) => (
                            <TableRow key={user.id}>
                                <TableCell>{user.username}</TableCell>
                                <TableCell align="center">
                                    {user.status === 'ACTIVE' ? (
                                        <Chip
                                            label="정상"
                                            color="success"
                                            size="small"
                                            icon={<CheckCircle />}
                                        />
                                    ) : (
                                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1 }}>
                                            <Chip
                                                label="가입신청"
                                                color="warning"
                                                size="small"
                                                icon={<HourglassEmpty />}
                                            />
                                            <Button
                                                size="small"
                                                variant="contained"
                                                color="primary"
                                                onClick={() => handleApprove(user.id)}
                                            >
                                                승인
                                            </Button>
                                        </Box>
                                    )}
                                </TableCell>
                                <TableCell align="center">
                                    <Tooltip title="삭제">
                                        <IconButton
                                            color="error"
                                            onClick={() => handleDelete(user.id)}
                                        >
                                            <Delete />
                                        </IconButton>
                                    </Tooltip>
                                </TableCell>
                            </TableRow>
                        ))}
                        {users.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={3} align="center" sx={{ py: 4 }}>
                                    <Typography color="text.secondary">
                                        등록된 사용자가 없습니다.
                                    </Typography>
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    )
}

export default UserManagementPage
