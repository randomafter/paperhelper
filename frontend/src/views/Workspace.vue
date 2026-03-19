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
              <div class="mat-actions">
                <button @click="applyToAI(item)" class="btn-ai-apply" :class="{ active: boundMat?.id === item.id }" title="应用到AI">✦ AI</button>
                <button @click="insertMat(item)" class="btn-insert">插入</button>
              </div>
            </div>
            <div class="mat-title" @click="openMatDetail(item)" style="cursor:pointer">{{ item.title }}</div>
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

      <div v-if="boundMat" class="ai-bound-banner">
        ✦ 已为 AI 绑定素材：<strong>{{ boundMat.title }}</strong>
        <button @click="boundMat = null" class="banner-close">✕</button>
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

      <!-- 悬浮 AI 助手 -->
      <div class="ai-float-wrap">
        <button class="ai-float-btn" @click="aiPanelOpen = !aiPanelOpen" :class="{ active: aiPanelOpen }" title="历史 AI 助手">
          <span>✦</span><span>AI</span>
        </button>
        <transition name="ai-pop">
        <div v-if="aiPanelOpen" class="ai-float-panel">
          <div class="ai-fp-header">
            <span class="ai-fp-title">✦ 历史助手</span>
            <button class="ai-fp-close" @click="aiPanelOpen = false">✕</button>
          </div>
          <div class="ai-fp-tabs">
            <button v-for="t in aiTabs" :key="t.key" :class="{ active: aiTab === t.key }" @click="aiTab = t.key; aiResult = ''; aiError = ''">{{ t.icon }} {{ t.label }}</button>
          </div>
          <div class="ai-fp-body">
            <div v-if="aiTab === 'scene'" class="ai-fp-form">
              <input v-model="sceneInput" class="ai-fp-input" placeholder="场景要素，如：盛唐曲江池，暮春，贵族宴饮" />
              <select v-model="sceneStyle" class="ai-fp-input">
                <option value="典雅">典雅古风</option><option value="清丽">清丽婉约</option>
                <option value="雄浑">雄浑壮阔</option><option value="写实">写实细腻</option>
              </select>
              <button @click="runScene" :disabled="aiLoading" class="ai-fp-run">{{ aiLoading ? '生成中...' : '生成描写' }}</button>
            </div>
            <div v-if="aiTab === 'polish'" class="ai-fp-form">
              <textarea v-model="polishText" class="ai-fp-textarea" placeholder="粘贴要润色的文段"></textarea>
              <button @click="polishText = editorContent.slice(0,600)" class="ai-fp-ghost">引用当前文章（前600字）</button>
              <button @click="runPolish" :disabled="aiLoading" class="ai-fp-run">{{ aiLoading ? '润色中...' : '开始润色' }}</button>
            </div>
            <div v-if="aiTab === 'check'" class="ai-fp-form">
              <textarea v-model="checkText" class="ai-fp-textarea" placeholder="粘贴要检测的文段"></textarea>
              <button @click="checkText = editorContent.slice(0,800)" class="ai-fp-ghost">引用当前文章（前800字）</button>
              <button @click="runCheck" :disabled="aiLoading" class="ai-fp-run">{{ aiLoading ? '检测中...' : '检测错误' }}</button>
            </div>
            <div v-if="aiTab === 'detail'" class="ai-fp-form">
              <input v-model="detailQ" class="ai-fp-input" placeholder="如：唐代七品官年俸是多少？" @keyup.enter="runDetail" />
              <button @click="runDetail" :disabled="aiLoading" class="ai-fp-run">{{ aiLoading ? '查询中...' : '查询' }}</button>
            </div>
            <div v-if="aiLoading" class="ai-fp-loading"><div class="ai-fp-spinner"></div><span>星火思考中...</span></div>
            <div v-if="aiError" class="ai-fp-error">{{ aiError }}</div>
            <div v-if="aiResult" class="ai-fp-result">
              <div class="ai-fp-result-bar">
                <span>AI 结果</span>
                <div><button @click="insertAiResult" class="ai-fp-insert">插入编辑器</button><button @click="aiResult='';aiError=''" class="ai-fp-clear">清除</button></div>
              </div>
              <pre class="ai-fp-result-text">{{ aiResult }}</pre>
            </div>
          </div>
        </div>
        </transition>
      </div>
    </div>
  </div>

  <!-- 素材详情弹窗 -->
  <transition name="modal-fade">
  <div v-if="matDetail" class="mat-modal-mask" @click.self="matDetail = null">
    <div class="mat-modal">
      <div class="mat-modal-header">
        <span class="mat-modal-cat">{{ matDetail.category }}</span>
        <button class="mat-modal-close" @click="matDetail = null">✕</button>
      </div>
      <h3 class="mat-modal-title">{{ matDetail.title }}</h3>
      <div class="mat-modal-tags" v-if="matDetail.tags?.length">
        <span v-for="tag in matDetail.tags" :key="tag" class="mat-tag">{{ tag }}</span>
      </div>
      <pre class="mat-modal-content">{{ matDetail.content }}</pre>
      <div class="mat-modal-actions">
        <button @click="applyToAI(matDetail); matDetail = null" class="btn-apply-ai">✦ 应用到 AI</button>
        <button @click="insertMat(matDetail); matDetail = null" class="btn-insert-modal">插入编辑器</button>
      </div>
    </div>
  </div>
  </transition>
