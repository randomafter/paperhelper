<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1>历史特色创作平台</h1>
      <p class="subtitle">登录</p>
      <form @submit.prevent="onSubmit">
        <input v-model="form.username" type="text" placeholder="用户名" required />
        <input v-model="form.password" type="password" placeholder="密码" required />
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" :disabled="loading">登录</button>
      </form>
      <p class="link">
        没有账号？<router-link to="/register">注册</router-link>
      </p>
      <p class="link small">
        忘记密码？<router-link to="/forgot-password">点击找回</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { authApi } from '../api/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const error = ref('')
const form = reactive({ username: '', password: '' })

async function onSubmit() {
  error.value = ''
  loading.value = true
  try {
    const res = await authApi.login(form)
    if (res.data?.code === 200 && res.data?.data) {
      auth.setAuth(res.data.data)
      router.replace(route.query.redirect || '/')
    } else {
      error.value = res.data?.message || '登录失败'
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}
.auth-card {
  background: rgba(255,255,255,0.06);
  padding: 2.5rem;
  border-radius: 12px;
  width: 100%;
  max-width: 380px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.3);
}
.auth-card h1 { margin: 0 0 0.5rem; font-size: 1.5rem; color: #e8e8e8; }
.subtitle { color: #aaa; margin-bottom: 1.5rem; }
.auth-card input {
  width: 100%;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  border: 1px solid #444;
  border-radius: 8px;
  background: rgba(255,255,255,0.08);
  color: #fff;
  font-size: 1rem;
}
.auth-card input::placeholder { color: #888; }
.auth-card button {
  width: 100%;
  padding: 0.75rem;
  border: none;
  border-radius: 8px;
  background: #e94560;
  color: #fff;
  font-size: 1rem;
  cursor: pointer;
}
.auth-card button:disabled { opacity: 0.6; cursor: not-allowed; }
.error { color: #e94560; font-size: 0.9rem; margin: -0.5rem 0 0.5rem; }
.link { margin-top: 1rem; color: #aaa; }
.link a { color: #e94560; }
.link.small { font-size: 0.85rem; opacity: 0.85; }
</style>
