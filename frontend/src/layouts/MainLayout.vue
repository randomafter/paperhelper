<template>
  <div class="layout">
    <header class="header">
      <router-link to="/" class="logo">历史特色创作平台</router-link>
      <nav>
        <router-link to="/">首页</router-link>
        <router-link to="/materials">素材库</router-link>
        <router-link v-if="isAdmin" to="/admin/materials">素材管理</router-link>
        <router-link to="/profile">个人中心</router-link>
        <button class="logout" @click="logout">退出</button>
      </nav>
    </header>
    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { computed } from 'vue'

const router = useRouter()
const auth = useAuthStore()

const isAdmin = computed(() => auth.user?.role === 'ADMIN')

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { min-height: 100vh; background: #0f0f1a; color: #e8e8e8; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
  height: 56px;
  background: #1a1a2e;
  border-bottom: 1px solid #2a2a4a;
}
.logo { font-weight: 700; color: #e94560; text-decoration: none; }
.header nav { display: flex; align-items: center; gap: 1.5rem; }
.header nav a { color: #b8b8d0; text-decoration: none; }
.header nav a.router-link-active { color: #e94560; }
.logout { background: transparent; border: 1px solid #555; color: #b8b8d0; padding: 0.4rem 0.8rem; border-radius: 6px; cursor: pointer; }
.logout:hover { border-color: #e94560; color: #e94560; }
.main { padding: 1.5rem; max-width: 1400px; margin: 0 auto; }
</style>
