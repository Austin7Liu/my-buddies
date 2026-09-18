import { reactive, readonly } from 'vue'

const STORAGE_KEY = 'my-buddies-session'

function readSession() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) ?? {}
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return {}
  }
}

const saved = readSession()
const state = reactive({
  account: saved.account ?? null,
  accessToken: saved.accessToken ?? '',
  refreshToken: saved.refreshToken ?? '',
  roles: saved.roles ?? [],
  rolesLoaded: false,
})

function persist() {
  if (!state.accessToken || !state.refreshToken) {
    localStorage.removeItem(STORAGE_KEY)
    return
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify({
    account: state.account,
    accessToken: state.accessToken,
    refreshToken: state.refreshToken,
    roles: state.roles,
  }))
}

export function setSession(account, tokens) {
  state.account = account ?? state.account
  state.accessToken = tokens.accessToken
  state.refreshToken = tokens.refreshToken
  persist()
}

export function clearSession() {
  state.account = null
  state.accessToken = ''
  state.refreshToken = ''
  state.roles = []
  state.rolesLoaded = false
  persist()
}

export function setRoles(roles) {
  state.roles = [...roles]
  state.rolesLoaded = true
  persist()
}

export const authState = readonly(state)
export const getAccessToken = () => state.accessToken
export const getRefreshToken = () => state.refreshToken
export const isAuthenticated = () => Boolean(state.accessToken && state.refreshToken)
export const rolesAreLoaded = () => state.rolesLoaded
export const hasRole = (role) => state.roles.includes(role)
export const isContentAdmin = () => hasRole('CONTENT_ADMIN') || hasRole('SUPER_ADMIN')
export const hasAnyRole = (roles) => hasRole('SUPER_ADMIN') || roles.some((role) => hasRole(role))
export const isAdmin = () => hasAnyRole(['CONTENT_ADMIN', 'RISK_REVIEWER', 'SECURITY_REVIEWER'])
export const isRiskReviewer = () => hasAnyRole(['RISK_REVIEWER'])
export const isSuperAdmin = () => hasRole('SUPER_ADMIN')
