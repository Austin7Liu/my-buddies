import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../api/identity.js', () => ({
  getMyIdentityVerification: vi.fn(),
}))

import { getMyIdentityVerification } from '../api/identity.js'
import {
  clearIdentityVerification,
  identityState,
  loadIdentityVerification,
  setIdentityVerification,
} from './identity.js'

describe('identity store', () => {
  beforeEach(() => {
    clearIdentityVerification()
    vi.clearAllMocks()
  })

  it('deduplicates concurrent status requests and keeps only the result in memory', async () => {
    getMyIdentityVerification.mockResolvedValue({ data: { status: 'VERIFIED' } })

    const [first, second] = await Promise.all([
      loadIdentityVerification(),
      loadIdentityVerification(),
    ])

    expect(getMyIdentityVerification).toHaveBeenCalledTimes(1)
    expect(first).toEqual({ status: 'VERIFIED' })
    expect(second).toEqual(first)
    expect(identityState.verification).toEqual(first)
  })

  it('replaces and clears the in-memory result', () => {
    setIdentityVerification({ status: 'FAILED' })
    expect(identityState.verification.status).toBe('FAILED')

    clearIdentityVerification()
    expect(identityState.verification).toBeNull()
  })
})
