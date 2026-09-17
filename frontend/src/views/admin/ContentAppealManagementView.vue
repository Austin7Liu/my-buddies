<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveAppeal, closeAppeal, getAdminAppeal, listAdminAppeals, rejectAppeal } from '../../api/appeal.js'
import ModerationReasonDialog from '../../components/admin/ModerationReasonDialog.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import { appealStatusLabel } from '../../utils/appeal.js'

const statuses = ['', 'PENDING', 'APPROVED', 'REJECTED', 'CLOSED']
const status = ref('PENDING')
const pageData = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(false)
const acting = ref(false)
const detail = ref(null)
const action = ref(null)

async function load(page = 1) {
  loading.value = true
  try { pageData.value = (await listAdminAppeals(status.value, page)).data } finally { loading.value = false }
}

async function openDetail(appealId) {
  detail.value = (await getAdminAppeal(appealId)).data
}

async function submitAction(note) {
  if (acting.value) return
  const current = action.value
  const handlers = { approve: approveAppeal, reject: rejectAppeal, close: closeAppeal }
  const labels = { approve: '通过该申诉并恢复相关内容', reject: '驳回该申诉', close: '关闭该申诉' }
  try {
    await ElMessageBox.confirm(`确定要${labels[current.type]}吗？`, '二次确认', { type: 'warning' })
  } catch {
    return
  }
  acting.value = true
  try {
    await handlers[current.type](current.row.id, note)
    action.value = null
    ElMessage.success('申诉已处理')
    await load(pageData.value.page)
  } finally {
    acting.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="admin-heading"><div><p class="eyebrow accent">CONTENT APPEALS</p><h1 class="admin-title">内容申诉</h1></div><el-select v-model="status" style="width: 190px" @change="load(1)"><el-option v-for="item in statuses" :key="item" :label="item ? appealStatusLabel(item) : '全部状态'" :value="item" /></el-select></div>
    <el-table v-loading="loading" :data="pageData.records"><el-table-column prop="reportId" label="举报编号" width="180" /><el-table-column prop="reason" label="申诉理由" min-width="360"><template #default="{ row }"><div class="post-content">{{ row.reason }}</div></template></el-table-column><el-table-column label="状态" width="110"><template #default="{ row }">{{ appealStatusLabel(row.status) }}</template></el-table-column><el-table-column prop="createdAt" label="提交时间" width="180" /><el-table-column label="操作" fixed="right" width="250"><template #default="{ row }"><el-button link @click="openDetail(row.id)">详情</el-button><template v-if="row.status === 'PENDING'"><el-button link type="success" @click="action = { row, type: 'approve' }">通过</el-button><el-button link type="danger" @click="action = { row, type: 'reject' }">驳回</el-button><el-button link @click="action = { row, type: 'close' }">关闭</el-button></template></template></el-table-column></el-table>
    <PaginationBar :page="Number(pageData.page)" :size="Number(pageData.size)" :total="Number(pageData.total)" @change="load" />
    <el-dialog :model-value="Boolean(detail)" title="申诉详情" width="min(700px, 92vw)" @update:model-value="!$event && (detail = null)"><div v-if="detail" class="report-detail"><p><strong>关联举报：</strong>#{{ detail.reportId }}</p><p><strong>申诉理由：</strong>{{ detail.reason }}</p><p><strong>状态：</strong>{{ appealStatusLabel(detail.status) }}</p><p><strong>处理说明：</strong>{{ detail.reviewNote || '尚未处理' }}</p><p><strong>提交时间：</strong>{{ detail.createdAt }}</p><p v-if="detail.reviewedAt"><strong>处理时间：</strong>{{ detail.reviewedAt }}</p></div></el-dialog>
    <ModerationReasonDialog :model-value="Boolean(action)" :loading="acting" :title="action?.type === 'approve' ? '通过内容申诉' : action?.type === 'reject' ? '驳回内容申诉' : '关闭内容申诉'" @update:model-value="!$event && (action = null)" @confirm="submitAction" />
  </section>
</template>
