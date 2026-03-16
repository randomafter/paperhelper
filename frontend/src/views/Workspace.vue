<template>
  <div class="workspace-shell">
    <aside class="material-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <span v-if="!sidebarCollapsed" class="sidebar-title">素材库</span>
        <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">{{ sidebarCollapsed ? '>' : '<' }}</button>
      </div>
      <template v-if="!sidebarCollapsed">
        <div class="source-tabs">
          <button :class="{ active: matSource === 'search' }" @click="matSource = 'search'">检索</button>
          <button :class="{ active: matSource === 'favorite' }" @click="matSource = 'favorite'; loadFavMats()">收藏</button>
        </div>
        <div v-if="matSource === 'search'" class="search-area">
          <select v-model="matCategory" class="mat-select">
            <option value="">全部分类</option>
            <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
          </select>
          <input v-model="matKeyword" placeholder="搜索素材..." class="mat-search" @keyup.enter="searchMats" />
          <button @click="searchMats" class="btn-search-sm">搜索</button>
        </div>
        <div v-if="matSource === 'favorite'" class="search-area">
          <input v-model="favSearch" placeholder="在收藏中搜索..." class="mat-search" />
        </div>
        <div class="mat-loading" v-if="matLoading">加载中...</div>
        <div class="mat-empty" v-else-if="displayMats.length === 0">暂无素材</div>
        <div class="mat-list" v-else>
          <div v-for="item in displayMats" :key="item.id" class="mat-item" :class="{ inserting: insertingId === item.id }">
            <div class="mat-item-top">
              <span class="mat-cat">{{ item.category }}</span>
              <button @click="insertMat(item)" class="btn-insert">插入</button>
            </div>
            <div class="mat-title">{{ item.title }}</div>
            <div class="mat-preview">{{ item.content?.slice(0,60) }}{{ (item.content?.length||0)>60?'...':'' }}</div>
          </div>
          <div v-if="matSource==='search' && matTotalPages>1" class="mat-pagination">
            <button @click="prevMatPage" :disabled="matPage<=1">‹</button>
            <span>{{ matPage }}/{{ matTotalPages }}</span>
            <button @click="nextMatPage" :disabled="matPage>=matTotalPages">›</button>
          </div>
        </div>
      </template>
    </aside>

    <div class="editor-area">
      <div class="editor-toolbar">
        <button @click="goWorks" class="btn-back">‹ 我的作品</button>
        <input v-model="docTitle" class="doc-title-input" placeholder="文章标题...">
        <div class="toolbar-actions">
          <span class="word-count">{{ wordCount }} 字</span>
          <span v-if="saveStatus" class="save-status" :class="saveStatus">{{ saveStatusText }}</span>
          <button @click="saveWork" :disabled="saving" class="btn-save">{{ saving ? '保存中...' : '保存' }}</button>
          <button @click="clearEditor" class="btn-tool">清空</button>
          <button @click="copyAll" class="btn-tool">复制</button>
        </div>
      </div>

      <div v-if="insertBanner" class="insert-banner">
        ✅ 已插入「{{ insertBanner }}」
        <button @click="insertBanner = ''" class="banner-close">✕</button>
      </div>

      <!-- 自动保存进度条 -->
      <div class="autosave-bar" :class="autoSaveState">
        <div class="autosave-progress" :style="{ width: autoSaveProgress + '%' }"></div>
        <span class="autosave-hint">
          <template v-if="autoSaveState === 'counting'">💾 {{ autoSaveCountdown }}秒后自动保存...</template>
          <template v-else-if="autoSaveState === 'saving'">⏳ 自动保存中...</template>
          <template v-else-if="autoSaveState === 'saved'">✓ 已自动保存</template>
          <template v-else-if="autoSaveState === 'idle' && workId">&nbsp;</template>
        </span>
      </div>

      <div class="rich-toolbar">
        <button @click="formatText('bold')" class="rich-btn" title="加粗"><b>B</b></button>
        <button @click="formatText('italic')" class="rich-btn" title="斜体"><i>I</i></button>
        <button @click="insertDivider" class="rich-btn" title="分隔线">—</button>
        <button @click="insertNewPara" class="rich-btn" title="新段落">¶</button>
        <span class="rich-sep">|</span>
        <button @click="fontSizeChange(-1)" class="rich-btn">A-</button>
        <span class="font-size-label">{{ fontSize }}px</span>
        <button @click="fontSizeChange(1)" class="rich-btn">A+</button>
        <span class="rich-sep">|</span>
        <select v-model="lineHeightVal" class="rich-select">
          <option value="1.6">行距 1.6</option>
          <option value="1.8">行距 1.8</option>
          <option value="2.0">行距 2.0</option>
          <option value="2.4">行距 2.4</option>
        </select>
      </div>

      <textarea
        ref="editorRef"
        v-model="editorContent"
        class="editor-textarea"
        :style="{ fontSize: fontSize + 'px', lineHeight: lineHeightVal }"
        placeholder="在此开始创作...&#10;&#10;从左侧点击「插入」将素材内容插入到光标位置。"
        @keydown.tab.prevent="handleTab"
        @input="startAutoSave"
      ></textarea>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { materialApi } from '../api/material'
