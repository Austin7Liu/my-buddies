import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../api/notification.js', () => ({
  getUnreadNotificationCount: vi.fn(),
  readAllNotifications: vi.fn(),
  readNotification: vi.fn(),
}))

import {
  getUnreadNotificationCount,
  readAllNotifications,
  readNotification,
} from '../api/notification.js'
import {
  clearNotificationState,
  loadUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
  notificationState,
} from './notification.js'

describe('notification store', () => {
  beforeEach(() => {
    clearNotificationState()
    vi.clearAllMocks()
  })

  it('deduplicates concurrent unread count requests', async () => {
    getUnreadNotificationCount.mockResolvedValue({ data: { count: 3 } })

    const [first, second] = await Promise.all([
      loadUnreadNotificationCount(),
      loadUnreadNotificationCount(),
    ])

    expect(getUnreadNotificationCount).toHaveBeenCalledTimes(1)
    expect(first).toBe(3)
    expect(second).toBe(3)
    expect(notificationState.unreadCount).toBe(3)
  })

  it('decrements only when an unread notification becomes read', async () => {
    getUnreadNotificationCount.mockResolvedValue({ data: { count: 2 } })
    readNotification.mockResolvedValue({ data: { id: '1', read: true } })
    await loadUnreadNotificationCount()

    await markNotificationRead({ id: '1', read: false })
    await markNotificationRead({ id: '1', read: true })

    expect(notificationState.unreadCount).toBe(1)
  })

  it('clears the count after marking all as read', async () => {
    getUnreadNotificationCount.mockResolvedValue({ data: { count: 4 } })
    readAllNotifications.mockResolvedValue({ data: { affected: 4 } })
    await loadUnreadNotificationCount()

    expect(await markAllNotificationsRead()).toBe(4)
    expect(notificationState.unreadCount).toBe(0)
  })
})
