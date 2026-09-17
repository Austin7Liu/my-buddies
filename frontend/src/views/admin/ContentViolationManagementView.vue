<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminViolation, listAdminViolations, revokeViolation } from '../../api/violation.js'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import {
  violationPenaltyLabel,
  violationSeverityLabel,
  violationStatusLabel,
} from '../../utils/violation.js'

const route = useRoute()
const router = useRouter()
const accountId = ref(String(route.query.accountId ?? ''))
const pageData = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(false)
const detail = ref(null)
const revoking = ref(false)
const revokeTarget = ref(null)
const revokeForm = reactive({ reason: '', revokeLinkedRestrictions: true })
const revokeError = ref('')

async function search(page = 1) {
  if (!/^\d+$/.test(accountId.value) || accountId.value === '0') {
    ElMessage.warning('请输入有效的账户 ID')
    return
  }
  loading.value = true
  try {
    pageData.value = (await listAdminViolations(accountId.value, page)).data
    await router.replace({ query: { accountId: accountId.value } })
  } finally {
    loading.value = false
  }
}

async function openDetail(id) {
  detail.value = (await getAdminViolation(id)).data
}

function openRevoke(row) {
  revokeTarget.value = row
  revokeForm.reason = ''
  revokeForm.revokeLinkedRestrictions = true
  revokeError.value = ''
}

async function submitRevoke() {
  if (revoking.value) return
  const reason = revokeForm.reason.trim()
  if (!reason) { revokeError.value = '请填写撤销原因'; return }
  if (reason.length > 500) { revokeError.value = '撤销原因不能超过 500 个字符'; return }
  try {
    await ElMessageBox.confirm(revokeForm.revokeLinkedRestrictions ? '将同时撤销仍有效的关联业务限制，确定继续吗？' : '确定只撤销违规记录并保留关联限制吗？', '二次确认', { type: 'warning' })
  } catch {
    return
  }
  revoking.value = true
  try {
    await revokeViolation(revokeTarget.value.id, { reason, revokeLinkedRestrictions: revokeForm.revokeLinkedRestrictions })
    revokeTarget.value = null
    ElMessage.success('违规记录已撤销')
    await search(pageData.value.page)
  } finally {
    revoking.value = false
  }
}

onMounted(async () => {
  if (accountId.value) await search()
  if (route.query.violationId) await openDetail(route.query.violationId)
})
</script>

<template>
  <section>
    <div class="admin-heading"><div><p class="eyebrow accent">CONTENT VIOLATIONS</p><h1 class="admin-title">违规记录</h1></div></div>
    <div class="admin-search-bar"><el-input v-model="accountId" placeholder="输入账户 ID 查询" @keyup.enter="search(1)" /><el-button type="primary" :loading="loading" @click="search(1)">查询</el-button></div>
    <el-table v-loading="loading" :data="pageData.records"><el-table-column prop="id" label="违规编号" min-width="170" /><el-table-column label="等级" width="90"><template #default="{ row }">{{ violationSeverityLabel(row.severity) }}</template></el-table-column><el-table-column label="处罚" min-width="170"><template #default="{ row }">{{ violationPenaltyLabel(row.penaltyType) }}</template></el-table-column><el-table-column prop="note" label="说明" min-width="240" /><el-table-column label="状态" width="100"><template #default="{ row }">{{ violationStatusLabel(row.status) }}</template></el-table-column><el-table-column label="操作" width="150"><template #default="{ row }"><el-button link @click="openDetail(row.id)">详情</el-button><el-button v-if="row.status === 'ACTIVE'" link type="danger" @click="openRevoke(row)">撤销</el-button></template></el-table-column></el-table>
    <EmptyState v-if="accountId && !loading && !pageData.records.length" title="暂无违规记录" description="该账户目前没有内容违规记录。" />
    <PaginationBar v-if="pageData.total" :page="Number(pageData.page)" :size="Number(pageData.size)" :total="Number(pageData.total)" @change="search" />
    <el-dialog :model-value="Boolean(detail)" title="违规详情" width="min(680px, 92vw)" @update:model-value="!$event && (detail = null)"><div v-if="detail" class="report-detail"><p><strong>账户：</strong>{{ detail.accountId }}</p><p><strong>关联举报：</strong>#{{ detail.reportId }}</p><p><strong>等级：</strong>{{ violationSeverityLabel(detail.severity) }}</p><p><strong>处罚：</strong>{{ violationPenaltyLabel(detail.penaltyType) }}</p><p><strong>截止时间：</strong>{{ detail.penaltyExpiresAt || '仅警告' }}</p><p><strong>说明：</strong>{{ detail.note }}</p><p><strong>关联限制：</strong>{{ detail.linkedRestrictionIds?.join('、') || '无' }}</p><p v-if="detail.revokeReason"><strong>撤销原因：</strong>{{ detail.revokeReason }}</p></div></el-dialog>
    <el-dialog :model-value="Boolean(revokeTarget)" title="撤销违规记录" width="min(560px, 92vw)" @update:model-value="!$event && (revokeTarget = null)"><el-input v-model="revokeForm.reason" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请填写可复核的撤销原因" @input="revokeError = ''" /><el-checkbox v-model="revokeForm.revokeLinkedRestrictions">同时撤销仍有效的关联业务限制</el-checkbox><p v-if="revokeError" class="form-error">{{ revokeError }}</p><template #footer><el-button @click="revokeTarget = null">取消</el-button><el-button type="danger" :loading="revoking" :disabled="revoking" @click="submitRevoke">确认撤销</el-button></template></el-dialog>
  </section>
</template>
