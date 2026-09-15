import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearSession, getAccessToken, getRefreshToken, setSession } from '../stores/auth.js'

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

let refreshPromise = null

http.interceptors.request.use((config) => {
  const accessToken = getAccessToken()
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const request = error.config
    const canRefresh = error.response?.status === 401
      && !request?.skipAuthRefresh
      && !request?._retried
      && Boolean(getRefreshToken())

    if (canRefresh) {
      request._retried = true
      try {
        refreshPromise ??= axios.post('/api/v1/auth/token/refresh', {
          refreshToken: getRefreshToken(),
        }).then((response) => response.data.data)
        const tokens = await refreshPromise
        setSession(null, tokens)
        request.headers.Authorization = `Bearer ${tokens.accessToken}`
        return http(request)
      } catch (refreshError) {
        clearSession()
        if (window.location.pathname !== '/login') window.location.assign('/login')
        return Promise.reject(refreshError)
      } finally {
        refreshPromise = null
      }
    }

    const silentStatuses = request?.silentStatuses ?? []
    const status = error.response?.status
    if (!silentStatuses.includes(status)) {
      const message = error.response?.data?.error?.message ?? '网络异常，请稍后重试'
      ElMessage.error(message)
    }
    return Promise.reject(error)
  },
)

export default http
