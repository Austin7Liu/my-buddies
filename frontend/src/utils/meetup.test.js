import { describe, expect, it } from 'vitest'
import {
  meetupAssociationPayload,
  fulfillmentResultLabel,
  meetupModeLabel,
  meetupStatusLabel,
  meetupTargetLocation,
  participantStatusLabel,
  validateMeetupReview,
  validateMeetupTimes,
} from './meetup.js'

describe('meetup helpers', () => {
  it('maps modes and lifecycle states', () => {
    expect(meetupModeLabel('ONLINE')).toBe('线上')
    expect(meetupStatusLabel('OPEN')).toBe('报名中')
    expect(participantStatusLabel('ACCEPTED')).toBe('已接受')
    expect(fulfillmentResultLabel('ATTENDED')).toBe('已出席')
  })

  it('lets the backend derive a circle meetup topic', () => {
    expect(meetupAssociationPayload('1003', '2001')).toEqual({ topicId: null, circleId: '2001' })
    expect(meetupAssociationPayload('1003', null)).toEqual({ topicId: '1003', circleId: null })
  })

  it('validates the activity time order', () => {
    expect(validateMeetupTimes('2026-10-02 10:00:00', '2026-10-02 12:00:00', '2026-10-01 20:00:00')).toBe('')
    expect(validateMeetupTimes('2026-10-02 10:00:00', '2026-10-02 09:00:00', '2026-10-01 20:00:00')).toBe('活动结束时间必须晚于开始时间')
  })

  it('uses browser coordinates as the meetup target location', () => {
    expect(meetupTargetLocation({ latitude: 30.2084, longitude: 120.2123 })).toEqual({
      locationLatitude: 30.2084,
      locationLongitude: 120.2123,
      checkInRadiusMeters: 300,
    })
  })

  it('validates meetup review input', () => {
    expect(validateMeetupReview(0, '')).toBe('请选择 1 到 5 星评分')
    expect(validateMeetupReview(5, '好'.repeat(501))).toBe('评价内容不能超过 500 个字符')
    expect(validateMeetupReview(5, '准时友善')).toBe('')
  })
})
