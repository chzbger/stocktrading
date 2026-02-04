import { useState, useEffect } from 'react'
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import DashboardPage from './pages/DashboardPage'
import UserManagementPage from './pages/UserManagementPage'

function App() {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false)

    useEffect(() => {
        const token = localStorage.getItem('authToken')
        if (token) {
            setIsAuthenticated(true)
        }
    }, [])

    const handleLogin = (token: string): void => {
        localStorage.setItem('authToken', token)
        setIsAuthenticated(true)
    }

    const handleLogout = (): void => {
        localStorage.removeItem('authToken')
        localStorage.removeItem('userRole')
        localStorage.removeItem('tokenTime')
        setIsAuthenticated(false)
    }

    return (
        <Router future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
            <Routes>
                <Route
                    path="/login"
                    element={
                        isAuthenticated ? <Navigate to="/" /> : <LoginPage onLogin={handleLogin} />
                    }
                />
                <Route
                    path="/signup"
                    element={
                        isAuthenticated ? <Navigate to="/" /> : <SignupPage />
                    }
                />
                <Route
                    path="/"
                    element={
                        isAuthenticated ? <DashboardPage onLogout={handleLogout} /> : <Navigate to="/login" />
                    }
                />
                <Route
                    path="/users"
                    element={
                        isAuthenticated ? <UserManagementPage /> : <Navigate to="/login" />
                    }
                />
            </Routes>
        </Router>
    )
}

export default App
