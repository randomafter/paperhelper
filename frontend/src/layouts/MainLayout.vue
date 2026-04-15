<template>
  <div class="layout">
    <header v-if="!isWorkspace" class="header">
      <div class="header-inner">
        <div class="header-left">
          <router-link to="/" class="logo">
            <span class="logo-text">历史小说智能创作助手</span>
          </router-link>
        </div>
        <div class="header-right">
          <div class="user-info">
            <span class="username">{{ auth.user?.nickname || auth.user?.username }}</span>
            <span class="role-badge" :class="{ admin: isAdmin }">{{ isAdmin ? '管理员' : '创作者' }}</span>
          </div>
          <button class="logout-btn" @click="logout">退出登录</button>
        </div>
      </div>
    </header>

    <div class="container" :style="{ marginTop: isWorkspace ? '0' : '56px' }">
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }" :style="{ height: 'calc(100vh - ' + (isWorkspace ? 0 : 56) + 'px)' }">
        <button class="sidebar-toggle" @click="toggleSidebar" :title="sidebarCollapsed ? '展开菜单' : '收起菜单'">
          {{ sidebarCollapsed ? '›' : '‹' }}
        </button>

        <nav class="nav-menu">
          <div class="nav-group">
            <router-link to="/" class="nav-item" :class="{ active: route.path === '/' }" :title="sidebarCollapsed ? '首页' : ''">
              <span class="icon">🏠</span><span class="nav-label">首页</span>
            </router-link>
          </div>

          <div class="nav-group">
            <div class="group-title">素材管理</div>
            <router-link to="/materials" class="nav-item" :class="{ active: route.path === '/materials' }" :title="sidebarCollapsed ? '素材检索' : ''">
              <span class="icon">📚</span><span class="nav-label">素材检索</span>
            </router-link>
            <router-link to="/favorites" class="nav-item" :class="{ active: route.path === '/favorites' }" :title="sidebarCollapsed ? '我的收藏' : ''">
              <span class="icon">❤️</span><span class="nav-label">我的收藏</span>
            </router-link>
          </div>

          <div class="nav-group">
            <div class="group-title">创作中心</div>
            <router-link to="/works" class="nav-item" :class="{ active: route.path === '/works' }" :title="sidebarCollapsed ? '我的作品' : ''">
              <span class="icon">📝</span><span class="nav-label">我的作品</span>
            </router-link>
            <router-link to="/workspace" class="nav-item" :class="{ active: route.path === '/workspace' }" :title="sidebarCollapsed ? '创作工作台' : ''">
              <span class="icon">🎨</span><span class="nav-label">创作工作台</span>
            </router-link>
          </div>

          <div class="nav-group">
            <router-link to="/profile" class="nav-item" :class="{ active: route.path === '/profile' }" :title="sidebarCollapsed ? '个人中心' : ''">
              <span class="icon">👤</span><span class="nav-label">个人中心</span>
              <span v-if="unreadCount > 0 && !sidebarCollapsed" class="nav-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
              <span v-if="unreadCount > 0 && sidebarCollapsed" class="nav-badge-dot"></span>
            </router-link>
          </div>

          <div v-if="isAdmin" class="nav-group">
            <div class="group-title">管理员</div>
            <router-link to="/admin/materials" class="nav-item" :class="{ active: route.path === '/admin/materials' }" :title="sidebarCollapsed ? '素材管理' : ''">
              <span class="icon">⚙️</span><span class="nav-label">素材管理</span>
            </router-link>
            <router-link to="/admin/review" class="nav-item" :class="{ active: route.path === '/admin/review' }" :title="sidebarCollapsed ? '素材审核' : ''">
              <span class="icon">🔍</span><span class="nav-label">素材审核</span>
            </router-link>
            <router-link to="/admin/feedback" class="nav-item" :class="{ active: route.path === '/admin/feedback' }" :title="sidebarCollapsed ? '用户反馈' : ''">
              <span class="icon">💬</span><span class="nav-label">用户反馈</span>
            </router-link>
          </div>
        </nav>
      </aside>

      <main class="main-content" :style="{ height: 'calc(100vh - ' + (isWorkspace ? 0 : 56) + 'px)' }">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { notificationApi } from '../api/notification'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
