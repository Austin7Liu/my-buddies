<script setup>
defineProps({ page: Number, size: Number, total: Number })
defineEmits(['change'])
</script>

<template>
  <el-pagination
    v-if="total > size"
    background
    layout="prev, pager, next"
    :current-page="page"
    :page-size="size"
    :total="total"
    @current-change="$emit('change', $event)"
  />
</template>
