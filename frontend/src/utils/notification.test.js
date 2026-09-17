import { describe, expect, it } from 'vitest'
import {
  notificationHasTarget,
  notificationTarget,
  notificationTypeLabel,
  postCommentNotificationTarget,
} from './notification.js'

describe('notification helpers', () => {
  it('maps direct business references to existing routes', () => {
    expect(notificationTarget({ referenceType: 'MEETUP', referenceId: '21' })).toEqual({
      name: 'meetup-detail',
      params: { meetupId: '21' },
    })
    expect(notificationTarget({ referenceType: 'POST_LIKE', referenceId: '31' })).toEqual({
      name: 'post-detail',
      params: { postId: '31' },
    })
  })

  it('does not invent routes for references without a public detail endpoint', () => {
    expect(notificationTarget({ referenceType: 'POST_COMMENT', referenceId: '41' })).toBeNull()
    expect(notificationHasTarget({ referenceType: 'POST_COMMENT', referenceId: '41' })).toBe(true)
    expect(postCommentNotificationTarget({ postId: '31', commentId: '41', page: 2 })).toEqual({
      name: 'post-detail',
      params: { postId: '31' },
      query: { page: '2', commentId: '41' },
    })
    expect(notificationTypeLabel('COMMENT_REPLIED')).toBe('评论回复')
    expect(notificationTypeLabel('UNKNOWN')).toBe('系统通知')
  })
})
