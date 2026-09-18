<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAssignedRoles, grantAdminRole, revokeAdminRole } from '../../api/admin.js'
import { ADMIN_ROLES, adminRoleLabel } from '../../utils/adminRole.js'

const accountId = ref('')
const assignedRoles = ref([])
const loading = ref(false)
const actingRole = ref('')
const loadedAccountId = ref('')

async function search() {
  if (!/^\d+$/.test(accountId.value) || accountId.value === '0') {
    ElMessage.warning('请输入有效的账户 ID')
    return
  }
  loading.value = true
  try {
    const response = (await getAssignedRoles(accountId.value)).data
    assignedRoles.value = response.roles
    loadedAccountId.value = String(response.accountId)
  } finally {
    loading.value = false
  }
}

async function changeRole(role, assigned) {
  if (actingRole.value) return
  const action = assigned ? '撤销' : '授予'
  try {
    await ElMessageBox.confirm(`确定要为账户 ${loadedAccountId.value} ${action}“${adminRoleLabel(role)}”吗？`, `${action}后台角色`, { type: 'warning' })
  } catch {
    return
  }
  actingRole.value = role
  try {
    const response = assigned
      ? await revokeAdminRole(loadedAccountId.value, role)
      : await grantAdminRole(loadedAccountId.value, role)
    assignedRoles.value = response.data.roles
    ElMessage.success(`角色已${action}`)
  } finally {
    actingRole.value = ''
  }
}
</script>

<template>
  <section>
    <div class="admin-heading"><div><p class="eyebrow accent">ROLE MANAGEMENT</p><h1 class="admin-title">角色管理</h1><p>只有超级管理员可以授予或撤销后台角色。</p></div></div>
    <div class="admin-search-bar"><el-input v-model="accountId" placeholder="输入账户 ID" @keyup.enter="search" /><el-button type="primary" :loading="loading" @click="search">查询</el-button></div>
    <div v-if="loadedAccountId" class="admin-role-grid">
      <article v-for="role in ADMIN_ROLES" :key="role"><div><strong>{{ adminRoleLabel(role) }}</strong><small>{{ role }}</small></div><el-switch :model-value="assignedRoles.includes(role)" :loading="actingRole === role" :disabled="Boolean(actingRole)" @change="changeRole(role, assignedRoles.includes(role))" /></article>
    </div>
  </section>
</template>
