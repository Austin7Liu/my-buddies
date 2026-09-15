<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template><main class="error-page"><span>403</span><h1>没有管理权限</h1><p>当前账户不具备内容管理员权限。</p><el-button type="primary" round @click="router.push('/')">返回首页</el-button></main></template>
