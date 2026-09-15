<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCircle, getMyMembership, joinCircle, leaveCircle, listCircleMembers } from '../../api/circle.js'
import { isAuthenticated } from '../../stores/auth.js'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'

const route = useRoute()
const router = useRouter()
const circleId = Number(route.params.circleId)
const circle = ref(null)
const membership = ref(null)
const members = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(true)
const acting = ref(false)

async function load(page = 1) {
  loading.value = true
  try {
    const [circleResponse, memberResponse] = await Promise.all([getCircle(circleId), listCircleMembers(circleId, page)])
    circle.value = circleResponse.data
    members.value = memberResponse.data
    if (isAuthenticated()) {
      try { membership.value = (await getMyMembership(circleId)).data } catch (error) {
        if (error.response?.status === 404) membership.value = null
      }
    }
  } finally { loading.value = false }
}

async function toggleMembership() {
  if (!isAuthenticated()) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  acting.value = true
  try {
    const active = membership.value?.status === 'ACTIVE'
    membership.value = (active ? await leaveCircle(circleId) : await joinCircle(circleId)).data
    ElMessage.success(active ? '已退出圈子' : '已加入圈子')
    await load(members.value.page)
  } finally { acting.value = false }
}

onMounted(load)
</script>

<template>
  <section v-loading="loading">
    <el-breadcrumb separator="/"><el-breadcrumb-item :to="{ name: 'home' }">发现</el-breadcrumb-item><el-breadcrumb-item>{{ circle?.name || '圈子' }}</el-breadcrumb-item></el-breadcrumb>
    <div v-if="circle" class="detail-hero">
      <div><p class="eyebrow accent">{{ circle.city }}{{ circle.district ? ` · ${circle.district}` : '' }}</p><h1>{{ circle.name }}</h1><p>{{ circle.description || '这个圈子还没有填写介绍。' }}</p><small>由 {{ circle.creator?.nickname || `用户 ${circle.creatorAccountId}` }} 创建</small></div>
      <el-button type="primary" size="large" round :loading="acting" @click="toggleMembership">{{ membership?.status === 'ACTIVE' ? '退出圈子' : '加入圈子' }}</el-button>
    </div>
    <div class="section-heading"><div><p class="eyebrow accent">MEMBERS</p><h2>圈子成员</h2></div><span>{{ members.total }} 人</span></div>
    <div class="member-list"><article v-for="member in members.records" :key="member.account.accountId"><span class="avatar">{{ member.account.nickname.slice(0, 1) }}</span><div><strong>{{ member.account.nickname }}</strong><small>{{ member.role === 'OWNER' ? '创建者' : '成员' }} · {{ member.account.verified ? '已实名' : '未实名' }}</small></div></article></div>
    <EmptyState v-if="!loading && !members.records.length" title="暂无成员" />
    <PaginationBar :page="Number(members.page)" :size="Number(members.size)" :total="Number(members.total)" @change="load" />
  </section>
</template>
