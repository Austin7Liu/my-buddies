import { describe, expect, it } from 'vitest'
import { adminRoleLabel, canAccessRoles } from './adminRole.js'

describe('admin role helpers', () => {
  it('allows super administrators to access every role route', () => {
    expect(canAccessRoles(['SUPER_ADMIN'], ['RISK_REVIEWER'])).toBe(true)
    expect(canAccessRoles(['CONTENT_ADMIN'], ['RISK_REVIEWER'])).toBe(false)
    expect(canAccessRoles(['RISK_REVIEWER'], [])).toBe(true)
  })

  it('renders role labels', () => {
    expect(adminRoleLabel('CONTENT_ADMIN')).toBe('内容管理员')
  })
})
