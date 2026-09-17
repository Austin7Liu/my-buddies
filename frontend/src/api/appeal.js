import http from './http.js'

export const createContentAppeal = (reportId, reason) => http.post('/content-appeals', { reportId, reason })
export const listMyAppeals = (page = 1, size = 20) => http.get('/content-appeals/mine', { params: { page, size } })
export const getMyAppeal = (appealId) => http.get(`/content-appeals/${appealId}`)
export const listAdminAppeals = (status, page = 1, size = 20) => http.get('/admin/content-appeals', {
  params: { status: status || undefined, page, size },
})
export const getAdminAppeal = (appealId) => http.get(`/admin/content-appeals/${appealId}`)
export const approveAppeal = (appealId, reviewNote) => http.post(`/admin/content-appeals/${appealId}/approve`, { reviewNote })
export const rejectAppeal = (appealId, reviewNote) => http.post(`/admin/content-appeals/${appealId}/reject`, { reviewNote })
export const closeAppeal = (appealId, reviewNote) => http.post(`/admin/content-appeals/${appealId}/close`, { reviewNote })
