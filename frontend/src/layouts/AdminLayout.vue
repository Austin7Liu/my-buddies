<script setup>
import { useRouter } from 'vue-router'
import { authState } from '../stores/auth.js'

const router = useRouter()
const menus = [
  { to: '/admin', label: '概览' },
  { to: '/admin/circles', label: 'Circle 审核' },
  { to: '/admin/posts', label: 'Post 审核' },
  { to: '/admin/meetups', label: 'Meetup 管理' },
  { to: '/admin/search', label: '搜索索引' },
]
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
