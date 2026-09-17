<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyProfile, updateMyProfile } from '../../api/profile.js'
import { avatarOptions } from '../../utils/profile.js'
import UserAvatar from '../../components/UserAvatar.vue'
import { setCurrentProfile } from '../../stores/profile.js'

const formRef = ref()
const profile = ref(null)
const loading = ref(true)
const saving = ref(false)
const form = reactive({ nickname: '', avatarCode: 'PANDA', bio: '', city: '', district: '' })
const rules = {
  nickname: [
    { required: true, message: '请输入昵称' },
    { min: 2, max: 30, message: '昵称长度应为 2 到 30 个字符' },
  ],
  avatarCode: [{ required: true, message: '请选择头像' }],
}

function fill(value) {
  profile.value = value
  setCurrentProfile(value)
  form.nickname = value.nickname
  form.avatarCode = value.avatarCode
  form.bio = value.bio ?? ''
  form.city = value.city ?? ''
  form.district = value.district ?? ''
}

async function load() {
  loading.value = true
  try {
    fill((await getMyProfile()).data)
  } finally {
    loading.value = false
  }
}

function nullable(value) {
  const normalized = value.trim()
  return normalized || null
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    fill((await updateMyProfile({
      nickname: form.nickname.trim(),
      avatarCode: form.avatarCode,
      bio: nullable(form.bio),
      city: nullable(form.city),
      district: nullable(form.district),
    })).data)
    ElMessage.success('个人资料已保存')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="profile-edit-page">
    <div class="section-heading"><div><p class="eyebrow accent">MY PROFILE</p><h1>个人资料</h1><p>这些资料会展示在帖子、圈子和活动中，请勿填写手机号、身份证等敏感信息。</p></div><div class="profile-actions"><RouterLink to="/me/reports"><el-button>我的举报</el-button></RouterLink><RouterLink to="/me/appeals"><el-button>我的申诉</el-button></RouterLink><RouterLink to="/me/account-safety"><el-button>违规与限制</el-button></RouterLink><RouterLink v-if="profile" :to="`/profiles/${profile.accountId}`"><el-button>查看公开主页</el-button></RouterLink></div></div>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="昵称" prop="nickname"><el-input v-model="form.nickname" maxlength="30" show-word-limit /></el-form-item>
      <el-form-item label="头像" prop="avatarCode"><div class="avatar-picker"><label v-for="option in avatarOptions" :key="option.value" class="avatar-option" :class="{ selected: form.avatarCode === option.value }"><input v-model="form.avatarCode" type="radio" :value="option.value" /><UserAvatar :avatar-code="option.value" :nickname="option.label" :size="68" /><span>{{ option.label }}</span></label></div></el-form-item>
      <el-form-item label="个人简介"><el-input v-model="form.bio" type="textarea" :rows="5" maxlength="200" show-word-limit placeholder="介绍一下你的兴趣、活动习惯或擅长领域" /></el-form-item>
      <div class="form-grid"><el-form-item label="城市"><el-input v-model="form.city" maxlength="64" /></el-form-item><el-form-item label="区域"><el-input v-model="form.district" maxlength="64" /></el-form-item></div>
      <el-button type="primary" size="large" :loading="saving" @click="save">保存资料</el-button>
    </el-form>
  </section>
</template>
