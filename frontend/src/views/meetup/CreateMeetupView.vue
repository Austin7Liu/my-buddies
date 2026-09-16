<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listCategories, listTopics } from '../../api/catalog.js'
import { getCircle } from '../../api/circle.js'
import { createMeetup } from '../../api/meetup.js'
import { sameId } from '../../utils/id.js'
import { meetupAssociationPayload, meetupTargetLocation, validateMeetupTimes } from '../../utils/meetup.js'
import { geolocationErrorMessage, getCurrentLocation } from '../../utils/meetupGeolocation.js'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const categories = ref([])
const topics = ref([])
const circle = ref(null)
const submitting = ref(false)
const locating = ref(false)
const locationAccuracy = ref(null)
const submitError = ref('')
const form = reactive({
  categoryId: null,
  topicId: route.query.topicId ? String(route.query.topicId) : null,
  circleId: route.query.circleId ? String(route.query.circleId) : null,
  meetupMode: 'OFFLINE',
  title: '', description: '', startTime: '', endTime: '', applicationDeadline: '',
  city: '', district: '', locationName: '', address: '',
  locationLatitude: null, locationLongitude: null, checkInRadiusMeters: null,
  onlinePlatform: '', serverRegion: '', accessInstructions: '',
  capacity: 4, minimumAge: 18, maximumAge: 60, genderRequirement: 'ANY', skillRequirement: '',
})
const rules = {
  topicId: [{ required: true, message: '请选择话题' }],
  meetupMode: [{ required: true, message: '请选择活动模式' }],
  title: [{ required: true, message: '请输入活动标题' }, { max: 100, message: '最多 100 字' }],
  description: [{ required: true, message: '请输入活动说明' }, { max: 2000, message: '最多 2000 字' }],
  startTime: [{ required: true, message: '请选择开始时间' }],
  endTime: [{ required: true, message: '请选择结束时间' }],
  applicationDeadline: [{ required: true, message: '请选择报名截止时间' }],
}

async function resolveTopic(topicId) {
  for (const category of categories.value) {
    const values = (await listTopics(category.id)).data
    const selected = values.find((topic) => sameId(topic.id, topicId))
    if (selected) {
      form.categoryId = category.id
      form.topicId = selected.id
      topics.value = values
      return
    }
  }
}

async function load() {
  categories.value = (await listCategories()).data
  if (form.circleId) {
    circle.value = (await getCircle(form.circleId)).data
    await resolveTopic(circle.value.topicId)
  } else if (form.topicId) {
    await resolveTopic(form.topicId)
  }
}

async function changeCategory(categoryId) {
  form.topicId = null
  topics.value = (await listTopics(categoryId)).data
}

function nullable(value) {
  return value === '' || value === undefined ? null : value
}

function payload() {
  const association = meetupAssociationPayload(form.topicId, form.circleId)
  const offline = form.meetupMode === 'OFFLINE'
  return {
    ...association,
    meetupMode: form.meetupMode,
    title: form.title.trim(), description: form.description.trim(),
    startTime: form.startTime, endTime: form.endTime, applicationDeadline: form.applicationDeadline,
    city: offline ? nullable(form.city.trim()) : null,
    district: offline ? nullable(form.district.trim()) : null,
    locationName: offline ? nullable(form.locationName.trim()) : null,
    address: offline ? nullable(form.address.trim()) : null,
    locationLatitude: offline ? nullable(form.locationLatitude) : null,
    locationLongitude: offline ? nullable(form.locationLongitude) : null,
    checkInRadiusMeters: offline ? nullable(form.checkInRadiusMeters) : null,
    onlinePlatform: offline ? null : nullable(form.onlinePlatform.trim()),
    serverRegion: offline ? null : nullable(form.serverRegion.trim()),
    accessInstructions: offline ? null : nullable(form.accessInstructions.trim()),
    capacity: form.capacity, minimumAge: form.minimumAge, maximumAge: form.maximumAge,
    genderRequirement: form.genderRequirement,
    skillRequirement: nullable(form.skillRequirement.trim()),
  }
}

async function useCurrentLocation() {
  if (locating.value) return
  locating.value = true
  try {
    const location = await getCurrentLocation(globalThis.navigator?.geolocation)
    Object.assign(form, meetupTargetLocation(location, form.checkInRadiusMeters ?? 300))
    locationAccuracy.value = location.accuracyMeters
    ElMessage.success('已使用浏览器当前位置作为活动签到位置')
  } catch (error) {
    ElMessage.error(geolocationErrorMessage(error))
  } finally {
    locating.value = false
  }
}

function clearCurrentLocation() {
  form.locationLatitude = null
  form.locationLongitude = null
  form.checkInRadiusMeters = null
  locationAccuracy.value = null
}

async function submit() {
  submitError.value = ''
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return ElMessage.warning('请检查必填项')
  const timeError = validateMeetupTimes(form.startTime, form.endTime, form.applicationDeadline)
  if (timeError) return ElMessage.warning(timeError)
  if (form.minimumAge > form.maximumAge) return ElMessage.warning('最低年龄不能大于最高年龄')
  if (form.meetupMode === 'OFFLINE' && ![form.city, form.district, form.locationName, form.address].every((value) => value.trim())) return ElMessage.warning('请填写完整的线下地点信息')
  if (form.meetupMode === 'ONLINE' && !form.onlinePlatform.trim()) return ElMessage.warning('请填写线上平台')
  submitting.value = true
  try {
    const response = await createMeetup(payload())
    ElMessage.success('活动草稿已创建，请确认内容后发布')
    router.replace({ name: 'meetup-detail', params: { meetupId: response.data.id } })
  } catch (error) {
    submitError.value = error.response?.data?.error?.message ?? '创建失败，请稍后重试'
  } finally { submitting.value = false }
}

