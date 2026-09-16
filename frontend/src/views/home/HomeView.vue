<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listCategories, listTopics } from '../../api/catalog.js'
import EmptyState from '../../components/EmptyState.vue'
import TopicCard from '../../components/TopicCard.vue'
import PostCard from '../../components/PostCard.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import { listPublicPosts } from '../../api/post.js'

const router = useRouter()
const keyword = ref('')
const categories = ref([])
const activeCategoryId = ref(null)
const topics = ref([])
const loading = ref(true)
const postLoading = ref(true)
const posts = ref({ records: [], page: 1, size: 20, total: 0 })

async function loadCategories() {
  loading.value = true
  try {
    const response = await listCategories()
    categories.value = response.data
    if (categories.value.length) await selectCategory(categories.value[0].id)
  } finally { loading.value = false }
}

async function selectCategory(categoryId) {
  activeCategoryId.value = categoryId
  const response = await listTopics(categoryId)
  topics.value = response.data
}

function searchNow() {
  const value = keyword.value.trim()
  if (value.length >= 2) router.push({ name: 'search', query: { keyword: value } })
}

function updateFollow(topic, followed) { topic.followedByMe = followed }
async function loadPosts(page = 1) {
  postLoading.value = true
  try { posts.value = (await listPublicPosts(page)).data } finally { postLoading.value = false }
}
onMounted(() => Promise.all([loadCategories(), loadPosts()]))
</script>

<template>
  <section class="home-view">
    <div class="hero-row">
      <div class="welcome-block"><p class="eyebrow accent">FIND YOUR PEOPLE</p><h1>从共同兴趣开始。</h1><p>发现话题、加入本地圈子，然后把计划变成一次真实的同行。</p></div>
      <div class="hero-search"><el-input v-model="keyword" size="large" placeholder="搜索网球、徒步、游戏……" @keyup.enter="searchNow" /><el-button type="primary" size="large" @click="searchNow">搜索</el-button></div>
    </div>
    <div class="section-heading"><div><p class="eyebrow accent">EXPLORE TOPICS</p><h2>兴趣分类</h2></div></div>
    <div class="category-tabs"><button v-for="category in categories" :key="category.id" :class="{ active: category.id === activeCategoryId }" @click="selectCategory(category.id)">{{ category.name }}</button></div>
    <div v-loading="loading" class="content-grid"><TopicCard v-for="topic in topics" :key="topic.id" :topic="topic" @follow-change="updateFollow(topic, $event)" /></div>
    <EmptyState v-if="!loading && !topics.length" title="这个分类还没有公开话题" />
    <div class="section-heading"><div><p class="eyebrow accent">LATEST POSTS</p><h2>最新帖子</h2></div><RouterLink to="/posts/create"><el-button type="primary" round>发布帖子</el-button></RouterLink></div>
    <div v-loading="postLoading" class="post-list"><PostCard v-for="post in posts.records" :key="post.id" :post="post" /></div>
    <EmptyState v-if="!postLoading && !posts.records.length" title="还没有公开帖子" description="登录后发布第一条内容吧。" />
    <PaginationBar :page="Number(posts.page)" :size="Number(posts.size)" :total="Number(posts.total)" @change="loadPosts" />
  </section>
</template>
