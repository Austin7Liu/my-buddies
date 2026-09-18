<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listCategories, listTopics } from '../../api/catalog.js'
import { createCircle, getOwnedCircle, updateCircle } from '../../api/circle.js'
import { sameId } from '../../utils/id.js'

const route = useRoute()
const router = useRouter()
const editing = computed(() => Boolean(route.params.circleId))
const formRef = ref()
const submitting = ref(false)
const submitError = ref('')
const needsIdentity = ref(false)
const categories = ref([])
const topics = ref([])
const form = reactive({ categoryId: null, topicId: route.query.topicId ? String(route.query.topicId) : null, name: '', description: '', city: '', district: '' })
const rules = { topicId: [{ required: true, message: '请选择话题' }], name: [{ required: true, message: '请输入圈子名称' }, { max: 64, message: '最多 64 字' }], city: [{ required: true, message: '请输入城市' }], description: [{ max: 500, message: '最多 500 字' }] }

async function load() {
  if (editing.value) {
    const circle = (await getOwnedCircle(route.params.circleId)).data
    if (!['PENDING_REVIEW', 'REJECTED'].includes(circle.status)) {
      ElMessage.warning('当前圈子状态不允许编辑')
      return router.replace({ name: 'my-interests', query: { tab: 'created' } })
    }
    Object.assign(form, {
      topicId: String(circle.topicId),
      name: circle.name,
      description: circle.description ?? '',
      city: circle.city,
      district: circle.district ?? '',
    })
  }
  categories.value = (await listCategories()).data
  if (form.topicId) {
    for (const category of categories.value) {
      const values = (await listTopics(category.id)).data
      const selectedTopic = values.find((topic) => sameId(topic.id, form.topicId))
      if (selectedTopic) {
        form.categoryId = category.id
        form.topicId = selectedTopic.id
        topics.value = values
        break
      }
    }
  }
}

async function changeCategory(categoryId) { form.topicId = null; topics.value = (await listTopics(categoryId)).data }
async function submit() {
  if (submitting.value) return

  submitError.value = ''
  needsIdentity.value = false
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    submitError.value = '请检查并完善标红的必填项'
    ElMessage.warning(submitError.value)
    return
  }

  submitting.value = true
  try {
    const values = { name: form.name, description: form.description || null, city: form.city, district: form.district || null }
    const response = editing.value
      ? await updateCircle(route.params.circleId, values)
      : await createCircle({ topicId: form.topicId, ...values })
    ElMessage.success(editing.value ? '圈子已重新提交审核' : '圈子已提交审核，审核通过后才会公开展示')
    router.replace({ name: 'my-interests', query: { tab: 'created', circleId: response.data.id } })
  } catch (error) {
    submitError.value = error.response?.data?.error?.message ?? '提交失败，请检查后端服务和网络连接'
    needsIdentity.value = submitError.value.includes('实名认证')
  } finally {
    submitting.value = false
  }
}

function goToIdentityVerification() {
  router.push({ name: 'my-identity', query: { redirect: route.fullPath } })
}
onMounted(load)
</script>

<template>
  <section class="form-page"><p class="eyebrow accent">{{ editing ? 'EDIT CIRCLE' : 'CREATE A CIRCLE' }}</p><h1>{{ editing ? '编辑圈子' : '创建圈子' }}</h1><p>为一个 Topic 创建更具体的本地社区。提交后需要内容管理员审核。</p>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" scroll-to-error>
      <div class="form-grid"><el-form-item label="分类"><el-select v-model="form.categoryId" :disabled="editing" placeholder="选择分类" @change="changeCategory"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item><el-form-item label="话题" prop="topicId"><el-select v-model="form.topicId" :disabled="editing" placeholder="选择话题"><el-option v-for="item in topics" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></div>
      <el-form-item label="圈子名称" prop="name"><el-input v-model="form.name" maxlength="64" show-word-limit /></el-form-item>
      <el-form-item label="介绍" prop="description"><el-input v-model="form.description" type="textarea" :rows="5" maxlength="500" show-word-limit /></el-form-item>
      <div class="form-grid"><el-form-item label="城市" prop="city"><el-input v-model="form.city" placeholder="例如：杭州" /></el-form-item><el-form-item label="区域"><el-input v-model="form.district" placeholder="例如：滨江区" /></el-form-item></div>
      <div v-if="submitError" class="form-guidance">
        <span>{{ submitError }}</span>
        <el-button v-if="needsIdentity" type="primary" link @click="goToIdentityVerification">去完成实名认证</el-button>
      </div>
      <el-button type="primary" size="large" :loading="submitting" @click="submit">{{ editing ? '保存并重新提交审核' : '提交审核' }}</el-button>
    </el-form>
  </section>
</template>
