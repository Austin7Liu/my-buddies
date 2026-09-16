<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { listBookmarkedPosts, listPostFeed } from '../../api/post.js'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import PostCard from '../../components/PostCard.vue'

const props = defineProps({ mode: { type: String, required: true } })
const posts = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(true)
const isFeed = computed(() => props.mode === 'feed')

async function load(page = 1) {
  loading.value = true
  try {
    posts.value = (await (isFeed.value ? listPostFeed(page) : listBookmarkedPosts(page))).data
  } finally {
    loading.value = false
  }
}

watch(() => props.mode, () => load(1))
onMounted(load)
</script>

<template>
  <section v-loading="loading">
    <p class="eyebrow accent">{{ isFeed ? 'PERSONAL FEED' : 'BOOKMARKS' }}</p>
    <div class="section-heading">
      <div><h1>{{ isFeed ? '我的动态' : '我的收藏' }}</h1><p>{{ isFeed ? '来自你关注 Topic 的最新公开帖子。' : '只有你能看到这里收藏的帖子。' }}</p></div>
    </div>
    <div class="post-list"><PostCard v-for="post in posts.records" :key="post.id" :post="post" /></div>
    <EmptyState v-if="!loading && !posts.records.length" :title="isFeed ? '信息流暂时为空' : '还没有收藏帖子'" :description="isFeed ? '先关注感兴趣的 Topic 吧。' : '在帖子上点击收藏即可保存。'" />
    <PaginationBar :page="Number(posts.page)" :size="Number(posts.size)" :total="Number(posts.total)" @change="load" />
  </section>
</template>
