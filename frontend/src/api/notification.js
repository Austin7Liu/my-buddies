import http from './http.js'

export const listNotifications = (unreadOnly = false, page = 1, size = 20) => http.get('/notifications', {
  params: { unreadOnly, page, size },
})
export const getUnreadNotificationCount = () => http.get('/notifications/unread-count')
export const readNotification = (notificationId) => http.post(`/notifications/${notificationId}/read`)
export const readAllNotifications = () => http.post('/notifications/read-all')
