<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listAdminMeetups, terminateMeetup } from '../../api/admin.js'
import ModerationReasonDialog from '../../components/admin/ModerationReasonDialog.vue'
import PaginationBar from '../../components/PaginationBar.vue'

const statuses = ['', 'DRAFT', 'OPEN', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'TERMINATED']
const status = ref('OPEN')
const page = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(false)
const target = ref(null)
const acting = ref(false)
async function load(current = 1) { loading.value = true; try { page.value = (await listAdminMeetups(status.value, current)).data } finally { loading.value = false } }
async function terminate(reason) { acting.value = true; try { await terminateMeetup(target.value.id, reason); target.value = null; ElMessage.success('活动已终止'); await load(page.value.page) } finally { acting.value = false } }
onMounted(load)
</script>

<template><section><div class="admin-heading"><div><p class="eyebrow accent">ACTIVITY SAFETY</p><h1 class="admin-title">Meetup 管理</h1></div><el-select v-model="status" style="width: 190px" @change="load(1)"><el-option v-for="item in statuses" :key="item" :label="item || '全部状态'" :value="item" /></el-select></div>
  <el-table v-loading="loading" :data="page.records"><el-table-column prop="title" label="标题" min-width="180" /><el-table-column label="创建者" width="140"><template #default="{ row }">{{ row.creator?.nickname || row.creatorAccountId }}</template></el-table-column><el-table-column prop="meetupMode" label="模式" width="90" /><el-table-column label="时间" width="180"><template #default="{ row }">{{ row.startTime }}</template></el-table-column><el-table-column label="人数" width="100"><template #default="{ row }">{{ row.acceptedCount }}/{{ row.capacity }}</template></el-table-column><el-table-column prop="status" label="状态" width="130" /><el-table-column label="操作" fixed="right" width="110"><template #default="{ row }"><el-button v-if="['OPEN', 'CONFIRMED'].includes(row.status)" link type="danger" @click="target = row">终止</el-button></template></el-table-column></el-table>
  <PaginationBar :page="Number(page.page)" :size="Number(page.size)" :total="Number(page.total)" @change="load" /><ModerationReasonDialog :model-value="Boolean(target)" title="终止 Meetup" confirm-text="确认终止" :loading="acting" @update:model-value="!$event && (target = null)" @confirm="terminate" />
</section></template>
