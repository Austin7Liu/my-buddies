<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { search } from '../../api/search.js'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import SafeHighlight from '../../components/SafeHighlight.vue'

const route = useRoute()
const router = useRouter()
const form = reactive({ keyword: String(route.query.keyword || ''), type: route.query.type || '', city: route.query.city || '', meetupMode: route.query.meetupMode || '' })
const result = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(false)
const searched = ref(false)

const typeLabels = { TOPIC: '话题', CIRCLE: '圈子', POST: '帖子', MEETUP: '活动' }
function detailRoute(document) {
  if (document.documentType === 'TOPIC') return `/topics/${document.businessId}`
  if (document.documentType === 'CIRCLE') return `/circles/${document.businessId}`
  return null
}

async function submit(page = 1) {
  if (form.keyword.trim().length < 2) return
  loading.value = true
  searched.value = true
  const query = Object.fromEntries(Object.entries(form).filter(([, value]) => value))
  router.replace({ query })
  try {
    const response = await search({ ...query, page, size: 20 })
    result.value = response.data
  } finally { loading.value = false }
}

onMounted(() => { if (form.keyword.length >= 2) submit() })
</script>

<template>
  <section>
    <div class="search-heading"><p class="eyebrow accent">SEARCH</p><h1>搜索共同兴趣</h1></div>
    <div class="search-panel">
      <el-input v-model="form.keyword" size="large" placeholder="至少输入两个字" @keyup.enter="submit(1)" />
      <el-select v-model="form.type" clearable placeholder="全部类型"><el-option v-for="(label, value) in typeLabels" :key="value" :label="label" :value="value" /></el-select>
      <el-input v-model="form.city" placeholder="城市" clearable />
      <el-select v-model="form.meetupMode" clearable placeholder="活动模式"><el-option label="线上" value="ONLINE" /><el-option label="线下" value="OFFLINE" /></el-select>
      <el-button type="primary" size="large" @click="submit(1)">搜索</el-button>
    </div>
    <div v-loading="loading" class="search-results">
      <component :is="detailRoute(hit.document) ? 'router-link' : 'article'" v-for="hit in result.records" :key="hit.document.id" :to="detailRoute(hit.document)" class="search-result">
        <div class="card-meta"><el-tag size="small">{{ typeLabels[hit.document.documentType] }}</el-tag><span v-if="hit.document.city">{{ hit.document.city }} {{ hit.document.district }}</span><span v-if="hit.document.topicName">#{{ hit.document.topicName }}</span></div>
        <h2><SafeHighlight :text="hit.titleHighlight || hit.document.title || hit.document.circleName || hit.document.topicName || '内容'" /></h2>
        <p><SafeHighlight :text="hit.contentHighlight || hit.document.content || ''" /></p>
        <small v-if="!detailRoute(hit.document)">详情页面将在下一阶段接入</small>
      </component>
    </div>
    <EmptyState v-if="searched && !loading && !result.records.length" title="没有找到匹配内容" description="试试更短的关键词或清空筛选条件。" />
    <PaginationBar :page="Number(result.page)" :size="Number(result.size)" :total="Number(result.total)" @change="submit" />
  </section>
</template>
