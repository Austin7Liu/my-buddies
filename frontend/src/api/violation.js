import http from './http.js'

export const listMyViolations = (page = 1, size = 20) => http.get('/violations/mine', { params: { page, size } })
export const getMyViolation = (violationId) => http.get(`/violations/${violationId}`)
export const confirmViolation = (reportId, payload) => http.post(`/admin/content-reports/${reportId}/violations`, payload)
export const listAdminViolations = (accountId, page = 1, size = 20) => http.get('/admin/content-violations', {
  params: { accountId, page, size },
})
export const getAdminViolation = (violationId) => http.get(`/admin/content-violations/${violationId}`)
export const revokeViolation = (violationId, payload) => http.post(`/admin/content-violations/${violationId}/revoke`, payload)
