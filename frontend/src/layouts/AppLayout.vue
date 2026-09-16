<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logoutSession } from '../api/auth.js'
import { authState, clearSession, getRefreshToken, isAuthenticated } from '../stores/auth.js'
import { isContentAdmin, rolesAreLoaded, setRoles } from '../stores/auth.js'
import { getMyRoles } from '../api/admin.js'
import { clearIdentityVerification, identityState, loadIdentityVerification } from '../stores/identity.js'

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

async function logout() {
  loggingOut.value = true
  try { await logoutSession(getRefreshToken()) } catch { /* 本地会话仍需清理。 */ }
  finally {
    clearSession()
    clearIdentityVerification()
    loggingOut.value = false
    ElMessage.success('已退出登录')
    router.replace('/')
  }
}

function login() { router.push({ name: 'login', query: { redirect: route.fullPath } }) }
onMounted(async () => {
  await loadIdentityStatus()
  if (isAuthenticated() && !rolesAreLoaded()) {
    try { setRoles((await getMyRoles()).data.roles) } catch { /* 普通页面不因角色读取失败而中断。 */ }
  }
})
watch(() => route.fullPath, loadIdentityStatus)
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink class="brand" to="/"><span class="brand-mark">MB</span><span>My Buddies</span></RouterLink>
      <nav class="main-nav" aria-label="主导航">
        <RouterLink to="/">发现</RouterLink><RouterLink to="/search">搜索</RouterLink><RouterLink v-if="isAuthenticated()" to="/me/interests">我的兴趣</RouterLink><RouterLink v-if="isContentAdmin()" to="/admin">管理后台</RouterLink>
      </nav>
      <div class="account-area">
        <template v-if="isAuthenticated()"><RouterLink class="identity-link" :class="{ verified: identityStatus === 'VERIFIED' }" to="/me/identity">{{ identityStatus === 'VERIFIED' ? '已实名' : '去认证' }}</RouterLink><div class="account-copy"><strong>{{ authState.account?.maskedPhone ?? '已登录用户' }}</strong><span>账户 #{{ authState.account?.id ?? '-' }}</span></div><el-button :loading="loggingOut" plain round @click="logout">退出</el-button></template>
        <el-button v-else type="primary" round @click="login">登录</el-button>
      </div>
    </header>
    <main class="page-container"><RouterView /></main>
  </div>
</template>