const isWorkspace = computed(() => route.path === '/workspace')
const unreadCount = ref(0)
const sidebarCollapsed = ref(localStorage.getItem('sidebar_collapsed') === 'true')
let pollTimer = null

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('sidebar_collapsed', sidebarCollapsed.value)
}

async function fetchUnread() {
  try {
    const res = await notificationApi.unreadCount()
    if (res.data?.code === 200) unreadCount.value = res.data.data || 0
  } catch(e) { /* ignore */ }
}

function logout() {
  auth.logout()
  router.push('/login')
}

onMounted(() => {
  fetchUnread()
  pollTimer = setInterval(fetchUnread, 30000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-base);
  color: var(--text-main);
}

.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: var(--bg-header);
  border-bottom: 1px solid var(--border);
  box-shadow: 0 2px 10px var(--shadow);
  height: 56px;
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1rem;
  height: 56px;
  flex-shrink: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  text-decoration: none;
  font-weight: 700;
  font-size: 1rem;
  color: var(--text-main);
  transition: opacity 0.2s;
}
.logo:hover { opacity: 0.82; }
.logo-text {
  color: var(--text-main);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.username {
  font-weight: 600;
  color: var(--text-main);
}
.role-badge {
  padding: 0.2rem 0.7rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  background: var(--tag-bg);
  color: var(--text-sub);
}
.role-badge.admin {
  background: linear-gradient(90deg, var(--primary), var(--primary-light));
  color: #fff;
}
.logout-btn {
  padding: 0.45rem 1.1rem;
  background: linear-gradient(90deg, var(--primary), var(--primary-light));
  border: none;
  border-radius: 6px;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s;
  font-size: 0.9rem;
}
.logout-btn:hover {
  opacity: 0.85;
  transform: translateY(-1px);
}

.container {
  display: flex;
  flex: 1;
  overflow: hidden;
  transition: margin-top 0.25s cubic-bezier(0.4,0,0.2,1);
}

.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border);
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0.75rem 0;
  transition: width 0.25s cubic-bezier(0.4,0,0.2,1), height 0.25s cubic-bezier(0.4,0,0.2,1);
  position: sticky;
  top: 0;
  align-self: flex-start;
}
.sidebar.collapsed { width: 52px; }

.sidebar-toggle {
  display: block;
  width: 28px;
  height: 28px;
  margin: 0.25rem 0.5rem 0.75rem auto;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-muted);
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 28px;
  text-align: center;
}
.sidebar-toggle:hover {
  background: var(--bg-hover);
  color: var(--primary);
  border-color: var(--primary);
}

.sidebar.collapsed .nav-label { display: none; }
.sidebar.collapsed .group-title { display: none; }
.sidebar.collapsed .nav-badge { display: none; }
.sidebar.collapsed .sidebar-toggle { margin-left: 0.5rem; }
.sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 0.65rem;
  border-left: 3px solid transparent;
}
.sidebar.collapsed .nav-item .icon { width: auto; font-size: 1.25rem; }

.nav-badge-dot {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 7px;
  height: 7px;
  background: #e53935;
  border-radius: 50%;
}

.nav-menu { display: flex; flex-direction: column; gap: 0.25rem; }
.nav-group { padding: 0.25rem 0; }
.group-title {
  padding: 0.5rem 1.25rem;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--text-muted);
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 1.25rem;
  color: var(--text-sub);
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
  border-left: 3px solid transparent;
  transition: all 0.2s;
}
.nav-item:hover {
  color: var(--primary);
  background: var(--bg-hover);
  border-left-color: var(--primary);
}
.nav-item.active {
  color: var(--primary);
  background: var(--bg-hover);
  border-left-color: var(--primary);
  font-weight: 600;
}
.nav-item .icon { font-size: 1.1rem; width: 1.4rem; text-align: center; }
.nav-badge { margin-left: auto; display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px; padding: 0 5px; background: #e53935; color: #fff; border-radius: 999px; font-size: 0.68rem; font-weight: 700; }

.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 2rem;
  background: var(--bg-base);
  box-sizing: border-box;
  transition: height 0.25s cubic-bezier(0.4,0,0.2,1);
}
</style>
