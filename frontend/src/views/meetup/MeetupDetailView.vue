<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  acceptMeetupApplication, applyMeetup, cancelMeetup, checkInMeetup, completeMeetup, confirmMeetup,
  createMeetupReview, getMeetup, getMyMeetupCheckIn, getMyMeetupFulfillment, getMyMeetupParticipation,
  listMeetupApplications, listMeetupCheckIns, listMeetupFulfillments, publishMeetup,
  listMeetupReviewCandidates, listMyMeetupReviews, rejectMeetupApplication, updateMeetupReview,
  withdrawMeetup,
} from '../../api/meetup.js'
import ModerationReasonDialog from '../../components/admin/ModerationReasonDialog.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import { authState, isAuthenticated } from '../../stores/auth.js'
import { fulfillmentResultLabel, meetupModeLabel, meetupStatusLabel, participantStatusLabel, validateMeetupReview } from '../../utils/meetup.js'
import { createLocationCheckInAction, geolocationErrorMessage, loadExistingCheckIn } from '../../utils/meetupGeolocation.js'

const route = useRoute()
const router = useRouter()
const meetupId = String(route.params.meetupId)
const meetup = ref(null)
const participation = ref(null)
const applications = ref({ records: [], page: 1, size: 20, total: 0 })
const checkIns = ref([])
const fulfillments = ref([])
const myFulfillment = ref(null)
const loading = ref(true)
const acting = ref(false)
const actingAccountId = ref(null)
const applyOpen = ref(false)
const applicationMessage = ref('')
const reasonAction = ref(null)
const checkIn = ref(null)
const checkInLoading = ref(false)
const reviewCandidates = ref([])
const myReviews = ref([])
const reviewTarget = ref(null)
const reviewForm = ref({ rating: 5, comment: '' })
const reviewSubmitting = ref(false)

const isCreator = computed(() => String(meetup.value?.creatorAccountId ?? '') === String(authState.account?.id ?? ''))
const isAccepted = computed(() => participation.value?.status === 'ACCEPTED')
const canApply = computed(() => !isCreator.value && meetup.value?.status === 'OPEN'
  && meetup.value?.remainingSlots > 0
  && (!participation.value || ['REJECTED', 'CANCELLED'].includes(participation.value.status)))
const canWithdraw = computed(() => !isCreator.value
  && ['APPLIED', 'ACCEPTED'].includes(participation.value?.status)
  && ['OPEN', 'CONFIRMED'].includes(meetup.value?.status))
const canAccessCheckIn = computed(() => isAuthenticated() && isAccepted.value
  && meetup.value?.meetupMode === 'OFFLINE'
  && meetup.value?.locationLatitude != null && meetup.value?.locationLongitude != null)
const canCheckIn = computed(() => canAccessCheckIn.value && meetup.value?.status === 'CONFIRMED')
const canReview = computed(() => meetup.value?.meetupMode === 'OFFLINE'
  && meetup.value?.status === 'COMPLETED'
  && myFulfillment.value?.result === 'ATTENDED')
const locationCheckInAction = createLocationCheckInAction(globalThis.navigator?.geolocation, checkInMeetup)

async function optionalData(request) {
  try {
    return (await request()).data
  } catch (error) {
    if (error.response?.status === 404) return null
    throw error
  }
}

async function loadApplications(page = 1) {
  if (isCreator.value) applications.value = (await listMeetupApplications(meetupId, page)).data
}

async function loadReviewState() {
  reviewCandidates.value = []
  myReviews.value = []
  if (!canReview.value) return
  const [candidates, reviews] = await Promise.all([
    listMeetupReviewCandidates(meetupId),
    listMyMeetupReviews(meetupId),
  ])
  reviewCandidates.value = candidates.data
  myReviews.value = reviews.data.records
}

