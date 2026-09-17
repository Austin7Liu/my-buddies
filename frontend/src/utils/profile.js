import pandaAvatar from '../assets/avatars/panda.png'
import catAvatar from '../assets/avatars/cat.png'
import dogAvatar from '../assets/avatars/dog.png'
import foxAvatar from '../assets/avatars/fox.png'
import rabbitAvatar from '../assets/avatars/rabbit.png'

const AVATAR_LABELS = {
  PANDA: '熊猫',
  CAT: '猫咪',
  DOG: '狗狗',
  FOX: '狐狸',
  RABBIT: '兔子',
}

const AVATAR_IMAGES = {
  PANDA: pandaAvatar,
  CAT: catAvatar,
  DOG: dogAvatar,
  FOX: foxAvatar,
  RABBIT: rabbitAvatar,
}

export const avatarOptions = Object.entries(AVATAR_LABELS).map(([value, label]) => ({ value, label }))

export function avatarLabel(code) {
  return AVATAR_LABELS[code] ?? code ?? '默认头像'
}

export function avatarImage(code) {
  return AVATAR_IMAGES[code] ?? AVATAR_IMAGES.PANDA
}

export function genderLabel(gender) {
  if (gender === 'MALE') return '男'
  if (gender === 'FEMALE') return '女'
  return '未公开'
}

export function percentageLabel(value) {
  return value == null ? '暂无数据' : `${value}%`
}
