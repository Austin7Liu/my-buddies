const STATUS_LABELS = {
  PENDING: '待处理',
  APPROVED: '申诉通过',
  REJECTED: '申诉驳回',
  CLOSED: '已关闭',
}

export function appealStatusLabel(status) {
  return STATUS_LABELS[status] ?? status
}

export function validateAppealReason(reason) {
  const normalized = reason?.trim() ?? ''
  if (!normalized) return '请填写申诉理由'
  if (normalized.length > 1000) return '申诉理由不能超过 1000 个字符'
  return ''
}