async function loadRelatedState() {
  participation.value = null
  checkIn.value = null
  myFulfillment.value = null
  checkIns.value = []
  fulfillments.value = []
  reviewCandidates.value = []
  myReviews.value = []
  if (!isAuthenticated()) return
  participation.value = await optionalData(() => getMyMeetupParticipation(meetupId))
  if (isCreator.value) {
    await loadApplications()
    if (meetup.value.meetupMode === 'OFFLINE') {
      if (canAccessCheckIn.value) {
        checkIn.value = await loadExistingCheckIn(meetupId, getMyMeetupCheckIn)
      }
      checkIns.value = (await listMeetupCheckIns(meetupId)).data.records
      if (meetup.value.status === 'COMPLETED') {
        fulfillments.value = (await listMeetupFulfillments(meetupId)).data.records
        myFulfillment.value = fulfillments.value.find((item) => String(item.accountId) === String(authState.account?.id)) ?? null
        await loadReviewState()
      }
    }
    return
  }
  if (canAccessCheckIn.value) checkIn.value = await loadExistingCheckIn(meetupId, getMyMeetupCheckIn)
  if (isAccepted.value && meetup.value.status === 'COMPLETED' && meetup.value.meetupMode === 'OFFLINE') {
    myFulfillment.value = await optionalData(() => getMyMeetupFulfillment(meetupId))
    await loadReviewState()
  }
}

async function load() {
  loading.value = true
  try {
    meetup.value = (await getMeetup(meetupId)).data
    await loadRelatedState()
  } finally {
    loading.value = false
  }
}

function requireLogin() {
  if (isAuthenticated()) return true
  router.push({ name: 'login', query: { redirect: route.fullPath } })
  return false
}

async function confirmAction(message, title, action, successMessage) {
  const confirmed = await ElMessageBox.confirm(message, title, {
    type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消',
  }).then(() => true).catch(() => false)
  if (!confirmed) return
  acting.value = true
  try {
    await action()
    ElMessage.success(successMessage)
    await load()
  } finally {
    acting.value = false
  }
}

function publish() {
  return confirmAction('发布后活动将公开展示，并开始接受用户报名。', '确认发布活动？',
    () => publishMeetup(meetupId), '活动已发布')
}

function confirmActivity() {
  return confirmAction('确认后停止接受新申请，并进入待签到状态。', '确认活动？',
    () => confirmMeetup(meetupId), '活动已确认')
}

function completeActivity() {
  return confirmAction('系统将根据签到记录结算所有已接受参与者的履约结果。', '确认完成活动？',
    () => completeMeetup(meetupId), '活动已完成并结算履约结果')
}

function openApplication() {
  if (!requireLogin()) return
  applicationMessage.value = ''
  applyOpen.value = true
}

async function submitApplication() {
  const message = applicationMessage.value.trim()
  if (message.length > 500) return ElMessage.warning('申请说明不能超过 500 个字符')
  acting.value = true
  try {
    participation.value = (await applyMeetup(meetupId, message)).data
    applyOpen.value = false
    ElMessage.success('申请已提交，等待创建者处理')
    meetup.value = (await getMeetup(meetupId)).data
  } finally {
    acting.value = false
  }
}

async function acceptApplication(application) {
  if (actingAccountId.value) return
  actingAccountId.value = application.accountId
  try {
    await acceptMeetupApplication(meetupId, application.accountId)
    ElMessage.success('已接受申请')
    await load()
  } finally {
    actingAccountId.value = null
  }
}

function openReason(action, application = null) {
  reasonAction.value = { action, application }
}

async function submitReason(reason) {
  acting.value = true
  try {
    if (reasonAction.value.action === 'reject') {
      await rejectMeetupApplication(meetupId, reasonAction.value.application.accountId, reason)
      ElMessage.success('已驳回申请')
    } else if (reasonAction.value.action === 'cancel') {
      await cancelMeetup(meetupId, reason)
      ElMessage.success('活动已取消')
    } else {
      await withdrawMeetup(meetupId, reason)
      ElMessage.success('已退出活动')
    }
    reasonAction.value = null
    await load()
  } finally {
    acting.value = false
  }
}

async function performCheckIn() {
  if (checkInLoading.value || checkIn.value || locationCheckInAction.isPending()) return
  checkInLoading.value = true
  try {
    checkIn.value = (await locationCheckInAction.execute(meetupId)).data
    ElMessage.success('签到成功')
    if (isCreator.value) checkIns.value = (await listMeetupCheckIns(meetupId)).data.records
  } catch (error) {
    if (!error.response) ElMessage.error(geolocationErrorMessage(error))
  } finally {
    checkInLoading.value = false
  }
}

