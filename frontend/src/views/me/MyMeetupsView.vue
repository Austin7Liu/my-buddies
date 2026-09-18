<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listMyMeetups } from '../../api/meetup.js'
import EmptyState from '../../components/EmptyState.vue'
import MeetupCard from '../../components/MeetupCard.vue'
import PaginationBar from '../../components/PaginationBar.vue'

const route = useRoute()
const router = useRouter()
const roles = ['', 'CREATOR', 'MEMBER']
const statuses = ['', 'DRAFT', 'OPEN', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'TERMINATED']
const role = ref(roles.includes(String(route.query.role ?? '')) ? String(route.query.role ?? '') : '')
const status = ref(statuses.includes(String(route.query.status ?? '')) ? String(route.query.status ?? '') : '')
const meetups = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(true)

async function load(page = 1) {
  loading.value = true
  try {
    meetups.value = (await listMyMeetups(role.value, status.value, page)).data
  } finally {
    loading.value = false
  }
}

async function changeFilter() {
  const query = {}
  if (role.value) query.role = role.value
  if (status.value) query.status = status.value
  await router.replace({ query })
  await load(1)
}

watch([role, status], changeFilter)
onMounted(load)
</script>

<template>
  <section v-loading="loading">
    <p class="eyebrow accent">MY MEETUPS</p>
    <div class="section-heading">
      <div><h1>我的活动</h1><p>统一管理我创建、报名和参与过的活动。</p></div>
      <RouterLink to="/meetups/create"><el-button type="primary" round>创建活动</el-button></RouterLink>
    </div>
    <div class="my-meetup-filters">
      <el-segmented v-model="role" :options="[{ label: '全部', value: '' }, { label: '我创建的', value: 'CREATOR' }, { label: '我参与的', value: 'MEMBER' }]" />
      <el-select v-model="status" placeholder="全部状态" clearable>
        <el-option label="草稿" value="DRAFT" />
        <el-option label="报名中" value="OPEN" />
        <el-option label="已确认" value="CONFIRMED" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已取消" value="CANCELLED" />
        <el-option label="已终止" value="TERMINATED" />
      </el-select>
    </div>
    <div class="meetup-grid">
      <div v-for="meetup in meetups.records" :key="meetup.id" class="my-meetup-item">
        <MeetupCard :meetup="meetup" />
        <RouterLink v-if="meetup.status === 'DRAFT'" :to="`/meetups/${meetup.id}/edit`">
          <el-button class="draft-edit-button" type="primary" plain>继续编辑草稿</el-button>
        </RouterLink>
      </div>
    </div>
    <EmptyState v-if="!loading && !meetups.records.length" title="当前没有活动" description="可以调整筛选条件，或创建一个新活动。" />
    <PaginationBar :page="Number(meetups.page)" :size="Number(meetups.size)" :total="Number(meetups.total)" @change="load" />
  </section>
</template>
