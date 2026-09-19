<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyAccount, requestAccountCancellation, revokeAccountCancellation } from '../../api/account.js'

const account = ref(null)
const loading = ref(true)
const submitting = ref(false)

async function load() {
  loading.value = true
  try {
    account.value = (await getMyAccount()).data
  } finally {
    loading.value = false
  }
}

async function changeCancellation() {
  if (submitting.value || !account.value) return
  const pending = account.value.accountStatus === 'CANCEL_PENDING'
  try {
    await ElMessageBox.confirm(
      pending ? '撤销注销申请后，账户将恢复正常状态。' : '申请后进入 7 天冷静期；到期后账户将注销且手机号会被释放。冷静期内可以撤销。',
      pending ? '撤销注销申请' : '申请注销账户',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  submitting.value = true
  try {
    account.value = (await (pending ? revokeAccountCancellation() : requestAccountCancellation())).data
    ElMessage.success(pending ? '注销申请已撤销' : '注销申请已提交')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="account-settings-page">
    <div class="section-heading"><div><p class="eyebrow accent">ACCOUNT</p><h1>账户设置</h1><p>查看账户状态及管理注销申请。</p></div></div>
    <el-card v-if="account">
      <p>手机号：{{ account.maskedPhone ?? '已解绑' }}</p>
      <p>账户状态：{{ account.accountStatus === 'ACTIVE' ? '正常' : account.accountStatus === 'CANCEL_PENDING' ? '注销冷静期' : account.accountStatus }}</p>
      <template v-if="account.accountStatus === 'CANCEL_PENDING'">
        <p>申请时间：{{ account.cancelRequestedAt }}</p>
        <p>预计注销时间：{{ account.cancellationEffectiveAt }}</p>
        <el-alert title="冷静期内可撤销；到期注销后无法恢复当前账户。" type="warning" :closable="false" />
      </template>
      <el-button v-if="['ACTIVE', 'CANCEL_PENDING'].includes(account.accountStatus)" :type="account.accountStatus === 'ACTIVE' ? 'danger' : 'primary'" :loading="submitting" :disabled="submitting" @click="changeCancellation">
        {{ account.accountStatus === 'ACTIVE' ? '申请注销' : '撤销注销申请' }}
      </el-button>
    </el-card>
  </section>
</template>
