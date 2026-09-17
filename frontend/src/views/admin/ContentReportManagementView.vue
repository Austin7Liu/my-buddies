<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  duplicateReport,
  getAdminReport,
  listAdminReports,
  rejectReport,
  resolveReport,
} from '../../api/report.js'
import ModerationReasonDialog from '../../components/admin/ModerationReasonDialog.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import { reportReasonLabel, reportStatusLabel, reportTargetLabel } from '../../utils/report.js'

const statuses = ['', 'PENDING', 'RESOLVED', 'REJECTED', 'DUPLICATE']
const status = ref('PENDING')
const pageData = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(false)
const acting = ref(false)
const detail = ref(null)
const action = ref(null)

async function load(page = 1) {
  loading.value = true
  try { pageData.value = (await listAdminReports(status.value, page)).data } finally { loading.value = false }
}

async function openDetail(reportId) {
  detail.value = (await getAdminReport(reportId)).data
}

async function submitAction(note) {
  if (acting.value) return
  const current = action.value
  const handlers = { resolve: resolveReport, reject: rejectReport, duplicate: duplicateReport }
  const labels = { resolve: '确认举报成立', reject: '驳回该举报', duplicate: '标记为重复举报' }
  try {
    await ElMessageBox.confirm(`确定要${labels[current.type]}吗？`, '二次确认', { type: 'warning' })
  } catch {
    return
  }
  acting.value = true
  try {
    await handlers[current.type](current.row.id, note)
    action.value = null
    ElMessage.success('举报已处理')
    await load(pageData.value.page)
  } finally {
    acting.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="admin-heading"><div><p class="eyebrow accent">CONTENT REPORTS</p><h1 class="admin-title">内容举报</h1></div><el-select v-model="status" style="width: 190px" @change="load(1)"><el-option v-for="item in statuses" :key="item" :label="item ? reportStatusLabel(item) : '全部状态'" :value="item" /></el-select></div>
    <el-table v-loading="loading" :data="pageData.records"><el-table-column label="目标" width="100"><template #default="{ row }">{{ reportTargetLabel(row.targetType) }}</template></el-table-column><el-table-column label="原因" width="120"><template #default="{ row }">{{ reportReasonLabel(row.reasonType) }}</template></el-table-column><el-table-column prop="contentSnapshot" label="举报快照" min-width="300"><template #default="{ row }"><div class="post-content">{{ row.contentSnapshot }}</div></template></el-table-column><el-table-column label="状态" width="110"><template #default="{ row }">{{ reportStatusLabel(row.status) }}</template></el-table-column><el-table-column prop="createdAt" label="提交时间" width="180" /><el-table-column label="操作" fixed="right" width="250"><template #default="{ row }"><el-button link @click="openDetail(row.id)">详情</el-button><template v-if="row.status === 'PENDING'"><el-button link type="success" @click="action = { row, type: 'resolve' }">成立</el-button><el-button link type="danger" @click="action = { row, type: 'reject' }">驳回</el-button><el-button link @click="action = { row, type: 'duplicate' }">重复</el-button></template></template></el-table-column></el-table>
    <PaginationBar :page="Number(pageData.page)" :size="Number(pageData.size)" :total="Number(pageData.total)" @change="load" />
    <el-dialog :model-value="Boolean(detail)" title="举报详情" width="min(700px, 92vw)" @update:model-value="!$event && (detail = null)"><div v-if="detail" class="report-detail"><p><strong>举报人：</strong>{{ detail.reporterAccountId }}</p><p><strong>目标：</strong>{{ reportTargetLabel(detail.targetType) }}</p><p><strong>举报原因：</strong>{{ reportReasonLabel(detail.reasonType) }}</p><p><strong>举报说明：</strong>{{ detail.description || '无' }}</p><p><strong>内容快照：</strong></p><pre>{{ detail.contentSnapshot }}</pre><p><strong>状态：</strong>{{ reportStatusLabel(detail.status) }}</p><p><strong>处理说明：</strong>{{ detail.resolutionNote || '尚未处理' }}</p></div></el-dialog>
    <ModerationReasonDialog :model-value="Boolean(action)" :loading="acting" :title="action?.type === 'resolve' ? '确认举报成立' : action?.type === 'reject' ? '驳回举报' : '标记重复举报'" @update:model-value="!$event && (action = null)" @confirm="submitAction" />
  </section>
</template>