</template>
<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { materialApi } from '../api/material'
import { userMaterialApi } from '../api/userMaterial'
import { worksApi } from '../api/works'
import { useWorkspaceStore } from '../stores/workspace'
import axios from 'axios'

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

const categories = ref([])

async function loadCategories() {
  try {
    const res = await import('../api/category').then(m => m.categoryApi.list())
    if (res.data?.code === 200) categories.value = (res.data.data || []).map(c => c.name)
  } catch(e) { console.error(e) }
}

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
    const [favRes, mineRes] = await Promise.all([
      materialApi.getFavorites(),
      userMaterialApi.list(),
    ])
    const favList = (favRes.data?.code === 200 && favRes.data?.data) ? favRes.data.data : []
    const mineMats = (mineRes.data?.code === 200 && mineRes.data?.data)
      ? mineRes.data.data.map(m => ({
          id: `mine_${m.id}`,
          category: m.category,
          title: m.title,
          content: m.content,
          tags: m.tags ? m.tags.split(',').map(t => t.trim()).filter(Boolean) : [],
          favoriteGroup: m.favoriteGroup || '我的灵感',
          _source: 'mine',
          _mineId: m.id,
          _status: m.status,
        }))
      : []
    const existingTitles = new Set(favList.map(f => f.title))
    const uniqueMine = mineMats.filter(m => !existingTitles.has(m.title))
    favorites.value = [...favList, ...uniqueMine]
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
  if (!workId.value) return
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

// 监听从收藏页应用到AI的素材
watch(() => workspaceStore.pendingAI, (mat) => {
  if (mat) { boundMat.value = mat; aiPanelOpen.value = true; workspaceStore.clearPendingAI() }
}, { immediate: true })

// ── 历史助手 ──────────────────────────────────────────────────
const aiPanelOpen = ref(false)
const aiTab = ref('scene')
const aiTabs = [
  { key: 'scene',  icon: '✍️', label: '生成情节' },
  { key: 'polish', icon: '✨', label: 'AI润色' },
  { key: 'check',  icon: '⚠️', label: '穿越警报' },
  { key: 'detail', icon: '📖', label: '素材问答' },
]
const aiLoading = ref(false)
const aiResult  = ref('')
const aiError   = ref('')

// 时空背景
const ctxYear  = ref('')
const ctxPlace = ref('')
// 穿越警报
const checkText = ref('')
// 细节查询
const detailQ = ref('')
// 环境描写
const sceneInput = ref('')
const sceneStyle = ref('典雅')
// AI润色
const polishText = ref('')
// 绑定素材
const boundMat = ref(null)
// 素材详情弹窗
const matDetail = ref(null)

function applyToAI(item) {
  boundMat.value = item
  aiPanelOpen.value = true
}

function openMatDetail(item) {
  matDetail.value = item
}

