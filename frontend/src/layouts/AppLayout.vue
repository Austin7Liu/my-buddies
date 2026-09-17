<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logoutSession } from '../api/auth.js'
import { clearSession, getRefreshToken, isAuthenticated } from '../stores/auth.js'
import { isContentAdmin, rolesAreLoaded, setRoles } from '../stores/auth.js'
import { getMyRoles } from '../api/admin.js'
import { clearIdentityVerification, identityState, loadIdentityVerification } from '../stores/identity.js'
import UserAvatar from '../components/UserAvatar.vue'
import { clearCurrentProfile, loadCurrentProfile, profileState } from '../stores/profile.js'
import {
  clearNotificationState,
  loadUnreadNotificationCount,
  notificationState,
} from '../stores/notification.js'

const route = useRoute()
const router = useRouter()
const loggingOut = ref(false)
const identityStatus = computed(() => identityState.verification?.status ?? '')

async function loadIdentityStatus() {
  if (!isAuthenticated()) {
    clearIdentityVerification()
    return
  }
  try {
    await loadIdentityVerification()
  } catch {
    clearIdentityVerification()
  }
}

async function loadAccountHeader() {
  if (!isAuthenticated()) {
    clearCurrentProfile()
    return
  }
  if (profileState.profile) return
  try {
    await loadCurrentProfile()
  } catch {
    clearCurrentProfile()
  }
}

async function loadNotificationHeader() {
  if (!isAuthenticated()) {
    clearNotificationState()
    return
  }
  try {
    await loadUnreadNotificationCount()
  } catch {
    clearNotificationState()
  }
}

async function logout() {
  loggingOut.value = true
  try { await logoutSession(getRefreshToken()) } catch { /* 本地会话仍需清理。 */ }
  finally {
    clearSession()
    clearIdentityVerification()
    clearCurrentProfile()
    clearNotificationState()
    loggingOut.value = false
    ElMessage.success('已退出登录')
    router.replace('/')
  }
}

function login() { router.push({ name: 'login', query: { redirect: route.fullPath } }) }
onMounted(async () => {
  await Promise.all([loadIdentityStatus(), loadAccountHeader(), loadNotificationHeader()])
  if (isAuthenticated() && !rolesAreLoaded()) {
    try { setRoles((await getMyRoles()).data.roles) } catch { /* 普通页面不因角色读取失败而中断。 */ }
  }
})
watch(() => route.fullPath, () => {
  loadIdentityStatus()
  loadAccountHeader()
  loadNotificationHeader()
})
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink class="brand" to="/"><span class="brand-mark">MB</span><span>My Buddies</span></RouterLink>
      <nav class="main-nav" aria-label="主导航">
        <RouterLink to="/">发现</RouterLink><RouterLink to="/meetups">活动</RouterLink><RouterLink to="/search">搜索</RouterLink><RouterLink v-if="isAuthenticated()" to="/me/feed">我的动态</RouterLink><RouterLink v-if="isAuthenticated()" to="/me/interests">我的兴趣</RouterLink><RouterLink v-if="isAuthenticated()" to="/me/bookmarks">我的收藏</RouterLink><RouterLink v-if="isAuthenticated()" to="/me/posts">我的帖子</RouterLink><RouterLink v-if="isContentAdmin()" to="/admin">管理后台</RouterLink>
      </nav>
      <div class="account-area">
        <template v-if="isAuthenticated()"><RouterLink class="identity-link" :class="{ verified: identityStatus === 'VERIFIED' }" to="/me/identity">{{ identityStatus === 'VERIFIED' ? '已实名' : '去认证' }}</RouterLink><RouterLink class="notification-link" to="/me/notifications" aria-label="进入通知中心"><el-badge :value="notificationState.unreadCount" :max="99" :hidden="notificationState.unreadCount === 0"><span>通知</span></el-badge></RouterLink><RouterLink class="account-profile-link" to="/me/profile" :aria-label="`进入${profileState.profile?.nickname ?? '我的'}个人资料`"><UserAvatar :avatar-code="profileState.profile?.avatarCode" :nickname="profileState.profile?.nickname" :size="44" /><strong>{{ profileState.profile?.nickname ?? '我的资料' }}</strong></RouterLink><el-button :loading="loggingOut" plain round @click="logout">退出</el-button></template>
        <el-button v-else type="primary" round @click="login">登录</el-button>
      </div>
    </header>
    <main class="page-container"><RouterView /></main>
  </div>
</template>
