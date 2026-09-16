const STATUS_LABELS = {
  UNVERIFIED: '未认证',
  VERIFYING: '认证中',
  VERIFIED: '已实名',
  FAILED: '认证失败',
}

const FAILURE_MESSAGES = {
  INVALID_REAL_NAME_FORMAT: '姓名格式不正确，请输入 2 至 50 位中文姓名',
  INVALID_IDENTITY_NUMBER: '身份证号码校验失败，请检查号码和校验位',
  INVALID_BIRTH_DATE: '身份证号码中的出生日期无效',
}

export function identityStatusLabel(status) {
  return STATUS_LABELS[status] ?? '状态未知'
}

export function identityFailureMessage(failureCode) {
  if (!failureCode) return ''
  return FAILURE_MESSAGES[failureCode] ?? '认证未通过，请检查填写信息'
}
