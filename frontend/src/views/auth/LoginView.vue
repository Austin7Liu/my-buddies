<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginBySms, sendSmsCode } from '../../api/auth.js'
import { setSession } from '../../stores/auth.js'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const sending = ref(false)
const loggingIn = ref(false)
const countdown = ref(0)
let countdownTimer

const form = reactive({ phone: '', code: '' })
const phoneValid = computed(() => /^1[3-9]\d{9}$/.test(form.phone))
const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的中国大陆手机号', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码是 6 位数字', trigger: 'blur' },
  ],
}

function startCountdown(seconds = 60) {
  countdown.value = seconds
  clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(countdownTimer)
  }, 1000)
}

async function requestCode() {
  if (!phoneValid.value || countdown.value > 0) return
  sending.value = true
  try {
    const response = await sendSmsCode(form.phone)
    startCountdown(Math.min(response.data.expiresInSeconds, 60))
    ElMessage.success('验证码已生成，请查看 Spring Boot 控制台日志')
  } finally {
    sending.value = false
  }
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loggingIn.value = true
  try {
    const response = await loginBySms(form.phone, form.code)
    setSession(response.data.account, response.data.tokens)
    ElMessage.success('登录成功')
    router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/')
  } finally {
    loggingIn.value = false
  }
}

onBeforeUnmount(() => clearInterval(countdownTimer))
</script>

<template>
  <main class="login-page">
    <section class="login-story">
      <div class="story-content">
        <p class="eyebrow">MAKE TIME TOGETHER</p>
        <h1>把兴趣，变成<br />真实的同行。</h1>
        <p>从一场球、一局游戏到一次城市漫步，找到认真赴约的人。</p>
        <div class="story-tags"><span>兴趣社区</span><span>可信活动</span><span>同城连接</span></div>
      </div>
    </section>
    <section class="login-panel">
      <div class="login-card">
        <div class="mobile-brand"><span class="brand-mark">MB</span> My Buddies</div>
        <p class="eyebrow accent">WELCOME BACK</p>
        <h2>手机号登录</h2>
        <p class="login-hint">未注册的手机号将自动创建账户</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" maxlength="11" size="large" placeholder="例如：18800001001" />
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <div class="code-row">
              <el-input v-model="form.code" maxlength="6" size="large" placeholder="6 位验证码" @keyup.enter="submit" />
              <el-button size="large" :loading="sending" :disabled="!phoneValid || countdown > 0" @click="requestCode">
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-button class="login-button" type="primary" size="large" :loading="loggingIn" native-type="submit">进入 My Buddies</el-button>
        </el-form>
        <p class="dev-tip">开发环境验证码会输出在后端控制台，不会发送真实短信。</p>
      </div>
    </section>
  </main>
</template>
