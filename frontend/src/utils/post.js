const STATUS_LABELS = {
  PENDING_REVIEW: '待审核',
  PUBLISHED: '已发布',
  REJECTED: '已驳回',
  OFFLINE: '已下架',
  DELETED: '已删除',
}

export function postStatusLabel(status) {
  return STATUS_LABELS[status] ?? status ?? '未知状态'
}

export function postAssociationPayload(topicId, circleId) {
  if (circleId) return { topicId: null, circleId: String(circleId) }
  if (topicId) return { topicId: String(topicId), circleId: null }
  return { topicId: null, circleId: null }
}

const COMMENT_STATUS_LABELS = {
  VISIBLE: '正常',
  DELETED_BY_AUTHOR: '该评论已被作者删除',
  HIDDEN_BY_ADMIN: '该评论因违反社区规范已被隐藏',
}

export function commentStatusLabel(status) {
  return COMMENT_STATUS_LABELS[status] ?? '该评论暂不可见'
}

export function validateComment(content) {
  const value = content.trim()
  if (!value) return '评论内容不能为空'
  if (value.length > 500) return '评论不能超过 500 个字符'
  return ''
}
