<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { authState } from '../stores/auth.js'
import { canAccessRoles } from '../utils/adminRole.js'

const router = useRouter()
const allMenus = [
  { to: '/admin', label: '概览', roles: [] },
  { to: '/admin/circles', label: 'Circle 审核', roles: ['CONTENT_ADMIN'] },
  { to: '/admin/posts', label: 'Post 审核', roles: ['CONTENT_ADMIN'] },
  { to: '/admin/comments', label: '评论管理', roles: ['CONTENT_ADMIN'] },
  { to: '/admin/meetup-reviews', label: '活动评价管理', roles: ['CONTENT_ADMIN'] },
  { to: '/admin/reports', label: '内容举报', roles: ['CONTENT_ADMIN'] },
  { to: '/admin/appeals', label: '内容申诉', roles: ['CONTENT_ADMIN'] },
  { to: '/admin/violations', label: '违规记录', roles: ['CONTENT_ADMIN'] },
  { to: '/admin/catalog', label: '分类与话题', roles: ['CONTENT_ADMIN'] },
  { to: '/admin/meetups', label: 'Meetup 管理', roles: ['CONTENT_ADMIN'] },
  { to: '/admin/search', label: '搜索索引', roles: ['CONTENT_ADMIN'] },
  { to: '/admin/risk', label: '风控限制', roles: ['RISK_REVIEWER'] },
  { to: '/admin/roles', label: '角色管理', roles: ['SUPER_ADMIN'] },
]
const menus = computed(() => allMenus.filter((menu) => canAccessRoles(authState.roles, menu.roles)))
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="brand admin-brand"><span class="brand-mark">MB</span><span>管理后台</span></div>
      <nav><RouterLink v-for="menu in menus" :key="menu.to" :to="menu.to" exact-active-class="active">{{ menu.label }}</RouterLink></nav>
      <div class="admin-account"><strong>{{ authState.account?.maskedPhone }}</strong><span>{{ authState.roles.join(' · ') }}</span><el-button text @click="router.push('/')">返回用户端</el-button></div>
    </aside>
    <main class="admin-content"><RouterView /></main>
  </div>
</template>
