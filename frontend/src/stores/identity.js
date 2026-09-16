import { reactive, readonly } from 'vue'
import { getMyIdentityVerification } from '../api/identity.js'

const state = reactive({ verification: null })
let loadingPromise = null

export function loadIdentityVerification() {
  loadingPromise ??= getMyIdentityVerification()
    .then((response) => {
      state.verification = response.data
      return state.verification
    })
    .finally(() => {
      loadingPromise = null
    })
  return loadingPromise
}

export function setIdentityVerification(verification) {
  state.verification = verification
}

export function clearIdentityVerification() {
  state.verification = null
}

export const identityState = readonly(state)