import { worksApi } from '../api/works'
import { useWorkspaceStore } from '../stores/workspace'

const router = useRouter()
const route = useRoute()
const workspaceStore = useWorkspaceStore()

const workId = ref(null)
const saving = ref(false)
const saveStatus = ref('')
const saveStatusText = computed(() =>
  ({ saving: '保存中...', saved: '✓ 已保存', error: '✗ 保存失败' })[saveStatus.value] || ''
)
let autoSaveTimer = null
let countdownTimer = null

// 自动保存状态：idle / counting / saving / saved
const autoSaveState = ref('idle')
const autoSaveProgress = ref(0)
const autoSaveCountdown = ref(10)
const AUTO_SAVE_DELAY = 10 // 秒

const docTitle = ref('')
const editorContent = ref('')
const editorRef = ref(null)
const insertBanner = ref('')
const insertingId = ref(null)
const fontSize = ref(16)
const lineHeightVal = ref('1.8')

const sidebarCollapsed = ref(false)
const matSource = ref('search')
const matCategory = ref('')
const matKeyword = ref('')
const matLoading = ref(false)
const searchResults = ref([])
const matPage = ref(1)
const matTotal = ref(0)
const matPageSize = 10
const matTotalPages = computed(() => Math.ceil(matTotal.value / matPageSize) || 1)
const favorites = ref([])
const favSearch = ref('')

const categories = [
  '历史沉淀','传统民俗','服饰装扮','行业手艺','宗教信仰',
  '兵器武林','饮食文化','玉石珍宝','传说典故','科技文明','五行异象','其他',
]

const wordCount = computed(() => editorContent.value.replace(/\s/g,'').length)

const displayMats = computed(() => {
  if (matSource.value === 'favorite') {
    const kw = favSearch.value.trim().toLowerCase()
    return kw ? favorites.value.filter(f =>
      f.title.toLowerCase().includes(kw) || f.content?.toLowerCase().includes(kw)
    ) : favorites.value
  }
  return searchResults.value
})

async function searchMats() {
  matLoading.value = true
  try {
    const res = await materialApi.search({
      category: matCategory.value || undefined,
      keyword: matKeyword.value || undefined,
      page: matPage.value, size: matPageSize,
    })
    if (res.data?.code === 200 && res.data?.data) {
      searchResults.value = res.data.data.records || []
      matTotal.value = res.data.data.total || 0
    }
  } catch(e) { console.error(e) } finally { matLoading.value = false }
}

function prevMatPage() { if (matPage.value > 1) { matPage.value--; searchMats() } }
function nextMatPage() { if (matPage.value < matTotalPages.value) { matPage.value++; searchMats() } }

async function loadFavMats() {
  matLoading.value = true
  try {
    const res = await materialApi.getFavorites()
    if (res.data?.code === 200 && res.data?.data) favorites.value = res.data.data
  } catch(e) { console.error(e) } finally { matLoading.value = false }
}

async function loadWork(id) {
  try {
    const res = await worksApi.get(id)
    if (res.data?.code === 200 && res.data?.data) {
      docTitle.value = res.data.data.title || '未命名'
      editorContent.value = res.data.data.content || ''
      workId.value = res.data.data.id
    }
  } catch(e) { console.error(e) }
}

async function saveWork() {
  saving.value = true; saveStatus.value = 'saving'
  try {
    let res
    if (!workId.value) {
      // 无作品ID时先创建
      res = await worksApi.create({
        title: docTitle.value || '未命名',
        content: editorContent.value,
      })
      if (res.data?.code === 200 && res.data?.data?.id) {
        workId.value = res.data.data.id
        // 更新URL，不刷新页面
        router.replace({ path: '/workspace', query: { workId: workId.value } })
      }
    } else {
      res = await worksApi.save(workId.value, {
        title: docTitle.value || '未命名',
        content: editorContent.value,
      })
    }
    if (res.data?.code === 200) {
      saveStatus.value = 'saved'
      setTimeout(() => { saveStatus.value = '' }, 2500)
    } else {
      saveStatus.value = 'error'
      setTimeout(() => { saveStatus.value = '' }, 3000)
    }
  } catch(e) {
    saveStatus.value = 'error'
    setTimeout(() => { saveStatus.value = '' }, 3000)
  } finally { saving.value = false }
}

