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
  persist()
}

export const authState = readonly(state)
export const getAccessToken = () => state.accessToken
export const getRefreshToken = () => state.refreshToken
export const isAuthenticated = () => Boolean(state.accessToken && state.refreshToken)
