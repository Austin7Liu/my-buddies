import { describe, expect, it } from 'vitest'
import { appealStatusLabel, validateAppealReason } from './appeal.js'

describe('appeal helpers', () => {
  it('validates and trims appeal reasons', () => {
    expect(validateAppealReason('')).toBe('请填写申诉理由')
    expect(validateAppealReason(' '.repeat(3))).toBe('请填写申诉理由')
    expect(validateAppealReason('a'.repeat(1001))).toBe('申诉理由不能超过 1000 个字符')
    expect(validateAppealReason('说明')).toBe('')
  })

  it('renders appeal statuses', () => {
    expect(appealStatusLabel('APPROVED')).toBe('申诉通过')
    expect(appealStatusLabel('UNKNOWN')).toBe('UNKNOWN')
  })
})
