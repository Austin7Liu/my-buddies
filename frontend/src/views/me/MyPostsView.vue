<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deletePost, listMyPosts, updatePost } from '../../api/post.js'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import PostCard from '../../components/PostCard.vue'

const route = useRoute()
const router = useRouter()
const statuses = ['', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'OFFLINE', 'DELETED']
const status = ref(statuses.includes(String(route.query.status ?? '')) ? String(route.query.status ?? '') : '')
const posts = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(true)
const editing = ref(false)
const editForm = reactive({ id: '', content: '' })

async function load(page = 1) {
  loading.value = true
  try {
    posts.value = (await listMyPosts(status.value, page)).data
  } finally {
    loading.value = false
  }
}

function startEdit(post) {
  editForm.id = post.id
  editForm.content = post.content
  editing.value = true
}

async function saveEdit() {
  const content = editForm.content.trim()
  if (!content || content.length > 2000) {
    ElMessage.warning('正文应为 1 至 2000 个字符')
    return
  }
  try {
    await updatePost(editForm.id, content)
    editing.value = false
    ElMessage.success('帖子已重新提交审核')
    await load(posts.value.page)
  } catch {
    // 统一请求拦截器已经展示后端错误。
  }
}

async function remove(post) {
  try {
    await ElMessageBox.confirm('删除后帖子不再公开，确认继续？', '删除帖子', { type: 'warning' })
    await deletePost(post.id)
    ElMessage.success('帖子已删除')
    await load(posts.value.page)
  } catch {
    // 用户取消或请求失败时保持当前页面。
  }
}

watch(status, async (value) => {
  await router.replace({ query: value ? { status: value } : {} })
  await load(1)
})
onMounted(load)
</script>

<template>
  <section v-loading="loading">
    <p class="eyebrow accent">MY POSTS</p>
    <div class="section-heading"><h1>我的帖子</h1><RouterLink to="/posts/create"><el-button type="primary" round>发布帖子</el-button></RouterLink></div>
    <el-tabs v-model="status">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待审核" name="PENDING_REVIEW" />
      <el-tab-pane label="已发布" name="PUBLISHED" />
      <el-tab-pane label="已驳回" name="REJECTED" />
      <el-tab-pane label="已下架" name="OFFLINE" />
      <el-tab-pane label="已删除" name="DELETED" />
    </el-tabs>
    <div class="post-list"><PostCard v-for="post in posts.records" :key="post.id" :post="post" show-status editable @edit="startEdit" @delete="remove" /></div>
    <EmptyState v-if="!loading && !posts.records.length" title="当前没有帖子" />
    <PaginationBar :page="Number(posts.page)" :size="Number(posts.size)" :total="Number(posts.total)" @change="load" />

    <el-dialog v-model="editing" title="编辑帖子" width="min(620px, 92vw)">
      <el-input v-model="editForm.content" type="textarea" :rows="9" maxlength="2000" show-word-limit />
      <template #footer><el-button @click="editing = false">取消</el-button><el-button type="primary" @click="saveEdit">重新提交审核</el-button></template>
    </el-dialog>
  </section>
</template>
