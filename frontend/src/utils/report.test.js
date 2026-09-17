import { describe, expect, it } from 'vitest'
import { reportReasonLabel, reportStatusLabel, validateReport } from './report.js'

describe('report helpers', () => {
  it('validates the conditional description rule', () => {
    expect(validateReport('', '')).toBe('请选择举报原因')
    expect(validateReport('OTHER', '   ')).toBe('选择其他原因时必须填写说明')
    expect(validateReport('OTHER', '其他问题')).toBe('')
    expect(validateReport('SPAM', '')).toBe('')
  })

  it('maps report values for display', () => {
    expect(reportReasonLabel('FRAUD')).toBe('欺诈信息')
    expect(reportStatusLabel('RESOLVED')).toBe('举报成立')
  })
})
