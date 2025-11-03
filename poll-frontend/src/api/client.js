import axios from 'axios'
import { getToken, clearToken } from '../utils/storage'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || 'http://localhost:8765', // gateway default
  timeout: 10000,
})

api.defaults.headers.post['Content-Type'] = 'application/json'

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers = {
      ...(config.headers || {}),
      Authorization: `Bearer ${token}`,
    }
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err?.response?.status
    if (status === 401) {
      // Token invalid/expired → clear and let app redirect on next guarded request
      clearToken()
    }
    return Promise.reject(err)
  }
)

export default api
