<template>
  <div class="workspace-shell" :class="{ 'focus-mode': focusMode }">
    <aside class="material-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <span v-if="!sidebarCollapsed" class="sidebar-title">{{ sidePanel === 'material' ? '素材库' : '章节大纲' }}</span>
        <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">{{ sidebarCollapsed ? '>' : '<' }}</button>
      </div>
      <template v-if="!sidebarCollapsed">
        <!-- 主 Tab：素材库 / 大纲 -->
        <div class="side-panel-tabs">
          <button :class="{ active: sidePanel === 'material' }" @click="sidePanel = 'material'">📚 素材库</button>
          <button :class="{ active: sidePanel === 'outline' }" @click="sidePanel = 'outline'">📋 大纲</button>
        </div>

        <!-- 素材库面板 -->
        <template v-if="sidePanel === 'material'">
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
                  <button @click="applyToAI(item)" class="btn-ai-apply" :class="{ active: boundMats.some(m => m.id === item.id) }" title="绑定到AI">✦ AI</button>
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

        <!-- 大纲面板 -->
        <template v-if="sidePanel === 'outline'">
          <div class="outline-toolbar">
            <button @click="addChapter" class="outline-btn-add">＋ 新增章节</button>
            <button @click="importOutlineFromAI" :disabled="outlineImporting" class="outline-btn-import" title="从AI助手中导入最新生成的大纲">
              {{ outlineImporting ? '导入中...' : '↓ AI导入' }}
            </button>
            <button @click="lockOutlineToAI" class="outline-btn-lock" title="将大纲锁定到AI续写上下文">🔒 锁定</button>
          </div>
          <div v-if="outlineChapters.length === 0" class="outline-empty">
            <p>暂无章节</p>
            <p>点击「＋ 新增章节」或「↓ AI导入」开始</p>
          </div>
          <div class="outline-list">
            <div v-for="ch in outlineChapters" :key="ch.id" class="outline-card" :class="{ editing: outlineEditId === ch.id }">
              <!-- 查看模式 -->
              <template v-if="outlineEditId !== ch.id">
                <div class="outline-card-header">
                  <span class="outline-status-dot" :class="ch.status"></span>
                  <span class="outline-ch-title">{{ ch.vol ? ch.vol + ' · ' : '' }}{{ ch.title }}</span>
                  <div class="outline-card-actions">
                    <button @click="editChapter(ch)" class="outline-act-btn" title="编辑">✏️</button>
                    <button @click="deleteChapter(ch.id)" class="outline-act-btn del" title="删除">✕</button>
                  </div>
                </div>
                <div v-if="ch.summary" class="outline-summary">{{ ch.summary }}</div>
                <div v-if="ch.characters" class="outline-chars">
                  <span v-for="c in ch.characters.split('，').filter(Boolean)" :key="c" class="outline-char-tag">{{ c }}</span>
                </div>
                <div class="outline-status-row">
                  <button v-for="s in ['todo','writing','done']" :key="s"
                    @click="setChapterStatus(ch.id, s)"
                    class="status-btn" :class="{ active: ch.status === s }">
                    {{ { todo: '未写', writing: '写作中', done: '已完成' }[s] }}
                  </button>
                  <button @click="insertChapterMark(ch)" class="outline-act-btn ins" title="在编辑器中插入章节标题">↓写</button>
                </div>
              </template>
              <!-- 编辑模式 -->
              <template v-else>
                <input v-model="outlineForm.vol" class="outline-input" placeholder="卷名（可选）如：第一卷" />
                <input v-model="outlineForm.title" class="outline-input" placeholder="章节标题 *" />
                <textarea v-model="outlineForm.summary" class="outline-textarea" rows="3" placeholder="核心情节简述..."></textarea>
                <input v-model="outlineForm.characters" class="outline-input" placeholder="关键人物，用逗号分隔" />
                <div class="outline-edit-actions">
                  <button @click="saveChapter" class="outline-save-btn">保存</button>
                  <button @click="outlineEditId = null" class="outline-cancel-btn">取消</button>
                </div>
              </template>
            </div>
          </div>
          <div v-if="outlineChapters.length" class="outline-stats">
            共 {{ outlineChapters.length }} 章 · 已完成 {{ outlineChapters.filter(c=>c.status==='done').length }} 章
          </div>
        </template>
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
          <button @click="focusMode = !focusMode" class="btn-tool" :class="{ active: focusMode }" title="专注模式">{{ focusMode ? '退出专注' : '专注' }}</button>
          <span class="word-count">{{ wordCount }} 字</span>
        </div>
      </div>

      <div v-if="insertBanner" class="insert-banner">
        ✅ 已插入「{{ insertBanner }}」
        <button @click="insertBanner = ''" class="banner-close">✕</button>
      </div>

      <div v-if="boundMats.length" class="ai-bound-banner">
        <div class="ai-bound-inner">
          <span class="ai-bound-label">✦ AI 已绑定素材：</span>
          <div class="ai-bound-tags">
            <span v-for="mat in boundMats" :key="mat.id" class="ai-bound-tag">
              {{ mat.title }}
              <button @click="boundMats.splice(boundMats.indexOf(mat), 1)" class="ai-bound-remove">✕</button>
            </span>
          </div>
          <button @click="boundMats = []" class="ai-bound-clear">全部清除</button>
        </div>
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
        <button @click="undo" class="rich-btn" title="撤销 Ctrl+Z" :disabled="!undoStack.length">↩</button>
        <button @click="redo" class="rich-btn" title="恢复 Ctrl+Y" :disabled="!redoStack.length">↪</button>
        <span class="rich-sep">|</span>
        <button @click="formatText('bold')" class="rich-btn" title="加粗 Ctrl+B"><b>B</b></button>
        <button @click="formatText('italic')" class="rich-btn" title="斜体 Ctrl+I"><i>I</i></button>
        <span class="rich-sep">|</span>
        <button @click="insertIndent" class="rich-btn" title="首行缩进（两个全角空格）">⇥</button>
        <button @click="insertQuote" class="rich-btn" title="插入引用块">"</button>
        <button @click="insertChapterTitle" class="rich-btn" title="插入章节标题">章</button>
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
        <span class="rich-sep">|</span>
        <button @click="exportTxt" class="rich-btn rich-btn-export" title="导出为 TXT">↓ TXT</button>
        <button @click="exportDocx" class="rich-btn rich-btn-export" title="导出为 Word">↓ Word</button>
      </div>

      <textarea
        ref="editorRef"
        v-model="editorContent"
        class="editor-textarea"
        :style="{ fontSize: fontSize + 'px', lineHeight: lineHeightVal }"
        placeholder="在此开始创作...&#10;&#10;Tab键首行缩进，Ctrl+B加粗，Ctrl+S保存，Ctrl+Z撤销"
        @keydown.tab.prevent="handleTab"
        @keydown="handleKeydown"
        @input="startAutoSave"
        @mouseup="handleEditorSelect"
        @keyup="handleEditorSelect"
      ></textarea>

      <!-- 选中文本浮动工具栏 -->
      <div v-if="selectionToolbar.show" class="selection-toolbar"
        :style="{ top: selectionToolbar.y + 'px', left: selectionToolbar.x + 'px' }">
        <button @click="quickPolish" class="sel-toolbar-btn polish">✨ 润色</button>
        <div class="sel-toolbar-divider"></div>
        <button @click="quickCheck" class="sel-toolbar-btn check">⚠️ 检测</button>
        <div class="sel-toolbar-divider"></div>
        <button @click="selectionToolbar.show = false" class="sel-toolbar-close">✕</button>
      </div>

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
          <!-- 已绑定素材列表 -->
          <div v-if="boundMats.length" class="ai-fp-bound-area">
            <div class="ai-fp-bound-header">
              <span class="ai-fp-bound-label">📎 已绑定素材（{{ boundMats.length }}/5）</span>
              <button @click="boundMats = []" class="ai-fp-bound-clearall">全清</button>
            </div>
            <div class="ai-fp-bound-list">
              <span v-for="mat in boundMats" :key="mat.id" class="ai-fp-bound-chip">
                <span class="ai-fp-bound-chip-name">{{ mat.title }}</span>
                <button @click="boundMats.splice(boundMats.indexOf(mat),1)" class="ai-fp-bound-chip-del">✕</button>
              </span>
            </div>
          </div>
          <div v-else class="ai-fp-bound-empty">
            <span>从左侧素材点击「✦ AI」可绑定素材（最多5个）</span>
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
            <div v-if="aiTab === 'continue'" class="ai-fp-form">
              <p class="ai-fp-hint">将自动读取编辑器末尾 600 字作为上文进行续写</p>
              <select v-model="continueStyle" class="ai-fp-input">
                <option value="典雅">典雅古风</option><option value="清丽">清丽婉约</option>
                <option value="雄浑">雄浑壮阔</option><option value="写实">写实细腻</option>
              </select>
              <div class="continue-words-row">
                <span class="ai-fp-sublabel">续写字数：</span>
                <button v-for="w in [100, 200, 400, 800]" :key="w"
                  @click="continueWords = w"
                  class="words-btn" :class="{ active: continueWords === w }">{{ w }}字</button>
              </div>
              <div class="ai-fp-section">
                <label class="ai-fp-sublabel">
                  📋 故事大纲
                  <span v-if="pinnedOutline" class="pin-badge">已锁定</span>
                  <span v-else class="pin-badge-off">未设置</span>
                </label>
                <textarea v-model="pinnedOutline" class="ai-fp-textarea" rows="3"
                  placeholder="粘贴故事大纲，锁定后AI续写将参考此大纲走向..."></textarea>
              </div>
              <div class="ai-fp-section">
                <label class="ai-fp-sublabel">
                  👤 人物设定
                  <span v-if="charProfiles" class="pin-badge">已填写</span>
                </label>
                <textarea v-model="charProfiles" class="ai-fp-textarea" rows="2"
                  placeholder="主要人物性格、身份，如：李明远，寒门举子，沉稳内敛，擅长权谋..."></textarea>
              </div>
              <button @click="runContinue" :disabled="aiLoading" class="ai-fp-run">{{ aiLoading ? '续写中...' : '开始续写 ' + continueWords + '字' }}</button>
            </div>
            <div v-if="aiTab === 'polish'" class="ai-fp-form">
              <textarea v-model="polishText" class="ai-fp-textarea" placeholder="粘贴要润色的文段"></textarea>
              <button @click="polishText = editorContent.slice(0,600)" class="ai-fp-ghost">引用当前文章（前600字）</button>
              <button @click="runPolish" :disabled="aiLoading" class="ai-fp-run">{{ aiLoading ? '润色中...' : '开始润色' }}</button>
            </div>
            <div v-if="aiTab === 'rewrite'" class="ai-fp-form">
              <textarea v-model="rewriteText" class="ai-fp-textarea" placeholder="粘贴要改写的原文"></textarea>
              <button @click="rewriteText = editorContent.slice(0,600)" class="ai-fp-ghost">引用当前文章（前600字）</button>
              <input v-model="rewriteReq" class="ai-fp-input" placeholder="改写要求，如：扩写到500字、改为第一人称、增加环境描写" />
              <button @click="runRewrite" :disabled="aiLoading" class="ai-fp-run">{{ aiLoading ? '改写中...' : '开始改写' }}</button>
            </div>
            <div v-if="aiTab === 'dialogue'" class="ai-fp-form">
              <input v-model="dialogueChars" class="ai-fp-input" placeholder="人物设定，如：李白（豪放不羁）与杜甫（沉郁忧国）" />
              <input v-model="dialogueScene" class="ai-fp-input" placeholder="场景，如：长安酒肆，秋夜，初次相遇" />
              <button @click="runDialogue" :disabled="aiLoading" class="ai-fp-run">{{ aiLoading ? '生成中...' : '生成对话' }}</button>
            </div>
            <div v-if="aiTab === 'outline'" class="ai-fp-form">
              <textarea v-model="outlineStory" class="ai-fp-textarea" placeholder="故事梗概，如：盛唐背景，一名寒门学子进京赶考，卷入朝廷党争..."></textarea>
              <button @click="outlineStory = editorContent.slice(0,400)" class="ai-fp-ghost">引用当前文章作为梗概</button>
              <button @click="runOutline" :disabled="aiLoading" class="ai-fp-run">{{ aiLoading ? '生成中...' : '生成大纲' }}</button>
            </div>
            <div v-if="aiTab === 'title'" class="ai-fp-form">
              <p class="ai-fp-hint">将自动读取当前文章内容生成标题，也可手动输入内容</p>
              <textarea v-model="titleContent" class="ai-fp-textarea" placeholder="（留空则自动使用当前文章内容）"></textarea>
              <button @click="runTitle" :disabled="aiLoading" class="ai-fp-run">{{ aiLoading ? '生成中...' : '生成5个标题' }}</button>
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
            <div v-if="aiTab === 'context'" class="ai-fp-form">
              <input v-model="ctxYear" class="ai-fp-input" placeholder="年份，如：627（贞观元年）" />
              <input v-model="ctxPlace" class="ai-fp-input" placeholder="地点，如：长安城东市" />
              <button @click="runContext" :disabled="aiLoading" class="ai-fp-run">{{ aiLoading ? '查询中...' : '生成时空背景' }}</button>
            </div>
            <div v-if="aiTab === 'history'" class="ai-fp-form">
              <div class="ai-history-bar">
                <span class="ai-history-count">共 {{ aiHistory.length }} 条记录</span>
                <button v-if="aiHistory.length" @click="clearHistory" class="ai-fp-ghost">清空</button>
              </div>
              <div v-if="aiHistory.length === 0" class="ai-fp-hint">暂无历史记录</div>
              <div v-for="item in aiHistory" :key="item.id" class="ai-history-item">
                <div class="ai-history-meta">
                  <span class="ai-history-tab">{{ aiTabs.find(t=>t.key===item.tab)?.icon }} {{ aiTabs.find(t=>t.key===item.tab)?.label }}</span>
                  <span class="ai-history-time">{{ item.time }}</span>
                </div>
                <div class="ai-history-preview">{{ item.result.slice(0, 80) }}{{ item.result.length > 80 ? '...' : '' }}</div>
                <button @click="aiResult = item.result; aiTab = item.tab" class="ai-fp-ghost">重新查看</button>
              </div>
            </div>
            <div v-if="aiLoading" class="ai-fp-loading"><div class="ai-fp-spinner"></div><span>星火思考中...</span></div>
            <div v-if="aiError" class="ai-fp-error">{{ aiError }}</div>
            <div v-if="aiResult" class="ai-fp-result">
              <div class="ai-fp-result-bar">
                <span>AI 结果</span>
                <div><button @click="insertAiResult" class="ai-fp-insert">全部插入</button><button @click="aiResult='';aiError=''" class="ai-fp-clear">清除</button></div>
              </div>
              <div v-if="aiResultParagraphs.length > 1" class="ai-fp-paragraphs">
                <div v-for="(para, idx) in aiResultParagraphs" :key="idx" class="ai-fp-para">
                  <pre class="ai-fp-para-text">{{ para }}</pre>
                  <button @click="insertAiParagraph(para)" class="ai-fp-insert-para">插入此段</button>
                </div>
              </div>
              <pre v-else class="ai-fp-result-text">{{ aiResult }}</pre>
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
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
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