function reasonDialogTitle() {
  if (reasonAction.value?.action === 'reject') return '驳回活动申请'
  if (reasonAction.value?.action === 'cancel') return '取消活动'
  return '退出活动'
}

function reviewFor(candidate) {
  return myReviews.value.find((review) => String(review.reviewee?.accountId) === String(candidate.accountId)) ?? null
}

function openReview(candidate) {
  const existing = reviewFor(candidate)
  if (existing?.status === 'HIDDEN') return
  reviewTarget.value = { candidate, existing }
  reviewForm.value = {
    rating: existing?.rating ?? 5,
    comment: existing?.comment ?? '',
  }
}

async function submitReview() {
  if (reviewSubmitting.value || !reviewTarget.value) return
  const error = validateMeetupReview(reviewForm.value.rating, reviewForm.value.comment)
  if (error) return ElMessage.warning(error)
  reviewSubmitting.value = true
  const payload = {
    rating: reviewForm.value.rating,
    comment: reviewForm.value.comment.trim() || null,
  }
  try {
    if (reviewTarget.value.existing) {
      await updateMeetupReview(meetupId, reviewTarget.value.existing.id, payload)
    } else {
      await createMeetupReview(meetupId, {
        revieweeAccountId: reviewTarget.value.candidate.accountId,
        ...payload,
      })
    }
    ElMessage.success(reviewTarget.value.existing ? '评价已更新' : '评价已提交')
    reviewTarget.value = null
    await loadReviewState()
  } finally {
    reviewSubmitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="meetup-detail-page">
    <el-breadcrumb separator="/"><el-breadcrumb-item :to="{ name: 'meetup-list' }">活动</el-breadcrumb-item><el-breadcrumb-item>{{ meetup?.title || '活动详情' }}</el-breadcrumb-item></el-breadcrumb>
    <div v-if="meetup" class="meetup-detail-hero">
      <div><div class="meetup-badges"><el-tag>{{ meetupModeLabel(meetup.meetupMode) }}</el-tag><el-tag effect="plain">{{ meetupStatusLabel(meetup.status) }}</el-tag></div><h1>{{ meetup.title }}</h1><p>{{ meetup.description }}</p><small>由 <RouterLink class="profile-link" :to="`/profiles/${meetup.creator?.accountId || meetup.creatorAccountId}`">{{ meetup.creator?.nickname || `用户 ${meetup.creatorAccountId}` }}</RouterLink> 创建</small></div>
      <div class="meetup-actions">
        <el-button v-if="isCreator && meetup.status === 'DRAFT'" type="primary" size="large" :loading="acting" @click="publish">发布活动</el-button>
        <el-button v-if="isCreator && meetup.status === 'OPEN'" type="primary" size="large" :loading="acting" @click="confirmActivity">确认活动</el-button>
        <el-button v-if="isCreator && meetup.status === 'CONFIRMED'" type="primary" size="large" :loading="acting" @click="completeActivity">完成活动</el-button>
        <el-button v-if="isCreator && ['DRAFT', 'OPEN', 'CONFIRMED'].includes(meetup.status)" size="large" :disabled="acting" @click="openReason('cancel')">取消活动</el-button>
        <el-button v-if="canApply" type="primary" size="large" @click="openApplication">申请加入</el-button>
        <el-button v-if="canWithdraw" size="large" :disabled="acting" @click="openReason('withdraw')">退出活动</el-button>
      </div>
    </div>

    <el-alert v-if="!isCreator && participation" class="meetup-state-alert" type="info" :closable="false">
      <template #title>我的参与状态：{{ participantStatusLabel(participation.status) }}</template>
      <p v-if="participation.decisionReason">处理原因：{{ participation.decisionReason }}</p>
    </el-alert>

    <div v-if="meetup" class="meetup-detail-grid">
      <article><span>时间</span><strong>{{ meetup.startTime }}</strong><p>至 {{ meetup.endTime }}</p><small>报名截止：{{ meetup.applicationDeadline }}</small></article>
      <article><span>人数</span><strong>{{ meetup.acceptedCount }}/{{ meetup.capacity }}</strong><p>剩余 {{ meetup.remainingSlots }} 个名额</p><small>容量包含创建者</small></article>
      <article><span>参与要求</span><strong>{{ meetup.minimumAge }}–{{ meetup.maximumAge }} 岁</strong><p>{{ meetup.genderRequirement === 'SAME_GENDER' ? '与创建者同性别' : '性别不限' }}</p><small>{{ meetup.skillRequirement || '无额外技能要求' }}</small></article>
    </div>

    <div v-if="meetup" class="meetup-location-panel">
      <template v-if="meetup.meetupMode === 'OFFLINE'"><p class="eyebrow accent">OFFLINE LOCATION</p><h2>{{ meetup.locationName }}</h2><p>{{ meetup.city }} {{ meetup.district }}</p><p v-if="meetup.address" class="private-detail">详细地址：{{ meetup.address }}</p><p v-else class="privacy-note">详细地址仅对创建者和已接受成员开放。</p></template>
      <template v-else><p class="eyebrow accent">ONLINE ACCESS</p><h2>{{ meetup.onlinePlatform }}</h2><p>{{ meetup.serverRegion || '未指定服务器区域' }}</p><p v-if="meetup.accessInstructions" class="private-detail">加入说明：{{ meetup.accessInstructions }}</p><p v-else class="privacy-note">加入说明仅对创建者和已接受成员开放。</p></template>
    </div>

    <section v-if="isCreator && meetup?.status !== 'DRAFT'" class="meetup-management-panel">
      <div class="section-heading"><div><p class="eyebrow accent">APPLICATIONS</p><h2>报名管理</h2></div><span>共 {{ applications.total }} 条参与记录</span></div>
      <el-table :data="applications.records" empty-text="暂无用户申请">
        <el-table-column label="申请人" min-width="150"><template #default="{ row }"><RouterLink class="profile-link" :to="`/profiles/${row.accountId}`">{{ row.profile?.nickname || `用户 ${row.accountId}` }}</RouterLink></template></el-table-column>
        <el-table-column prop="applicationMessage" label="申请说明" min-width="200" />
        <el-table-column label="状态" width="110"><template #default="{ row }">{{ participantStatusLabel(row.status) }}</template></el-table-column>
        <el-table-column prop="decisionReason" label="处理原因" min-width="160" />
        <el-table-column label="操作" width="150"><template #default="{ row }"><template v-if="row.status === 'APPLIED'"><el-button link type="success" :loading="String(actingAccountId) === String(row.accountId)" @click="acceptApplication(row)">接受</el-button><el-button link type="danger" @click="openReason('reject', row)">驳回</el-button></template><span v-else>已处理</span></template></el-table-column>
      </el-table>
      <PaginationBar :page="Number(applications.page)" :size="Number(applications.size)" :total="Number(applications.total)" @change="loadApplications" />
    </section>

    <div v-if="canAccessCheckIn" class="meetup-check-in-panel">
      <div><p class="eyebrow accent">ARRIVAL CHECK-IN</p><template v-if="checkIn"><h2>已到场签到</h2><p>签到时间：{{ checkIn.checkedInAt }}</p><p>距活动地点：{{ checkIn.distanceMeters }} 米</p></template><template v-else-if="canCheckIn"><h2>浏览器实时定位签到</h2><p>仅在本次签到时使用实时位置进行地理围栏校验，不保存你的精确坐标。</p></template><template v-else><h2>当前活动状态不可签到</h2><p>只有已确认的线下活动开放到场签到。</p></template></div>
      <el-button v-if="!checkIn && canCheckIn" type="primary" size="large" :loading="checkInLoading" :disabled="checkInLoading" @click="performCheckIn">到场签到</el-button>
      <el-tag v-else-if="checkIn" type="success" size="large">签到成功</el-tag>
    </div>

    <section v-if="isCreator && meetup?.meetupMode === 'OFFLINE' && ['CONFIRMED', 'COMPLETED'].includes(meetup.status)" class="meetup-management-panel">
      <div class="section-heading"><div><p class="eyebrow accent">CHECK-INS</p><h2>签到名单</h2></div><span>{{ checkIns.length }} 人已签到</span></div>
      <el-table :data="checkIns" empty-text="暂无签到记录"><el-table-column prop="accountId" label="账户 ID" min-width="180" /><el-table-column prop="checkedInAt" label="签到时间" min-width="180" /><el-table-column label="距离" width="120"><template #default="{ row }">{{ row.distanceMeters }} 米</template></el-table-column></el-table>
    </section>

    <section v-if="isCreator && fulfillments.length" class="meetup-management-panel">
      <div class="section-heading"><div><p class="eyebrow accent">FULFILLMENT</p><h2>履约结果</h2></div></div>
      <el-table :data="fulfillments"><el-table-column label="参与者" min-width="160"><template #default="{ row }"><RouterLink class="profile-link" :to="`/profiles/${row.accountId}`">{{ row.profile?.nickname || `用户 ${row.accountId}` }}</RouterLink></template></el-table-column><el-table-column label="结果" width="120"><template #default="{ row }">{{ fulfillmentResultLabel(row.result) }}</template></el-table-column><el-table-column prop="settledAt" label="结算时间" min-width="180" /></el-table>
    </section>

    <el-alert v-if="myFulfillment" class="meetup-state-alert" type="success" :closable="false" :title="`我的履约结果：${fulfillmentResultLabel(myFulfillment.result)}`" />

    <section v-if="canReview" class="meetup-management-panel">
      <div class="section-heading"><div><p class="eyebrow accent">PEER REVIEWS</p><h2>评价同行伙伴</h2></div><span>评价期为活动完成后 7 天</span></div>
      <el-table :data="reviewCandidates" empty-text="暂无其他实际出席者可评价">
        <el-table-column label="伙伴" min-width="180"><template #default="{ row }"><RouterLink class="profile-link" :to="`/profiles/${row.accountId}`">{{ row.nickname }}</RouterLink></template></el-table-column>
        <el-table-column label="我的评价" min-width="240"><template #default="{ row }"><template v-if="reviewFor(row)"><span>{{ '★'.repeat(reviewFor(row).rating) }}{{ '☆'.repeat(5 - reviewFor(row).rating) }}</span><small v-if="reviewFor(row).status === 'HIDDEN'" class="review-hidden-label">评价已被管理员隐藏</small></template><span v-else class="muted-text">尚未评价</span></template></el-table-column>
        <el-table-column label="操作" width="120"><template #default="{ row }"><el-button v-if="reviewFor(row)?.status !== 'HIDDEN'" link type="primary" @click="openReview(row)">{{ reviewFor(row) ? '编辑评价' : '去评价' }}</el-button><span v-else class="muted-text">不可编辑</span></template></el-table-column>
      </el-table>
    </section>
    <el-alert v-if="meetup?.closedReason" type="warning" :closable="false" :title="meetup.closedReason" />

    <el-dialog v-model="applyOpen" title="申请加入活动" width="min(560px, 92vw)">
      <p>创建者会根据参与要求审核申请。请勿填写手机号、身份证等敏感信息。</p>
      <el-input v-model="applicationMessage" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="简单介绍你的参与经验或时间安排（选填）" />
      <template #footer><el-button @click="applyOpen = false">取消</el-button><el-button type="primary" :loading="acting" @click="submitApplication">提交申请</el-button></template>
    </el-dialog>

    <el-dialog :model-value="Boolean(reviewTarget)" :title="reviewTarget?.existing ? '编辑活动评价' : '评价活动伙伴'" width="min(560px, 92vw)" @close="reviewTarget = null">
      <p>评价对象：<strong>{{ reviewTarget?.candidate.nickname }}</strong></p>
      <el-rate v-model="reviewForm.rating" show-score score-template="{value} 星" />
      <el-input v-model="reviewForm.comment" class="review-comment-input" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="分享真实、友善且与活动相关的评价（选填）" />
      <template #footer><el-button @click="reviewTarget = null">取消</el-button><el-button type="primary" :loading="reviewSubmitting" :disabled="reviewSubmitting" @click="submitReview">{{ reviewTarget?.existing ? '保存修改' : '提交评价' }}</el-button></template>
    </el-dialog>

    <ModerationReasonDialog :model-value="Boolean(reasonAction)" :title="reasonDialogTitle()" :confirm-text="reasonAction?.action === 'reject' ? '确认驳回' : '确认提交'" :loading="acting" @update:model-value="!$event && (reasonAction = null)" @confirm="submitReason" />
  </section>
</template>
