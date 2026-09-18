import { describe, expect, it } from 'vitest'
import { compactAdminFilters } from './adminModeration.js'

describe('admin moderation filters', () => {
  it('removes empty filters and keeps valid zero-free values', () => {
    expect(compactAdminFilters({ status: '', postId: '10', rating: 1, authorAccountId: null }))
      .toEqual({ postId: '10', rating: 1 })
  })
})
