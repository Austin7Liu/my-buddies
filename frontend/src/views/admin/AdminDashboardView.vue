<script setup>
import { onMounted, ref } from 'vue'
import { listAdminCircles, listAdminPosts, listAdminMeetups } from '../../api/admin.js'

const cards = ref([
  { label: '待审核 Circle', value: '-', to: '/admin/circles' },
  { label: '待审核 Post', value: '-', to: '/admin/posts' },
  { label: '进行中 Meetup', value: '-', to: '/admin/meetups' },
])

onMounted(async () => {
  const [circles, posts, meetups] = await Promise.all([listAdminCircles('PENDING_REVIEW', 1, 1), listAdminPosts('PENDING_REVIEW', 1, 1), listAdminMeetups('OPEN', 1, 1)])
  cards.value[0].value = circles.data.total
  cards.value[1].value = posts.data.total
  cards.value[2].value = meetups.data.total
})
</script>

<template><section><p class="eyebrow accent">ADMIN CONSOLE</p><h1 class="admin-title">内容管理概览</h1><div class="admin-stats"><RouterLink v-for="card in cards" :key="card.label" :to="card.to"><span>{{ card.value }}</span><strong>{{ card.label }}</strong><small>查看列表 →</small></RouterLink></div></section></template>
