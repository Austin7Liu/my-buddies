import { reactive, readonly } from 'vue'
import { getMyProfile } from '../api/profile.js'

const state = reactive({ profile: null })
let loadingPromise = null

export function loadCurrentProfile() {
  loadingPromise ??= getMyProfile()
    .then((response) => {
      state.profile = response.data
      return state.profile
    })
    .finally(() => {
      loadingPromise = null
    })
  return loadingPromise
}

export function setCurrentProfile(profile) {
  state.profile = profile
}

export function clearCurrentProfile() {
  state.profile = null
}

export const profileState = readonly(state)
