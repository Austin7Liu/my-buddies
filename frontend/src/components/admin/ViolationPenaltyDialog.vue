<script setup>
import { reactive, ref, watch } from 'vue'
import { validateViolationPenalty } from '../../utils/violation.js'

const props = defineProps({ modelValue: Boolean, report: Object, loading: Boolean })
const emit = defineEmits(['update:modelValue', 'confirm'])
const error = ref('')
const form = reactive({ severity: 'MINOR', penaltyType: 'WARNING_ONLY', penaltyExpiresAt: '', note: '' })

watch(() => props.modelValue, (open) => {
  if (!open) return
  Object.assign(form, { severity: 'MINOR', penaltyType: 'WARNING_ONLY', penaltyExpiresAt: '', note: '' })
  error.value = ''
})

watch(() => form.penaltyType, (value) => {
  if (value === 'WARNING_ONLY') form.penaltyExpiresAt = ''
})

function confirm() {
  error.value = validateViolationPenalty(form)
  if (error.value) return
  emit('confirm', {
    severity: form.severity,
    penaltyType: form.penaltyType,
    penaltyExpiresAt: form.penaltyExpiresAt || null,
    note: form.note.trim(),
  })
}
</script>

<template>
  <el-dialog :model-value="modelValue" title="确认内容违规" width="min(620px, 94vw)" @close="$emit('update:modelValue', false)">
    <el-alert v-if="report?.appealDeadlineAt" type="warning" :closable="false" :title="`申诉截止时间：${report.appealDeadlineAt}。申诉期未结束时后端会拒绝处罚。`" />
    <el-form label-position="top" class="penalty-form">
      <el-form-item label="违规等级"><el-select v-model="form.severity"><el-option label="轻微" value="MINOR" /><el-option label="中等" value="MODERATE" /><el-option label="严重" value="SEVERE" /></el-select></el-form-item>
      <el-form-item label="处罚类型"><el-select v-model="form.penaltyType"><el-option label="仅警告" value="WARNING_ONLY" /><el-option label="禁止发布帖子" value="POST_DISABLED" /><el-option label="禁止发表评论" value="COMMENT_DISABLED" /><el-option label="禁止发布帖子和评论" value="CONTENT_CREATE_DISABLED" /></el-select></el-form-item>
      <el-form-item v-if="form.penaltyType !== 'WARNING_ONLY'" label="处罚到期时间"><el-date-picker v-model="form.penaltyExpiresAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="限制期限必须在 1 到 30 天之间" style="width: 100%" /></el-form-item>
      <el-form-item label="处罚说明"><el-input v-model="form.note" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item>
    </el-form>
    <p v-if="error" class="form-error">{{ error }}</p>
    <template #footer><el-button @click="$emit('update:modelValue', false)">取消</el-button><el-button type="danger" :loading="loading" :disabled="loading" @click="confirm">确认处罚</el-button></template>
  </el-dialog>
</template>
