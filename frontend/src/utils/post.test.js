import { describe, expect, it } from 'vitest'
import { commentStatusLabel, postAssociationPayload, postStatusLabel, validateComment } from './post.js'

describe('post helpers', () => {
  it('lets the backend derive a circle post topic', () => {
    expect(postAssociationPayload('1003', '2001')).toEqual({ topicId: null, circleId: '2001' })
  })

  it('supports topic and unassociated posts', () => {
    expect(postAssociationPayload('1003', null)).toEqual({ topicId: '1003', circleId: null })
    expect(postAssociationPayload(null, null)).toEqual({ topicId: null, circleId: null })
  })

  it('maps moderation states', () => {
    expect(postStatusLabel('PENDING_REVIEW')).toBe('待审核')
    expect(postStatusLabel('REJECTED')).toBe('已驳回')
  })

  it('describes hidden comment states without exposing content', () => {
    expect(commentStatusLabel('DELETED_BY_AUTHOR')).toBe('该评论已被作者删除')
    expect(commentStatusLabel('HIDDEN_BY_ADMIN')).toBe('该评论因违反社区规范已被隐藏')
  })

  it('validates comment content', () => {
    expect(validateComment('  ')).toBe('评论内容不能为空')
    expect(validateComment('a'.repeat(501))).toBe('评论不能超过 500 个字符')
    expect(validateComment(' 一起打球 ')).toBe('')
  })
})
