import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../api/profile.js', () => ({
  getMyProfile: vi.fn(),
}))

import { getMyProfile } from '../api/profile.js'
import {
  clearCurrentProfile,
  loadCurrentProfile,
  profileState,
  setCurrentProfile,
} from './profile.js'

describe('profile store', () => {
  beforeEach(() => {
    clearCurrentProfile()
    vi.clearAllMocks()
  })

  it('deduplicates concurrent profile requests', async () => {
    getMyProfile.mockResolvedValue({ data: { nickname: '网球搭子', avatarCode: 'FOX' } })

    const [first, second] = await Promise.all([
      loadCurrentProfile(),
      loadCurrentProfile(),
    ])

    expect(getMyProfile).toHaveBeenCalledTimes(1)
    expect(first).toEqual(second)
    expect(profileState.profile).toEqual(first)
  })

  it('replaces and clears the current profile', () => {
    setCurrentProfile({ nickname: '跑步搭子', avatarCode: 'RABBIT' })
    expect(profileState.profile.avatarCode).toBe('RABBIT')

    clearCurrentProfile()
    expect(profileState.profile).toBeNull()
  })
})