// 左侧面板主 Tab：素材库 / 大纲
const sidePanel = ref('material') // 'material' | 'outline'

// ── 大纲模块 ─────────────────────────────────────────────────
const outlineChapters = ref([]) // { id, vol, title, summary, characters, status }
const outlineEditId = ref(null)  // 正在编辑的章节 id
const outlineForm = ref({ vol: '', title: '', summary: '', characters: '' })
const outlineImporting = ref(false)
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
      // 恢复大纲和人物设定
      pinnedOutline.value = res.data.data.pinnedOutline || ''
      charProfiles.value = res.data.data.charProfiles || ''
      // 恢复结构化大纲
      try {
        const od = res.data.data.outlineData
        outlineChapters.value = od ? JSON.parse(od) : []
      } catch(e) { outlineChapters.value = [] }
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
        pinnedOutline: pinnedOutline.value,
        charProfiles: charProfiles.value,
        outlineData: JSON.stringify(outlineChapters.value)
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
  // 首行缩进用两个全角空格
  editorContent.value = editorContent.value.slice(0,s) + '　　' + editorContent.value.slice(s)
  setTimeout(() => ta.setSelectionRange(s+2, s+2), 0)
}

// ── 撤销/重做历史栈 ───────────────────────────────────────────
const undoStack = ref([])
const redoStack = ref([])
const MAX_HISTORY = 100
let isUndoRedo = false

