import http from './http.js'

export const getMyAccount = () => http.get('/accounts/me')
export const requestAccountCancellation = () => http.post('/accounts/me/cancellation')
export const revokeAccountCancellation = () => http.post('/accounts/me/cancellation/revoke')
