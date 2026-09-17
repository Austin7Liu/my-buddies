<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getPublicProfile, getPublicReputation, listReceivedMeetupReviews } from '../../api/profile.js'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import { avatarLabel, genderLabel, percentageLabel } from '../../utils/profile.js'
import UserAvatar from '../../components/UserAvatar.vue'

const route = useRoute()
const accountId = String(route.params.accountId)
const profile = ref(null)
const reputation = ref(null)
const reviews = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(true)

async function loadReviews(page = 1) {
  reviews.value = (await listReceivedMeetupReviews(accountId, page)).data
}

async function load() {
  loading.value = true
  try {
    const [profileResponse, reputationResponse] = await Promise.all([
      getPublicProfile(accountId),
      getPublicReputation(accountId),
      loadReviews(),
    ])
    profile.value = profileResponse.data
    reputation.value = reputationResponse.data
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="public-profile-page">
    <div v-if="profile" class="profile-hero">
      <UserAvatar :avatar-code="profile.avatarCode" :nickname="profile.nickname" :size="92" />
      <div><div class="profile-title"><h1>{{ profile.nickname }}</h1><el-tag v-if="profile.verified" type="success">已实名</el-tag><el-tag v-if="profile.accountRestricted" type="danger">账户受限</el-tag></div><p>{{ profile.bio || '这个用户还没有填写个人简介。' }}</p><div class="profile-meta"><span>{{ avatarLabel(profile.avatarCode) }}</span><span v-if="profile.city">{{ profile.city }}{{ profile.district ? ` · ${profile.district}` : '' }}</span><span v-if="profile.age">{{ profile.age }} 岁 · {{ genderLabel(profile.gender) }}</span><span>加入于 {{ profile.joinedAt }}</span></div></div>
    </div>

    <div v-if="reputation" class="reputation-grid">
      <article><span>线下履约</span><strong>{{ reputation.offlineFulfillmentCount }}</strong><small>累计活动</small></article>
      <article><span>出席率</span><strong>{{ percentageLabel(reputation.attendanceRate) }}</strong><small>出席 {{ reputation.attendedCount }} · 缺席 {{ reputation.absentCount }} · 请假 {{ reputation.excusedCount }}</small></article>
      <article><span>活动评价</span><strong>{{ reputation.averageRating ?? '暂无' }}</strong><small>{{ reputation.receivedReviewCount }} 条可见评价</small></article>
    </div>

    <div class="section-heading"><div><p class="eyebrow accent">MEETUP REVIEWS</p><h2>收到的活动评价</h2></div><span>{{ reviews.total }} 条</span></div>
    <div class="review-list"><article v-for="review in reviews.records" :key="review.id" class="review-card"><div class="review-head"><RouterLink :to="`/profiles/${review.reviewer.accountId}`"><strong>{{ review.reviewer.nickname }}</strong></RouterLink><span>{{ '★'.repeat(review.rating) }}{{ '☆'.repeat(5 - review.rating) }}</span></div><p>{{ review.comment || '评价人没有填写文字内容。' }}</p><small>活动 #{{ review.meetupId }} · {{ review.createdAt }}</small></article></div>
    <EmptyState v-if="!loading && !reviews.records.length" title="暂无活动评价" description="完成线下活动并收到评价后会显示在这里。" />
    <PaginationBar :page="Number(reviews.page)" :size="Number(reviews.size)" :total="Number(reviews.total)" @change="loadReviews" />
  </section>
</template>
