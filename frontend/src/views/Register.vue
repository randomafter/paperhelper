<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="card-header">
        <h1>历史特色创作</h1>
        <p class="subtitle">创建新账户</p>
      </div>

      <form @submit.prevent="onSubmit" class="register-form">
        <div class="form-group">
          <input v-model="form.username" type="text" placeholder="用户名（2-20字）" required minlength="2" maxlength="20" />
        </div>
        <div class="form-group">
          <input v-model="form.password" type="password" placeholder="密码（至少6位）" required minlength="6" />
        </div>
        <div class="form-group">
          <input v-model="form.nickname" type="text" placeholder="昵称（可选）" />
        </div>
        <div class="form-group">
          <input v-model="form.securityQuestion" type="text" placeholder="安全问题（如：我母亲的名字？）" required />
        </div>
        <div class="form-group">
          <input v-model="form.securityAnswer" type="text" placeholder="安全答案" required />
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" :disabled="loading" class="btn-register">
          {{ loading ? '注册中...' : '创建账户' }}
        </button>
      </form>

      <div class="links">
        <p>已有账户？<router-link to="/login" class="link">登录</router-link></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { authApi } from '../api/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const error = ref('')
const form = reactive({
  username: '',
  password: '',
  nickname: '',
  securityQuestion: '',
  securityAnswer: ''
})

async function onSubmit() {
  error.value = ''
  loading.value = true
  try {
    const res = await authApi.register(form)
    if (res.data?.code === 200 && res.data?.data) {
      auth.setAuth(res.data.data)
      router.replace('/')
    } else {
      error.value = res.data?.message || '注册失败'
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f0f1a 0%, #1a1a2e 50%, #16213e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.auth-card {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95), rgba(22, 33, 62, 0.95));
  border: 1px solid rgba(233, 69, 96, 0.2);
  border-radius: 16px;
  width: 100%;
  max-width: 380px;
  padding: 2.5rem;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
  max-height: 90vh;
  overflow-y: auto;
}

.auth-card::-webkit-scrollbar {
  width: 6px;
}

.auth-card::-webkit-scrollbar-thumb {
  background: #2a2a4a;
  border-radius: 3px;
}

.card-header {
  text-align: center;
  margin-bottom: 2rem;
}

.card-header h1 {
  margin: 0 0 0.5rem;
  font-size: 1.75rem;
  background: linear-gradient(90deg, #e94560, #ff6b9d);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  margin: 0;
  color: #b8b8d0;
  font-size: 0.95rem;
}

.register-form {
  margin-bottom: 1.5rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group input {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid #444;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  font-size: 1rem;
  transition: all 0.3s ease;
}

.form-group input:focus {
  outline: none;
  border-color: #e94560;
  background: rgba(255, 255, 255, 0.12);
  box-shadow: 0 0 12px rgba(233, 69, 96, 0.2);
}

.form-group input::placeholder {
  color: #888;
}

.error {
  color: #ff6b6b;
  font-size: 0.9rem;
  margin: 1rem 0;
  padding: 0.75rem;
  background: rgba(255, 107, 107, 0.1);
  border-left: 3px solid #ff6b6b;
  border-radius: 4px;
}

.btn-register {
  width: 100%;
  padding: 0.875rem;
  background: linear-gradient(90deg, #e94560, #ff6b9d);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(233, 69, 96, 0.3);
}

.btn-register:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(233, 69, 96, 0.4);
}

.btn-register:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.links {
  text-align: center;
  color: #b8b8d0;
  font-size: 0.9rem;
}

.link {
  color: #e94560;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;
}

.link:hover {
  color: #ff6b9d;
}

@media (max-width: 480px) {
  .auth-card {
    padding: 1.5rem;
  }

  .card-header h1 {
    font-size: 1.5rem;
  }
}
</style>
