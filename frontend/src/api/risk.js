import http from './http.js'

export const listAccountRestrictions = (accountId, page = 1, size = 20) => http.get(`/admin/risk/accounts/${accountId}/restrictions`, { params: { page, size } })
export const createRestriction = (payload) => http.post('/admin/risk/restrictions', payload)
export const revokeRestriction = (restrictionId, reason) => http.post(`/admin/risk/restrictions/${restrictionId}/revoke`, { reason })
