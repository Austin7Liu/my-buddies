<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listFollowedTopics } from '../../api/catalog.js'
import { listCreatedCircles, listJoinedCircles } from '../../api/circle.js'
import TopicCard from '../../components/TopicCard.vue'
import CircleCard from '../../components/CircleCard.vue'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'

const route = useRoute()
const router = useRouter()
const tabs = ['followed', 'created', 'joined']
const activeTab = ref(tabs.includes(String(route.query.tab)) ? String(route.query.tab) : 'followed')
const followed = ref({ records: [], page: 1, size: 20, total: 0 })
const created = ref({ records: [], page: 1, size: 20, total: 0 })
const joined = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(true)

async function loadTab(page = 1) {
  loading.value = true
  try {
    if (activeTab.value === 'created') created.value = (await listCreatedCircles(page)).data
    else if (activeTab.value === 'joined') joined.value = (await listJoinedCircles(page)).data
    else followed.value = (await listFollowedTopics(page)).data
  } finally {
    loading.value = false
  }
}

watch(activeTab, async (tab) => {
  await router.replace({ query: { tab } })
  await loadTab(1)
})
onMounted(loadTab)
</script>

<template>
  <section v-loading="loading">
    <p class="eyebrow accent">MY INTERESTS</p>
    <div class="section-heading"><h1>我的兴趣</h1><RouterLink to="/circles/create"><el-button type="primary" round>创建圈子</el-button></RouterLink></div>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="关注的话题" name="followed">
        <div class="content-grid"><TopicCard v-for="topic in followed.records" :key="topic.id" :topic="topic" @follow-change="topic.followedByMe = $event" /></div>
        <EmptyState v-if="!loading && !followed.records.length" title="还没有关注话题" />
        <PaginationBar :page="Number(followed.page)" :size="Number(followed.size)" :total="Number(followed.total)" @change="loadTab" />
      </el-tab-pane>
      <el-tab-pane label="我创建的圈子" name="created">
        <div class="content-grid">
          <div v-for="circle in created.records" :key="circle.id" class="managed-circle-item">
            <CircleCard :circle="circle" show-status manage />
            <RouterLink v-if="['PENDING_REVIEW', 'REJECTED'].includes(circle.status)" :to="`/circles/${circle.id}/edit`"><el-button type="primary" plain>编辑{{ circle.status === 'REJECTED' ? '并重新提交' : '' }}</el-button></RouterLink>
            <p v-if="circle.rejectionReason" class="moderation-reason">驳回原因：{{ circle.rejectionReason }}</p>
          </div>
        </div>
        <EmptyState v-if="!loading && !created.records.length" title="还没有创建圈子" />
        <PaginationBar :page="Number(created.page)" :size="Number(created.size)" :total="Number(created.total)" @change="loadTab" />
      </el-tab-pane>
      <el-tab-pane label="我加入的圈子" name="joined">
        <div class="content-grid"><CircleCard v-for="circle in joined.records" :key="circle.id" :circle="circle" /></div>
        <EmptyState v-if="!loading && !joined.records.length" title="还没有加入圈子" />
        <PaginationBar :page="Number(joined.page)" :size="Number(joined.size)" :total="Number(joined.total)" @change="loadTab" />
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
