<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listCategories, listTopics } from '../../api/catalog.js'
import { createCircle } from '../../api/circle.js'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const categories = ref([])
const topics = ref([])
const form = reactive({ categoryId: null, topicId: Number(route.query.topicId) || null, name: '', description: '', city: '', district: '' })
const rules = { topicId: [{ required: true, message: '请选择话题' }], name: [{ required: true, message: '请输入圈子名称' }, { max: 64, message: '最多 64 字' }], city: [{ required: true, message: '请输入城市' }], description: [{ max: 500, message: '最多 500 字' }] }

async function load() {
  categories.value = (await listCategories()).data
  if (form.topicId) {
    for (const category of categories.value) {
      const values = (await listTopics(category.id)).data
      if (values.some((topic) => topic.id === form.topicId)) { form.categoryId = category.id; topics.value = values; break }
    }
  }
}

async function changeCategory(categoryId) { form.topicId = null; topics.value = (await listTopics(categoryId)).data }
async function submit() {
  if (!await formRef.value.validate().catch(() => false)) return
  submitting.value = true
  try {
    const response = await createCircle({ topicId: form.topicId, name: form.name, description: form.description || null, city: form.city, district: form.district || null })
    ElMessage.success('圈子已提交审核，审核通过后才会公开展示')
    router.replace({ name: 'my-interests', query: { tab: 'created', circleId: response.data.id } })
  } finally { submitting.value = false }
}
onMounted(load)
</script>

<template>
  <section class="form-page"><p class="eyebrow accent">CREATE A CIRCLE</p><h1>创建圈子</h1><p>为一个 Topic 创建更具体的本地社区。提交后需要内容管理员审核。</p>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="form-grid"><el-form-item label="分类"><el-select v-model="form.categoryId" placeholder="选择分类" @change="changeCategory"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item><el-form-item label="话题" prop="topicId"><el-select v-model="form.topicId" placeholder="选择话题"><el-option v-for="item in topics" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></div>
      <el-form-item label="圈子名称" prop="name"><el-input v-model="form.name" maxlength="64" show-word-limit /></el-form-item>
      <el-form-item label="介绍" prop="description"><el-input v-model="form.description" type="textarea" :rows="5" maxlength="500" show-word-limit /></el-form-item>
      <div class="form-grid"><el-form-item label="城市" prop="city"><el-input v-model="form.city" placeholder="例如：杭州" /></el-form-item><el-form-item label="区域"><el-input v-model="form.district" placeholder="例如：滨江区" /></el-form-item></div>
      <el-button type="primary" size="large" :loading="submitting" @click="submit">提交审核</el-button>
    </el-form>
  </section>
</template>
