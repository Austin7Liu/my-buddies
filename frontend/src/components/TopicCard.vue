<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { followTopic, unfollowTopic } from '../api/catalog.js'
import { isAuthenticated } from '../stores/auth.js'
import { useRoute, useRouter } from 'vue-router'

const props = defineProps({ topic: { type: Object, required: true } })
const emit = defineEmits(['follow-change'])
const router = useRouter()
const route = useRoute()
const pending = ref(false)

async function toggleFollow() {
  if (!isAuthenticated()) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  pending.value = true
  try {
    const response = props.topic.followedByMe
      ? await unfollowTopic(props.topic.id)
      : await followTopic(props.topic.id)
    emit('follow-change', response.data.followedByMe)
    ElMessage.success(response.data.followedByMe ? '已关注话题' : '已取消关注')
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <article class="content-card topic-card">
    <RouterLink :to="`/topics/${topic.id}`" class="card-main">
      <span class="card-kicker">#{{ topic.code }}</span>
      <h3>{{ topic.name }}</h3>
      <p>{{ topic.description || '等待更多伙伴共同丰富这个话题。' }}</p>
    </RouterLink>
    <el-button :loading="pending" :type="topic.followedByMe ? 'default' : 'primary'" round @click="toggleFollow">
      {{ topic.followedByMe ? '已关注' : '关注' }}
    </el-button>
  </article>
</template>
