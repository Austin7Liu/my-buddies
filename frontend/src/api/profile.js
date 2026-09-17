import http from './http.js'

export const getMyProfile = () => http.get('/profiles/me')
export const updateMyProfile = (payload) => http.put('/profiles/me', payload)
export const getPublicProfile = (accountId) => http.get(`/profiles/${accountId}`)
export const getPublicReputation = (accountId) => http.get(`/profiles/${accountId}/reputation`)
export const listReceivedMeetupReviews = (accountId, page = 1, size = 20) => http.get(`/profiles/${accountId}/reviews`, {
  params: { page, size },
})
