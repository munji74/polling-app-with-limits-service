import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import api from '../api/client'
import { getToken, setToken, clearToken } from '../utils/storage'

const AuthContext = createContext(null)

function authHeader() {
  const t = getToken()
  return t ? { Authorization: `Bearer ${t}` } : {}
}

function parseJwt(token) {
  try {
    const payload = token.split('.')[1]
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    return decoded || null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function fetchProfile() {
      const token = getToken()
      if (!token) return setLoading(false)
      try {
        const claims = parseJwt(token)
        const email = claims?.email
        if (!email) throw new Error('No email in token')
        const { data } = await api.post('/api/users/get-user-details', { usernameOrEmail: email }, { headers: authHeader() })
        setUser(data)
      } catch {
        clearToken()
        setUser(null)
      } finally {
        setLoading(false)
      }
    }
    fetchProfile()
  }, [])

  const login = async (email, password) => {
    const res = await api.post('/auth/sign-in', { email, password })
    const token = res?.data?.accessToken
    if (!token) throw new Error('Login succeeded but no token returned')

    setToken(token)
    const { data } = await api.post('/api/users/get-user-details', { usernameOrEmail: email }, { headers: authHeader() })
    setUser(data)
  }

  /**
   * Register user via gateway, then optionally fetch profile if token returned
   */
  const register = async ({ name, email, password, passwordConfirm }) => {
    try {
      const res = await api.post('/auth/sign-up', { name, email, password })
      const token = res?.data?.accessToken || null
      if (token) {
        setToken(token)
        const { data } = await api.post('/api/users/get-user-details', { usernameOrEmail: email }, { headers: authHeader() })
        setUser(data)
      }
      return true
    } catch (err) {
      const status = err?.response?.status
      const data = err?.response?.data
      if (status === 409) {
        const map = typeof data === 'object' && data ? data : { email: 'Email already in use' }
        throw map
      }
      if (status === 400 && typeof data === 'object' && data) {
        throw data
      }
      throw (data?.message || data?.error || 'Registration failed')
    }
  }

  const logout = () => {
    clearToken()
    setUser(null)
  }

  const value = useMemo(() => ({ user, login, register, logout, loading }), [user, loading])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
