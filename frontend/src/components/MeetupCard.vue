<script setup>
import { meetupModeLabel, meetupStatusLabel } from '../utils/meetup.js'

defineProps({ meetup: { type: Object, required: true } })
</script>

<template>
  <RouterLink class="meetup-card" :to="`/meetups/${meetup.id}`">
    <div class="meetup-card-top"><span class="card-kicker">{{ meetupModeLabel(meetup.meetupMode) }}</span><el-tag effect="plain">{{ meetupStatusLabel(meetup.status) }}</el-tag></div>
    <h3>{{ meetup.title }}</h3>
    <p>{{ meetup.description }}</p>
    <div class="meetup-facts"><span>{{ meetup.startTime }}</span><span>{{ meetup.acceptedCount }}/{{ meetup.capacity }} 人</span><span v-if="meetup.meetupMode === 'OFFLINE'">{{ meetup.city }} {{ meetup.district }}</span><span v-else>{{ meetup.onlinePlatform }}</span></div>
    <small>由 {{ meetup.creator?.nickname || `用户 ${meetup.creatorAccountId}` }} 创建</small>
  </RouterLink>
</template>
