<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listNotifications } from '../../api/notification.js'
import EmptyState from '../../components/EmptyState.vue'
import PaginationBar from '../../components/PaginationBar.vue'
import {
  markAllNotificationsRead,
  markNotificationRead,
  notificationState,
} from '../../stores/notification.js'
import { notificationTarget, notificationTypeLabel } from '../../utils/notification.js'

const router = useRouter()
const unreadOnly = ref(false)
const loading = ref(true)
const readingAll = ref(false)
const readingIds = ref(new Set())
const pageData = ref({ records: [], page: 1, size: 20, total: 0 })
const hasUnread = computed(() => notificationState.unreadCount > 0
  || pageData.value.records.some((item) => !item.read))

async function load(page = 1) {
  loading.value = true
  try {
    pageData.value = (await listNotifications(unreadOnly.value, page)).data
  } finally {
    loading.value = false
  }
}

async function openNotification(notification) {
  if (readingIds.value.has(notification.id)) return
  const target = notificationTarget(notification)
  if (!notification.read) {
    readingIds.value.add(notification.id)
    try {
      const updated = await markNotificationRead(notification)
      Object.assign(notification, updated)
      if (unreadOnly.value) pageData.value.records = pageData.value.records.filter((item) => item.id !== notification.id)
    } finally {
      readingIds.value.delete(notification.id)
    }
  }
  if (target) await router.push(target)
}

async function readAll() {
  if (!hasUnread.value) return
  readingAll.value = true
  try {
    const affected = await markAllNotificationsRead()
    ElMessage.success(affected > 0 ? `已将 ${affected} 条通知标记为已读` : '没有新的未读通知')
    await load(1)
  } finally {
    readingAll.value = false
  }
}

watch(unreadOnly, () => load(1))
onMounted(() => load())
</script>

<template>
  <section class="notification-page">
    <div class="section-heading notification-heading">
      <div><p class="eyebrow accent">NOTIFICATIONS</p><h1>通知中心</h1><p>活动、互动和内容治理结果都会汇集在这里。</p></div>
      <el-button :loading="readingAll" :disabled="!hasUnread" @click="readAll">全部已读</el-button>
    </div>

    <div class="notification-toolbar">
      <el-radio-group v-model="unreadOnly">
        <el-radio-button :value="false">全部通知</el-radio-button>
        <el-radio-button :value="true">仅看未读</el-radio-button>
      </el-radio-group>
      <span>共 {{ pageData.total }} 条</span>
    </div>

    <div v-loading="loading" class="notification-list">
      <article
        v-for="notification in pageData.records"
        :key="notification.id"
        class="notification-card"
        :class="{ unread: !notification.read }"
      >
        <span class="notification-dot" aria-hidden="true"></span>
        <div class="notification-content">
          <div class="notification-meta"><span>{{ notificationTypeLabel(notification.notificationType) }}</span><time>{{ notification.createdAt }}</time></div>
          <h2>{{ notification.title }}</h2>
          <p>{{ notification.content }}</p>
        </div>
        <el-button
          v-if="!notification.read || notificationTarget(notification)"
          text
          :loading="readingIds.has(notification.id)"
          @click="openNotification(notification)"
        >
          {{ notificationTarget(notification) ? '查看相关内容' : '标为已读' }}
        </el-button>
      </article>
      <EmptyState v-if="!loading && !pageData.records.length" title="暂无通知" :description="unreadOnly ? '目前没有未读通知。' : '有新的业务动态时会显示在这里。'" />
    </div>
    <PaginationBar :page="Number(pageData.page)" :size="Number(pageData.size)" :total="Number(pageData.total)" @change="load" />
  </section>
</template>