function startAutoSave() {
  // 清除已有定时器
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  if (countdownTimer) clearInterval(countdownTimer)

  autoSaveState.value = 'counting'
  autoSaveCountdown.value = AUTO_SAVE_DELAY
  autoSaveProgress.value = 0

  // 进度条动画
  const startTime = Date.now()
  countdownTimer = setInterval(() => {
    const elapsed = (Date.now() - startTime) / 1000
    autoSaveCountdown.value = Math.max(0, Math.ceil(AUTO_SAVE_DELAY - elapsed))
    autoSaveProgress.value = Math.min(100, (elapsed / AUTO_SAVE_DELAY) * 100)
    if (elapsed >= AUTO_SAVE_DELAY) {
      clearInterval(countdownTimer)
    }
  }, 200)

  autoSaveTimer = setTimeout(async () => {
    clearInterval(countdownTimer)
    autoSaveProgress.value = 100
    autoSaveState.value = 'saving'
    await saveWork()
    autoSaveState.value = 'saved'
    setTimeout(() => { autoSaveState.value = 'idle'; autoSaveProgress.value = 0 }, 2500)
  }, AUTO_SAVE_DELAY * 1000)
}

function insertMat(item) {
  insertingId.value = item.id
  setTimeout(() => { insertingId.value = null }, 600)
  const ta = editorRef.value; if (!ta) return
  const s = ta.selectionStart, e = ta.selectionEnd
  const ins = `\n【${item.title}】\n${item.content}\n`
  editorContent.value = editorContent.value.slice(0,s) + ins + editorContent.value.slice(e)
  insertBanner.value = item.title
  setTimeout(() => { insertBanner.value = '' }, 3000)
  const np = s + ins.length
  setTimeout(() => { ta.focus(); ta.setSelectionRange(np,np) }, 0)
}

function formatText(type) {
  const ta = editorRef.value; if (!ta) return
  const s = ta.selectionStart, e = ta.selectionEnd
  const sel = editorContent.value.slice(s,e); if (!sel) return
  const w = type === 'bold' ? `**${sel}**` : `*${sel}*`
  editorContent.value = editorContent.value.slice(0,s) + w + editorContent.value.slice(e)
  setTimeout(() => ta.setSelectionRange(s, s+w.length), 0)
}

function insertDivider() {
  const ta = editorRef.value; if (!ta) return
  const p = ta.selectionStart
  const d = '\n\n————————————\n\n'
  editorContent.value = editorContent.value.slice(0,p) + d + editorContent.value.slice(p)
  setTimeout(() => ta.setSelectionRange(p+d.length, p+d.length), 0)
}

function insertNewPara() {
  const ta = editorRef.value; if (!ta) return
  const p = ta.selectionStart
  editorContent.value = editorContent.value.slice(0,p) + '\n\n' + editorContent.value.slice(p)
  setTimeout(() => ta.setSelectionRange(p+2, p+2), 0)
}

function fontSizeChange(d) { fontSize.value = Math.max(12, Math.min(28, fontSize.value + d)) }

