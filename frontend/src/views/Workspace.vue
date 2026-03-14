<template>
  <div class="workspace-shell">
    <!-- 左侧：我的收藏素材栏 -->
    <aside class="material-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <span v-if="!sidebarCollapsed" class="sidebar-title">我的收藏</span>
        <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed" :title="sidebarCollapsed ? '展开素材栏' : '折叠素材栏'">
          {{ sidebarCollapsed ? '"' : '"' }}
        </button>
      </div>
      <template v-if="!sidebarCollapsed">
        <div class="sidebar-search">
          <input v-model="matSearch" placeholder="搜索素材..." class="mat-search" />
        </div>
        <div class="mat-loading" v-if="matLoading">加载中...</div>
        <div class="mat-empty" v-else-if="filteredMats.length === 0">暂无收藏素材</div>
        <div class="mat-list" v-else>
          <div
            v-for="item in filteredMats"
            :key="item.id"
            class="mat-item"
            :class="{ inserting: insertingId === item.id }"
          >
            <div class="mat-item-top">
              <span class="mat-cat">{{ item.category }}</span>
              <button @click="insertMat(item)" class="btn-insert" title="插入到光标位置">插入</button>
            </div>
            <div class="mat-title">{{ item.title }}</div>
            <div class="mat-preview">{{ item.content?.slice(0, 60) }}{{ (item.content?.length||0) > 60 ? '...' : '' }}</div>
          </div>
        </div>
      </template>
    </aside>

    <!-- 主编辑区 -->
    <div class="editor-area">
      <div class="editor-toolbar">
        <input v-model="docTitle" class="doc-title-input" placeholder="文章标题..." />
        <div class="toolbar-actions">
          <span class="word-count">{{ wordCount }} 字</span>
          <button @click="clearEditor" class="btn-tool">清空</button>
          <button @click="copyAll" class="btn-tool">复制全文</button>
        </div>
      </div>

      <!-- 插入提示横幅 -->
      <div v-if="insertBanner" class="insert-banner">
        ✅ 已插入素材「{{ insertBanner }}」
        <button @click="insertBanner = ''" class="banner-close">✕</button>
      </div>

      <textarea
        ref="editorRef"
        v-model="editorContent"
        class="editor-textarea"
        placeholder="在此开始创作...&#10;&#10;从左侧收藏素材中点击「插入」，将素材内容插入到当前光标位置。"
        @keydown.tab.prevent="handleTab"
      ></textarea>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { materialApi } from '../api/material'
import { useWorkspaceStore } from '../stores/workspace'

const workspaceStore = useWorkspaceStore()

const sidebarCollapsed = ref(false)
const matSearch = ref('')
const matLoading = ref(false)
const favorites = ref([])
const insertingId = ref(null)
const insertBanner = ref('')
const docTitle = ref('')
const editorContent = ref(workspaceStore.content || '')
const editorRef = ref(null)

const wordCount = computed(() => {
  return editorContent.value.replace(/\s/g, '').length
})

const filteredMats = computed(() => {
  const kw = matSearch.value.trim().toLowerCase()
  if (!kw) return favorites.value
  return favorites.value.filter(f =>
    f.title.toLowerCase().includes(kw) ||
    f.content?.toLowerCase().includes(kw) ||
    f.category?.toLowerCase().includes(kw)
  )
})

async function loadFavorites() {
  matLoading.value = true
  try {
    const res = await materialApi.getFavorites()
    if (res.data?.code === 200 && res.data?.data) favorites.value = res.data.data
  } catch(e) { console.error(e) } finally { matLoading.value = false }
}

function insertMat(item) {
  insertingId.value = item.id
  setTimeout(() => { insertingId.value = null }, 600)

  const textarea = editorRef.value
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const before = editorContent.value.slice(0, start)
  const after = editorContent.value.slice(end)
  const insertText = `\n【${item.title}】\n${item.content}\n`
  editorContent.value = before + insertText + after

  insertBanner.value = item.title
  setTimeout(() => { insertBanner.value = '' }, 3000)

  // 光标移到插入内容末尾
  const newPos = start + insertText.length
  setTimeout(() => {
    textarea.focus()
    textarea.setSelectionRange(newPos, newPos)
  }, 0)
}

function handleTab(e) {
  const textarea = editorRef.value
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  editorContent.value = editorContent.value.slice(0, start) + '  ' + editorContent.value.slice(end)
  setTimeout(() => textarea.setSelectionRange(start + 2, start + 2), 0)
}

function clearEditor() {
  if (editorContent.value && !confirm('确认清空全部内容？')) return
  editorContent.value = ''
}

async function copyAll() {
  try {
    await navigator.clipboard.writeText(editorContent.value)
    insertBanner.value = '已复制到剪贴板'
    setTimeout(() => { insertBanner.value = '' }, 2000)
  } catch(e) { console.error(e) }
}

// 监听从收藏页引用过来的素材
watch(() => workspaceStore.pendingInsert, (mat) => {
  if (mat) {
    insertMat(mat)
    workspaceStore.clearPending()
  }
}, { immediate: true })

