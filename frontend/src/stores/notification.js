import { reactive, readonly } from 'vue'
import {
  getUnreadNotificationCount,
  readAllNotifications,
  readNotification,
} from '../api/notification.js'

const state = reactive({ unreadCount: 0, loaded: false })
let loadingPromise = null

export function loadUnreadNotificationCount() {
  loadingPromise ??= getUnreadNotificationCount()
    .then((response) => {
      state.unreadCount = Number(response.data.count)
      state.loaded = true
      return state.unreadCount
    })
    .finally(() => {
      loadingPromise = null
    })
  return loadingPromise
}

export async function markNotificationRead(notification) {
  const response = await readNotification(notification.id)
  if (!notification.read && state.unreadCount > 0) state.unreadCount -= 1
  return response.data
}

export async function markAllNotificationsRead() {
  const response = await readAllNotifications()
  state.unreadCount = 0
  state.loaded = true
  return response.data.affected
}

export function clearNotificationState() {
  state.unreadCount = 0
  state.loaded = false
}

export const notificationState = readonly(state)
