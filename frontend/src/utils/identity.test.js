import { describe, expect, it } from 'vitest'
import { identityFailureMessage, identityStatusLabel } from './identity.js'

describe('identity display mapping', () => {
  it('maps verification statuses', () => {
    expect(identityStatusLabel('UNVERIFIED')).toBe('未认证')
    expect(identityStatusLabel('VERIFIED')).toBe('已实名')
  })

  it('maps provider failure codes without exposing submitted identity data', () => {
    expect(identityFailureMessage('INVALID_REAL_NAME_FORMAT')).toContain('姓名格式')
    expect(identityFailureMessage('INVALID_IDENTITY_NUMBER')).toContain('身份证号码')
  })
})
