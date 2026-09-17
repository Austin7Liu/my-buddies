const REASON_LABELS = {
  SPAM: '垃圾广告',
  HARASSMENT: '骚扰攻击',
  PORNOGRAPHY: '色情低俗',
  VIOLENCE: '暴力内容',
  FRAUD: '欺诈信息',
  PRIVACY: '泄露隐私',
  ILLEGAL: '违法内容',
  OTHER: '其他',
}

const STATUS_LABELS = {
  PENDING: '待处理',
  RESOLVED: '举报成立',
  REJECTED: '举报驳回',
  DUPLICATE: '重复举报',
}

export const reportReasonOptions = Object.entries(REASON_LABELS)
  .map(([value, label]) => ({ value, label }))

export const reportReasonLabel = (value) => REASON_LABELS[value] ?? value
export const reportStatusLabel = (value) => STATUS_LABELS[value] ?? value
export const reportTargetLabel = (value) => value === 'POST_COMMENT' ? '评论' : '帖子'

export function validateReport(reasonType, description) {
  if (!reasonType) return '请选择举报原因'
  if (reasonType === 'OTHER' && !description.trim()) return '选择其他原因时必须填写说明'
  if (description.length > 500) return '举报说明不能超过 500 个字符'
  return ''
}
