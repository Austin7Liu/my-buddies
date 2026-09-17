<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createContentAppeal } from '../../api/appeal.js'
import { validateAppealReason } from '../../utils/appeal.js'

const route = useRoute()
const router = useRouter()
const reportId = computed(() => String(route.query.reportId ?? '').trim())
const reason = ref('')
const error = ref('')
const submitting = ref(false)

async function submit() {
  if (submitting.value) return
  if (!/^\d+$/.test(reportId.value) || reportId.value === '0') {
    error.value = '缺少有效的举报编号，请从内容处置通知进入申诉页面'
    return
  }
  error.value = validateAppealReason(reason.value)
  if (error.value) return
  submitting.value = true
  try {
    const appeal = (await createContentAppeal(reportId.value, reason.value.trim())).data
    ElMessage.success('申诉已提交，请等待平台复核')
    await router.replace({ name: 'my-appeals', query: { appealId: String(appeal.id) } })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="appeal-form-page">
    <div class="section-heading"><div><p class="eyebrow accent">CONTENT APPEAL</p><h1>内容申诉</h1><p>仅被处置内容的作者可以申诉，最终结果以后端业务校验为准。</p></div></div>
    <div class="appeal-form-card">
      <p><strong>关联举报：</strong>#{{ reportId || '未提供' }}</p>
      <el-input v-model="reason" type="textarea" :rows="8" maxlength="1000" show-word-limit placeholder="请说明内容不存在违规或应当恢复的具体理由" @input="error = ''" />
      <p v-if="error" class="form-error">{{ error }}</p>
      <div class="form-actions"><el-button @click="router.push({ name: 'my-appeals' })">取消</el-button><el-button type="primary" :loading="submitting" :disabled="submitting" @click="submit">提交申诉</el-button></div>
    </div>
  </section>
</template>
