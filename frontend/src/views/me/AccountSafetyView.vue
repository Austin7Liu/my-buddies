<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listMyRestrictions } from '../../api/restriction.js'
import { getMyViolation, listMyViolations } from '../../api/violation.js'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import {
  restrictionStatusLabel,
  restrictionTypeLabel,
  violationPenaltyLabel,
  violationSeverityLabel,
  violationStatusLabel,
} from '../../utils/violation.js'

const route = useRoute()
const router = useRouter()
const activeTab = ref(route.query.tab === 'restrictions' ? 'restrictions' : 'violations')
const violations = ref({ records: [], page: 1, size: 20, total: 0 })
const restrictions = ref({ records: [], page: 1, size: 20, total: 0 })
const loadingViolations = ref(false)
const loadingRestrictions = ref(false)
const violationDetail = ref(null)
const restrictionDetail = ref(null)

async function loadViolations(page = 1) {
  loadingViolations.value = true
  try { violations.value = (await listMyViolations(page)).data } finally { loadingViolations.value = false }
}

async function loadRestrictions(page = 1) {
  loadingRestrictions.value = true
  try { restrictions.value = (await listMyRestrictions(page)).data } finally { loadingRestrictions.value = false }
}

async function openViolation(id) {
  violationDetail.value = (await getMyViolation(id)).data
  await router.replace({ query: { tab: 'violations', violationId: String(id) } })
}

async function openRestriction(restriction) {
  restrictionDetail.value = restriction
  await router.replace({ query: { tab: 'restrictions', restrictionId: String(restriction.id) } })
}

async function closeDetail() {
  violationDetail.value = null
  restrictionDetail.value = null
  await router.replace({ query: { tab: activeTab.value } })
}

onMounted(async () => {
  await Promise.all([loadViolations(), loadRestrictions()])
  if (route.query.violationId) await openViolation(route.query.violationId)
  if (route.query.restrictionId) {
    const target = restrictions.value.records.find((item) => String(item.id) === String(route.query.restrictionId))
    if (target) await openRestriction(target)
  }
})
</script>

<template>
  <section class="account-safety-page">
    <div class="section-heading"><div><p class="eyebrow accent">ACCOUNT SAFETY</p><h1>违规与限制</h1><p>查看平台确认的内容违规及当前账户业务能力限制。</p></div></div>
    <el-tabs v-model="activeTab" @tab-change="router.replace({ query: { tab: activeTab } })">
      <el-tab-pane label="违规记录" name="violations"><div v-loading="loadingViolations" class="report-list"><article v-for="item in violations.records" :key="item.id" class="report-card"><div><el-tag effect="plain">{{ violationStatusLabel(item.status) }}</el-tag><span>{{ violationSeverityLabel(item.severity) }} · {{ violationPenaltyLabel(item.penaltyType) }}</span></div><p>{{ item.note }}</p><small>{{ item.confirmedAt }}</small><el-button text @click="openViolation(item.id)">查看详情</el-button></article><EmptyState v-if="!loadingViolations && !violations.records.length" title="暂无违规记录" description="当前账户没有内容违规记录。" /></div><PaginationBar :page="Number(violations.page)" :size="Number(violations.size)" :total="Number(violations.total)" @change="loadViolations" /></el-tab-pane>
      <el-tab-pane label="业务限制" name="restrictions"><div v-loading="loadingRestrictions" class="report-list"><article v-for="item in restrictions.records" :key="item.id" class="report-card" :class="{ 'active-restriction': item.effective }"><div><el-tag effect="plain">{{ item.effective ? '当前有效' : restrictionStatusLabel(item.status) }}</el-tag><span>{{ restrictionTypeLabel(item.restrictionType) }}</span></div><p>{{ item.reason }}</p><small>{{ item.startsAt }} ～ {{ item.expiresAt }}</small><el-button text @click="openRestriction(item)">查看详情</el-button></article><EmptyState v-if="!loadingRestrictions && !restrictions.records.length" title="暂无业务限制" description="当前账户没有业务能力限制。" /></div><PaginationBar :page="Number(restrictions.page)" :size="Number(restrictions.size)" :total="Number(restrictions.total)" @change="loadRestrictions" /></el-tab-pane>
    </el-tabs>
    <el-dialog :model-value="Boolean(violationDetail)" title="违规详情" width="min(650px, 92vw)" @update:model-value="!$event && closeDetail()"><div v-if="violationDetail" class="report-detail"><p><strong>状态：</strong>{{ violationStatusLabel(violationDetail.status) }}</p><p><strong>等级：</strong>{{ violationSeverityLabel(violationDetail.severity) }}</p><p><strong>处罚：</strong>{{ violationPenaltyLabel(violationDetail.penaltyType) }}</p><p><strong>说明：</strong>{{ violationDetail.note }}</p><p><strong>处罚截止：</strong>{{ violationDetail.penaltyExpiresAt || '仅警告，无限制期限' }}</p><p><strong>关联限制：</strong>{{ violationDetail.linkedRestrictionIds?.join('、') || '无' }}</p><p v-if="violationDetail.revokeReason"><strong>撤销原因：</strong>{{ violationDetail.revokeReason }}</p></div></el-dialog>
    <el-dialog :model-value="Boolean(restrictionDetail)" title="业务限制详情" width="min(650px, 92vw)" @update:model-value="!$event && closeDetail()"><div v-if="restrictionDetail" class="report-detail"><p><strong>类型：</strong>{{ restrictionTypeLabel(restrictionDetail.restrictionType) }}</p><p><strong>状态：</strong>{{ restrictionDetail.effective ? '当前有效' : restrictionStatusLabel(restrictionDetail.status) }}</p><p><strong>原因：</strong>{{ restrictionDetail.reason }}</p><p><strong>有效期：</strong>{{ restrictionDetail.startsAt }} ～ {{ restrictionDetail.expiresAt }}</p><p v-if="restrictionDetail.revokeReason"><strong>撤销原因：</strong>{{ restrictionDetail.revokeReason }}</p></div></el-dialog>
  </section>
</template>
