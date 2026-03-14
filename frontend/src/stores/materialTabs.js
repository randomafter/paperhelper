import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useMaterialTabsStore = defineStore('materialTabs', () => {
  // 已打开的素材标签，用于右侧详情抽屉和创作中心复用
  const tabs = ref([]) // { id, title }
  const activeId = ref(null)

  function openTab(material) {
    if (!material || !material.id) return
    if (!tabs.value.find(t => t.id === material.id)) {
      tabs.value.push({ id: material.id, title: material.title || `素材 #${material.id}` })
    }
    activeId.value = material.id
  }

  function closeTab(id) {
    tabs.value = tabs.value.filter(t => t.id !== id)
    if (activeId.value === id) {
      activeId.value = tabs.value.length ? tabs.value[tabs.value.length - 1].id : null
    }
  }

  function clearTabs() {
    tabs.value = []
    activeId.value = null
  }

  return {
    tabs,
    activeId,
    openTab,
    closeTab,
    clearTabs,
  }
})

