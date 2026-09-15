import { beforeEach, describe, expect, it, vi } from 'vitest'

function memoryStorage() {
  const values = new Map()
  return {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  }
}

describe('auth store', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.stubGlobal('localStorage', memoryStorage())
  })

  it('persists and clears a login session', async () => {
    const auth = await import('./auth.js')
    auth.setSession({ id: 1 }, { accessToken: 'access', refreshToken: 'refresh' })

    expect(auth.isAuthenticated()).toBe(true)
    expect(auth.getAccessToken()).toBe('access')
    expect(JSON.parse(localStorage.getItem('my-buddies-session')).account.id).toBe(1)

    auth.clearSession()

    expect(auth.isAuthenticated()).toBe(false)
    expect(localStorage.getItem('my-buddies-session')).toBeNull()
  })

  it('recognizes effective content administration roles', async () => {
    const auth = await import('./auth.js')
    auth.setRoles(['SUPER_ADMIN'])

    expect(auth.rolesAreLoaded()).toBe(true)
    expect(auth.isContentAdmin()).toBe(true)
  })
})
