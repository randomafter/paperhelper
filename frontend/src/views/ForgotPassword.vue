<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1>历史特色创作平台</h1>
      <p class="subtitle">忘记密码</p>
      
      <!-- 第一步：输入用户名 -->
      <form v-if="step === 1" @submit.prevent="onSubmitUsername">
        <input v-model="form.username" type="text" placeholder="请输入用户名" required />
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" :disabled="loading">下一步</button>
      </form>
      
      <!-- 第二步：回答安全问题 -->
      <form v-else-if="step === 2" @submit.prevent="onSubmitAnswer">
        <p class="question">{{ securityQuestion }}</p>
        <input v-model="form.answer" type="text" placeholder="请输入答案" required />
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" :disabled="loading">验证答案</button>
      </form>
      
      <!-- 第三步：重置密码 -->
      <form v-else-if="step === 3" @submit.prevent="onSubmitPassword">
        <input v-model="form.newPassword" type="password" placeholder="请输入新密码" required minlength="6" />
        <input v-model="form.confirmPassword" type="password" placeholder="请确认新密码" required minlength="6" />
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">{{ success }}</p>
        <button type="submit" :disabled="loading">重置密码</button>
      </form>
      
      <p class="link">
        记起来了？<router-link to="/login">返回登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api/auth'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const success = ref('')
const step = ref(1)
const securityQuestion = ref('')
const form = reactive({ 
  username: '', 
  answer: '', 
  newPassword: '', 
  confirmPassword: '' 
})

async function onSubmitUsername() {
  error.value = ''
  loading.value = true
  try {
    const res = await authApi.getSecurityQuestion(form.username)
    if (res.data?.code === 200 && res.data?.data) {
      securityQuestion.value = res.data.data
      step.value = 2
    } else {
      error.value = res.data?.message || '用户不存在'
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '获取安全问题失败'
  } finally {
    loading.value = false
  }
}

async function onSubmitAnswer() {
  error.value = ''
  loading.value = true
  try {
    const res = await authApi.verifySecurityAnswer({
      username: form.username,
      answer: form.answer
    })
    if (res.data?.code === 200) {
      step.value = 3
    } else {
      error.value = res.data?.message || '答案错误'
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '验证失败'
  } finally {
    loading.value = false
  }
}

async function onSubmitPassword() {
  error.value = ''
  success.value = ''
  
  if (form.newPassword !== form.confirmPassword) {
    error.value = '两次输入的密码不一致'
    return
  }
  
  loading.value = true
  try {
    const res = await authApi.resetPasswordByAnswer({
      username: form.username,
      answer: form.answer,
      newPassword: form.newPassword
    })
    if (res.data?.code === 200) {
      success.value = res.data?.data || '密码重置成功'
      setTimeout(() => {
        router.push('/login')
      }, 2000)
    } else {
      error.value = res.data?.message || '重置失败'
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '重置失败'
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
.question {
  color: #e8e8e8;
  font-size: 1rem;
  margin-bottom: 1rem;
  padding: 0.75rem;
  background: rgba(255,255,255,0.1);
  border-radius: 8px;
}
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
.success { color: #4caf50; font-size: 0.9rem; margin: -0.5rem 0 0.5rem; }
.link { margin-top: 1rem; color: #aaa; }
.link a { color: #e94560; }
</style>
