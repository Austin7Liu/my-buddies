<script setup>
defineProps({ title: { type: String, default: '这里暂时没有内容' }, description: { type: String, default: '' } })
</script>

<template>
  <div class="empty-state">
    <span>○</span>
    <strong>{{ title }}</strong>
    <p v-if="description">{{ description }}</p>
  </div>
</template>
