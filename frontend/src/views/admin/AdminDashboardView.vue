<script setup>
import { onMounted, ref } from 'vue'
import { listAdminCircles, listAdminPosts, listAdminMeetups } from '../../api/admin.js'
import { authState } from '../../stores/auth.js'
import { canAccessRoles } from '../../utils/adminRole.js'

const cards = ref([])

onMounted(async () => {
  if (canAccessRoles(authState.roles, ['CONTENT_ADMIN'])) {
    const [circles, posts, meetups] = await Promise.all([listAdminCircles('PENDING_REVIEW', 1, 1), listAdminPosts('PENDING_REVIEW', 1, 1), listAdminMeetups('OPEN', 1, 1)])
    cards.value.push(
      { label: '待审核 Circle', value: circles.data.total, to: '/admin/circles' },
      { label: '待审核 Post', value: posts.data.total, to: '/admin/posts' },
      { label: '进行中 Meetup', value: meetups.data.total, to: '/admin/meetups' },
      { label: '分类与话题', value: '→', to: '/admin/catalog' },
      { label: '评论管理', value: '→', to: '/admin/comments' },
      { label: '活动评价管理', value: '→', to: '/admin/meetup-reviews' },
    )
  }
  if (canAccessRoles(authState.roles, ['RISK_REVIEWER'])) cards.value.push({ label: '风控限制', value: '→', to: '/admin/risk' })
  if (canAccessRoles(authState.roles, ['SUPER_ADMIN'])) cards.value.push({ label: '后台角色管理', value: '→', to: '/admin/roles' })
})
</script>

<template><section><p class="eyebrow accent">ADMIN CONSOLE</p><h1 class="admin-title">管理后台概览</h1><div class="admin-stats"><RouterLink v-for="card in cards" :key="card.label" :to="card.to"><span>{{ card.value }}</span><strong>{{ card.label }}</strong><small>查看功能 →</small></RouterLink></div><el-empty v-if="!cards.length" description="当前角色尚无可操作的后台模块" /></section></template>
