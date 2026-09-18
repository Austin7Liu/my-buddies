<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createRestriction, listAccountRestrictions, revokeRestriction } from '../../api/risk.js'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import { restrictionStatusLabel, restrictionTypeLabel } from '../../utils/violation.js'

const accountId = ref('')
const loadedAccountId = ref('')
const pageData = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(false)
const creating = ref(false)
const createOpen = ref(false)
const createError = ref('')
const form = reactive({ restrictionType: 'MEETUP_CREATE_DISABLED', reason: '', expiresAt: '' })
const revokeTarget = ref(null)
const revokeReason = ref('')
const revokeError = ref('')
const revoking = ref(false)

async function search(page = 1) {
  if (!/^\d+$/.test(accountId.value) || accountId.value === '0') {
    ElMessage.warning('请输入有效的账户 ID')
    return
  }
  loading.value = true
  try {
    pageData.value = (await listAccountRestrictions(accountId.value, page)).data
    loadedAccountId.value = accountId.value
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { restrictionType: 'MEETUP_CREATE_DISABLED', reason: '', expiresAt: '' })
  createError.value = ''
  createOpen.value = true
}

async function submitCreate() {
  if (creating.value) return
  const reason = form.reason.trim()
  if (!reason) { createError.value = '请填写限制原因'; return }
  if (reason.length > 255) { createError.value = '限制原因不能超过 255 个字符'; return }
  if (!form.expiresAt) { createError.value = '请选择限制到期时间'; return }
  try {
    await ElMessageBox.confirm(`确定要为账户 ${loadedAccountId.value} 新增“${restrictionTypeLabel(form.restrictionType)}”吗？`, '二次确认', { type: 'warning' })
  } catch {
    return
  }
  creating.value = true
  try {
    await createRestriction({ accountId: loadedAccountId.value, restrictionType: form.restrictionType, reason, expiresAt: form.expiresAt })
    createOpen.value = false
    ElMessage.success('业务限制已创建')
    await search(1)
  } finally {
    creating.value = false
  }
}

function openRevoke(row) {
  revokeTarget.value = row
  revokeReason.value = ''
  revokeError.value = ''
}

async function submitRevoke() {
  if (revoking.value) return
  const reason = revokeReason.value.trim()
  if (!reason) { revokeError.value = '请填写撤销原因'; return }
  if (reason.length > 255) { revokeError.value = '撤销原因不能超过 255 个字符'; return }
  try {
    await ElMessageBox.confirm('确定要撤销该业务限制吗？', '二次确认', { type: 'warning' })
  } catch {
    return
  }
  revoking.value = true
  try {
    await revokeRestriction(revokeTarget.value.id, reason)
    revokeTarget.value = null
    ElMessage.success('业务限制已撤销')
    await search(pageData.value.page)
  } finally {
    revoking.value = false
  }
}
</script>

<template>
  <section>
    <div class="admin-heading"><div><p class="eyebrow accent">RISK CONTROL</p><h1 class="admin-title">风控限制</h1><p>按账户管理临时业务能力限制，所有变更由后端记录审计。</p></div><el-button v-if="loadedAccountId" type="primary" @click="openCreate">新增限制</el-button></div>
    <div class="admin-search-bar"><el-input v-model="accountId" placeholder="输入账户 ID" @keyup.enter="search(1)" /><el-button type="primary" :loading="loading" @click="search(1)">查询</el-button></div>
    <el-table v-loading="loading" :data="pageData.records"><el-table-column label="限制类型" min-width="190"><template #default="{ row }">{{ restrictionTypeLabel(row.restrictionType) }}</template></el-table-column><el-table-column prop="reason" label="原因" min-width="240" /><el-table-column label="状态" width="100"><template #default="{ row }">{{ row.effective ? '当前有效' : restrictionStatusLabel(row.status) }}</template></el-table-column><el-table-column prop="expiresAt" label="截止时间" width="180" /><el-table-column label="操作" width="100"><template #default="{ row }"><el-button v-if="row.effective" link type="danger" @click="openRevoke(row)">撤销</el-button></template></el-table-column></el-table>
    <EmptyState v-if="loadedAccountId && !loading && !pageData.records.length" title="暂无业务限制" description="该账户目前没有业务能力限制。" />
    <PaginationBar v-if="pageData.total" :page="Number(pageData.page)" :size="Number(pageData.size)" :total="Number(pageData.total)" @change="search" />
    <el-dialog v-model="createOpen" title="新增业务限制" width="min(600px, 92vw)"><el-form label-position="top"><el-form-item label="限制类型"><el-select v-model="form.restrictionType"><el-option label="禁止创建 Meetup" value="MEETUP_CREATE_DISABLED" /><el-option label="禁止报名 Meetup" value="MEETUP_JOIN_DISABLED" /><el-option label="禁止发布 Post" value="POST_CREATE_DISABLED" /><el-option label="禁止发布 Comment" value="COMMENT_CREATE_DISABLED" /></el-select></el-form-item><el-form-item label="到期时间"><el-date-picker v-model="form.expiresAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="后端当前时间后 1～30 天" style="width: 100%" /></el-form-item><el-form-item label="限制原因"><el-input v-model="form.reason" type="textarea" :rows="4" maxlength="255" show-word-limit @input="createError = ''" /></el-form-item></el-form><p v-if="createError" class="form-error">{{ createError }}</p><template #footer><el-button @click="createOpen = false">取消</el-button><el-button type="danger" :loading="creating" :disabled="creating" @click="submitCreate">确认新增</el-button></template></el-dialog>
    <el-dialog :model-value="Boolean(revokeTarget)" title="撤销业务限制" width="min(540px, 92vw)" @update:model-value="!$event && (revokeTarget = null)"><el-input v-model="revokeReason" type="textarea" :rows="4" maxlength="255" show-word-limit placeholder="请填写撤销原因" @input="revokeError = ''" /><p v-if="revokeError" class="form-error">{{ revokeError }}</p><template #footer><el-button @click="revokeTarget = null">取消</el-button><el-button type="danger" :loading="revoking" :disabled="revoking" @click="submitRevoke">确认撤销</el-button></template></el-dialog>
  </section>
</template>
