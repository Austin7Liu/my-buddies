<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createPostComment,
  deletePostComment,
  getPost,
  listPostComments,
  updatePostComment,
} from '../../api/post.js'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import PostCard from '../../components/PostCard.vue'
import { authState, isAuthenticated } from '../../stores/auth.js'
import { commentStatusLabel, validateComment } from '../../utils/post.js'

const route = useRoute()
const router = useRouter()
const postId = String(route.params.postId)
const post = ref(null)
const comments = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(true)
const commentLoading = ref(false)
const composer = reactive({ content: '', parentCommentId: null, replyingTo: '' })
const editing = reactive({ open: false, id: '', content: '' })

const currentAccountId = computed(() => String(authState.account?.id ?? ''))

function isMine(comment) {
  return currentAccountId.value && String(comment.author?.accountId ?? '') === currentAccountId.value
}

function login() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

async function loadComments(page = 1) {
  comments.value = (await listPostComments(postId, page)).data
}

async function load() {
  loading.value = true
  try {
    const [postResponse] = await Promise.all([getPost(postId), loadComments()])
    post.value = postResponse.data
  } finally {
    loading.value = false
  }
}

function replyTo(comment) {
  if (!isAuthenticated()) return login()
  composer.parentCommentId = comment.id
  composer.replyingTo = comment.author?.nickname || '该用户'
}

function cancelReply() {
  composer.parentCommentId = null
  composer.replyingTo = ''
}

async function submitComment() {
  if (!isAuthenticated()) return login()
  const error = validateComment(composer.content)
  if (error) return ElMessage.warning(error)
  commentLoading.value = true
  try {
    await createPostComment(postId, composer.content.trim(), composer.parentCommentId)
    composer.content = ''
    cancelReply()
    ElMessage.success('评论已发布')
    await loadComments(1)
  } finally {
    commentLoading.value = false
  }
}

function startEdit(comment) {
  editing.id = comment.id
  editing.content = comment.content
  editing.open = true
}

async function saveEdit() {
  const error = validateComment(editing.content)
  if (error) return ElMessage.warning(error)
  commentLoading.value = true
  try {
    await updatePostComment(postId, editing.id, editing.content.trim())
    editing.open = false
    ElMessage.success('评论已更新')
    await loadComments(comments.value.page)
  } finally {
    commentLoading.value = false
  }
}

async function removeComment(comment) {
  const confirmed = await ElMessageBox.confirm(
    '删除后评论正文将不再显示，且不能恢复。',
    '确认删除评论？',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
  ).then(() => true).catch(() => false)
  if (!confirmed) return
  await deletePostComment(postId, comment.id)
  ElMessage.success('评论已删除')
  await loadComments(comments.value.page)
}

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="post-detail-page">
    <p class="eyebrow accent">POST DETAIL</p>
    <PostCard v-if="post" :post="post" />

    <div class="section-heading"><div><h1>评论</h1><p>共 {{ comments.total }} 条评论，当前支持一级回复。</p></div></div>
    <div class="comment-composer">
      <div v-if="composer.parentCommentId" class="reply-target">正在回复 {{ composer.replyingTo }}<el-button link @click="cancelReply">取消回复</el-button></div>
      <el-input v-model="composer.content" type="textarea" :rows="4" maxlength="500" show-word-limit :placeholder="isAuthenticated() ? '友善交流，分享真实体验' : '登录后参与评论'" :disabled="!isAuthenticated()" />
      <div class="composer-actions"><el-button v-if="!isAuthenticated()" @click="login">登录后评论</el-button><el-button v-else type="primary" :loading="commentLoading" @click="submitComment">发布评论</el-button></div>
    </div>

    <div class="comment-list">
      <article v-for="comment in comments.records" :key="comment.id" class="comment-card" :class="{ reply: comment.parentCommentId }">
        <div class="comment-head"><div class="post-author"><span class="avatar">{{ comment.author?.nickname?.slice(0, 1) || '?' }}</span><div><RouterLink v-if="comment.author?.accountId" class="profile-link" :to="`/profiles/${comment.author.accountId}`"><strong>{{ comment.author.nickname }}</strong></RouterLink><strong v-else>已注销用户</strong><small>{{ comment.createdAt }}</small></div></div><span v-if="comment.parentCommentId" class="reply-label">回复评论 #{{ comment.parentCommentId }}</span></div>
        <p v-if="comment.status === 'VISIBLE'" class="comment-content">{{ comment.content }}</p>
        <p v-else class="comment-placeholder">{{ commentStatusLabel(comment.status) }}</p>
        <footer v-if="comment.status === 'VISIBLE'"><el-button v-if="!comment.parentCommentId" link @click="replyTo(comment)">回复</el-button><template v-if="isMine(comment)"><el-button link type="primary" @click="startEdit(comment)">编辑</el-button><el-button link type="danger" @click="removeComment(comment)">删除</el-button></template></footer>
      </article>
    </div>
    <EmptyState v-if="!loading && !comments.records.length" title="还没有评论" description="来发表第一条友善评论吧。" />
    <PaginationBar :page="Number(comments.page)" :size="Number(comments.size)" :total="Number(comments.total)" @change="loadComments" />

    <el-dialog v-model="editing.open" title="编辑评论" width="min(560px, 92vw)">
      <el-input v-model="editing.content" type="textarea" :rows="5" maxlength="500" show-word-limit />
      <template #footer><el-button @click="editing.open = false">取消</el-button><el-button type="primary" :loading="commentLoading" @click="saveEdit">保存修改</el-button></template>
    </el-dialog>
  </section>
</template>
