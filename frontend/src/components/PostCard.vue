<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { bookmarkPost, likePost, removePostBookmark, unlikePost } from '../api/post.js'
import { authState, isAuthenticated } from '../stores/auth.js'
import { postStatusLabel } from '../utils/post.js'
import ContentReportDialog from './ContentReportDialog.vue'

const props = defineProps({
  post: { type: Object, required: true },
  showStatus: { type: Boolean, default: false },
  editable: { type: Boolean, default: false },
  interactive: { type: Boolean, default: true },
})
defineEmits(['edit', 'delete'])

const route = useRoute()
const router = useRouter()
const liked = ref(Boolean(props.post.likedByMe))
const bookmarked = ref(Boolean(props.post.bookmarkedByMe))
const likeCount = ref(Number(props.post.likeCount ?? 0))
const interactionLoading = ref(false)
const reportOpen = ref(false)
const isOwnPost = () => String(authState.account?.id ?? '') === String(props.post.author?.accountId ?? props.post.authorAccountId)

watch(() => props.post, (post) => {
  liked.value = Boolean(post.likedByMe)
  bookmarked.value = Boolean(post.bookmarkedByMe)
  likeCount.value = Number(post.likeCount ?? 0)
})

function requireLogin() {
  if (isAuthenticated()) return true
  router.push({ name: 'login', query: { redirect: route.fullPath } })
  return false
}

async function toggleLike() {
  if (!requireLogin() || interactionLoading.value) return
  interactionLoading.value = true
  try {
    const response = await (liked.value ? unlikePost(props.post.id) : likePost(props.post.id))
    liked.value = response.data.likedByMe
    bookmarked.value = response.data.bookmarkedByMe
    likeCount.value = Number(response.data.likeCount)
  } finally {
    interactionLoading.value = false
  }
}

async function toggleBookmark() {
  if (!requireLogin() || interactionLoading.value) return
  interactionLoading.value = true
  try {
    const response = await (bookmarked.value ? removePostBookmark(props.post.id) : bookmarkPost(props.post.id))
    liked.value = response.data.likedByMe
    bookmarked.value = response.data.bookmarkedByMe
    likeCount.value = Number(response.data.likeCount)
    ElMessage.success(bookmarked.value ? '已收藏' : '已取消收藏')
  } finally {
    interactionLoading.value = false
  }
}

function openReport() {
  if (!requireLogin()) return
  reportOpen.value = true
}
</script>

<template>
  <article class="post-card">
    <header>
      <div class="post-author">
        <span class="avatar">{{ post.author?.nickname?.slice(0, 1) || '?' }}</span>
        <div><RouterLink class="profile-link" :to="`/profiles/${post.author?.accountId || post.authorAccountId}`"><strong>{{ post.author?.nickname || `用户 ${post.authorAccountId}` }}</strong></RouterLink><small>{{ post.createdAt }}</small></div>
      </div>
      <el-tag v-if="showStatus" effect="plain">{{ postStatusLabel(post.status) }}</el-tag>
    </header>
    <RouterLink v-if="post.status === 'PUBLISHED'" class="post-body post-body-link" :to="`/posts/${post.id}`">{{ post.content }}</RouterLink>
    <p v-else class="post-body">{{ post.content }}</p>
    <div class="post-meta">
      <span v-if="post.circleId">Circle #{{ post.circleId }}</span>
      <span v-else-if="post.topicId">#Topic {{ post.topicId }}</span>
      <span v-else>首页动态</span>
      <template v-if="interactive && post.status === 'PUBLISHED'">
        <el-button link :type="liked ? 'danger' : ''" :loading="interactionLoading" @click="toggleLike">{{ liked ? '♥' : '♡' }} {{ likeCount }}</el-button>
        <el-button link :type="bookmarked ? 'primary' : ''" :disabled="interactionLoading" @click="toggleBookmark">{{ bookmarked ? '已收藏' : '收藏' }}</el-button>
        <el-button v-if="!isOwnPost()" link type="danger" @click="openReport">举报</el-button>
      </template>
      <span v-else>♡ {{ likeCount }}</span>
    </div>
    <p v-if="post.moderationReason" class="moderation-reason">审核原因：{{ post.moderationReason }}</p>
    <footer v-if="editable && post.status !== 'DELETED'">
      <el-button v-if="['PENDING_REVIEW', 'PUBLISHED', 'REJECTED'].includes(post.status)" link type="primary" @click="$emit('edit', post)">编辑</el-button>
      <el-button link type="danger" @click="$emit('delete', post)">删除</el-button>
    </footer>
    <ContentReportDialog v-model="reportOpen" target-type="POST" :target-id="post.id" />
  </article>
</template>
