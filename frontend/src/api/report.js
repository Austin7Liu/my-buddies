import http from './http.js'

export const reportPost = (postId, payload) => http.post(`/reports/posts/${postId}`, payload)
export const reportPostComment = (commentId, payload) => http.post(`/reports/post-comments/${commentId}`, payload)
export const listMyReports = (page = 1, size = 20) => http.get('/reports/mine', { params: { page, size } })
export const getMyReport = (reportId) => http.get(`/reports/${reportId}`)
export const listAdminReports = (status, page = 1, size = 20) => http.get('/admin/content-reports', {
  params: { status: status || undefined, page, size },
})
export const getAdminReport = (reportId) => http.get(`/admin/content-reports/${reportId}`)
export const resolveReport = (reportId, resolutionNote) => http.post(`/admin/content-reports/${reportId}/resolve`, { resolutionNote })
export const rejectReport = (reportId, resolutionNote) => http.post(`/admin/content-reports/${reportId}/reject`, { resolutionNote })
export const duplicateReport = (reportId, resolutionNote) => http.post(`/admin/content-reports/${reportId}/duplicate`, { resolutionNote })
