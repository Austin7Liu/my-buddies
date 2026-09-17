<script setup>
import { onMounted, ref } from 'vue'
import { getMyReport, listMyReports } from '../../api/report.js'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import { reportReasonLabel, reportStatusLabel, reportTargetLabel } from '../../utils/report.js'

const loading = ref(true)
const pageData = ref({ records: [], page: 1, size: 20, total: 0 })
const detail = ref(null)

async function load(page = 1) {
  loading.value = true
  try { pageData.value = (await listMyReports(page)).data } finally { loading.value = false }
}

async function openDetail(reportId) {
  detail.value = (await getMyReport(reportId)).data
}

onMounted(load)
</script>

<template>
  <section class="report-page">
    <div class="section-heading"><div><p class="eyebrow accent">MY REPORTS</p><h1>我的举报</h1><p>查看举报受理状态和平台处理结果。</p></div></div>
    <div v-loading="loading" class="report-list">
      <article v-for="report in pageData.records" :key="report.id" class="report-card">
        <div><el-tag effect="plain">{{ reportStatusLabel(report.status) }}</el-tag><span>{{ reportTargetLabel(report.targetType) }} · {{ reportReasonLabel(report.reasonType) }}</span></div>
        <p>{{ report.description || '未填写补充说明' }}</p>
        <small>{{ report.createdAt }}</small>
        <el-button text @click="openDetail(report.id)">查看详情</el-button>
      </article>
      <EmptyState v-if="!loading && !pageData.records.length" title="暂无举报" description="你提交的内容举报会显示在这里。" />
    </div>
    <PaginationBar :page="Number(pageData.page)" :size="Number(pageData.size)" :total="Number(pageData.total)" @change="load" />
    <el-dialog :model-value="Boolean(detail)" title="举报详情" width="min(620px, 92vw)" @update:model-value="!$event && (detail = null)">
      <div v-if="detail" class="report-detail"><p><strong>状态：</strong>{{ reportStatusLabel(detail.status) }}</p><p><strong>目标：</strong>{{ reportTargetLabel(detail.targetType) }}</p><p><strong>原因：</strong>{{ reportReasonLabel(detail.reasonType) }}</p><p><strong>说明：</strong>{{ detail.description || '无' }}</p><p><strong>处理结果：</strong>{{ detail.resolutionNote || '平台尚未处理' }}</p><p><strong>提交时间：</strong>{{ detail.createdAt }}</p><p v-if="detail.handledAt"><strong>处理时间：</strong>{{ detail.handledAt }}</p></div>
    </el-dialog>
  </section>
</template>
