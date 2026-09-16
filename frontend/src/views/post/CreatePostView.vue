<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPost } from '../../api/post.js'
import { postAssociationPayload } from '../../utils/post.js'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const errorMessage = ref('')
const needsIdentity = ref(false)
const form = reactive({ content: '' })
const context = postAssociationPayload(route.query.topicId, route.query.circleId)
const contextLabel = computed(() => {
  if (context.circleId) return `发布到 Circle #${context.circleId}`
  if (context.topicId) return `发布到 #Topic ${context.topicId}`
  return '发布到首页，不关联 Topic 或 Circle'
})
const rules = {
  content: [
    { required: true, message: '请输入帖子内容', trigger: 'blur' },
    { max: 2000, message: '帖子内容不能超过 2000 个字符', trigger: 'blur' },
  ],
}

async function submit() {
  if (submitting.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  errorMessage.value = ''
  needsIdentity.value = false
  try {
    await createPost({ content: form.content, ...context })
    ElMessage.success('帖子已提交审核')
    router.replace({ name: 'my-posts', query: { status: 'PENDING_REVIEW' } })
  } catch (error) {
    errorMessage.value = error.response?.data?.error?.message ?? '帖子提交失败'
    needsIdentity.value = errorMessage.value.includes('实名认证')
  } finally {
    submitting.value = false
  }
}

function verifyIdentity() {
  router.push({ name: 'my-identity', query: { redirect: route.fullPath } })
}
</script>

<template>
  <section class="form-page">
    <p class="eyebrow accent">CREATE A POST</p>
    <h1>发布帖子</h1>
    <p>{{ contextLabel }}。提交后需要内容管理员审核。</p>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="正文" prop="content">
        <el-input v-model="form.content" type="textarea" :rows="10" maxlength="2000" show-word-limit placeholder="分享你的想法、经验或计划……" />
      </el-form-item>
      <div v-if="errorMessage" class="form-guidance">
        <span>{{ errorMessage }}</span>
        <el-button v-if="needsIdentity" type="primary" link @click="verifyIdentity">去完成实名认证</el-button>
      </div>
      <el-button type="primary" size="large" :loading="submitting" @click="submit">提交审核</el-button>
    </el-form>
  </section>
</template>