function handleTab() {
  const ta = editorRef.value; if (!ta) return
  const s = ta.selectionStart
  editorContent.value = editorContent.value.slice(0,s) + '　' + editorContent.value.slice(s)
  setTimeout(() => ta.setSelectionRange(s+1, s+1), 0)
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

function goWorks() { router.push('/works') }

// 监听从收藏页引用过来的素材
watch(() => workspaceStore.pendingInsert, (mat) => {
  if (mat) { insertMat(mat); workspaceStore.clearPending() }
}, { immediate: true })

onMounted(() => {
  const wid = route.query.workId
  if (wid) {
    workId.value = Number(wid)
    loadWork(workId.value)
  }
  searchMats()
})

onBeforeUnmount(() => {
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.workspace-shell { display: flex; height: calc(100vh - 60px); overflow: hidden; background: var(--bg-base); }

/* 侧边栏 */
.material-sidebar { width: 270px; flex-shrink: 0; background: var(--bg-sidebar); border-right: 1px solid var(--border); display: flex; flex-direction: column; transition: width 0.25s; overflow: hidden; }
.material-sidebar.collapsed { width: 36px; }
.sidebar-header { display: flex; align-items: center; justify-content: space-between; padding: 0.75rem; border-bottom: 1px solid var(--border); flex-shrink: 0; }
.sidebar-title { font-size: 0.8rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-muted); }
.collapse-btn { background: transparent; border: none; color: var(--text-muted); cursor: pointer; font-size: 1rem; padding: 0.1rem 0.3rem; border-radius: 4px; }
.collapse-btn:hover { color: var(--primary); }
.source-tabs { display: flex; border-bottom: 1px solid var(--border); flex-shrink: 0; }
.source-tabs button { flex: 1; padding: 0.5rem; border: none; background: transparent; color: var(--text-muted); font-size: 0.82rem; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -1px; transition: all 0.2s; }
.source-tabs button.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 600; }
.search-area { padding: 0.6rem 0.65rem; display: flex; flex-direction: column; gap: 0.4rem; flex-shrink: 0; border-bottom: 1px solid var(--border); }
.mat-select { width: 100%; padding: 0.4rem 0.65rem; border: 1px solid var(--border); border-radius: 6px; background: var(--bg-input); color: var(--text-main); font-size: 0.82rem; }
.mat-select:focus { outline: none; border-color: var(--primary); }
.mat-search { width: 100%; padding: 0.4rem 0.65rem; border: 1px solid var(--border); border-radius: 6px; background: var(--bg-input); color: var(--text-main); font-size: 0.82rem; font-family: inherit; box-sizing: border-box; }
.mat-search:focus { outline: none; border-color: var(--primary); }
.btn-search-sm { padding: 0.35rem 0.75rem; border-radius: 5px; border: none; background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; font-size: 0.8rem; font-weight: 600; cursor: pointer; }
.mat-loading, .mat-empty { text-align: center; padding: 1.5rem; color: var(--text-muted); font-size: 0.82rem; }
.mat-list { flex: 1; overflow-y: auto; padding: 0.5rem; display: flex; flex-direction: column; gap: 0.4rem; }
.mat-item { background: var(--bg-card); border: 1px solid var(--border); border-radius: 7px; padding: 0.65rem 0.7rem; transition: all 0.2s; }
.mat-item:hover { border-color: var(--primary); background: var(--bg-hover); }
.mat-item.inserting { border-color: var(--primary); transform: scale(0.97); }
.mat-item-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.25rem; }
.mat-cat { font-size: 0.68rem; padding: 0.08rem 0.4rem; background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; border-radius: 20px; font-weight: 600; }
.btn-insert { padding: 0.15rem 0.5rem; border-radius: 4px; border: 1px solid var(--primary); background: transparent; color: var(--primary); font-size: 0.72rem; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.btn-insert:hover { background: var(--primary); color: #fff; }
.mat-title { font-size: 0.82rem; font-weight: 600; color: var(--text-main); margin-bottom: 0.18rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mat-preview { font-size: 0.72rem; color: var(--text-muted); line-height: 1.4; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.mat-pagination { display: flex; align-items: center; justify-content: center; gap: 0.6rem; padding: 0.5rem; border-top: 1px solid var(--border); margin-top: 0.25rem; }
.mat-pagination button { padding: 0.2rem 0.6rem; border-radius: 4px; border: 1px solid var(--border); background: transparent; color: var(--text-sub); cursor: pointer; font-size: 0.9rem; }
.mat-pagination button:disabled { opacity: 0.35; cursor: not-allowed; }
.mat-pagination span { font-size: 0.78rem; color: var(--text-muted); }

/* 编辑区 */
.editor-area { flex: 1; display: flex; flex-direction: column; min-width: 0; overflow: hidden; }
.editor-toolbar { display: flex; align-items: center; gap: 0.75rem; padding: 0.65rem 1rem; border-bottom: 1px solid var(--border); background: var(--bg-card); flex-shrink: 0; }
.btn-back { padding: 0.35rem 0.8rem; border-radius: 6px; border: 1px solid var(--border); background: transparent; color: var(--text-sub); font-size: 0.85rem; cursor: pointer; white-space: nowrap; transition: all 0.2s; }
.btn-back:hover { border-color: var(--primary); color: var(--primary); }
.doc-title-input { flex: 1; padding: 0.5rem 0.85rem; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-input); color: var(--text-main); font-size: 1.05rem; font-weight: 600; font-family: inherit; transition: border-color 0.2s; }
.doc-title-input:focus { outline: none; border-color: var(--primary); }
.doc-title-input::placeholder { color: var(--text-muted); font-weight: 400; }
.toolbar-actions { display: flex; align-items: center; gap: 0.5rem; flex-shrink: 0; }
.word-count { font-size: 0.8rem; color: var(--text-muted); white-space: nowrap; }
.save-status { font-size: 0.8rem; white-space: nowrap; }
.save-status.saved { color: #43a047; }
.save-status.saving { color: var(--text-muted); }
.save-status.error { color: #e53935; }
.btn-save { padding: 0.4rem 1rem; border-radius: 6px; border: none; background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; font-weight: 700; font-size: 0.85rem; cursor: pointer; transition: opacity 0.2s; white-space: nowrap; }
.btn-save:hover:not(:disabled) { opacity: 0.85; }
.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-tool { padding: 0.4rem 0.8rem; border-radius: 6px; border: 1px solid var(--border); background: transparent; color: var(--text-sub); font-size: 0.85rem; cursor: pointer; transition: all 0.2s; }
.btn-tool:hover { border-color: var(--primary); color: var(--primary); background: var(--bg-hover); }
.insert-banner { background: var(--bg-hover); border-bottom: 1px solid var(--primary); padding: 0.4rem 1rem; font-size: 0.85rem; color: var(--primary); font-weight: 600; display: flex; align-items: center; justify-content: space-between; flex-shrink: 0; animation: slidedown 0.3s ease; }
@keyframes slidedown { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: translateY(0); } }
.banner-close { background: transparent; border: none; color: var(--primary); cursor: pointer; font-size: 1rem; }

/* 自动保存进度条 */
.autosave-bar { position: relative; height: 24px; background: var(--bg-input); border-bottom: 1px solid var(--border); display: flex; align-items: center; flex-shrink: 0; overflow: hidden; transition: opacity 0.3s; }
.autosave-bar.idle { opacity: 0; pointer-events: none; }
.autosave-bar.counting { opacity: 1; }
.autosave-bar.saving { opacity: 1; }
.autosave-bar.saved { opacity: 1; }
.autosave-progress { position: absolute; left: 0; top: 0; bottom: 0; background: linear-gradient(90deg, var(--primary), var(--primary-light)); opacity: 0.18; transition: width 0.2s linear; pointer-events: none; }
.autosave-bar.saved .autosave-progress { background: #43a047; opacity: 0.2; }
.autosave-bar.saving .autosave-progress { opacity: 0.25; animation: shimmer 1s ease-in-out infinite; }
@keyframes shimmer { 0%,100% { opacity: 0.2; } 50% { opacity: 0.35; } }
.autosave-hint { position: relative; z-index: 1; padding: 0 1rem; font-size: 0.75rem; color: var(--text-muted); white-space: nowrap; }
.autosave-bar.saved .autosave-hint { color: #43a047; font-weight: 600; }
.autosave-bar.saving .autosave-hint { color: var(--primary); }
.rich-toolbar { display: flex; align-items: center; gap: 0.25rem; padding: 0.4rem 1rem; border-bottom: 1px solid var(--border); background: var(--bg-input); flex-shrink: 0; flex-wrap: wrap; }
.rich-btn { padding: 0.25rem 0.6rem; border-radius: 4px; border: 1px solid var(--border); background: transparent; color: var(--text-sub); font-size: 0.85rem; cursor: pointer; transition: all 0.2s; min-width: 28px; }
.rich-btn:hover { border-color: var(--primary); color: var(--primary); background: var(--bg-hover); }
.rich-sep { color: var(--border); padding: 0 0.2rem; }
.font-size-label { font-size: 0.78rem; color: var(--text-muted); min-width: 36px; text-align: center; }
.rich-select { padding: 0.25rem 0.5rem; border: 1px solid var(--border); border-radius: 4px; background: var(--bg-input); color: var(--text-main); font-size: 0.8rem; cursor: pointer; }
.rich-select:focus { outline: none; border-color: var(--primary); }
.editor-textarea { flex: 1; resize: none; border: none; border-radius: 0; padding: 1.5rem 2rem; background: var(--bg-card); color: var(--text-main); font-family: 'Segoe UI', 'Microsoft YaHei', 'SimSun', sans-serif; transition: none; overflow-y: auto; }
.editor-textarea:focus { outline: none; }
.editor-textarea::placeholder { color: var(--text-muted); }
</style>
