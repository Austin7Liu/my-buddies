import http from './http.js'

export const sendSmsCode = (phone) => http.post('/auth/sms-codes', { phone })

export const loginBySms = (phone, code) => http.post('/auth/token', { phone, code })

export const refreshTokens = (refreshToken) => http.post(
  '/auth/token/refresh',
  { refreshToken },
  { skipAuthRefresh: true },
)

export const logoutSession = (refreshToken) => http.post('/auth/logout', { refreshToken })
