<script setup>
import { computed } from 'vue'
import { avatarImage, avatarLabel } from '../utils/profile.js'

const props = defineProps({
  avatarCode: { type: String, default: 'PANDA' },
  nickname: { type: String, default: '用户' },
  size: { type: Number, default: 44 },
})

const imageUrl = computed(() => avatarImage(props.avatarCode))
const alternativeText = computed(() => `${props.nickname}的${avatarLabel(props.avatarCode)}头像`)
</script>

<template>
  <img
    class="user-avatar"
    :src="imageUrl"
    :alt="alternativeText"
    :width="size"
    :height="size"
  />
</template>