// 自动保存内容到store
watch(editorContent, (val) => { workspaceStore.setContent(val) })

onMounted(loadFavorites)
</script>

<style scoped>
.workspace-shell { display: flex; height: calc(100vh - 60px); overflow: hidden; background: var(--bg-base); }
.material-sidebar { width: 260px; flex-shrink: 0; background: var(--bg-sidebar); border-right: 1px solid var(--border); display: flex; flex-direction: column; transition: width 0.25s; overflow: hidden; }
.material-sidebar.collapsed { width: 36px; }
.sidebar-header { display: flex; align-items: center; justify-content: space-between; padding: 0.75rem 0.75rem 0.5rem; border-bottom: 1px solid var(--border); flex-shrink: 0; }
.sidebar-title { font-size: 0.82rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-muted); white-space: nowrap; }
.collapse-btn { background: transparent; border: none; color: var(--text-muted); cursor: pointer; font-size: 1rem; padding: 0.1rem 0.2rem; border-radius: 4px; transition: color 0.2s; }
.collapse-btn:hover { color: var(--primary); }
.sidebar-search { padding: 0.6rem 0.75rem; flex-shrink: 0; }
.mat-search { width: 100%; padding: 0.45rem 0.7rem; border: 1px solid var(--border); border-radius: 6px; background: var(--bg-input); color: var(--text-main); font-size: 0.85rem; font-family: inherit; }
.mat-search:focus { outline: none; border-color: var(--primary); }
.mat-loading, .mat-empty { text-align: center; padding: 1.5rem; color: var(--text-muted); font-size: 0.85rem; }
.mat-list { flex: 1; overflow-y: auto; padding: 0.5rem 0.5rem; display: flex; flex-direction: column; gap: 0.45rem; }
.mat-item { background: var(--bg-card); border: 1px solid var(--border); border-radius: 8px; padding: 0.7rem 0.75rem; transition: all 0.2s; }
.mat-item:hover { border-color: var(--primary); background: var(--bg-hover); }
.mat-item.inserting { border-color: var(--primary); background: var(--bg-hover); transform: scale(0.98); }
.mat-item-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.3rem; }
.mat-cat { font-size: 0.7rem; padding: 0.1rem 0.45rem; background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; border-radius: 20px; font-weight: 600; }
.btn-insert { padding: 0.18rem 0.55rem; border-radius: 4px; border: 1px solid var(--primary); background: transparent; color: var(--primary); font-size: 0.75rem; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.btn-insert:hover { background: var(--primary); color: #fff; }
.mat-title { font-size: 0.85rem; font-weight: 600; color: var(--text-main); margin-bottom: 0.2rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mat-preview { font-size: 0.75rem; color: var(--text-muted); line-height: 1.4; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.editor-area { flex: 1; display: flex; flex-direction: column; min-width: 0; padding: 1.25rem; gap: 0.75rem; overflow: hidden; }
.editor-toolbar { display: flex; align-items: center; gap: 1rem; flex-shrink: 0; }
.doc-title-input { flex: 1; padding: 0.6rem 1rem; border: 1px solid var(--border); border-radius: 8px; background: var(--bg-input); color: var(--text-main); font-size: 1.1rem; font-weight: 600; font-family: inherit; transition: border-color 0.2s; }
.doc-title-input:focus { outline: none; border-color: var(--primary); }
.doc-title-input::placeholder { color: var(--text-muted); font-weight: 400; }
.toolbar-actions { display: flex; align-items: center; gap: 0.6rem; flex-shrink: 0; }
.word-count { font-size: 0.82rem; color: var(--text-muted); white-space: nowrap; }
.btn-tool { padding: 0.45rem 0.9rem; border-radius: 6px; border: 1px solid var(--border); background: transparent; color: var(--text-sub); font-size: 0.85rem; cursor: pointer; transition: all 0.2s; }
.btn-tool:hover { border-color: var(--primary); color: var(--primary); background: var(--bg-hover); }
.insert-banner { background: var(--bg-hover); border: 1px solid var(--primary); border-radius: 7px; padding: 0.5rem 1rem; font-size: 0.85rem; color: var(--primary); font-weight: 600; display: flex; align-items: center; justify-content: space-between; flex-shrink: 0; animation: slidedown 0.3s ease; }
@keyframes slidedown { from { opacity: 0; transform: translateY(-8px); } to { opacity: 1; transform: translateY(0); } }
.banner-close { background: transparent; border: none; color: var(--primary); cursor: pointer; font-size: 1rem; }
.editor-textarea { flex: 1; resize: none; border: 1px solid var(--border); border-radius: 10px; padding: 1.25rem; background: var(--bg-card); color: var(--text-main); font-size: 0.95rem; font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif; line-height: 1.8; transition: border-color 0.2s; overflow-y: auto; }
.editor-textarea:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--border-accent); }
.editor-textarea::placeholder { color: var(--text-muted); }
</style>
