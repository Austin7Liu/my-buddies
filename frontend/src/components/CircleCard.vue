<script setup>
defineProps({ circle: { type: Object, required: true }, showStatus: Boolean })
</script>

<template>
  <RouterLink :to="`/circles/${circle.id}`" class="content-card circle-card">
    <div class="card-meta">
      <span>{{ circle.city }}{{ circle.district ? ` · ${circle.district}` : '' }}</span>
      <el-tag v-if="showStatus" size="small" effect="plain">{{ circle.status }}</el-tag>
    </div>
    <h3>{{ circle.name }}</h3>
    <p>{{ circle.description || '这个圈子还没有填写介绍。' }}</p>
    <small>创建者：{{ circle.creator?.nickname || `用户 ${circle.creatorAccountId}` }}</small>
  </RouterLink>
</template>
