import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

// 主题只通过 data-theme 切换，具体配色在 App.vue 中统一维护
export const useThemeStore = defineStore('theme', () => {
  const theme = ref(localStorage.getItem('theme') || 'dark')

  function applyTheme(t) {
    const root = document.documentElement
    const next = t === 'light' ? 'light' : 'dark'
    root.setAttribute('data-theme', next)
  }

  function setTheme(newTheme) {
    theme.value = newTheme
    localStorage.setItem('theme', newTheme)
    applyTheme(newTheme)
  }

  watch(theme, (newTheme) => {
    applyTheme(newTheme)
  })

  // 初始化时根据当前值应用主题
  applyTheme(theme.value)

  return {
    theme,
    setTheme,
    applyTheme,
  }
})