// 提示词模板
const PROMPTS = {
  context: (year, place) =>
    `你是专业的中国历史顾问，服务于历史小说创作者。
请根据以下时空信息，用结构化方式输出创作参考（不超过400字）：
时间：${year}年，地点：${place}

请按以下维度回答：
① 政治格局（皇帝/朝代/重大事件）
② 社会制度（官制/科举/兵制要点）
③ 日常生活（服饰特征/饮食习惯）
④ 物价参考（米价/布价/常见物价）
⑤ 创作提示（该时空写作需注意的禁忌或特色）
语言简洁，以条目形式呈现，方便作者快速参考。`,

  check: (text) =>
    `你是历史考证专家，专门为历史小说作者纠错。
请检查以下文段中的历史错误，包括但不限于：食材/器物使用时间错误、词汇/称谓时代错误、制度/礼仪不符等。

文段：
${text}

请按以下格式输出：
【错误点 N】原文："xxx" → 问题：xxx → 建议修改为：xxx

若无明显错误，请回复"未发现明显时代错误，但建议注意……"并给出1-2条创作建议。`,

  detail: (q) =>
    `你是中国历史百科专家，服务于历史小说创作。
请简洁准确地回答以下历史细节问题（200字以内），并在末尾注明朝代适用范围：

问题：${q}

要求：数据准确、来源可靠、实用性强，直接给出答案。`,

  scene: (scene, style) =>
    `你是擅长历史题材的文学作家，文风${style}。
请根据以下场景要素，生成一段150-250字的环境描写，要求：
- 历史细节准确（器物、植物、建筑符合时代）
- 调动视觉、听觉、嗅觉多种感官
- 语言${style}，可直接嵌入小说正文
- 不出现现代词汇

场景要素：${scene}`,
}

async function callSpark(prompt) {
  aiLoading.value = true
  aiResult.value = ''
  aiError.value = ''
  try {
    const token = localStorage.getItem('token')
    const payload = { prompt }
    if (boundMat.value?.id) payload.materialId = boundMat.value.id
    const res = await axios.post('/api/spark/generate',
      payload,
      { headers: { Authorization: `Bearer ${token}` } }
    )
    if (res.data?.code === 200) {
      aiResult.value = res.data.data?.result || ''
    } else {
      aiError.value = res.data?.message || '请求失败'
    }
  } catch(e) {
    aiError.value = e.response?.data?.message || '网络错误，请检查后端服务'
  } finally {
    aiLoading.value = false
  }
}

function runContext() {
  if (!ctxYear.value.trim() || !ctxPlace.value.trim()) { aiError.value = '请填写年份和地点'; return }
  callSpark(PROMPTS.context(ctxYear.value.trim(), ctxPlace.value.trim()))
}
function runCheck() {
  if (!checkText.value.trim()) { aiError.value = '请输入要检查的文段'; return }
  callSpark(PROMPTS.check(checkText.value.trim()))
}
function runDetail() {
  if (!detailQ.value.trim()) { aiError.value = '请输入问题'; return }
  callSpark(PROMPTS.detail(detailQ.value.trim()))
}
function runScene() {
  if (!sceneInput.value.trim()) { aiError.value = '请输入场景要素'; return }
  callSpark(PROMPTS.scene(sceneInput.value.trim(), sceneStyle.value))
}
function runPolish() {
  if (!polishText.value.trim()) { aiError.value = '请输入要润色的文段'; return }
  const prompt = `你是专业的历史文学编辑，请对以下文段进行润色，保持原意，提升文学性和流畅度，语言典雅，直接返回润色后的文本：\n\n${polishText.value.trim()}`
  callSpark(prompt)
}

function insertAiResult() {
  if (!aiResult.value || !editorRef.value) return
  const el = editorRef.value
  const start = el.selectionStart
  const before = editorContent.value.slice(0, start)
  const after  = editorContent.value.slice(start)
  editorContent.value = before + '\n\n' + aiResult.value + '\n\n' + after
  insertBanner.value = 'AI助手结果'
  setTimeout(() => insertBanner.value = '', 2000)
}

