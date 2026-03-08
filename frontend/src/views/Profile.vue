<template>
  <div class="profile">
    <h1>个人中心</h1>
    <div v-if="profile" class="card">
      <p class="field"><strong>用户名</strong> {{ profile.username }}</p>
      <p class="field"><strong>角色</strong> {{ profile.role === 'ADMIN' ? '管理员' : '创作者' }}</p>

      <div class="form-group">
        <label>昵称</label>
        <input v-model="form.nickname" type="text" placeholder="请输入昵称" />
      </div>
      <div class="form-group">
        <label>邮箱</label>
        <input v-model="form.email" type="email" placeholder="请输入邮箱" />
      </div>
      <div class="form-group">
        <label>手机</label>
        <input v-model="form.phone" type="text" placeholder="请输入手机号" />
      </div>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="success" class="success">{{ success }}</p>

      <div class="actions">
        <button @click="onSave" :disabled="saving">保存资料</button>
        <button class="secondary" @click="loadProfile" :disabled="saving">刷新</button>
      </div>
    </div>
    <p v-else>加载中…</p>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { authApi } from '../api/auth'
const profile = ref(null)
const form = reactive({ nickname: '', email: '', phone: '' })
const saving = ref(false)
const error = ref('')
const success = ref('')

async function loadProfile() {
  try {
    const res = await authApi.getProfile()
    if (res.data?.data) {
      profile.value = res.data.data
      form.nickname = res.data.data.nickname || ''
      form.email = res.data.data.email || ''
      form.phone = res.data.data.phone || ''
    }
  } catch (e) {
    console.error(e)
  }
}

async function onSave() {
  error.value = ''
  success.value = ''
  saving.value = true
  try {
    const params = {
      nickname: form.nickname || undefined,
      email: form.email || undefined,
      phone: form.phone || undefined,
    }
    const res = await authApi.updateProfile(params)
    if (res.data?.code === 200 && res.data?.data) {
      success.value = '资料已保存'
      await loadProfile()
    } else {
      error.value = res.data?.message || '保存失败'
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile h1 { margin-bottom: 1rem; }
.card { background: #1a1a2e; padding: 1.5rem; border-radius: 10px; max-width: 420px; }
.field { margin: 0.5rem 0; }
.form-group { margin-top: 0.75rem; display: flex; flex-direction: column; gap: 0.25rem; }
.form-group label { font-size: 0.9rem; color: #ccc; }
.form-group input {
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  border: 1px solid #333;
  background: #121225;
  color: #fff;
}
.actions { margin-top: 1rem; display: flex; gap: 0.5rem; }
.actions button {
  padding: 0.5rem 1rem;
  border-radius: 6px;
  border: none;
  cursor: pointer;
}
.actions button:first-of-type { background: #e94560; color: #fff; }
.actions .secondary { background: transparent; border: 1px solid #444; color: #ddd; }
.error { margin-top: 0.5rem; color: #e94560; font-size: 0.9rem; }
.success { margin-top: 0.5rem; color: #4caf50; font-size: 0.9rem; }
</style>
