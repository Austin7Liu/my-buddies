const SEVERITY_LABELS = { MINOR: '轻微', MODERATE: '中等', SEVERE: '严重' }
const PENALTY_LABELS = {
  WARNING_ONLY: '仅警告',
  POST_DISABLED: '禁止发布帖子',
  COMMENT_DISABLED: '禁止发表评论',
  CONTENT_CREATE_DISABLED: '禁止发布帖子和评论',
}
const VIOLATION_STATUS_LABELS = { ACTIVE: '生效中', REVOKED: '已撤销' }
const RESTRICTION_TYPE_LABELS = {
  MEETUP_CREATE_DISABLED: '禁止创建活动',
  MEETUP_JOIN_DISABLED: '禁止报名活动',
  POST_CREATE_DISABLED: '禁止发布帖子',
  COMMENT_CREATE_DISABLED: '禁止发表评论',
}
const RESTRICTION_STATUS_LABELS = { ACTIVE: '生效中', REVOKED: '已撤销', EXPIRED: '已到期' }

export const violationSeverityLabel = (value) => SEVERITY_LABELS[value] ?? value
export const violationPenaltyLabel = (value) => PENALTY_LABELS[value] ?? value
export const violationStatusLabel = (value) => VIOLATION_STATUS_LABELS[value] ?? value
export const restrictionTypeLabel = (value) => RESTRICTION_TYPE_LABELS[value] ?? value
export const restrictionStatusLabel = (value) => RESTRICTION_STATUS_LABELS[value] ?? value

export function validateViolationPenalty(form) {
  if (!form.severity) return '请选择违规等级'
  if (!form.penaltyType) return '请选择处罚类型'
  if (form.penaltyType === 'WARNING_ONLY' && form.penaltyExpiresAt) return '仅警告不能填写处罚到期时间'
  if (form.penaltyType !== 'WARNING_ONLY' && !form.penaltyExpiresAt) return '业务限制必须填写处罚到期时间'
  const note = form.note?.trim() ?? ''
  if (!note) return '请填写处罚说明'
  if (note.length > 500) return '处罚说明不能超过 500 个字符'
  return ''
}
