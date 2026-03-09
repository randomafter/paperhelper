<template>
  <div class="profile">
    <h1>个人中心</h1>
    <div v-if="profile" class="card">
      <p class="field"><strong>用户ID</strong> {{ profile.id }}</p>
      <p class="field"><strong>账号</strong> {{ profile.username }}</p>
      <p class="field"><strong>角色</strong> {{ profile.role === 'ADMIN' ? '管理员' : '创作者' }}</p>
      <p class="field"><strong>注册时间</strong> {{ profile.createdAt || '-' }}</p>

      <div class="form-group">
        <label>昵称（2-20 字）</label>
        <input v-model="form.nickname" type="text" placeholder="请输入昵称" />
      </div>
      <div class="form-group">
        <label>个人简介（0-200 字，可选）</label>
        <textarea v-model="form.intro" rows="4" placeholder="可简单描述你的创作风格、擅长题材等" />
      </div>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="success" class="success">{{ success }}</p>

      <div class="actions">
        <button @click="onSaveProfile" :disabled="savingProfile">保存资料</button>
        <button class="secondary" @click="loadProfile" :disabled="savingProfile">刷新</button>
      </div>
    </div>

    <div v-if="profile" class="card password-card">
      <h2>修改密码</h2>
      <div class="form-group">
        <label>原密码</label>
        <input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" />
      </div>
      <div class="form-group">
        <label>新密码（6-32 位）</label>
        <input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码" />
      </div>
      <div class="form-group">
        <label>确认新密码</label>
        <input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
      </div>

      <p v-if="pwdError" class="error">{{ pwdError }}</p>
      <p v-if="pwdSuccess" class="success">{{ pwdSuccess }}</p>

      <div class="actions">
        <button @click="onChangePassword" :disabled="changingPwd">修改密码</button>
      </div>
    </div>
    <p v-else>加载中…</p>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { authApi } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const profile = ref(null)
const form = reactive({ nickname: '', intro: '' })
const savingProfile = ref(false)
const error = ref('')
const success = ref('')

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const changingPwd = ref(false)
const pwdError = ref('')
const pwdSuccess = ref('')

async function loadProfile() {
  try {
    const res = await authApi.getProfile()
    if (res.data?.data) {
      profile.value = res.data.data
      form.nickname = res.data.data.nickname || ''
      form.intro = res.data.data.intro || ''
    }
  } catch (e) {
    console.error(e)
  }
}

async function onSaveProfile() {
  error.value = ''
  success.value = ''
  savingProfile.value = true
  try {
    const nickname = (form.nickname || '').trim()
    const intro = (form.intro || '').trim()
    if (!nickname || nickname.length < 2 || nickname.length > 20) {
      error.value = '昵称长度需在 2-20 个字符之间'
      return
    }
    if (intro.length > 200) {
      error.value = '个人简介长度不能超过 200 个字符'
      return
    }
    const params = {
      nickname,
      intro,
    }
    const res = await authApi.updateProfile(params)
    if (res.data?.code === 200 && res.data?.data) {
      success.value = '资料已保存'
      await auth.fetchProfile()
      await loadProfile()
    } else {
      error.value = res.data?.message || '保存失败'
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '保存失败'
  } finally {
    savingProfile.value = false
  }
}

async function onChangePassword() {
  pwdError.value = ''
  pwdSuccess.value = ''
  changingPwd.value = true
  try {
    if (!pwdForm.oldPassword || !pwdForm.newPassword || !pwdForm.confirmPassword) {
      pwdError.value = '请填写完整的密码信息'
      return
    }
    if (pwdForm.newPassword.length < 6 || pwdForm.newPassword.length > 32) {
      pwdError.value = '新密码长度需在 6-32 位之间'
      return
    }
    if (pwdForm.newPassword !== pwdForm.confirmPassword) {
      pwdError.value = '两次输入的新密码不一致'
      return
    }
    const res = await authApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword,
    })
    if (res.data?.code === 200) {
      pwdSuccess.value = '密码修改成功'
      pwdForm.oldPassword = ''
      pwdForm.newPassword = ''
      pwdForm.confirmPassword = ''
    } else {
      pwdError.value = res.data?.message || '密码修改失败'
    }
  } catch (e) {
    pwdError.value = e.response?.data?.message || e.message || '密码修改失败'
  } finally {
    changingPwd.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile h1 { margin-bottom: 1rem; }
.card { background: #1a1a2e; padding: 1.5rem; border-radius: 10px; max-width: 480px; margin-bottom: 1rem; }
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
.form-group textarea {
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  border: 1px solid #333;
  background: #121225;
  color: #fff;
  resize: vertical;
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
.password-card h2 { margin: 0 0 0.75rem; font-size: 1.1rem; }
</style>
