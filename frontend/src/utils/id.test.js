import { describe, expect, it } from 'vitest'
import { sameId } from './id.js'

describe('sameId', () => {
  it('matches a numeric API id with a route query string id', () => {
    expect(sameId(1003, '1003')).toBe(true)
  })

  it('does not match different or absent ids', () => {
    expect(sameId(1003, '1004')).toBe(false)
    expect(sameId(null, null)).toBe(false)
  })
})
