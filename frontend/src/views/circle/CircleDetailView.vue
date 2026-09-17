<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCircle, getMyMembership, joinCircle, leaveCircle, listCircleMembers } from '../../api/circle.js'
import { isAuthenticated } from '../../stores/auth.js'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import PostCard from '../../components/PostCard.vue'
import { listCirclePosts } from '../../api/post.js'
import { listCircleMeetups } from '../../api/meetup.js'
import MeetupCard from '../../components/MeetupCard.vue'

const route = useRoute()
const router = useRouter()
const circleId = String(route.params.circleId)
const circle = ref(null)
const membership = ref(null)
const members = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(true)
const acting = ref(false)
const postLoading = ref(true)
const posts = ref({ records: [], page: 1, size: 20, total: 0 })
const meetupLoading = ref(true)
const meetups = ref({ records: [], page: 1, size: 20, total: 0 })

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

async function loadPosts(page = 1) {
  postLoading.value = true
  try { posts.value = (await listCirclePosts(circleId, page)).data } finally { postLoading.value = false }
}

function createPost() {
  router.push({ name: 'post-create', query: { circleId } })
}

function createMeetup() { router.push({ name: 'meetup-create', query: { circleId } }) }

async function loadMeetups(page = 1) {
  meetupLoading.value = true
  try { meetups.value = (await listCircleMeetups(circleId, page)).data } finally { meetupLoading.value = false }
}

onMounted(() => Promise.all([load(), loadPosts(), loadMeetups()]))
</script>

<template>
  <section v-loading="loading">
    <el-breadcrumb separator="/"><el-breadcrumb-item :to="{ name: 'home' }">发现</el-breadcrumb-item><el-breadcrumb-item>{{ circle?.name || '圈子' }}</el-breadcrumb-item></el-breadcrumb>
    <div v-if="circle" class="detail-hero">
      <div><p class="eyebrow accent">{{ circle.city }}{{ circle.district ? ` · ${circle.district}` : '' }}</p><h1>{{ circle.name }}</h1><p>{{ circle.description || '这个圈子还没有填写介绍。' }}</p><small>由 <RouterLink class="profile-link" :to="`/profiles/${circle.creator?.accountId || circle.creatorAccountId}`">{{ circle.creator?.nickname || `用户 ${circle.creatorAccountId}` }}</RouterLink> 创建</small></div>
      <el-button type="primary" size="large" round :loading="acting" @click="toggleMembership">{{ membership?.status === 'ACTIVE' ? '退出圈子' : '加入圈子' }}</el-button>
    </div>
    <div class="section-heading"><div><p class="eyebrow accent">MEMBERS</p><h2>圈子成员</h2></div><span>{{ members.total }} 人</span></div>
    <div class="member-list"><article v-for="member in members.records" :key="member.account.accountId"><span class="avatar">{{ member.account.nickname.slice(0, 1) }}</span><div><RouterLink class="profile-link" :to="`/profiles/${member.account.accountId}`"><strong>{{ member.account.nickname }}</strong></RouterLink><small>{{ member.role === 'OWNER' ? '创建者' : '成员' }} · {{ member.account.verified ? '已实名' : '未实名' }}</small></div></article></div>
    <EmptyState v-if="!loading && !members.records.length" title="暂无成员" />
    <PaginationBar :page="Number(members.page)" :size="Number(members.size)" :total="Number(members.total)" @change="load" />
    <div class="section-heading"><div><p class="eyebrow accent">CIRCLE MEETUPS</p><h2>圈子活动</h2></div><el-button type="primary" round @click="createMeetup">在圈子创建活动</el-button></div>
    <div v-loading="meetupLoading" class="meetup-grid"><MeetupCard v-for="meetup in meetups.records" :key="meetup.id" :meetup="meetup" /></div>
    <EmptyState v-if="!meetupLoading && !meetups.records.length" title="圈子里还没有公开活动" />
    <PaginationBar :page="Number(meetups.page)" :size="Number(meetups.size)" :total="Number(meetups.total)" @change="loadMeetups" />
    <div class="section-heading"><div><p class="eyebrow accent">CIRCLE POSTS</p><h2>圈子帖子</h2></div><el-button type="primary" round @click="createPost">在圈子发帖</el-button></div>
    <div v-loading="postLoading" class="post-list"><PostCard v-for="post in posts.records" :key="post.id" :post="post" /></div>
    <EmptyState v-if="!postLoading && !posts.records.length" title="圈子里还没有公开帖子" />
    <PaginationBar :page="Number(posts.page)" :size="Number(posts.size)" :total="Number(posts.total)" @change="loadPosts" />
  </section>
</template>
