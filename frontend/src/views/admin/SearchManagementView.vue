<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { rebuildSearchIndex } from '../../api/admin.js'

const rebuilding = ref(false)
const result = ref(null)
async function rebuild() {
  await ElMessageBox.confirm('全量重建会创建新索引并在完成后切换 Alias，确认继续？', '重建搜索索引', { type: 'warning', confirmButtonText: '开始重建' })
  rebuilding.value = true
  try { result.value = (await rebuildSearchIndex()).data; ElMessage.success('搜索索引重建完成') } finally { rebuilding.value = false }
}
</script>

<template><section><p class="eyebrow accent">ELASTICSEARCH</p><h1 class="admin-title">搜索索引</h1><div class="index-panel"><div><h2>全量重建</h2><p>从 MySQL 读取公开的 Topic、Circle、Post 和 Meetup，创建新物理索引并原子切换 <code>my-buddies-search</code> Alias。</p><p>通常只需首次初始化、Mapping 升级或数据修复时执行，日常变更由 Outbox 自动同步。</p></div><el-button type="primary" size="large" :loading="rebuilding" :disabled="rebuilding" @click="rebuild">{{ rebuilding ? '正在重建…' : '开始全量重建' }}</el-button></div>
  <el-result v-if="result" icon="success" title="重建完成"><template #sub-title><p>索引：{{ result.indexName }}</p><p>文档数：{{ result.documentCount }} · 完成时间：{{ result.completedAt }}</p></template></el-result>
</section></template>
