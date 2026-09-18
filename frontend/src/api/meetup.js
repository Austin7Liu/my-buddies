import http from './http.js'

export const listMeetups = (page = 1, size = 20) => http.get('/meetups', { params: { page, size } })
export const listMyMeetups = (role, status, page = 1, size = 20) => http.get('/meetups/mine', {
  params: { role: role || undefined, status: status || undefined, page, size },
})
export const listTopicMeetups = (topicId, page = 1, size = 20) => http.get(`/topics/${topicId}/meetups`, { params: { page, size } })
export const listCircleMeetups = (circleId, page = 1, size = 20) => http.get(`/circles/${circleId}/meetups`, { params: { page, size } })
export const getMeetup = (meetupId) => http.get(`/meetups/${meetupId}`)
export const createMeetup = (payload) => http.post('/meetups', payload)
export const updateMeetup = (meetupId, payload) => http.put(`/meetups/${meetupId}`, payload)
export const publishMeetup = (meetupId) => http.post(`/meetups/${meetupId}/publish`)
export const confirmMeetup = (meetupId) => http.post(`/meetups/${meetupId}/confirm`)
export const completeMeetup = (meetupId) => http.post(`/meetups/${meetupId}/complete`)
export const cancelMeetup = (meetupId, reason) => http.post(`/meetups/${meetupId}/cancel`, { reason })
export const applyMeetup = (meetupId, message) => http.post(`/meetups/${meetupId}/applications`, { message: message || null })
export const getMyMeetupParticipation = (meetupId) => http.get(`/meetups/${meetupId}/participation/me`, {
  silentStatuses: [404],
})
export const listMeetupApplications = (meetupId, page = 1, size = 20) => http.get(`/meetups/${meetupId}/applications`, { params: { page, size } })
export const acceptMeetupApplication = (meetupId, accountId) => http.post(`/meetups/${meetupId}/applications/${accountId}/accept`)
export const rejectMeetupApplication = (meetupId, accountId, reason) => http.post(`/meetups/${meetupId}/applications/${accountId}/reject`, { reason })
export const withdrawMeetup = (meetupId, reason) => http.post(`/meetups/${meetupId}/withdraw`, { reason })
export const checkInMeetup = (meetupId, location) => http.post(`/meetups/${meetupId}/check-ins`, location)
export const getMyMeetupCheckIn = (meetupId) => http.get(`/meetups/${meetupId}/check-ins/me`, {
  silentStatuses: [404],
})
export const listMeetupCheckIns = (meetupId, page = 1, size = 100) => http.get(`/meetups/${meetupId}/check-ins`, { params: { page, size } })
export const getMyMeetupFulfillment = (meetupId) => http.get(`/meetups/${meetupId}/fulfillment/me`, {
  silentStatuses: [404],
})
export const listMeetupFulfillments = (meetupId, page = 1, size = 100) => http.get(`/meetups/${meetupId}/fulfillments`, { params: { page, size } })
export const listMeetupReviewCandidates = (meetupId) => http.get(`/meetups/${meetupId}/review-candidates`)
export const listMyMeetupReviews = (meetupId, page = 1, size = 100) => http.get(`/meetups/${meetupId}/reviews/mine`, { params: { page, size } })
export const createMeetupReview = (meetupId, payload) => http.post(`/meetups/${meetupId}/reviews`, payload)
export const updateMeetupReview = (meetupId, reviewId, payload) => http.put(`/meetups/${meetupId}/reviews/${reviewId}`, payload)
