import { describe, expect, it } from 'vitest'
import { restrictionTypeLabel, validateViolationPenalty, violationPenaltyLabel } from './violation.js'

describe('violation helpers', () => {
  it('validates warning and restriction expiry contracts', () => {
    expect(validateViolationPenalty({ severity: 'MINOR', penaltyType: 'WARNING_ONLY', penaltyExpiresAt: '2026-10-01 12:00:00', note: '说明' })).toBe('仅警告不能填写处罚到期时间')
    expect(validateViolationPenalty({ severity: 'MODERATE', penaltyType: 'POST_DISABLED', penaltyExpiresAt: '', note: '说明' })).toBe('业务限制必须填写处罚到期时间')
    expect(validateViolationPenalty({ severity: 'MODERATE', penaltyType: 'POST_DISABLED', penaltyExpiresAt: '2026-10-01 12:00:00', note: '说明' })).toBe('')
  })

  it('renders penalty and restriction labels', () => {
    expect(violationPenaltyLabel('CONTENT_CREATE_DISABLED')).toBe('禁止发布帖子和评论')
    expect(restrictionTypeLabel('MEETUP_JOIN_DISABLED')).toBe('禁止报名活动')
  })
})
