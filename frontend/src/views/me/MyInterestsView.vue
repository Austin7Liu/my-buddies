<script setup>
import { onMounted, ref } from 'vue'
import { listFollowedTopics } from '../../api/catalog.js'
import { listCreatedCircles, listJoinedCircles } from '../../api/circle.js'
import TopicCard from '../../components/TopicCard.vue'
import CircleCard from '../../components/CircleCard.vue'
import EmptyState from '../../components/EmptyState.vue'

const activeTab = ref('followed')
const followed = ref([])
const created = ref([])
const joined = ref([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const [followedResponse, createdResponse, joinedResponse] = await Promise.all([listFollowedTopics(), listCreatedCircles(), listJoinedCircles()])
    followed.value = followedResponse.data.records
    created.value = createdResponse.data.records
    joined.value = joinedResponse.data.records
  } finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <section v-loading="loading"><p class="eyebrow accent">MY INTERESTS</p><div class="section-heading"><h1>我的兴趣</h1><RouterLink to="/circles/create"><el-button type="primary" round>创建圈子</el-button></RouterLink></div>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="关注的话题" name="followed"><div class="content-grid"><TopicCard v-for="topic in followed" :key="topic.id" :topic="topic" @follow-change="topic.followedByMe = $event" /></div><EmptyState v-if="!followed.length" title="还没有关注话题" /></el-tab-pane>
      <el-tab-pane label="我创建的圈子" name="created"><div class="content-grid"><CircleCard v-for="circle in created" :key="circle.id" :circle="circle" show-status /></div><EmptyState v-if="!created.length" title="还没有创建圈子" /></el-tab-pane>
      <el-tab-pane label="我加入的圈子" name="joined"><div class="content-grid"><CircleCard v-for="circle in joined" :key="circle.id" :circle="circle" /></div><EmptyState v-if="!joined.length" title="还没有加入圈子" /></el-tab-pane>
    </el-tabs>
  </section>
</template>
