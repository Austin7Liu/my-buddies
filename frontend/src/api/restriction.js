import http from './http.js'

export const listMyRestrictions = (page = 1, size = 20) => http.get('/risk/restrictions/me', { params: { page, size } })
