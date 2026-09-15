import http from './http.js'

export const search = (params) => http.get('/search', { params })
