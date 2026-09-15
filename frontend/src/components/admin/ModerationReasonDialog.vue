<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ modelValue: Boolean, title: String, confirmText: { type: String, default: '确认提交' }, loading: Boolean })
const emit = defineEmits(['update:modelValue', 'confirm'])
const reason = ref('')
const error = ref('')

watch(() => props.modelValue, (open) => { if (open) { reason.value = ''; error.value = '' } })
function confirm() {
  const value = reason.value.trim()
  if (!value) { error.value = '原因不能为空'; return }
  if (value.length > 255) { error.value = '原因不能超过 255 个字符'; return }
  emit('confirm', value)
}
</script>

<template>
  <el-dialog :model-value="modelValue" :title="title" width="min(500px, 92vw)" @close="$emit('update:modelValue', false)">
    <el-input v-model="reason" type="textarea" :rows="4" maxlength="255" show-word-limit placeholder="请填写具体、可复核的处理原因" />
    <p v-if="error" class="form-error">{{ error }}</p>
    <template #footer><el-button @click="$emit('update:modelValue', false)">取消</el-button><el-button type="danger" :loading="loading" @click="confirm">{{ confirmText }}</el-button></template>
  </el-dialog>
</template>
