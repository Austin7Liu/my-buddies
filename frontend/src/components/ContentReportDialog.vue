<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { reportPost, reportPostComment } from '../api/report.js'
import { reportReasonOptions, validateReport } from '../utils/report.js'

const props = defineProps({
  modelValue: Boolean,
  targetType: { type: String, required: true },
  targetId: { type: [String, Number], required: true },
})
const emit = defineEmits(['update:modelValue', 'submitted'])
const submitting = ref(false)
const form = reactive({ reasonType: '', description: '' })

watch(() => props.modelValue, (open) => {
  if (open) Object.assign(form, { reasonType: '', description: '' })
})

async function submit() {
  const error = validateReport(form.reasonType, form.description)
  if (error) return ElMessage.warning(error)
  submitting.value = true
  try {
    const payload = {
      reasonType: form.reasonType,
      description: form.description.trim() || null,
    }
    const response = await (props.targetType === 'POST'
      ? reportPost(props.targetId, payload)
      : reportPostComment(props.targetId, payload))
    emit('update:modelValue', false)
    emit('submitted', response.data)
    ElMessage.success('举报已提交，平台会尽快处理')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog :model-value="modelValue" title="举报内容" width="min(520px, 92vw)" @update:model-value="emit('update:modelValue', $event)">
    <el-form label-position="top">
      <el-form-item label="举报原因" required><el-select v-model="form.reasonType" placeholder="请选择原因" style="width: 100%"><el-option v-for="option in reportReasonOptions" :key="option.value" :label="option.label" :value="option.value" /></el-select></el-form-item>
      <el-form-item :label="form.reasonType === 'OTHER' ? '补充说明（必填）' : '补充说明'"><el-input v-model="form.description" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item>
    </el-form>
    <template #footer><el-button @click="emit('update:modelValue', false)">取消</el-button><el-button type="danger" :loading="submitting" @click="submit">提交举报</el-button></template>
  </el-dialog>
</template>