watch(editorContent, (newVal, oldVal) => {
  if (isUndoRedo) return
  undoStack.value.push(oldVal)
  if (undoStack.value.length > MAX_HISTORY) undoStack.value.shift()
  redoStack.value = []
})

function undo() {
  if (!undoStack.value.length) return
  isUndoRedo = true
  redoStack.value.push(editorContent.value)
  editorContent.value = undoStack.value.pop()
  nextTick(() => { isUndoRedo = false })
}

function redo() {
  if (!redoStack.value.length) return
  isUndoRedo = true
  undoStack.value.push(editorContent.value)
  editorContent.value = redoStack.value.pop()
  nextTick(() => { isUndoRedo = false })
}

// ── 格式插入辅助 ─────────────────────────────────────────────
function insertAtCursor(before, after = '') {
  const ta = editorRef.value; if (!ta) return
  const s = ta.selectionStart, e = ta.selectionEnd
  const sel = editorContent.value.slice(s, e)
  const replacement = before + sel + after
  editorContent.value = editorContent.value.slice(0, s) + replacement + editorContent.value.slice(e)
  const cursor = s + before.length + sel.length + after.length
  setTimeout(() => ta.setSelectionRange(cursor, cursor), 0)
}

function insertIndent() {
  const ta = editorRef.value; if (!ta) return
  const s = ta.selectionStart
  // 在当前行开头插入两个全角空格（首行缩进）
  const before = editorContent.value.slice(0, s)
  const lineStart = before.lastIndexOf('\n') + 1
  editorContent.value = editorContent.value.slice(0, lineStart) + '　　' + editorContent.value.slice(lineStart)
  setTimeout(() => ta.setSelectionRange(s + 2, s + 2), 0)
}

function insertQuote() {
  const ta = editorRef.value; if (!ta) return
  const s = ta.selectionStart, e = ta.selectionEnd
  const sel = editorContent.value.slice(s, e).trim() || '引用内容'
  const q = `\n\n　　「${sel}」\n\n`
  editorContent.value = editorContent.value.slice(0, s) + q + editorContent.value.slice(e)
  setTimeout(() => ta.setSelectionRange(s + q.length, s + q.length), 0)
}

function insertChapterTitle() {
  const ta = editorRef.value; if (!ta) return
  const s = ta.selectionStart
  const t = '\n\n第　章\n\n'
  editorContent.value = editorContent.value.slice(0, s) + t + editorContent.value.slice(s)
  const pos = s + 4 // 光标落在「第」后
  setTimeout(() => { ta.focus(); ta.setSelectionRange(pos, pos) }, 0)
}

// ── 导出功能 ─────────────────────────────────────────────────
function exportTxt() {
  const content = editorContent.value
  const title = docTitle.value || '未命名'
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = `${title}.txt`
  a.click(); URL.revokeObjectURL(url)
}

async function exportDocx() {
  const { Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType } = await import('docx')
  const title = docTitle.value || '未命名'
  const lines = editorContent.value.split('\n')
  const paragraphs = lines.map(line => {
    const trimmed = line.trim()
    if (!trimmed) return new Paragraph({ text: '', spacing: { after: 0 } })
    // 检测章节标题（第X章）
    if (/^第[\u4e00-\u9fa5\d]+章/.test(trimmed)) {
      return new Paragraph({
        children: [new TextRun({ text: trimmed, bold: true, size: 28, font: '宋体' })],
        heading: HeadingLevel.HEADING_2,
        alignment: AlignmentType.CENTER,
        spacing: { before: 400, after: 200 }
      })
    }
    return new Paragraph({
      children: [new TextRun({ text: line, size: 24, font: '宋体' })],
      indent: { firstLine: 480 },
      spacing: { line: 360, after: 0 }
    })
  })
  const doc = new Document({
    sections: [{
      properties: {},
      children: [
        new Paragraph({
          children: [new TextRun({ text: title, bold: true, size: 36, font: '宋体' })],
          heading: HeadingLevel.HEADING_1,
          alignment: AlignmentType.CENTER,
          spacing: { after: 400 }
        }),
        ...paragraphs
      ]
    }]
  })
  const buffer = await Packer.toBlob(doc)
  const url = URL.createObjectURL(buffer)
  const a = document.createElement('a')
  a.href = url; a.download = `${title}.docx`
  a.click(); URL.revokeObjectURL(url)
}

