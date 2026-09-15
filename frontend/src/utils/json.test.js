import { describe, expect, it } from 'vitest'
import JSONbig from 'json-bigint'

describe('large JSON identifiers', () => {
  it('keeps snowflake ids as exact strings', () => {
    const value = JSONbig({ storeAsString: true }).parse('{"id":900000000000000001}')

    expect(value.id).toBe('900000000000000001')
  })
})
