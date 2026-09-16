<script setup>
import { onMounted, ref } from 'vue'
import { listMeetups } from '../../api/meetup.js'
import EmptyState from '../../components/EmptyState.vue'
import MeetupCard from '../../components/MeetupCard.vue'
import PaginationBar from '../../components/PaginationBar.vue'

const meetups = ref({ records: [], page: 1, size: 20, total: 0 })
const loading = ref(true)
async function load(page = 1) {
  loading.value = true
  try { meetups.value = (await listMeetups(page)).data } finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <section v-loading="loading">
    <p class="eyebrow accent">MEETUPS</p>
    <div class="section-heading"><div><h1>一起行动</h1><p>发现线上组队和线下活动，把共同兴趣变成一次真实约定。</p></div><RouterLink to="/meetups/create"><el-button type="primary" size="large" round>创建活动</el-button></RouterLink></div>
    <div class="meetup-grid"><MeetupCard v-for="meetup in meetups.records" :key="meetup.id" :meetup="meetup" /></div>
    <EmptyState v-if="!loading && !meetups.records.length" title="还没有公开活动" description="登录后创建第一个活动吧。" />
    <PaginationBar :page="Number(meetups.page)" :size="Number(meetups.size)" :total="Number(meetups.total)" @change="load" />
  </section>
</template>
