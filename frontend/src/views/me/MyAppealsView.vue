<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMyAppeal, listMyAppeals } from '../../api/appeal.js'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import { appealStatusLabel } from '../../utils/appeal.js'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const pageData = ref({ records: [], page: 1, size: 20, total: 0 })
const detail = ref(null)

async function load(page = 1) {
  loading.value = true
  try { pageData.value = (await listMyAppeals(page)).data } finally { loading.value = false }
}

async function openDetail(appealId) {
  detail.value = (await getMyAppeal(appealId)).data
  await router.replace({ query: { appealId: String(appealId) } })
}

async function closeDetail() {
  detail.value = null
  await router.replace({ query: {} })
}

onMounted(async () => {
  await load()
  if (route.query.appealId) await openDetail(route.query.appealId)
})
</script>

<template>
  <section class="appeal-page">
    <div class="section-heading"><div><p class="eyebrow accent">MY APPEALS</p><h1>我的申诉</h1><p>查看内容申诉的复核状态和平台处理说明。</p></div></div>
    <div v-loading="loading" class="report-list">
      <article v-for="appeal in pageData.records" :key="appeal.id" class="report-card">
        <div><el-tag effect="plain">{{ appealStatusLabel(appeal.status) }}</el-tag><span>举报 #{{ appeal.reportId }}</span></div>
        <p>{{ appeal.reason }}</p><small>{{ appeal.createdAt }}</small>
        <el-button text @click="openDetail(appeal.id)">查看详情</el-button>
      </article>
      <EmptyState v-if="!loading && !pageData.records.length" title="暂无申诉" description="被处置内容的申诉记录会显示在这里。" />
    </div>
    <PaginationBar :page="Number(pageData.page)" :size="Number(pageData.size)" :total="Number(pageData.total)" @change="load" />
    <el-dialog :model-value="Boolean(detail)" title="申诉详情" width="min(620px, 92vw)" @update:model-value="!$event && closeDetail()">
      <div v-if="detail" class="report-detail"><p><strong>状态：</strong>{{ appealStatusLabel(detail.status) }}</p><p><strong>关联举报：</strong>#{{ detail.reportId }}</p><p><strong>申诉理由：</strong>{{ detail.reason }}</p><p><strong>处理说明：</strong>{{ detail.reviewNote || '平台尚未处理' }}</p><p><strong>提交时间：</strong>{{ detail.createdAt }}</p><p v-if="detail.reviewedAt"><strong>处理时间：</strong>{{ detail.reviewedAt }}</p></div>
    </el-dialog>
  </section>
</template>
