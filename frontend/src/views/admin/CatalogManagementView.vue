<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdminCategory,
  createAdminTopic,
  listAdminCategories,
  listAdminTopics,
  setAdminCategoryEnabled,
  setAdminTopicEnabled,
  updateAdminCategory,
  updateAdminTopic,
} from '../../api/adminCatalog.js'
import CatalogEditorDialog from '../../components/admin/CatalogEditorDialog.vue'
import EmptyState from '../../components/EmptyState.vue'

const categories = ref([])
const topics = ref([])
const selectedCategoryId = ref('')
const loadingCategories = ref(false)
const loadingTopics = ref(false)
const saving = ref(false)
const editor = ref(null)
const togglingId = ref('')
const selectedCategory = computed(() => categories.value.find((item) => String(item.id) === selectedCategoryId.value) ?? null)

async function loadCategories(preferredId = selectedCategoryId.value) {
  loadingCategories.value = true
  try {
    categories.value = (await listAdminCategories()).data
    const exists = categories.value.some((item) => String(item.id) === String(preferredId))
    selectedCategoryId.value = exists ? String(preferredId) : String(categories.value[0]?.id ?? '')
  } finally {
    loadingCategories.value = false
  }
  await loadTopics()
}

async function loadTopics() {
  if (!selectedCategoryId.value) { topics.value = []; return }
  loadingTopics.value = true
  try { topics.value = (await listAdminTopics(selectedCategoryId.value)).data } finally { loadingTopics.value = false }
}

async function saveEditor(payload) {
  if (saving.value) return
  saving.value = true
  try {
    let result
    if (editor.value.type === 'category') {
      result = editor.value.value
        ? await updateAdminCategory(editor.value.value.id, payload)
        : await createAdminCategory(payload)
      editor.value = null
      ElMessage.success('分类已保存')
      await loadCategories(result.data.id)
    } else {
      result = editor.value.value
        ? await updateAdminTopic(editor.value.value.id, payload)
        : await createAdminTopic(selectedCategoryId.value, payload)
      editor.value = null
      ElMessage.success('话题已保存')
      await loadTopics()
    }
  } finally {
    saving.value = false
  }
}

async function toggleCategory(row) {
  if (togglingId.value) return
  const enabled = !row.enabled
  const message = enabled
    ? '启用分类后，其中原本启用的话题会重新对普通用户可见。确定继续吗？'
    : '停用分类后，该分类及其所有话题将从普通用户入口和搜索中隐藏，但历史数据不会删除。确定继续吗？'
  try { await ElMessageBox.confirm(message, `${enabled ? '启用' : '停用'}分类`, { type: 'warning' }) } catch { return }
  togglingId.value = `category:${row.id}`
  try {
    await setAdminCategoryEnabled(row.id, enabled)
    ElMessage.success(`分类已${enabled ? '启用' : '停用'}`)
    await loadCategories(row.id)
  } finally {
    togglingId.value = ''
  }
}

async function toggleTopic(row) {
  if (togglingId.value) return
  const enabled = !row.enabled
  if (enabled && !selectedCategory.value?.enabled) {
    ElMessage.warning('请先启用所属分类')
    return
  }
  try { await ElMessageBox.confirm(`${enabled ? '启用' : '停用'}话题会更新其公开与搜索可见性，确定继续吗？`, `${enabled ? '启用' : '停用'}话题`, { type: 'warning' }) } catch { return }
  togglingId.value = `topic:${row.id}`
  try {
    await setAdminTopicEnabled(row.id, enabled)
    ElMessage.success(`话题已${enabled ? '启用' : '停用'}`)
    await loadTopics()
  } finally {
    togglingId.value = ''
  }
}

onMounted(loadCategories)
</script>

<template>
  <section>
    <div class="admin-heading"><div><p class="eyebrow accent">CATALOG MANAGEMENT</p><h1 class="admin-title">分类与话题</h1><p>维护平台一级分类和 Topic；停用只影响公开可见性，不删除历史业务数据。</p></div><el-button type="primary" @click="editor = { type: 'category', value: null }">新建分类</el-button></div>
    <div class="catalog-admin-grid">
      <section class="catalog-admin-panel"><div class="catalog-panel-heading"><h2>Category</h2><span>{{ categories.length }} 个</span></div><div v-loading="loadingCategories" class="catalog-admin-list"><button v-for="item in categories" :key="item.id" type="button" :class="{ selected: String(item.id) === selectedCategoryId, disabled: !item.enabled }" @click="selectedCategoryId = String(item.id); loadTopics()"><span><strong>{{ item.name }}</strong><small>{{ item.code }} · 排序 {{ item.sortOrder }}</small></span><el-tag :type="item.enabled ? 'success' : 'info'" effect="plain">{{ item.enabled ? '启用' : '停用' }}</el-tag></button><EmptyState v-if="!loadingCategories && !categories.length" title="暂无分类" description="请先创建第一个分类。" /></div></section>
      <section class="catalog-admin-panel"><div class="catalog-panel-heading"><div><h2>{{ selectedCategory?.name || 'Topic' }}</h2><small v-if="selectedCategory">{{ selectedCategory.description || '暂无描述' }}</small></div><div class="catalog-panel-actions"><el-button v-if="selectedCategory" @click="editor = { type: 'category', value: selectedCategory }">编辑分类</el-button><el-button v-if="selectedCategory" :loading="togglingId === `category:${selectedCategory.id}`" @click="toggleCategory(selectedCategory)">{{ selectedCategory.enabled ? '停用分类' : '启用分类' }}</el-button><el-button v-if="selectedCategory" type="primary" @click="editor = { type: 'topic', value: null }">新建话题</el-button></div></div>
        <el-table v-loading="loadingTopics" :data="topics"><el-table-column prop="name" label="话题" min-width="130" /><el-table-column prop="code" label="编码" min-width="130" /><el-table-column prop="sortOrder" label="排序" width="80" /><el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="150"><template #default="{ row }"><el-button link @click="editor = { type: 'topic', value: row }">编辑</el-button><el-button link :loading="togglingId === `topic:${row.id}`" @click="toggleTopic(row)">{{ row.enabled ? '停用' : '启用' }}</el-button></template></el-table-column></el-table>
        <EmptyState v-if="selectedCategory && !loadingTopics && !topics.length" title="暂无话题" description="可以在当前分类下创建 Topic。" />
      </section>
    </div>
    <CatalogEditorDialog :model-value="Boolean(editor)" :type="editor?.type" :value="editor?.value" :loading="saving" @update:model-value="!$event && (editor = null)" @confirm="saveEditor" />
  </section>
</template>
