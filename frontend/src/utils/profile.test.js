import { describe, expect, it } from 'vitest'
import { avatarImage, avatarLabel, genderLabel, percentageLabel } from './profile.js'

describe('profile helpers', () => {
  it('maps safe profile values for display', () => {
    expect(avatarLabel('PANDA')).toBe('熊猫')
    expect(avatarImage('FOX')).toContain('fox.png')
    expect(avatarImage('UNKNOWN')).toContain('panda.png')
    expect(genderLabel('FEMALE')).toBe('女')
    expect(percentageLabel('87.50')).toBe('87.50%')
    expect(percentageLabel(null)).toBe('暂无数据')
  })
})