// ── 全局键盘快捷键 ───────────────────────────────────────────
function handleKeydown(e) {
  if (e.ctrlKey || e.metaKey) {
    switch(e.key.toLowerCase()) {
      case 's': e.preventDefault(); saveWork(); break
      case 'b': e.preventDefault(); formatText('bold'); break
      case 'i': e.preventDefault(); formatText('italic'); break
      case 'z': e.preventDefault(); if (e.shiftKey) { redo() } else { undo() }; break
      case 'y': e.preventDefault(); redo(); break
      case 'enter': e.preventDefault(); insertIndent(); break
    }
  }
}

// ── 大纲操作函数 ─────────────────────────────────────────────
function addChapter() {
  outlineForm.value = { vol: '', title: '', summary: '', characters: '' }
  const newId = Date.now()
  outlineChapters.value.push({ id: newId, vol: '', title: '新章节', summary: '', characters: '', status: 'todo' })
  outlineEditId.value = newId
}

function editChapter(ch) {
  outlineForm.value = { vol: ch.vol || '', title: ch.title || '', summary: ch.summary || '', characters: ch.characters || '' }
  outlineEditId.value = ch.id
}

function saveChapter() {
  if (!outlineForm.value.title.trim()) return
  const idx = outlineChapters.value.findIndex(c => c.id === outlineEditId.value)
  if (idx >= 0) {
    outlineChapters.value[idx] = {
      ...outlineChapters.value[idx],
      vol: outlineForm.value.vol.trim(),
      title: outlineForm.value.title.trim(),
      summary: outlineForm.value.summary.trim(),
      characters: outlineForm.value.characters.trim()
    }
  }
  outlineEditId.value = null
  saveOutline()
}

function deleteChapter(id) {
  outlineChapters.value = outlineChapters.value.filter(c => c.id !== id)
  saveOutline()
}

function setChapterStatus(id, status) {
  const ch = outlineChapters.value.find(c => c.id === id)
  if (ch) { ch.status = status; saveOutline() }
}

function insertChapterMark(ch) {
  const ta = editorRef.value; if (!ta) return
  const p = ta.selectionStart
  const mark = `\n\n${ch.vol ? ch.vol + '\n' : ''}${ch.title}\n${'─'.repeat(16)}\n\n`
  editorContent.value = editorContent.value.slice(0, p) + mark + editorContent.value.slice(p)
  setTimeout(() => { ta.focus(); ta.setSelectionRange(p + mark.length, p + mark.length) }, 0)
}

// 将大纲锁定到续写面板
function lockOutlineToAI() {
  if (!outlineChapters.value.length) { return }
  const text = outlineChapters.value.map((ch, i) =>
    `第${i + 1}章${ch.vol ? '（' + ch.vol + '）' : ''}：${ch.title}${ ch.summary ? '\n  ' + ch.summary : '' }`
  ).join('\n')
  pinnedOutline.value = text
  aiPanelOpen.value = true
  aiTab.value = 'continue'
  // 短暂提示
  const prev = aiError.value
  aiError.value = ''
  aiResult.value = ''
  setTimeout(() => { aiError.value = prev }, 100)
}

// AI生成大纲导入（解析 aiResult 中的大纲文本）
function importOutlineFromAI() {
  const text = aiResult.value
  if (!text) {
    aiError.value = '请先在 AI 助手的「章节大纲」功能生成大纲'
    aiPanelOpen.value = true
    aiTab.value = 'outline'
    return
  }
  outlineImporting.value = true
  try {
    // 解析格式：第N章：标题（支持多种格式）
    const lines = text.split('\n').filter(l => l.trim())
    const chapters = []
    let currentCh = null
    for (const line of lines) {
      const chMatch = line.match(/^[第]?\s*[\d一二三四五六七八九十]+\s*[章节回][:：.、\s]?(.+)/)
      if (chMatch) {
        if (currentCh) chapters.push(currentCh)
        currentCh = { id: Date.now() + chapters.length, vol: '', title: chMatch[1].trim(), summary: '', characters: '', status: 'todo' }
      } else if (currentCh && line.trim().startsWith('核心情节')) {
        currentCh.summary = line.replace(/^核心情节[：:]?/, '').trim()
      } else if (currentCh && line.trim().startsWith('关键人物')) {
        currentCh.characters = line.replace(/^关键人物[：:]?/, '').replace(/[、,，]/g, '，').trim()
      } else if (currentCh && !currentCh.summary && line.trim() && !line.includes('：')) {
        currentCh.summary = (currentCh.summary + ' ' + line.trim()).trim()
      }
    }
    if (currentCh) chapters.push(currentCh)
    if (chapters.length > 0) {
      outlineChapters.value = chapters
      sidePanel.value = 'outline'
      saveOutline()
    } else {
      aiError.value = '未能解析大纲格式，请确保AI生成的是标准分章大纲'
    }
  } finally {
    outlineImporting.value = false
  }
}