onMounted(load)
</script>

<template>
  <section class="form-page meetup-form-page">
    <p class="eyebrow accent">CREATE A MEETUP</p><h1>创建活动</h1><p>先保存为草稿，检查无误后再发布并接受报名。活动容量包含创建者。</p>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" scroll-to-error>
      <h2>1. 关联范围</h2>
      <el-alert v-if="circle" type="info" :closable="false" :title="`活动将发布到 Circle：${circle.name}，Topic 由后端自动确认。`" />
      <div class="form-grid"><el-form-item label="分类"><el-select v-model="form.categoryId" :disabled="Boolean(circle)" placeholder="选择分类" @change="changeCategory"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item><el-form-item label="话题" prop="topicId"><el-select v-model="form.topicId" :disabled="Boolean(circle)" placeholder="选择话题"><el-option v-for="item in topics" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></div>

      <h2>2. 活动内容</h2>
      <el-form-item label="活动模式" prop="meetupMode"><el-radio-group v-model="form.meetupMode"><el-radio-button value="OFFLINE">线下活动</el-radio-button><el-radio-button value="ONLINE">线上组队</el-radio-button></el-radio-group></el-form-item>
      <el-form-item label="标题" prop="title"><el-input v-model="form.title" maxlength="100" show-word-limit /></el-form-item>
      <el-form-item label="活动说明" prop="description"><el-input v-model="form.description" type="textarea" :rows="6" maxlength="2000" show-word-limit /></el-form-item>

      <h2>3. 时间与人数</h2>
      <div class="form-grid"><el-form-item label="报名截止" prop="applicationDeadline"><el-date-picker v-model="form.applicationDeadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择报名截止时间" /></el-form-item><el-form-item label="开始时间" prop="startTime"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择开始时间" /></el-form-item></div>
      <div class="form-grid"><el-form-item label="结束时间" prop="endTime"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择结束时间" /></el-form-item><el-form-item label="总容量（包含创建者）"><el-input-number v-model="form.capacity" :min="2" :max="100" /></el-form-item></div>

      <template v-if="form.meetupMode === 'OFFLINE'"><h2>4. 线下地点</h2><div class="form-grid"><el-form-item label="城市" required><el-input v-model="form.city" /></el-form-item><el-form-item label="区域" required><el-input v-model="form.district" /></el-form-item><el-form-item label="地点名称" required><el-input v-model="form.locationName" /></el-form-item><el-form-item label="详细地址" required><el-input v-model="form.address" /></el-form-item></div><el-alert type="info" :closable="false" title="保存草稿时可以暂不设置签到坐标，但发布线下活动前必须补齐。坐标按 WGS84 处理。" /><div class="location-capture-actions"><el-button :loading="locating" :disabled="locating" @click="useCurrentLocation">使用当前位置作为签到位置</el-button><el-button v-if="form.locationLatitude != null" @click="clearCurrentLocation">清除签到位置</el-button><span v-if="locationAccuracy != null">本次定位精度约 {{ Math.round(locationAccuracy) }} 米</span></div><div class="form-grid"><el-form-item label="纬度（草稿可选）"><el-input-number v-model="form.locationLatitude" :min="-90" :max="90" :precision="6" /></el-form-item><el-form-item label="经度（草稿可选）"><el-input-number v-model="form.locationLongitude" :min="-180" :max="180" :precision="6" /></el-form-item></div><el-form-item label="签到半径（设置坐标后必填）"><el-input-number v-model="form.checkInRadiusMeters" :disabled="form.locationLatitude == null || form.locationLongitude == null" :min="50" :max="1000" :step="50" /></el-form-item></template>
      <template v-else><h2>4. 线上方式</h2><div class="form-grid"><el-form-item label="线上平台" required><el-input v-model="form.onlinePlatform" placeholder="例如：王者荣耀、Steam、腾讯会议" /></el-form-item><el-form-item label="服务器区域"><el-input v-model="form.serverRegion" /></el-form-item></div><el-form-item label="加入说明"><el-input v-model="form.accessInstructions" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="只有创建者和已接受成员能够看到" /></el-form-item></template>

      <h2>5. 参与要求</h2>
      <div class="form-grid"><el-form-item label="最低年龄"><el-input-number v-model="form.minimumAge" :min="18" :max="120" /></el-form-item><el-form-item label="最高年龄"><el-input-number v-model="form.maximumAge" :min="18" :max="120" /></el-form-item></div>
      <el-form-item label="性别要求"><el-select v-model="form.genderRequirement"><el-option label="不限" value="ANY" /><el-option label="与创建者同性别" value="SAME_GENDER" /></el-select></el-form-item>
      <el-form-item label="技能要求"><el-input v-model="form.skillRequirement" maxlength="255" show-word-limit /></el-form-item>
      <div v-if="submitError" class="form-guidance"><span>{{ submitError }}</span><RouterLink v-if="submitError.includes('实名')" to="/me/identity">去完成实名认证</RouterLink></div>
      <el-button type="primary" size="large" :loading="submitting" @click="submit">保存草稿</el-button>
    </el-form>
  </section>
</template>
