<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitIdentityVerification } from '../../api/identity.js'
import { identityFailureMessage, identityStatusLabel } from '../../utils/identity.js'
import { identityState, loadIdentityVerification, setIdentityVerification } from '../../stores/identity.js'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(true)
const submitting = ref(false)
const pageError = ref('')
const verification = computed(() => identityState.verification)
const form = reactive({ realName: '', identityNumber: '' })
const rules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '姓名长度应为 2 至 50 个字符', trigger: 'blur' },
  ],
  identityNumber: [
    { required: true, message: '请输入身份证号码', trigger: 'blur' },
    { pattern: /^[1-9]\d{16}[0-9Xx]$/, message: '请输入正确的 18 位身份证号码', trigger: 'blur' },
  ],
}

const statusLabel = computed(() => identityStatusLabel(verification.value?.status))
const failureMessage = computed(() => identityFailureMessage(verification.value?.failureCode))
const canSubmit = computed(() => verification.value?.status !== 'VERIFIED')

async function load() {
  loading.value = true
  pageError.value = ''
  try {
    await loadIdentityVerification()
  } catch (error) {
    pageError.value = error.response?.data?.error?.message ?? '认证状态加载失败'
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (submitting.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  pageError.value = ''
  try {
    const result = (await submitIdentityVerification(form.realName, form.identityNumber)).data
    setIdentityVerification(result)
    if (verification.value.status === 'VERIFIED') {
      ElMessage.success('实名认证已完成')
    } else {
      ElMessage.warning(failureMessage.value || '实名认证未通过')
    }
  } catch (error) {
    pageError.value = error.response?.data?.error?.message ?? '实名认证提交失败'
  } finally {
    form.identityNumber = ''
    submitting.value = false
  }
}

function returnToPreviousPage() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
  router.replace(redirect)
}

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="form-page identity-page">
    <p class="eyebrow accent">TRUST &amp; SAFETY</p>
    <h1>实名认证</h1>
    <p>认证结果用于成年校验和社区安全。平台不会保存身份证号码明文。</p>

    <el-alert v-if="pageError" :title="pageError" type="error" show-icon :closable="false" />

    <div v-if="verification" class="identity-status" :class="`status-${verification.status.toLowerCase()}`">
      <div>
        <small>当前状态</small>
        <strong>{{ statusLabel }}</strong>
      </div>
      <p v-if="failureMessage">{{ failureMessage }}</p>
      <p v-else-if="verification.status === 'VERIFIED'">
        {{ verification.adult ? '已通过成年人校验' : '未通过成年人校验' }}
        <template v-if="verification.birthDate"> · 出生日期 {{ verification.birthDate }}</template>
        <template v-if="verification.gender"> · {{ verification.gender === 'MALE' ? '男' : '女' }}</template>
      </p>
      <p v-else>填写信息后即可提交认证。</p>
    </div>

    <el-form v-if="canSubmit" ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-alert title="开发环境仅进行姓名格式和身份证号码规则校验，不代表真实二要素核验。" type="warning" show-icon :closable="false" />
      <el-form-item label="真实姓名" prop="realName">
        <el-input v-model="form.realName" maxlength="50" autocomplete="name" placeholder="请输入本人真实姓名" />
      </el-form-item>
      <el-form-item label="身份证号码" prop="identityNumber">
        <el-input v-model="form.identityNumber" maxlength="18" autocomplete="off" placeholder="请输入 18 位身份证号码" @keyup.enter="submit" />
      </el-form-item>
      <el-button type="primary" size="large" :loading="submitting" @click="submit">提交认证</el-button>
    </el-form>

    <el-button v-if="verification?.status === 'VERIFIED' && route.query.redirect" type="primary" size="large" @click="returnToPreviousPage">
      返回继续操作
    </el-button>
  </section>
</template>
