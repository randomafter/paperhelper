<template>
  <div class="profile-page">
    <div class="theme-switcher">
      <button class="theme-btn" :class="{ active: theme.theme === 'dark' }" @click="theme.setTheme('dark')" title="深色主题">🌙</button>
      <button class="theme-btn" :class="{ active: theme.theme === 'light' }" @click="theme.setTheme('light')" title="浅色主题">☀️</button>
    </div>

    <div class="profile-container">
      <div class="profile-card">
        <div class="card-header">
          <h1>个人中心</h1>
          <p class="subtitle">管理你的账户信息</p>
        </div>

        <div v-if="profile" class="card-content">
          <div class="info-section">
            <div class="section-title"><span class="icon">👤</span><span>基本信息</span></div>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">账号</span>
                <span class="value">{{ profile.username }}</span>
              </div>
              <div class="info-item">
                <span class="label">角色</span>
                <span class="value role" :class="{ admin: profile.role === 'ADMIN' }">{{ profile.role === 'ADMIN' ? '👑 管理员' : '✨ 创作者' }}</span>
              </div>
              <div class="info-item">
                <span class="label">注册时间</span>
                <span class="value">{{ formatDate(profile.createdAt) }}</span>
              </div>
            </div>
          </div>

          <div class="info-section">
            <div class="section-title"><span class="icon">✏️</span><span>个人资料</span></div>
            <div class="form-group">
              <label>昵称（2-20 字）</label>
              <input v-model="form.nickname" type="text" placeholder="请输入昵称" />
            </div>
            <div class="form-group">
              <label>个人简介（0-200 字，可选）</label>
              <textarea v-model="form.intro" rows="3" placeholder="可简单描述你的创作风格、擅长题材等" />
            </div>
            <p v-if="profileError" class="error">{{ profileError }}</p>
            <p v-if="profileSuccess" class="success">✓ {{ profileSuccess }}</p>
            <button @click="onSaveProfile" :disabled="savingProfile" class="btn-primary">{{ savingProfile ? '保存中...' : '保存资料' }}</button>
          </div>

          <div class="info-section collapsible">
            <div class="section-title" @click="togglePasswordForm" style="cursor: pointer">
              <span class="icon">🔐</span><span>修改密码</span>
              <span class="toggle-icon" :class="{ open: showPasswordForm }">▼</span>
            </div>
            <div v-if="showPasswordForm" class="collapsible-content">
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
              <p v-if="pwdSuccess" class="success">✓ {{ pwdSuccess }}</p>
              <button @click="onChangePassword" :disabled="changingPwd" class="btn-primary">{{ changingPwd ? '修改中...' : '修改密码' }}</button>
            </div>
          </div>

          <div class="info-section">
            <div class="section-title">
              <span class="icon">📥</span><span>收件箱</span>
              <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }}</span>
              <button v-if="unreadCount > 0" @click="markAllRead" class="btn-mark-all">全部已读</button>
            </div>
            <p class="section-desc">查看素材审核结果等系统通知</p>
            <div v-if="notifLoading" class="loading">加载中…</div>
            <div v-else-if="notifications.length === 0" class="empty-box">暂无新通知</div>
            <ul v-else class="notif-list">
              <li
                v-for="n in notifications"
                :key="n.id"
                class="notif-item"
                :class="{ unread: !n.isRead }"
              >
                <div class="notif-icon">{{ n.type === 'material_approved' ? '✅' : n.type === 'material_rejected' ? '❌' : '📢' }}</div>
                <div class="notif-main">
                  <div class="notif-title">{{ n.title }}</div>
                  <div class="notif-msg">{{ n.content }}</div>
                </div>
                <div class="notif-meta">
                  <span class="notif-time">{{ formatDate(n.createdAt) }}</span>
                  <button
                    v-if="!n.isRead"
                    class="notif-btn"
                    @click="markNotifRead(n)"
                  >
                    已读
                  </button>
                </div>
              </li>
            </ul>
          </div>

          <div class="info-section">
            <div class="section-title"><span class="icon">💬</span><span>反馈意见</span></div>
            <p class="section-desc">有任何建议或问题？我们很乐意听取你的意见</p>
            <button @click="showFeedbackModal = true" class="btn-secondary">📝 提交反馈</button>
          </div>
        </div>
        <p v-else class="loading">加载中…</p>
      </div>
    </div>

    <div v-if="showFeedbackModal" class="modal-overlay" @click.self="showFeedbackModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2>提交反馈意见</h2>
          <button class="close-btn" @click="showFeedbackModal = false">✕</button>
        </div>
        <div class="modal-body">
          <textarea v-model="feedbackForm.content" placeholder="请输入你的反馈意见（最多1000字）..." rows="6" maxlength="1000" />
          <div class="char-count">{{ feedbackForm.content.length }}/1000</div>
          <p v-if="feedbackError" class="error">{{ feedbackError }}</p>
          <p v-if="feedbackSuccess" class="success">✓ {{ feedbackSuccess }}</p>
        </div>
        <div class="modal-footer">
          <button @click="showFeedbackModal = false" class="btn-secondary">取消</button>
          <button @click="onSubmitFeedback" :disabled="submittingFeedback" class="btn-primary">{{ submittingFeedback ? '提交中...' : '提交反馈' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useThemeStore } from '../stores/theme'
import { authApi } from '../api/auth'
import { feedbackApi } from '../api/feedback'
import { notificationApi } from '../api/notification'

const auth = useAuthStore()
const theme = useThemeStore()

const profile = ref(null)
const form = reactive({ nickname: '', intro: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const feedbackForm = reactive({ content: '' })

const savingProfile = ref(false)
const changingPwd = ref(false)
const submittingFeedback = ref(false)

const profileError = ref('')
const profileSuccess = ref('')
const pwdError = ref('')
const pwdSuccess = ref('')
const feedbackError = ref('')
const feedbackSuccess = ref('')

const notifications = ref([])
const notifLoading = ref(false)
const unreadCount = ref(0)

const showPasswordForm = ref(false)
const showFeedbackModal = ref(false)

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function togglePasswordForm() {
  showPasswordForm.value = !showPasswordForm.value
}

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

async function loadNotifications() {
  notifLoading.value = true
  try {
    const res = await notificationApi.list()
    if (res.data?.code === 200 && res.data?.data) {
      notifications.value = res.data.data
      unreadCount.value = notifications.value.filter(n => !n.isRead).length
    }
  } catch (e) {
    console.error(e)
  } finally {
    notifLoading.value = false
  }
}

async function markNotifRead(n) {
  try {
    await notificationApi.markRead(n.id)
    n.isRead = true
    unreadCount.value = notifications.value.filter(n => !n.isRead).length
  } catch (e) {
    console.error(e)
  }
}

async function markAllRead() {
  try {
    await notificationApi.markAllRead()
    notifications.value.forEach(n => { n.isRead = true })
    unreadCount.value = 0
  } catch (e) {
    console.error(e)
  }
}

async function onSaveProfile() {
  profileError.value = ''
  profileSuccess.value = ''
  savingProfile.value = true
  try {
    const nickname = (form.nickname || '').trim()
    const intro = (form.intro || '').trim()
    if (!nickname || nickname.length < 2 || nickname.length > 20) {
      profileError.value = '昵称长度需在 2-20 个字符之间'
      return
    }
    if (intro.length > 200) {
      profileError.value = '个人简介长度不能超过 200 个字符'
      return
    }
    const params = { nickname, intro }
    const res = await authApi.updateProfile(params)
    if (res.data?.code === 200 && res.data?.data) {
      profileSuccess.value = '资料已保存'
      await auth.fetchProfile()
      await loadProfile()
    } else {
      profileError.value = res.data?.message || '保存失败'
    }
  } catch (e) {
    profileError.value = e.response?.data?.message || e.message || '保存失败'
  } finally {
    savingProfile.value = false
  }
}

async function onChangePassword() {
  pwdError.value = ''
  pwdSuccess.value = ''
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
  changingPwd.value = true
  try {
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
      showPasswordForm.value = false
    } else {
      pwdError.value = res.data?.message || '密码修改失败'
    }
  } catch (e) {
    pwdError.value = e.response?.data?.message || e.message || '密码修改失败'
  } finally {
    changingPwd.value = false
  }
}

async function onSubmitFeedback() {
  feedbackError.value = ''
  feedbackSuccess.value = ''
  const content = (feedbackForm.content || '').trim()
  if (!content) {
    feedbackError.value = '反馈内容不能为空'
    return
  }
  if (content.length > 1000) {
    feedbackError.value = '反馈内容不能超过1000个字符'
    return
  }
  submittingFeedback.value = true
  try {
    const res = await feedbackApi.submitFeedback({ content })
    if (res.data?.code === 200) {
      feedbackSuccess.value = '感谢你的反馈！'
      feedbackForm.content = ''
      setTimeout(() => {
        showFeedbackModal.value = false
      }, 1500)
    } else {
      feedbackError.value = res.data?.message || '提交失败'
    }
  } catch (e) {
    feedbackError.value = e.response?.data?.message || e.message || '提交失败'
  } finally {
    submittingFeedback.value = false
  }
}

onMounted(() => {
  loadProfile()
  loadNotifications()
})
</script>


<style scoped>
.profile-page { min-height: 100vh; padding: 2rem; position: relative; background: var(--bg-base); color: var(--text-main); }
.theme-switcher { position: fixed; top: 72px; right: 2rem; display: flex; gap: 0.5rem; z-index: 50; }
.theme-btn { width: 38px; height: 38px; border-radius: 50%; border: 2px solid var(--border); background: var(--bg-input); color: var(--text-sub); font-size: 1.1rem; cursor: pointer; transition: all 0.25s; display: flex; align-items: center; justify-content: center; }
.theme-btn:hover { border-color: var(--primary); color: var(--primary); }
.theme-btn.active { background: linear-gradient(90deg, var(--primary), var(--primary-light)); border-color: var(--primary); color: #fff; }
.profile-container { max-width: 860px; margin: 0 auto; }
.profile-card { background: var(--bg-card); border: 1px solid var(--border-accent); border-radius: 14px; overflow: hidden; box-shadow: 0 6px 24px var(--shadow); }
.card-header { padding: 2rem; background: var(--bg-hover); border-bottom: 1px solid var(--border-accent); }
.card-header h1 { margin: 0 0 0.4rem; font-size: 1.8rem; background: linear-gradient(90deg, var(--primary), var(--primary-light)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.subtitle { margin: 0; color: var(--text-sub); font-size: 0.9rem; }
.card-content { padding: 2rem; }
.info-section { margin-bottom: 2rem; padding-bottom: 2rem; border-bottom: 1px solid var(--border); }
.info-section:last-child { border-bottom: none; margin-bottom: 0; padding-bottom: 0; }
.section-title { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1.25rem; font-size: 1rem; font-weight: 600; color: var(--text-main); }
.section-title .icon { font-size: 1.3rem; }
.toggle-icon { margin-left: auto; transition: transform 0.3s; font-size: 0.8rem; color: var(--text-muted); }
.toggle-icon.open { transform: rotate(180deg); }
.section-desc { color: var(--text-sub); margin: 0 0 1rem; font-size: 0.9rem; }
.info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 1rem; margin-bottom: 1rem; }
.info-item { display: flex; flex-direction: column; gap: 0.4rem; padding: 1rem; background: var(--bg-input); border-radius: 8px; border: 1px solid var(--border); }
.info-item .label { color: var(--text-muted); font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.5px; }
.info-item .value { color: var(--text-main); font-weight: 600; font-size: 0.95rem; }
.info-item .value.role { display: inline-block; padding: 0.2rem 0.65rem; background: var(--tag-bg); border-radius: 20px; width: fit-content; color: var(--text-sub); }
.info-item .value.role.admin { background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; }
.form-group { margin-bottom: 1.25rem; }
.form-group label { display: block; margin-bottom: 0.4rem; color: var(--text-main); font-weight: 600; font-size: 0.875rem; }
.form-group input, .form-group textarea { width: 100%; padding: 0.7rem 1rem; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-input); color: var(--text-main); font-size: 0.95rem; font-family: inherit; transition: all 0.2s; }
.form-group input:focus, .form-group textarea:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--border-accent); }
.form-group input::placeholder, .form-group textarea::placeholder { color: var(--text-muted); }
.collapsible-content { margin-top: 1rem; padding-top: 1rem; border-top: 1px solid var(--border); }
.error { color: #e53935; font-size: 0.875rem; margin: 0.75rem 0; padding: 0.65rem 0.9rem; background: rgba(229,57,53,0.1); border-left: 3px solid #e53935; border-radius: 4px; }
.success { color: #43a047; font-size: 0.875rem; margin: 0.75rem 0; padding: 0.65rem 0.9rem; background: rgba(67,160,71,0.1); border-left: 3px solid #43a047; border-radius: 4px; }
.btn-primary, .btn-secondary { padding: 0.65rem 1.4rem; border: none; border-radius: 7px; font-size: 0.95rem; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.btn-primary { background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; box-shadow: 0 3px 10px var(--border-accent); }
.btn-primary:hover:not(:disabled) { opacity: 0.88; transform: translateY(-1px); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { background: transparent; border: 1px solid var(--border); color: var(--text-sub); }
.btn-secondary:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); background: var(--bg-hover); }
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }
.loading { text-align: center; color: var(--text-muted); padding: 2rem; }
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: var(--bg-card); border: 1px solid var(--border-accent); border-radius: 14px; width: 90%; max-width: 500px; box-shadow: 0 16px 48px var(--shadow); }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 1.25rem 1.5rem; border-bottom: 1px solid var(--border); }
.modal-header h2 { margin: 0; color: var(--text-main); font-size: 1.1rem; }
.close-btn { background: transparent; border: none; color: var(--text-muted); font-size: 1.4rem; cursor: pointer; transition: color 0.2s; }
.close-btn:hover { color: var(--primary); }
.modal-body { padding: 1.5rem; }
.modal-body textarea { width: 100%; padding: 0.9rem 1rem; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-input); color: var(--text-main); font-size: 0.95rem; font-family: inherit; resize: vertical; transition: all 0.2s; }
.modal-body textarea:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--border-accent); }
.modal-body textarea::placeholder { color: var(--text-muted); }
.char-count { text-align: right; color: var(--text-muted); font-size: 0.82rem; margin-top: 0.4rem; }
.modal-footer { display: flex; gap: 1rem; padding: 1.25rem 1.5rem; border-top: 1px solid var(--border); }
.modal-footer button { flex: 1; }
@media (max-width: 768px) { .profile-page { padding: 1rem; } .theme-switcher { top: auto; bottom: 1.5rem; right: 1rem; } .card-header { padding: 1.25rem; } .card-content { padding: 1.25rem; } .info-grid { grid-template-columns: 1fr; } .modal-content { width: 95%; } }
.notif-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.6rem; }
.notif-item { display: flex; gap: 0.75rem; align-items: flex-start; padding: 0.9rem 1rem; border-radius: 8px; background: var(--bg-input); border: 1px solid var(--border); transition: all 0.2s; }
.notif-item.unread { border-left: 3px solid var(--primary); background: var(--bg-hover); }
.notif-icon { font-size: 1.2rem; flex-shrink: 0; margin-top: 0.1rem; }
.notif-main { flex: 1; min-width: 0; }
.notif-title { font-weight: 700; color: var(--text-main); font-size: 0.9rem; margin-bottom: 0.25rem; }
.notif-msg { font-size: 0.85rem; color: var(--text-sub); line-height: 1.5; }
.notif-meta { display: flex; flex-direction: column; align-items: flex-end; gap: 0.4rem; flex-shrink: 0; }
.notif-time { font-size: 0.75rem; color: var(--text-muted); white-space: nowrap; }
.notif-btn { padding: 0.2rem 0.6rem; border-radius: 4px; border: 1px solid var(--primary); background: transparent; color: var(--primary); font-size: 0.75rem; cursor: pointer; white-space: nowrap; transition: all 0.2s; }
.notif-btn:hover { background: var(--primary); color: #fff; }
.empty-box { text-align: center; padding: 2rem; color: var(--text-muted); font-size: 0.9rem; }
.unread-badge { display: inline-flex; align-items: center; justify-content: center; min-width: 20px; height: 20px; padding: 0 6px; background: #e53935; color: #fff; border-radius: 999px; font-size: 0.72rem; font-weight: 700; margin-left: 0.25rem; }
.btn-mark-all { margin-left: auto; padding: 0.22rem 0.7rem; border-radius: 5px; border: 1px solid var(--border); background: transparent; color: var(--text-muted); font-size: 0.78rem; cursor: pointer; transition: all 0.2s; }
.btn-mark-all:hover { border-color: var(--primary); color: var(--primary); }
</style>
