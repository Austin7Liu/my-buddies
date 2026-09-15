import http from './http.js'

export const listTopicCircles = (topicId, page = 1, size = 20) => http.get(`/topics/${topicId}/circles`, {
  params: { page, size },
})
export const getCircle = (circleId) => http.get(`/circles/${circleId}`)
export const getMyMembership = (circleId) => http.get(`/circles/${circleId}/memberships/me`, {
  silentStatuses: [404],
})
export const listCircleMembers = (circleId, page = 1, size = 20) => http.get(`/circles/${circleId}/members`, {
  params: { page, size },
})
export const joinCircle = (circleId) => http.post(`/circles/${circleId}/memberships`)
export const leaveCircle = (circleId) => http.delete(`/circles/${circleId}/memberships/me`)
export const createCircle = (payload) => http.post('/circles', payload)
export const listCreatedCircles = (page = 1, size = 20) => http.get('/circles/mine', {
  params: { page, size },
})
export const listJoinedCircles = (page = 1, size = 20) => http.get('/me/circles', {
  params: { page, size },
})
