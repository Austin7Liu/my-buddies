<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTopic } from '../../api/catalog.js'
import { listTopicCircles } from '../../api/circle.js'
import TopicCard from '../../components/TopicCard.vue'
import CircleCard from '../../components/CircleCard.vue'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'

const route = useRoute()
const router = useRouter()
const topic = ref(null)
const circles = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(true)
const topicId = String(route.params.topicId)

async function load(page = 1) {
  loading.value = true
  try {
    const [topicResponse, circleResponse] = await Promise.all([getTopic(topicId), listTopicCircles(topicId, page)])
    topic.value = topicResponse.data
    circles.value = circleResponse.data
  } finally { loading.value = false }
}

function create() { router.push({ name: 'circle-create', query: { topicId } }) }
onMounted(load)
</script>

<template>
  <section v-loading="loading">
    <el-breadcrumb separator="/"><el-breadcrumb-item :to="{ name: 'home' }">发现</el-breadcrumb-item><el-breadcrumb-item>{{ topic?.name || '话题' }}</el-breadcrumb-item></el-breadcrumb>
    <TopicCard v-if="topic" class="topic-feature" :topic="topic" @follow-change="topic.followedByMe = $event" />
    <div class="section-heading"><div><p class="eyebrow accent">LOCAL CIRCLES</p><h2>相关圈子</h2></div><el-button type="primary" round @click="create">创建圈子</el-button></div>
    <div class="content-grid"><CircleCard v-for="circle in circles.records" :key="circle.id" :circle="circle" /></div>
    <EmptyState v-if="!loading && !circles.records.length" title="还没有公开圈子" description="你可以创建第一个更具体的本地社区。" />
    <PaginationBar :page="Number(circles.page)" :size="Number(circles.size)" :total="Number(circles.total)" @change="load" />
  </section>
</template>