// 保存大纲到数据库（异步静默保存）
function saveOutline() {
  if (!workId.value) return
  const token = localStorage.getItem('token')
  fetch(`/api/works/${workId.value}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
    body: JSON.stringify({
      title: docTitle.value || '未命名',
      content: editorContent.value,
      pinnedOutline: pinnedOutline.value,
      charProfiles: charProfiles.value,
      outlineData: JSON.stringify(outlineChapters.value)
    })
  }).catch(e => console.error('大纲保存失败', e))
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
  if (mat) {
    const idx = boundMats.value.findIndex(m => m.id === mat.id)
    if (idx < 0) {
      if (boundMats.value.length >= 5) boundMats.value.shift()
      boundMats.value.push(mat)
    }
    aiPanelOpen.value = true
    workspaceStore.clearPendingAI()
  }
}, { immediate: true })

// ── 历史助手 ──────────────────────────────────────────────────
const aiPanelOpen = ref(false)
const aiTab = ref('scene')
const aiTabs = [
  { key: 'scene',    icon: '✍️', label: '生成情节' },
  { key: 'continue', icon: '➡️', label: '智能续写' },
  { key: 'polish',   icon: '✨', label: 'AI润色' },
  { key: 'rewrite',  icon: '🔄', label: '改写扩写' },
  { key: 'dialogue', icon: '💬', label: '对话生成' },
  { key: 'outline',  icon: '📋', label: '章节大纲' },
  { key: 'title',    icon: '📌', label: '生成标题' },
  { key: 'check',    icon: '⚠️', label: '穿越警报' },
  { key: 'detail',   icon: '📖', label: '素材问答' },
  { key: 'context',  icon: '🗺️', label: '时空速查' },
  { key: 'history',  icon: '🕒', label: '历史记录' },
]
const aiLoading = ref(false)
const aiResult  = ref('')
const aiError   = ref('')

// 时空背景
const ctxYear  = ref('')
const ctxPlace = ref('')
// 改写/扩写
const rewriteText = ref('')
const rewriteReq  = ref('')
// 对话生成
const dialogueChars = ref('')
const dialogueScene = ref('')
// 大纲生成
const outlineStory = ref('')
// 标题生成
const titleContent = ref('')
// 专注模式
const focusMode = ref(false)
// 穿越警报
const checkText = ref('')
// 细节查询
const detailQ = ref('')
// 环境描写
const sceneInput = ref('')
const sceneStyle = ref('典雅')
// AI润色
const polishText = ref('')
// 智能续写
const continueStyle = ref('典雅')
const continueWords = ref(200)
const pinnedOutline = ref('')
const charProfiles = ref('')
// 绑定素材（支持多个）
const boundMats = ref([])
const boundMat = computed(() => boundMats.value[0] || null) // 向后兼容
// 素材详情弹窗
const matDetail = ref(null)
// AI历史记录
const aiHistory = ref(JSON.parse(localStorage.getItem('ai_history') || '[]'))
function saveToHistory(tab, prompt, result) {
  const item = { id: Date.now(), tab, prompt: prompt.slice(0, 100), result, time: new Date().toLocaleString('zh-CN') }
  aiHistory.value = [item, ...aiHistory.value].slice(0, 20)
  localStorage.setItem('ai_history', JSON.stringify(aiHistory.value))
}
function clearHistory() {
  aiHistory.value = []
  localStorage.removeItem('ai_history')
}

function applyToAI(item) {
  // 若已绑定则取消，否则添加（最多5个）
  const idx = boundMats.value.findIndex(m => m.id === item.id)
  if (idx >= 0) {
    boundMats.value.splice(idx, 1)
  } else {
    if (boundMats.value.length >= 5) {
      boundMats.value.shift() // 超过5个时移除最早绑定的
    }
    boundMats.value.push(item)
  }
  aiPanelOpen.value = true
}

function openMatDetail(item) {
  matDetail.value = item
}

// 选中文本浮动工具栏
const selectionToolbar = ref({ show: false, x: 0, y: 0, text: '' })

function handleEditorSelect(e) {
  const ta = editorRef.value
  if (!ta) return
  const start = ta.selectionStart
  const end = ta.selectionEnd
  const selected = editorContent.value.slice(start, end).trim()
  if (selected.length > 4) {
    const rect = ta.getBoundingClientRect()
    const containerRect = ta.parentElement.getBoundingClientRect()
    selectionToolbar.value = {
      show: true,
      x: Math.min(e.clientX - containerRect.left, containerRect.width - 160),
      y: e.clientY - containerRect.top - 44,
      text: selected
    }
  } else {
    selectionToolbar.value.show = false
  }
}

function quickPolish() {
  const text = selectionToolbar.value.text
  if (!text) return
  selectionToolbar.value.show = false
  polishText.value = text
  aiTab.value = 'polish'
  aiPanelOpen.value = true
}

function quickCheck() {
  const text = selectionToolbar.value.text
  if (!text) return
  selectionToolbar.value.show = false
  checkText.value = text
  aiTab.value = 'check'
  aiPanelOpen.value = true
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

  continue: (text, style, wordCount, outline, chars) =>
    `你是擅长历史题材的文学作家，文风${style}。
${outline ? '【当前故事大纲】\n' + outline + '\n\n' : ''}${chars ? '【主要人物设定】\n' + chars + '\n\n' : ''}请根据以下已有文段，续写约${wordCount || 200}字，要求：
- 风格、人称、视角与原文保持一致
- 情节自然流畅，符合大纲走向（如有）
- 历史细节准确，不出现现代词汇
- 直接输出续写内容，不加任何说明

已有文段（以下为正文末尾，请从此处自然续写）：
${text}`,

  rewrite: (text, req) =>
    `你是专业的历史文学编辑。请按照以下要求对文段进行改写，直接输出改写后的内容，不加说明：

改写要求：${req}

原文：
${text}`,

  dialogue: (chars, scene, outline) =>
    `你是擅长历史题材的文学作家。请根据以下人物设定和场景，生成一段真实自然的人物对话（150-250字）：
${outline ? '【故事大纲背景】\n' + outline + '\n\n' : ''}- 对话要符合各自的性格、身份和时代背景
- 使用符合历史时代的语言风格
- 包含适当的动作/神态描写
- 直接输出对话内容

人物设定：${chars}
场景：${scene}`,

  outline: (story) =>
    `你是专业的历史小说策划编辑。请根据以下故事梗概，生成一份详细的分章大纲：
- 建议分为5-8章
- 每章包含：章节标题、核心情节、关键人物、情感基调
- 注意历史背景的合理性
- 情节起伏有张有弛

故事梗概：${story}`,

  title: (content) =>
    `你是资深的历史题材文学编辑。请根据以下文章内容，生成5个候选标题：
- 风格古典雅致，富有意境
- 长度在4-12字之间
- 能体现文章核心主题
- 避免俗套，富有新意
请直接列出5个标题，每行一个，前面加序号。

文章内容（节选）：
${content.slice(0, 500)}`,
}

async function callSpark(prompt, tabKey) {
  aiLoading.value = true
  aiResult.value = ''
  aiError.value = ''
  const token = localStorage.getItem('token')
  const payload = { prompt }
  if (boundMats.value.length === 1) {
    payload.materialId = boundMats.value[0].id
  } else if (boundMats.value.length > 1) {
    payload.materialIds = boundMats.value.map(m => m.id)
  }

  try {
    const response = await fetch('/api/spark/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(payload)
    })

    if (!response.ok) {
      const err = await response.json().catch(() => ({}))
      const code = response.status
      if (code === 429) aiError.value = '⏰ AI 调用过于频繁，请稍后再试'
      else if (code === 403) aiError.value = '📊 AI 调用额度已用完，请联系管理员'
      else if (code === 401) aiError.value = '🔑 登录已过期，请重新登录'
      else aiError.value = err?.message || '生成失败，请稍后重试'
      aiLoading.value = false
      return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    // eslint-disable-next-line no-constant-condition
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() // 保留未完整的行
      for (const line of lines) {
        const trimmed = line.trim()
        if (trimmed.startsWith('event:error')) {
          // 下一行的 data 是错误信息，暂存标记
        } else if (trimmed.startsWith('data:')) {
          const data = trimmed.slice(5).trim()
          if (data === '[DONE]') {
            // 流结束，保存历史
            saveToHistory(tabKey || aiTab.value, prompt, aiResult.value)
            aiLoading.value = false
            return
          }
          // 判断是否为错误事件的数据
          if (aiError.value === '__error__') {
            aiError.value = data
            aiLoading.value = false
            return
          }
          // 正常 chunk，追加到结果
          aiResult.value += data
        } else if (trimmed === 'event:error') {
          aiError.value = '__error__' // 标记下一个 data 是错误
        }
      }
    }
    // 流正常结束但未收到 [DONE]，也保存历史
    if (aiResult.value) saveToHistory(tabKey || aiTab.value, prompt, aiResult.value)
  } catch(e) {
    if (!navigator.onLine) {
      aiError.value = '📡 网络已断开，请检查网络连接'
    } else {
      aiError.value = '⚠️ 服务暂时不可用，请检查后端服务是否启动'
    }
  } finally {
    aiLoading.value = false
  }
}

function runContext() {
  if (!ctxYear.value.trim() || !ctxPlace.value.trim()) { aiError.value = '请填写年份和地点'; return }
  callSpark(PROMPTS.context(ctxYear.value.trim(), ctxPlace.value.trim()), 'context')
}
function runCheck() {
  if (!checkText.value.trim()) { aiError.value = '请输入要检查的文段'; return }
  callSpark(PROMPTS.check(checkText.value.trim()), 'check')
}
function runDetail() {
  if (!detailQ.value.trim()) { aiError.value = '请输入问题'; return }
  callSpark(PROMPTS.detail(detailQ.value.trim()), 'detail')
}
function runScene() {
  if (!sceneInput.value.trim()) { aiError.value = '请输入场景要素'; return }
  callSpark(PROMPTS.scene(sceneInput.value.trim(), sceneStyle.value), 'scene')
}
function runPolish() {
  if (!polishText.value.trim()) { aiError.value = '请输入要润色的文段'; return }
  const prompt = `你是专业的历史文学编辑，请对以下文段进行润色，保持原意，提升文学性和流畅度，语言典雅，直接返回润色后的文本：\n\n${polishText.value.trim()}`
  callSpark(prompt, 'polish')
}
function runContinue() {
  const text = editorContent.value.slice(-600).trim()
  if (!text) { aiError.value = '编辑器内容为空，无法续写'; return }
  callSpark(PROMPTS.continue(text, continueStyle.value, continueWords.value, pinnedOutline.value, charProfiles.value), 'continue')
}
function runRewrite() {
  if (!rewriteText.value.trim()) { aiError.value = '请输入要改写的文段'; return }
  if (!rewriteReq.value.trim()) { aiError.value = '请填写改写要求'; return }
  callSpark(PROMPTS.rewrite(rewriteText.value.trim(), rewriteReq.value.trim()), 'rewrite')
}
function runDialogue() {
  if (!dialogueChars.value.trim()) { aiError.value = '请填写人物设定'; return }
  if (!dialogueScene.value.trim()) { aiError.value = '请填写对话场景'; return }
  callSpark(PROMPTS.dialogue(dialogueChars.value.trim(), dialogueScene.value.trim(), pinnedOutline.value), 'dialogue')
}
function runOutline() {
  if (!outlineStory.value.trim()) { aiError.value = '请输入故事梗概'; return }
  callSpark(PROMPTS.outline(outlineStory.value.trim()), 'outline')
}
function runTitle() {
  const content = titleContent.value.trim() || editorContent.value.trim()
  if (!content) { aiError.value = '编辑器内容为空，无法生成标题'; return }
  callSpark(PROMPTS.title(content), 'title')
}

// 将AI结果分段，支持逐段插入
const aiResultParagraphs = computed(() => {
  if (!aiResult.value) return []
  return aiResult.value.split(/\n{2,}/).map(p => p.trim()).filter(Boolean)
})

function insertAiParagraph(para) {
  if (!editorRef.value) return
  const el = editorRef.value
  const start = el.selectionStart
  const before = editorContent.value.slice(0, start)
  const after  = editorContent.value.slice(start)
  editorContent.value = before + '\n\n' + para + '\n\n' + after
  insertBanner.value = '已插入段落'
  setTimeout(() => insertBanner.value = '', 2000)
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
.side-panel-tabs { display: flex; border-bottom: 2px solid var(--border); flex-shrink: 0; background: var(--bg-sidebar); }
.side-panel-tabs button { flex: 1; padding: 0.55rem 0.25rem; border: none; background: transparent; color: var(--text-muted); font-size: 0.8rem; font-weight: 600; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s; }
.side-panel-tabs button.active { color: var(--primary); border-bottom-color: var(--primary); background: var(--bg-hover); }
.side-panel-tabs button:hover:not(.active) { color: var(--text-main); background: var(--bg-hover); }

/* 大纲工具栏 */
.outline-toolbar { display: flex; gap: 0.3rem; padding: 0.5rem 0.6rem; border-bottom: 1px solid var(--border); flex-shrink: 0; }
.outline-btn-add { flex: 1; padding: 0.3rem 0.4rem; background: var(--primary); color: #fff; border: none; border-radius: 5px; font-size: 0.75rem; font-weight: 600; cursor: pointer; transition: opacity 0.2s; }
.outline-btn-add:hover { opacity: 0.85; }
.outline-btn-import { padding: 0.3rem 0.5rem; background: transparent; border: 1px solid var(--border-accent); color: var(--primary); border-radius: 5px; font-size: 0.72rem; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.outline-btn-import:hover { background: var(--primary); color: #fff; border-color: var(--primary); }
.outline-btn-import:disabled { opacity: 0.5; cursor: not-allowed; }
.outline-btn-lock { padding: 0.3rem 0.5rem; background: transparent; border: 1px solid var(--border); color: var(--text-sub); border-radius: 5px; font-size: 0.72rem; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.outline-btn-lock:hover { background: var(--bg-hover); color: var(--primary); border-color: var(--primary); }

/* 大纲空状态 */
.outline-empty { padding: 2rem 1rem; text-align: center; color: var(--text-muted); font-size: 0.8rem; line-height: 1.8; }

/* 章节卡片 */
.outline-list { flex: 1; overflow-y: auto; padding: 0.4rem 0.5rem; display: flex; flex-direction: column; gap: 0.4rem; }
.outline-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: 7px; padding: 0.55rem 0.65rem; transition: border-color 0.2s; }
.outline-card:hover { border-color: var(--border-accent); }
.outline-card.editing { border-color: var(--primary); box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.12); }
.outline-card-header { display: flex; align-items: center; gap: 0.35rem; margin-bottom: 0.3rem; }
.outline-status-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; background: var(--border); }
.outline-status-dot.todo { background: var(--text-muted); }
.outline-status-dot.writing { background: #f59e0b; }
.outline-status-dot.done { background: #10b981; }
.outline-ch-title { flex: 1; font-size: 0.82rem; font-weight: 700; color: var(--text-main); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.outline-card-actions { display: flex; gap: 0.2rem; flex-shrink: 0; }
.outline-act-btn { background: transparent; border: none; color: var(--text-muted); font-size: 0.75rem; cursor: pointer; padding: 0.15rem 0.3rem; border-radius: 3px; transition: all 0.15s; }
.outline-act-btn:hover { background: var(--bg-hover); color: var(--primary); }
.outline-act-btn.del:hover { color: #ef4444; }
.outline-act-btn.ins { color: var(--primary); font-weight: 600; font-size: 0.7rem; }
.outline-summary { font-size: 0.75rem; color: var(--text-sub); line-height: 1.5; margin-bottom: 0.3rem; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.outline-chars { display: flex; flex-wrap: wrap; gap: 0.2rem; margin-bottom: 0.35rem; }
.outline-char-tag { font-size: 0.65rem; padding: 0.1rem 0.4rem; background: var(--tag-bg); color: var(--text-sub); border-radius: 20px; }
.outline-status-row { display: flex; align-items: center; gap: 0.2rem; }
.status-btn { font-size: 0.65rem; padding: 0.12rem 0.4rem; border: 1px solid var(--border); border-radius: 4px; background: transparent; color: var(--text-muted); cursor: pointer; transition: all 0.15s; white-space: nowrap; }
.status-btn.active.todo { background: var(--bg-hover); color: var(--text-sub); border-color: var(--text-muted); }
.status-btn.active.writing { background: rgba(245,158,11,0.15); color: #f59e0b; border-color: #f59e0b; }
.status-btn.active.done { background: rgba(16,185,129,0.15); color: #10b981; border-color: #10b981; }
.status-btn:hover:not(.active) { border-color: var(--primary); color: var(--primary); }

/* 大纲编辑表单 */
.outline-input { width: 100%; padding: 0.3rem 0.5rem; border: 1px solid var(--border); border-radius: 4px; background: var(--bg-input); color: var(--text-main); font-size: 0.78rem; margin-bottom: 0.3rem; box-sizing: border-box; }
.outline-input:focus { border-color: var(--primary); outline: none; }
.outline-textarea { width: 100%; padding: 0.3rem 0.5rem; border: 1px solid var(--border); border-radius: 4px; background: var(--bg-input); color: var(--text-main); font-size: 0.78rem; resize: vertical; margin-bottom: 0.3rem; box-sizing: border-box; font-family: inherit; }
.outline-textarea:focus { border-color: var(--primary); outline: none; }
.outline-edit-actions { display: flex; gap: 0.3rem; margin-top: 0.3rem; }
.outline-save-btn { flex: 1; padding: 0.3rem; background: var(--primary); color: #fff; border: none; border-radius: 4px; font-size: 0.75rem; font-weight: 600; cursor: pointer; }
.outline-cancel-btn { padding: 0.3rem 0.6rem; background: transparent; border: 1px solid var(--border); color: var(--text-sub); border-radius: 4px; font-size: 0.75rem; cursor: pointer; }

/* 大纲统计栏 */
.outline-stats { padding: 0.4rem 0.75rem; font-size: 0.72rem; color: var(--text-muted); border-top: 1px solid var(--border); text-align: center; flex-shrink: 0; }

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
.rich-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.rich-btn:disabled:hover { border-color: var(--border); color: var(--text-sub); background: transparent; }
.rich-btn-export { color: var(--primary); border-color: var(--border-accent); font-size: 0.78rem; font-weight: 600; }
.rich-btn-export:hover { background: var(--primary); color: #fff; border-color: var(--primary); }
.rich-sep { color: var(--border); padding: 0 0.2rem; }
.font-size-label { font-size: 0.78rem; color: var(--text-muted); min-width: 36px; text-align: center; }
.rich-select { padding: 0.25rem 0.5rem; border: 1px solid var(--border); border-radius: 4px; background: var(--bg-input); color: var(--text-main); font-size: 0.8rem; cursor: pointer; }
.rich-select:focus { outline: none; border-color: var(--primary); }
.editor-textarea {
  flex: 1; resize: none; border: none; border-radius: 0;
  padding: 3.5rem 12% 4rem;
  background: var(--bg-editor, var(--bg-card));
  color: var(--text-main);
  font-family: 'Noto Serif SC', 'SimSun', 'STSong', Georgia, serif;
  font-size: 1rem;
  line-height: 2.1;
  letter-spacing: 0.06em;
  transition: none;
  overflow-y: auto;
  caret-color: var(--primary);
  background-image: repeating-linear-gradient(
    to bottom,
    transparent,
    transparent calc(2.1em - 1px),
    var(--editor-line, rgba(255,255,255,0.025)) calc(2.1em - 1px),
    var(--editor-line, rgba(255,255,255,0.025)) calc(2.1em)
  );
  background-attachment: local;
}
.selection-toolbar {
  position: absolute;
  z-index: 200;
  display: flex;
  gap: 0;
  align-items: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border: 1px solid rgba(255,255,255,0.10);
  border-radius: 12px;
  padding: 0.45rem 0.6rem;
  box-shadow: 0 8px 32px rgba(0,0,0,0.45), 0 1px 0 rgba(255,255,255,0.06) inset;
  pointer-events: all;
  gap: 0.3rem;
  animation: toolbar-appear 0.18s cubic-bezier(0.34,1.56,0.64,1);
}
@keyframes toolbar-appear {
  from { opacity: 0; transform: translateY(6px) scale(0.92); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}
.sel-toolbar-divider { width: 1px; height: 18px; background: rgba(255,255,255,0.12); margin: 0 0.1rem; }
.sel-toolbar-btn {
  display: flex; align-items: center; gap: 0.3rem;
  font-size: 0.78rem; font-weight: 600;
  padding: 0.35rem 0.75rem;
  background: transparent;
  color: rgba(255,255,255,0.85);
  border: 1px solid transparent;
  border-radius: 7px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
  letter-spacing: 0.02em;
}
.sel-toolbar-btn:hover {
  background: rgba(255,255,255,0.1);
  border-color: rgba(255,255,255,0.15);
  color: #fff;
}
.sel-toolbar-btn.polish:hover { background: rgba(233,69,96,0.25); border-color: rgba(233,69,96,0.4); }
.sel-toolbar-btn.check:hover  { background: rgba(255,193,7,0.18); border-color: rgba(255,193,7,0.35); color: #ffc107; }
.sel-toolbar-close {
  font-size: 0.68rem; padding: 0.25rem 0.4rem;
  background: transparent; color: rgba(255,255,255,0.35);
  border: none; cursor: pointer; border-radius: 4px;
  transition: color 0.15s;
}
.sel-toolbar-close:hover { color: rgba(255,255,255,0.7); }
.word-count { font-size: 0.78rem; color: var(--text-muted); padding: 0 0.4rem; white-space: nowrap; user-select: none; }
.btn-tool.active { background: var(--primary); color: #fff; border-color: var(--primary); }

/* 专注模式：全屏覆盖整个页面 */
.focus-mode .material-sidebar { display: none !important; }
.focus-mode .ai-float-wrap { display: none !important; }
.focus-mode.workspace-shell {
  position: fixed !important;
  inset: 0 !important;
  z-index: 1000 !important;
  height: 100vh !important;
}
.focus-mode .editor-area {
  max-width: 100% !important;
  width: 100% !important;
  margin: 0 !important;
  background: var(--bg-editor, var(--bg-card)) !important;
}
.focus-mode .editor-textarea {
  padding: 4rem 18% 6rem !important;
  font-size: 1.1rem !important;
  line-height: 2.4 !important;
  background: var(--bg-editor, var(--bg-card)) !important;
}
.focus-mode .editor-toolbar {
  padding: 0.5rem 18% !important;
  background: var(--bg-editor, var(--bg-card)) !important;
  border-bottom-color: transparent !important;
}
.focus-mode .rich-toolbar {
  justify-content: center !important;
  padding: 0.35rem 18% !important;
  background: var(--bg-editor, var(--bg-card)) !important;
  border-bottom-color: transparent !important;
}
.focus-mode .autosave-bar {
  background: transparent !important;
  border-bottom-color: transparent !important;
}
/* 专注模式退出按钮始终可见 */
.focus-mode .btn-back { opacity: 0.4; transition: opacity 0.2s; }
.focus-mode .btn-back:hover { opacity: 1; }
.editor-textarea:focus { outline: none; }
.editor-textarea::placeholder {
  color: var(--text-muted);
  font-family: 'Noto Serif SC', 'SimSun', serif;
  font-style: italic;
  opacity: 0.45;
  letter-spacing: 0.08em;
}

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
.ai-fp-hint { font-size: 0.78rem; color: var(--text-muted); margin-bottom: 0.5rem; padding: 0.4rem 0.6rem; background: var(--bg-input); border-radius: 6px; }
.continue-words-row { display: flex; align-items: center; gap: 0.35rem; margin-bottom: 0.55rem; flex-wrap: wrap; }
.ai-fp-sublabel { font-size: 0.72rem; color: var(--text-muted); white-space: nowrap; }
.words-btn { padding: 0.18rem 0.5rem; border: 1px solid var(--border); border-radius: 4px; background: transparent; color: var(--text-sub); font-size: 0.75rem; cursor: pointer; transition: all 0.15s; }
.words-btn.active { background: var(--primary); color: #fff; border-color: var(--primary); }
.words-btn:hover:not(.active) { border-color: var(--primary); color: var(--primary); }
.ai-fp-section { margin-bottom: 0.55rem; }
.pin-badge { font-size: 0.62rem; padding: 0.08rem 0.35rem; background: var(--primary); color: #fff; border-radius: 3px; margin-left: 0.3rem; vertical-align: middle; }
.pin-badge-off { font-size: 0.62rem; padding: 0.08rem 0.35rem; background: var(--bg-hover); color: var(--text-muted); border-radius: 3px; margin-left: 0.3rem; vertical-align: middle; }
.ai-history-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.6rem; }
.ai-history-count { font-size: 0.78rem; color: var(--text-muted); }
.ai-history-item { background: var(--bg-input); border-radius: 8px; padding: 0.7rem; margin-bottom: 0.5rem; border: 1px solid var(--border); }
.ai-history-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.4rem; }
.ai-history-tab { font-size: 0.75rem; font-weight: 600; color: var(--primary); }
.ai-history-time { font-size: 0.7rem; color: var(--text-muted); }
.ai-history-preview { font-size: 0.78rem; color: var(--text-sub); line-height: 1.5; margin-bottom: 0.4rem; white-space: pre-wrap; }
.ai-fp-paragraphs { display: flex; flex-direction: column; gap: 0.5rem; }
.ai-fp-para { background: var(--bg-input); border-radius: 8px; padding: 0.7rem; border: 1px solid var(--border); }
.ai-fp-para-text { font-size: 0.85rem; color: var(--text-main); white-space: pre-wrap; word-break: break-all; margin: 0 0 0.5rem; line-height: 1.8; font-family: inherit; }
.ai-fp-insert-para { font-size: 0.75rem; padding: 0.25rem 0.7rem; background: var(--primary); color: #fff; border: none; border-radius: 5px; cursor: pointer; }
.ai-fp-insert-para:hover { opacity: 0.85; }
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
.ai-bound-banner {
  border-bottom: 1px solid var(--border-accent);
  padding: 0.4rem 1rem;
  font-size: 0.82rem;
  display: flex; align-items: center; flex-shrink: 0;
  background: var(--bg-hover);
}
.ai-bound-inner { display: flex; align-items: center; gap: 0.5rem; width: 100%; flex-wrap: wrap; }
.ai-bound-label { font-size: 0.75rem; color: var(--primary); font-weight: 700; white-space: nowrap; }
.ai-bound-tags { display: flex; flex-wrap: wrap; gap: 0.3rem; flex: 1; }
.ai-bound-tag {
  display: inline-flex; align-items: center; gap: 0.25rem;
  padding: 0.15rem 0.5rem 0.15rem 0.6rem;
  background: var(--primary); color: #fff;
  border-radius: 20px; font-size: 0.72rem; font-weight: 600;
}
.ai-bound-remove {
  background: rgba(255,255,255,0.25); border: none; color: #fff;
  border-radius: 50%; width: 14px; height: 14px;
  font-size: 0.6rem; cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: background 0.15s;
}
.ai-bound-remove:hover { background: rgba(255,255,255,0.45); }
.ai-bound-clear { margin-left: auto; font-size: 0.72rem; padding: 0.15rem 0.5rem; border: 1px solid var(--border-accent); border-radius: 4px; background: transparent; color: var(--primary); cursor: pointer; white-space: nowrap; }
.ai-bound-clear:hover { background: var(--primary); color: #fff; }

/* AI面板内的素材绑定区 */
.ai-fp-bound-area { padding: 0.55rem 0.75rem; border-bottom: 1px solid var(--border); background: var(--bg-input); }
.ai-fp-bound-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.4rem; }
.ai-fp-bound-label { font-size: 0.72rem; font-weight: 700; color: var(--primary); }
.ai-fp-bound-clearall { font-size: 0.68rem; padding: 0.1rem 0.4rem; border: 1px solid var(--border-accent); border-radius: 4px; background: transparent; color: var(--text-muted); cursor: pointer; transition: all 0.15s; }
.ai-fp-bound-clearall:hover { background: var(--primary); color: #fff; border-color: var(--primary); }
.ai-fp-bound-list { display: flex; flex-wrap: wrap; gap: 0.3rem; }
.ai-fp-bound-chip {
  display: inline-flex; align-items: center; gap: 0.2rem;
  padding: 0.18rem 0.3rem 0.18rem 0.55rem;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  color: #fff; border-radius: 20px; font-size: 0.7rem; font-weight: 600;
  max-width: 130px;
}
.ai-fp-bound-chip-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ai-fp-bound-chip-del {
  background: rgba(255,255,255,0.2); border: none; color: #fff;
  border-radius: 50%; width: 13px; height: 13px; flex-shrink: 0;
  font-size: 0.55rem; cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: background 0.15s;
}
.ai-fp-bound-chip-del:hover { background: rgba(255,255,255,0.45); }
.ai-fp-bound-empty { padding: 0.4rem 0.75rem; font-size: 0.72rem; color: var(--text-muted); border-bottom: 1px solid var(--border); text-align: center; }
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