const TYPE_LABELS = {
  MEETUP_APPLICATION_ACCEPTED: '报名通过',
  MEETUP_APPLICATION_REJECTED: '报名未通过',
  MEETUP_CANCELLED: '活动取消',
  MEETUP_TERMINATED: '活动终止',
  FULFILLMENT_ADJUSTED: '履约修正',
  REVIEW_HIDDEN: '评价隐藏',
  REVIEW_RESTORED: '评价恢复',
  RISK_RESTRICTION_CREATED: '账户限制',
  RISK_RESTRICTION_REVOKED: '限制解除',
  POST_COMMENTED: '帖子评论',
  COMMENT_REPLIED: '评论回复',
  REPORT_RESOLVED: '举报成立',
  REPORT_REJECTED: '举报驳回',
  REPORTED_CONTENT_OFFLINED: '内容下架',
  REPORTED_COMMENT_HIDDEN: '评论隐藏',
  CONTENT_APPEAL_APPROVED: '申诉通过',
  CONTENT_APPEAL_REJECTED: '申诉驳回',
  CONTENT_APPEAL_CLOSED: '申诉关闭',
  CONTENT_VIOLATION_CONFIRMED: '违规确认',
  CONTENT_VIOLATION_REVOKED: '违规撤销',
  POST_LIKED: '帖子点赞',
}

export function notificationTypeLabel(type) {
  return TYPE_LABELS[type] ?? '系统通知'
}

export function notificationTarget(notification) {
  const referenceId = String(notification.referenceId)
  if (notification.referenceType === 'MEETUP') {
    return { name: 'meetup-detail', params: { meetupId: referenceId } }
  }
  if (notification.referenceType === 'POST_LIKE') {
    return { name: 'post-detail', params: { postId: referenceId } }
  }
  if (notification.referenceType === 'CONTENT_REPORT') {
    if (['REPORTED_CONTENT_OFFLINED', 'REPORTED_COMMENT_HIDDEN'].includes(notification.notificationType)) {
      return { name: 'appeal-create', query: { reportId: referenceId } }
    }
    return { name: 'my-reports', query: { reportId: referenceId } }
  }
  if (notification.referenceType === 'CONTENT_APPEAL') {
    return { name: 'my-appeals', query: { appealId: referenceId } }
  }
  if (notification.referenceType === 'CONTENT_VIOLATION') {
    return { name: 'account-safety', query: { tab: 'violations', violationId: referenceId } }
  }
  if (notification.referenceType === 'RISK_RESTRICTION') {
    return { name: 'account-safety', query: { tab: 'restrictions', restrictionId: referenceId } }
  }
  return null
}

export function notificationHasTarget(notification) {
  return notification.referenceType === 'POST_COMMENT' || notificationTarget(notification) != null
}

export function postCommentNotificationTarget(location) {
  return {
    name: 'post-detail',
    params: { postId: String(location.postId) },
    query: { page: String(location.page), commentId: String(location.commentId) },
  }
}
