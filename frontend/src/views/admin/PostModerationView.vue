<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approvePost, listAdminPosts, offlinePost, rejectPost, restorePost } from '../../api/admin.js'
import ModerationReasonDialog from '../../components/admin/ModerationReasonDialog.vue'
import PaginationBar from '../../components/PaginationBar.vue'

const statuses = ['', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'OFFLINE', 'DELETED']
const status = ref('PENDING_REVIEW')
const page = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(false)
const actingId = ref(null)
const reasonAction = ref(null)
async function load(current = 1) { loading.value = true; try { page.value = (await listAdminPosts(status.value, current)).data } finally { loading.value = false } }
async function simpleAction(row, action) {
  const approving = action === 'approve'
  const confirmed = await ElMessageBox.confirm(
    approving ? '通过后，这篇帖子将对用户公开展示。' : '恢复后，这篇帖子将重新公开展示。',
    `确认${approving ? '通过审核' : '恢复帖子'}？`,
    {
      type: 'warning',
      confirmButtonText: approving ? '确认通过' : '确认恢复',
      cancelButtonText: '取消',
      distinguishCancelAndClose: true,
    },
  ).then(() => true).catch(() => false)
  if (!confirmed) return
  actingId.value = row.id; try { await (action === 'approve' ? approvePost(row.id) : restorePost(row.id)); ElMessage.success('操作成功'); await load(page.value.page) } finally { actingId.value = null }
}
async function submitReason(reason) {
  const { row, action } = reasonAction.value; actingId.value = row.id
  try { await (action === 'reject' ? rejectPost(row.id, reason) : offlinePost(row.id, reason)); reasonAction.value = null; ElMessage.success('操作成功'); await load(page.value.page) } finally { actingId.value = null }
}
onMounted(load)
</script>

<template><section><div class="admin-heading"><div><p class="eyebrow accent">MODERATION</p><h1 class="admin-title">Post 审核</h1></div><el-select v-model="status" style="width: 190px" @change="load(1)"><el-option v-for="item in statuses" :key="item" :label="item || '全部状态'" :value="item" /></el-select></div>
  <el-table v-loading="loading" :data="page.records"><el-table-column label="作者" width="140"><template #default="{ row }">{{ row.author?.nickname || row.authorAccountId }}</template></el-table-column><el-table-column prop="content" label="正文" min-width="380"><template #default="{ row }"><div class="post-content">{{ row.content }}</div></template></el-table-column><el-table-column prop="status" label="状态" width="140" /><el-table-column prop="createdAt" label="创建时间" width="180" /><el-table-column label="操作" fixed="right" width="230"><template #default="{ row }"><el-button v-if="row.status === 'PENDING_REVIEW' || row.status === 'REJECTED'" link type="success" @click="simpleAction(row, 'approve')">通过</el-button><el-button v-if="row.status === 'PENDING_REVIEW'" link type="danger" @click="reasonAction = { row, action: 'reject' }">驳回</el-button><el-button v-if="row.status === 'PUBLISHED'" link type="danger" @click="reasonAction = { row, action: 'offline' }">下架</el-button><el-button v-if="row.status === 'OFFLINE'" link @click="simpleAction(row, 'restore')">恢复</el-button></template></el-table-column></el-table>
  <PaginationBar :page="Number(page.page)" :size="Number(page.size)" :total="Number(page.total)" @change="load" /><ModerationReasonDialog :model-value="Boolean(reasonAction)" :title="reasonAction?.action === 'reject' ? '驳回 Post' : '下架 Post'" :loading="Boolean(actingId)" @update:model-value="!$event && (reasonAction = null)" @confirm="submitReason" />
</section></template>
