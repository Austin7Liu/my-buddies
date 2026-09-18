const STATUS_LABELS = {
  DRAFT: '草稿',
  OPEN: '报名中',
  CONFIRMED: '已确认',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  TERMINATED: '已终止',
}

const PARTICIPANT_STATUS_LABELS = {
  APPLIED: '等待审核',
  ACCEPTED: '已接受',
  REJECTED: '已驳回',
  CANCELLED: '已退出',
}

const FULFILLMENT_RESULT_LABELS = {
  ATTENDED: '已出席',
  ABSENT: '缺席',
  EXCUSED: '已请假',
}

export function meetupStatusLabel(status) {
  return STATUS_LABELS[status] ?? status ?? '未知状态'
}

export function meetupModeLabel(mode) {
  return mode === 'ONLINE' ? '线上' : '线下'
}

export function participantStatusLabel(status) {
  return PARTICIPANT_STATUS_LABELS[status] ?? status ?? '尚未申请'
}

export function fulfillmentResultLabel(result) {
  return FULFILLMENT_RESULT_LABELS[result] ?? result ?? '尚未结算'
}

export function meetupAssociationPayload(topicId, circleId) {
  if (circleId) return { topicId: null, circleId: String(circleId) }
  return { topicId: topicId ? String(topicId) : null, circleId: null }
}

export function meetupTargetLocation(location, radius = 300) {
  return {
    locationLatitude: location.latitude,
    locationLongitude: location.longitude,
    checkInRadiusMeters: radius,
  }
}

export function validateMeetupTimes(startTime, endTime, applicationDeadline) {
  if (!startTime || !endTime || !applicationDeadline) return '请填写完整的活动时间'
  const start = new Date(startTime.replace(' ', 'T')).getTime()
  const end = new Date(endTime.replace(' ', 'T')).getTime()
  const deadline = new Date(applicationDeadline.replace(' ', 'T')).getTime()
  if (deadline >= start) return '报名截止时间必须早于活动开始时间'
  if (end <= start) return '活动结束时间必须晚于开始时间'
  return ''
}

export function validateMeetupReview(rating, comment) {
  if (!Number.isInteger(rating) || rating < 1 || rating > 5) return '请选择 1 到 5 星评分'
  if ((comment ?? '').trim().length > 500) return '评价内容不能超过 500 个字符'
  return ''
}
