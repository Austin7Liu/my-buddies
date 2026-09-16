import http from './http.js'

export const getMyIdentityVerification = () => http.get('/identity-verification/me')

export const submitIdentityVerification = (realName, identityNumber) => http.post(
  '/identity-verification',
  { realName, identityNumber },
)
