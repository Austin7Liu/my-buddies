import { describe, expect, it, vi } from 'vitest'
import { createLocationCheckInAction, geolocationErrorMessage, loadExistingCheckIn } from './meetupGeolocation.js'

function geolocationSuccess(coords) {
  return { getCurrentPosition: (success) => success({ coords }) }
}

function geolocationFailure(code) {
  return { getCurrentPosition: (_success, failure) => failure({ code }) }
}

describe('meetup browser geolocation', () => {
  it('submits browser latitude, longitude and accuracy with realtime options', async () => {
    const submit = vi.fn().mockResolvedValue({ data: { checkedInAt: '2026-09-16 18:00:00', distanceMeters: 12 } })
    const getCurrentPosition = vi.fn((success) => success({ coords: { latitude: 30.2, longitude: 120.1, accuracy: 18.5 } }))
    const action = createLocationCheckInAction({ getCurrentPosition }, submit)
    const response = await action.execute('9001')
    expect(submit).toHaveBeenCalledWith('9001', { latitude: 30.2, longitude: 120.1, accuracyMeters: 18.5 })
    expect(getCurrentPosition.mock.calls[0][2]).toEqual({ enableHighAccuracy: true, timeout: 10000, maximumAge: 0 })
    expect(response.data).toEqual({ checkedInAt: '2026-09-16 18:00:00', distanceMeters: 12 })
  })

  it.each([
    [1, '你拒绝了定位授权，无法完成签到'],
    [2, '当前位置不可用，请检查系统定位服务后重试'],
    [3, '获取位置超时，请到开阔处后重试'],
  ])('does not submit when geolocation fails with code %s', async (code, message) => {
    const submit = vi.fn()
    const action = createLocationCheckInAction(geolocationFailure(code), submit)
    await expect(action.execute('9001')).rejects.toEqual({ code })
    expect(submit).not.toHaveBeenCalled()
    expect(geolocationErrorMessage({ code })).toBe(message)
  })

  it('uses one in-flight request for repeated clicks', async () => {
    let resolveSubmit
    const submit = vi.fn(() => new Promise((resolve) => { resolveSubmit = resolve }))
    const action = createLocationCheckInAction(
      geolocationSuccess({ latitude: 30.2, longitude: 120.1, accuracy: 15 }), submit,
    )
    const first = action.execute('9001')
    const second = action.execute('9001')
    expect(first).toBe(second)
    await Promise.resolve()
    expect(submit).toHaveBeenCalledTimes(1)
    resolveSubmit({ data: { checkedInAt: '2026-09-16 18:00:00', distanceMeters: 8 } })
    await first
  })

  it('treats only the current check-in 404 as not checked in', async () => {
    const fetchCheckIn = vi.fn().mockRejectedValue({ response: { status: 404 } })
    await expect(loadExistingCheckIn('9001', fetchCheckIn)).resolves.toBeNull()
    expect(fetchCheckIn).toHaveBeenCalledWith('9001')
  })

  it('returns an existing check-in without requesting browser location again', async () => {
    const geolocation = { getCurrentPosition: vi.fn() }
    const submit = vi.fn()
    const existing = { checkedInAt: '2026-09-16 18:00:00', distanceMeters: 8 }
    const action = createLocationCheckInAction(geolocation, submit)
    await expect(action.execute('9001', existing)).resolves.toEqual({ data: existing })
    expect(geolocation.getCurrentPosition).not.toHaveBeenCalled()
    expect(submit).not.toHaveBeenCalled()
  })
})
