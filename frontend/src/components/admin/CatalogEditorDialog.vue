<script setup>
import { reactive, ref, watch } from 'vue'
import { catalogPayload, validateCatalogEditor } from '../../utils/catalogAdmin.js'

const props = defineProps({ modelValue: Boolean, type: String, value: Object, loading: Boolean })
const emit = defineEmits(['update:modelValue', 'confirm'])
const form = reactive({ code: '', name: '', description: '', sortOrder: 0 })
const error = ref('')

watch(() => props.modelValue, (open) => {
  if (!open) return
  Object.assign(form, {
    code: props.value?.code ?? '',
    name: props.value?.name ?? '',
    description: props.value?.description ?? '',
    sortOrder: props.value?.sortOrder ?? 0,
  })
  error.value = ''
})

function submit() {
  error.value = validateCatalogEditor(props.type, form, Boolean(props.value))
  if (error.value) return
  emit('confirm', catalogPayload(form, Boolean(props.value)))
}
</script>

<template>
  <el-dialog :model-value="modelValue" :title="`${value ? '编辑' : '新建'}${type === 'category' ? '分类' : '话题'}`" width="min(600px, 92vw)" @close="$emit('update:modelValue', false)">
    <el-form label-position="top">
      <el-form-item label="编码"><el-input v-model="form.code" :disabled="Boolean(value)" :maxlength="type === 'category' ? 32 : 64" placeholder="小写字母开头，仅限小写字母、数字和连字符" /></el-form-item>
      <el-form-item label="名称"><el-input v-model="form.name" maxlength="64" show-word-limit /></el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="4" maxlength="255" show-word-limit /></el-form-item>
      <el-form-item label="排序值"><el-input-number v-model="form.sortOrder" :min="0" :step="1" /></el-form-item>
    </el-form>
    <p v-if="error" class="form-error">{{ error }}</p>
    <template #footer><el-button @click="$emit('update:modelValue', false)">取消</el-button><el-button type="primary" :loading="loading" :disabled="loading" @click="submit">保存</el-button></template>
  </el-dialog>
</template>
