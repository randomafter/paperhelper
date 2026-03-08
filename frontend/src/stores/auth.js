import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isLoggedIn = computed(() => !!token.value)

  function setAuth(data) {
    token.value = data.token
    user.value = {
      userId: data.userId,
      username: data.username,
      role: data.role,
      nickname: data.nickname,
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  async function fetchProfile() {
    const res = await authApi.getProfile()
    if (res.data?.data && user.value) {
      user.value = { ...user.value, ...res.data.data }
      localStorage.setItem('user', JSON.stringify(user.value))
    }
  }

  return { token, user, isAdmin, isLoggedIn, setAuth, logout, fetchProfile }
})
