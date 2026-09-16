<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveCircle, listAdminCircles, rejectCircle, setCircleEnabled } from '../../api/admin.js'
import ModerationReasonDialog from '../../components/admin/ModerationReasonDialog.vue'
import PaginationBar from '../../components/PaginationBar.vue'

const statuses = ['', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'DISABLED']
const status = ref('PENDING_REVIEW')
const page = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(false)
const actingId = ref(null)
const rejectTarget = ref(null)

async function load(current = 1) { loading.value = true; try { page.value = (await listAdminCircles(status.value, current)).data } finally { loading.value = false } }
async function approve(row) {
  const confirmed = await ElMessageBox.confirm(
    `通过后，“${row.name}”将对用户公开展示。`,
    '确认通过 Circle 审核？',
    {
      type: 'warning',
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      distinguishCancelAndClose: true,
    },
  ).then(() => true).catch(() => false)
  if (!confirmed) return
  actingId.value = row.id; try { await approveCircle(row.id); ElMessage.success('审核已通过'); await load(page.value.page) } finally { actingId.value = null }
}
async function reject(reason) { actingId.value = rejectTarget.value.id; try { await rejectCircle(rejectTarget.value.id, reason); rejectTarget.value = null; ElMessage.success('已驳回'); await load(page.value.page) } finally { actingId.value = null } }
async function toggle(row) {
  const enabled = row.status === 'DISABLED'
  const confirmed = await ElMessageBox.confirm(
    enabled ? `启用后，“${row.name}”将恢复公开展示。` : `停用后，“${row.name}”将不再公开展示。`,
    `确认${enabled ? '启用' : '停用'} Circle？`,
    {
      type: enabled ? 'warning' : 'error',
      confirmButtonText: `确认${enabled ? '启用' : '停用'}`,
      cancelButtonText: '取消',
      distinguishCancelAndClose: true,
    },
  ).then(() => true).catch(() => false)
  if (!confirmed) return
  actingId.value = row.id; try { await setCircleEnabled(row.id, enabled); ElMessage.success('状态已更新'); await load(page.value.page) } finally { actingId.value = null }
}
onMounted(load)
</script>

<template><section><div class="admin-heading"><div><p class="eyebrow accent">MODERATION</p><h1 class="admin-title">Circle 审核</h1></div><el-select v-model="status" style="width: 190px" @change="load(1)"><el-option v-for="item in statuses" :key="item" :label="item || '全部状态'" :value="item" /></el-select></div>
  <el-table v-loading="loading" :data="page.records"><el-table-column prop="name" label="名称" min-width="150" /><el-table-column label="创建者" min-width="130"><template #default="{ row }">{{ row.creator?.nickname || row.creatorAccountId }}</template></el-table-column><el-table-column label="位置" min-width="130"><template #default="{ row }">{{ row.city }} {{ row.district }}</template></el-table-column><el-table-column prop="description" label="介绍" min-width="240" show-overflow-tooltip /><el-table-column prop="status" label="状态" width="140" /><el-table-column label="操作" fixed="right" width="220"><template #default="{ row }"><el-button v-if="row.status === 'PENDING_REVIEW' || row.status === 'REJECTED'" link type="success" :loading="actingId === row.id" @click="approve(row)">通过</el-button><el-button v-if="row.status === 'PENDING_REVIEW'" link type="danger" @click="rejectTarget = row">驳回</el-button><el-button v-if="row.status === 'APPROVED' || row.status === 'DISABLED'" link @click="toggle(row)">{{ row.status === 'DISABLED' ? '启用' : '停用' }}</el-button></template></el-table-column></el-table>
  <PaginationBar :page="Number(page.page)" :size="Number(page.size)" :total="Number(page.total)" @change="load" /><ModerationReasonDialog :model-value="Boolean(rejectTarget)" title="驳回 Circle" confirm-text="确认驳回" :loading="Boolean(actingId)" @update:model-value="!$event && (rejectTarget = null)" @confirm="reject" />
</section></template>
