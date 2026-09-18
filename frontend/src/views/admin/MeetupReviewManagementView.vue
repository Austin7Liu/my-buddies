<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { hideMeetupReview, listAdminMeetupReviews, restoreMeetupReview } from '../../api/admin.js'
import ModerationReasonDialog from '../../components/admin/ModerationReasonDialog.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import { compactAdminFilters } from '../../utils/adminModeration.js'

const filters = reactive({ status: '', meetupId: '', reviewerAccountId: '', revieweeAccountId: '', rating: '' })
const pageData = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(false)
const actingId = ref(null)
const reasonAction = ref(null)

function queryFilters() {
  return compactAdminFilters(filters)
}
async function load(page = 1) {
  loading.value = true
  try { pageData.value = (await listAdminMeetupReviews(queryFilters(), page)).data } finally { loading.value = false }
}
async function submitReason(reason) {
  const { row, action } = reasonAction.value
  actingId.value = row.id
  try {
    await (action === 'hide' ? hideMeetupReview(row.id, reason) : restoreMeetupReview(row.id, reason))
    reasonAction.value = null
    ElMessage.success(action === 'hide' ? '评价已隐藏' : '评价已恢复')
    await load(pageData.value.page)
  } finally { actingId.value = null }
}
function reset() {
  Object.assign(filters, { status: '', meetupId: '', reviewerAccountId: '', revieweeAccountId: '', rating: '' })
  load(1)
}
onMounted(load)
</script>

<template>
  <section>
    <div class="admin-heading"><div><p class="eyebrow accent">TRUST & SAFETY</p><h1 class="admin-title">活动评价管理</h1></div></div>
    <div class="admin-filter-panel review-filters">
      <el-select v-model="filters.status" placeholder="全部状态" clearable><el-option label="VISIBLE" value="VISIBLE" /><el-option label="HIDDEN" value="HIDDEN" /></el-select>
      <el-input v-model="filters.meetupId" placeholder="Meetup ID" clearable />
      <el-input v-model="filters.reviewerAccountId" placeholder="评价人账户 ID" clearable />
      <el-input v-model="filters.revieweeAccountId" placeholder="被评价人账户 ID" clearable />
      <el-select v-model="filters.rating" placeholder="全部评分" clearable><el-option v-for="rating in 5" :key="rating" :label="`${rating} 星`" :value="rating" /></el-select>
      <el-button type="primary" @click="load(1)">查询</el-button><el-button @click="reset">重置</el-button>
    </div>
    <el-table v-loading="loading" :data="pageData.records">
      <el-table-column label="Meetup ID" width="190"><template #default="{ row }"><RouterLink class="profile-link" :to="`/meetups/${row.meetupId}`">{{ row.meetupId }}</RouterLink></template></el-table-column>
      <el-table-column label="评价关系" min-width="230"><template #default="{ row }"><RouterLink class="profile-link" :to="`/profiles/${row.reviewerAccountId}`">{{ row.reviewer?.nickname || row.reviewerAccountId }}</RouterLink><span> → </span><RouterLink class="profile-link" :to="`/profiles/${row.revieweeAccountId}`">{{ row.reviewee?.nickname || row.revieweeAccountId }}</RouterLink></template></el-table-column>
      <el-table-column label="评分" width="90"><template #default="{ row }">{{ row.rating }} 星</template></el-table-column>
      <el-table-column label="评价内容" min-width="260"><template #default="{ row }"><div class="post-content">{{ row.comment || '未填写文字评价' }}</div></template></el-table-column>
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" fixed="right" width="130"><template #default="{ row }"><el-button v-if="row.status === 'VISIBLE'" link type="danger" @click="reasonAction = { row, action: 'hide' }">隐藏</el-button><el-button v-else link @click="reasonAction = { row, action: 'restore' }">恢复</el-button></template></el-table-column>
    </el-table>
    <PaginationBar :page="Number(pageData.page)" :size="Number(pageData.size)" :total="Number(pageData.total)" @change="load" />
    <ModerationReasonDialog :model-value="Boolean(reasonAction)" :title="reasonAction?.action === 'hide' ? '隐藏活动评价' : '恢复活动评价'" :confirm-text="reasonAction?.action === 'hide' ? '确认隐藏' : '确认恢复'" :loading="Boolean(actingId)" @update:model-value="!$event && (reasonAction = null)" @confirm="submitReason" />
  </section>
</template>
