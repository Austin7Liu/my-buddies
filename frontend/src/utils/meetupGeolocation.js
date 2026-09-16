export function geolocationErrorMessage(error) {
  if (error?.code === 'UNSUPPORTED') return '当前浏览器不支持定位，请更换浏览器后重试'
  if (error?.code === 1) return '你拒绝了定位授权，无法完成签到'
  if (error?.code === 2) return '当前位置不可用，请检查系统定位服务后重试'
  if (error?.code === 3) return '获取位置超时，请到开阔处后重试'
  return '获取实时位置失败，请稍后重试'
}

export function getCurrentLocation(geolocation) {
  if (!geolocation) return Promise.reject({ code: 'UNSUPPORTED' })
  return new Promise((resolve, reject) => {
    geolocation.getCurrentPosition(
      (position) => resolve({
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
        accuracyMeters: position.coords.accuracy,
      }),
      reject,
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 },
    )
  })
}

export async function loadExistingCheckIn(meetupId, fetchCheckIn) {
  try {
    return (await fetchCheckIn(meetupId)).data
  } catch (error) {
    if (error.response?.status === 404) return null
    throw error
  }
}

export function createLocationCheckInAction(geolocation, submit) {
  let pending = null
  return {
    isPending: () => Boolean(pending),
    execute(meetupId, existingCheckIn = null) {
      if (existingCheckIn) return Promise.resolve({ data: existingCheckIn })
      if (pending) return pending
      pending = getCurrentLocation(geolocation)
        .then((location) => submit(meetupId, location))
        .finally(() => { pending = null })
      return pending
    },
  }
}