onMounted(() => {
  const wid = route.query.workId
  if (wid) {
    workId.value = Number(wid)
    loadWork(workId.value)
  }
  searchMats()
  loadCategories()
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

/* ── 悬浮 AI 助手 ─────────────────────────────────────────── */
.ai-float-wrap { position: absolute; bottom: 1.5rem; right: 1.5rem; z-index: 100; display: flex; flex-direction: column; align-items: flex-end; gap: 0.5rem; }
.editor-area { position: relative; }
.ai-float-btn { width: 52px; height: 52px; border-radius: 50%; border: none; background: linear-gradient(135deg, var(--primary), var(--primary-light)); color: #fff; font-size: 0.78rem; font-weight: 800; cursor: pointer; box-shadow: 0 4px 18px rgba(0,0,0,0.18); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 1px; transition: transform 0.2s, box-shadow 0.2s; }
.ai-float-btn:hover, .ai-float-btn.active { transform: scale(1.1); box-shadow: 0 6px 24px rgba(0,0,0,0.28); }
.ai-float-btn span:first-child { font-size: 1rem; line-height: 1; }
.ai-float-btn span:last-child { font-size: 0.62rem; letter-spacing: 1px; }
.ai-float-panel { width: 320px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 14px; box-shadow: 0 8px 40px rgba(0,0,0,0.18); overflow: hidden; }
.ai-fp-header { display: flex; align-items: center; justify-content: space-between; padding: 0.75rem 1rem; background: linear-gradient(135deg, var(--primary), var(--primary-light)); }
.ai-fp-title { color: #fff; font-weight: 700; font-size: 0.9rem; }
.ai-fp-close { background: rgba(255,255,255,0.2); border: none; color: #fff; border-radius: 50%; width: 24px; height: 24px; cursor: pointer; font-size: 0.8rem; display: flex; align-items: center; justify-content: center; transition: background 0.2s; }
.ai-fp-close:hover { background: rgba(255,255,255,0.35); }
.ai-fp-tabs { display: flex; border-bottom: 1px solid var(--border); }
.ai-fp-tabs button { flex: 1; padding: 0.45rem 0.2rem; border: none; background: transparent; color: var(--text-muted); font-size: 0.72rem; cursor: pointer; border-bottom: 2px solid transparent; transition: all 0.18s; white-space: nowrap; }
.ai-fp-tabs button.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 700; background: var(--bg-hover); }
.ai-fp-body { padding: 0.75rem; display: flex; flex-direction: column; gap: 0.5rem; max-height: 380px; overflow-y: auto; }
.ai-fp-form { display: flex; flex-direction: column; gap: 0.45rem; }
.ai-fp-input { width: 100%; padding: 0.4rem 0.65rem; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-input); color: var(--text-main); font-size: 0.82rem; box-sizing: border-box; font-family: inherit; }
.ai-fp-input:focus { outline: none; border-color: var(--primary); }
.ai-fp-textarea { width: 100%; height: 80px; padding: 0.4rem 0.65rem; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-input); color: var(--text-main); font-size: 0.82rem; resize: vertical; box-sizing: border-box; font-family: inherit; }
.ai-fp-textarea:focus { outline: none; border-color: var(--primary); }
.ai-fp-run { width: 100%; padding: 0.45rem; border: none; border-radius: 7px; background: linear-gradient(135deg, var(--primary), var(--primary-light)); color: #fff; font-weight: 700; font-size: 0.85rem; cursor: pointer; transition: opacity 0.2s; }
.ai-fp-run:disabled { opacity: 0.5; cursor: not-allowed; }
.ai-fp-run:hover:not(:disabled) { opacity: 0.88; }
.ai-fp-ghost { width: 100%; padding: 0.35rem; border: 1px dashed var(--border); border-radius: 6px; background: transparent; color: var(--text-muted); font-size: 0.75rem; cursor: pointer; transition: all 0.18s; }
.ai-fp-ghost:hover { border-color: var(--primary); color: var(--primary); }
.ai-fp-loading { display: flex; align-items: center; gap: 0.5rem; color: var(--text-muted); font-size: 0.82rem; padding: 0.5rem 0; }
.ai-fp-spinner { width: 16px; height: 16px; border: 2px solid var(--border); border-top-color: var(--primary); border-radius: 50%; animation: spin 0.8s linear infinite; flex-shrink: 0; }
@keyframes spin { to { transform: rotate(360deg); } }
.ai-fp-error { padding: 0.45rem 0.65rem; background: rgba(229,57,53,0.08); border: 1px solid rgba(229,57,53,0.25); border-radius: 7px; color: #e53935; font-size: 0.78rem; }
.ai-fp-result { display: flex; flex-direction: column; gap: 0.4rem; }
.ai-fp-result-bar { display: flex; align-items: center; justify-content: space-between; }
.ai-fp-result-bar > span { font-size: 0.75rem; font-weight: 700; color: var(--text-muted); }
.ai-fp-result-bar div { display: flex; gap: 0.35rem; }
.ai-fp-insert { padding: 0.2rem 0.55rem; border-radius: 5px; border: none; background: var(--primary); color: #fff; font-size: 0.72rem; font-weight: 600; cursor: pointer; }
.ai-fp-insert:hover { opacity: 0.85; }
.ai-fp-clear { padding: 0.2rem 0.55rem; border-radius: 5px; border: 1px solid var(--border); background: transparent; color: var(--text-muted); font-size: 0.72rem; cursor: pointer; }
.ai-fp-result-text { font-size: 0.78rem; color: var(--text-main); line-height: 1.7; white-space: pre-wrap; word-break: break-all; background: var(--bg-input); border: 1px solid var(--border); border-radius: 7px; padding: 0.6rem 0.75rem; margin: 0; max-height: 200px; overflow-y: auto; }
.ai-pop-enter-active { transition: all 0.2s cubic-bezier(0.34,1.56,0.64,1); }
.ai-pop-leave-active { transition: all 0.15s ease; }
.ai-pop-enter-from { opacity: 0; transform: scale(0.85) translateY(10px); transform-origin: bottom right; }
.ai-pop-leave-to { opacity: 0; transform: scale(0.9) translateY(6px); transform-origin: bottom right; }

/* 应用到AI按钮 */
.mat-actions { display: flex; gap: 0.3rem; align-items: center; }
.btn-ai-apply { padding: 0.12rem 0.45rem; border-radius: 4px; border: 1px solid var(--primary); background: transparent; color: var(--primary); font-size: 0.68rem; font-weight: 700; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.btn-ai-apply:hover, .btn-ai-apply.active { background: var(--primary); color: #fff; }

/* AI 绑定素材提示条 */
.ai-bound-banner { background: linear-gradient(90deg, rgba(var(--primary-rgb),0.08), rgba(var(--primary-rgb),0.04)); border-bottom: 1px solid var(--primary); padding: 0.35rem 1rem; font-size: 0.82rem; color: var(--primary); display: flex; align-items: center; justify-content: space-between; flex-shrink: 0; }
.ai-bound-banner strong { font-weight: 700; }

/* 素材详情弹窗 */
.mat-modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.mat-modal { background: var(--bg-card); border-radius: 14px; width: 560px; max-width: 92vw; max-height: 80vh; display: flex; flex-direction: column; box-shadow: 0 12px 48px rgba(0,0,0,0.22); overflow: hidden; }
.mat-modal-header { display: flex; align-items: center; justify-content: space-between; padding: 1rem 1.2rem 0.5rem; flex-shrink: 0; }
.mat-modal-cat { font-size: 0.72rem; padding: 0.1rem 0.55rem; background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; border-radius: 20px; font-weight: 600; }
.mat-modal-close { background: transparent; border: none; color: var(--text-muted); font-size: 1.1rem; cursor: pointer; border-radius: 50%; width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; transition: background 0.2s; }
.mat-modal-close:hover { background: var(--bg-hover); }
.mat-modal-title { font-size: 1.15rem; font-weight: 700; color: var(--text-main); padding: 0 1.2rem 0.5rem; margin: 0; flex-shrink: 0; }
.mat-modal-tags { display: flex; flex-wrap: wrap; gap: 0.4rem; padding: 0 1.2rem 0.75rem; flex-shrink: 0; }
.mat-tag { font-size: 0.72rem; padding: 0.1rem 0.5rem; border: 1px solid var(--border); border-radius: 20px; color: var(--text-muted); }
.mat-modal-content { flex: 1; overflow-y: auto; padding: 0 1.2rem; font-size: 0.88rem; color: var(--text-main); line-height: 1.8; white-space: pre-wrap; word-break: break-all; margin: 0; font-family: inherit; }
.mat-modal-actions { display: flex; gap: 0.75rem; padding: 1rem 1.2rem; border-top: 1px solid var(--border); flex-shrink: 0; }
.btn-apply-ai { flex: 1; padding: 0.55rem; border: none; border-radius: 8px; background: linear-gradient(135deg, var(--primary), var(--primary-light)); color: #fff; font-weight: 700; font-size: 0.9rem; cursor: pointer; transition: opacity 0.2s; }
.btn-apply-ai:hover { opacity: 0.88; }
.btn-insert-modal { flex: 1; padding: 0.55rem; border: 1px solid var(--border); border-radius: 8px; background: transparent; color: var(--text-sub); font-size: 0.9rem; cursor: pointer; transition: all 0.2s; }
.btn-insert-modal:hover { border-color: var(--primary); color: var(--primary); }
.modal-fade-enter-active { transition: all 0.2s ease; }
.modal-fade-leave-active { transition: all 0.15s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-from .mat-modal { transform: scale(0.95) translateY(12px); }
</style>