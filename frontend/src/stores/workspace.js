import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useWorkspaceStore = defineStore('workspace', () => {
  // 工作台编辑内容
  const content = ref('')
  // 待插入的素材队列（从收藏页引用过来的）
  const pendingInsert = ref(null)

  function insertMaterial(material) {
    pendingInsert.value = material
  }

  function clearPending() {
    pendingInsert.value = null
  }

  function setContent(val) {
    content.value = val
  }

  return { content, pendingInsert, insertMaterial, clearPending, setContent }
}, { persist: false })
