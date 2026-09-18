<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { hideComment, listAdminComments, restoreComment } from '../../api/admin.js'
import ModerationReasonDialog from '../../components/admin/ModerationReasonDialog.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import { compactAdminFilters } from '../../utils/adminModeration.js'

const filters = reactive({ status: '', postId: '', authorAccountId: '' })
const pageData = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(false)
const actingId = ref(null)
const hideTarget = ref(null)

function queryFilters() {
  return compactAdminFilters(filters)
}

async function load(page = 1) {
  loading.value = true
  try { pageData.value = (await listAdminComments(queryFilters(), page)).data } finally { loading.value = false }
}

async function submitHide(reason) {
  actingId.value = hideTarget.value.id
  try {
    await hideComment(hideTarget.value.id, reason)
    hideTarget.value = null
    ElMessage.success('评论已隐藏')
    await load(pageData.value.page)
  } finally { actingId.value = null }
}

async function restore(row) {
  const confirmed = await ElMessageBox.confirm('恢复后评论正文将重新对用户可见。', '确认恢复评论？', {
    type: 'warning', confirmButtonText: '确认恢复', cancelButtonText: '取消',
  }).then(() => true).catch(() => false)
  if (!confirmed) return
  actingId.value = row.id
  try {
    await restoreComment(row.id)
    ElMessage.success('评论已恢复')
    await load(pageData.value.page)
  } finally { actingId.value = null }
}

function reset() {
  Object.assign(filters, { status: '', postId: '', authorAccountId: '' })
  load(1)
}

onMounted(load)
</script>

<template>
  <section>
    <div class="admin-heading"><div><p class="eyebrow accent">CONTENT GOVERNANCE</p><h1 class="admin-title">评论管理</h1></div></div>
    <div class="admin-filter-panel">
      <el-select v-model="filters.status" placeholder="全部状态" clearable><el-option label="VISIBLE" value="VISIBLE" /><el-option label="HIDDEN_BY_ADMIN" value="HIDDEN_BY_ADMIN" /><el-option label="DELETED_BY_AUTHOR" value="DELETED_BY_AUTHOR" /></el-select>
      <el-input v-model="filters.postId" placeholder="帖子 ID" clearable />
      <el-input v-model="filters.authorAccountId" placeholder="作者账户 ID" clearable />
      <el-button type="primary" @click="load(1)">查询</el-button><el-button @click="reset">重置</el-button>
    </div>
    <el-table v-loading="loading" :data="pageData.records">
      <el-table-column label="作者" width="160"><template #default="{ row }"><RouterLink class="profile-link" :to="`/profiles/${row.authorAccountId}`">{{ row.author?.nickname || row.authorAccountId }}</RouterLink></template></el-table-column>
      <el-table-column label="帖子 ID" width="190"><template #default="{ row }"><RouterLink class="profile-link" :to="`/posts/${row.postId}`">{{ row.postId }}</RouterLink></template></el-table-column>
      <el-table-column label="评论正文" min-width="300"><template #default="{ row }"><div class="post-content">{{ row.content }}</div></template></el-table-column>
      <el-table-column prop="status" label="状态" width="170" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" fixed="right" width="150"><template #default="{ row }"><el-button v-if="row.status === 'VISIBLE'" link type="danger" :disabled="actingId === row.id" @click="hideTarget = row">隐藏</el-button><el-button v-if="row.status === 'HIDDEN_BY_ADMIN'" link :disabled="actingId === row.id" @click="restore(row)">恢复</el-button><span v-if="row.status === 'DELETED_BY_AUTHOR'" class="muted-text">作者已删除</span></template></el-table-column>
    </el-table>
    <PaginationBar :page="Number(pageData.page)" :size="Number(pageData.size)" :total="Number(pageData.total)" @change="load" />
    <ModerationReasonDialog :model-value="Boolean(hideTarget)" title="隐藏评论" confirm-text="确认隐藏" :loading="Boolean(actingId)" @update:model-value="!$event && (hideTarget = null)" @confirm="submitHide" />
  </section>
</template>
