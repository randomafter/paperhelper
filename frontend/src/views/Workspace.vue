<template>
  <div class="workspace-shell" :class="{ 'focus-mode': focusMode }">
    <aside class="material-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <span v-if="!sidebarCollapsed" class="sidebar-title">
          {{ sidePanel === 'material' ? '素材库' : sidePanel === 'outline' ? '章节大纲' : sidePanel === 'chars' ? '人物卡' : sidePanel === 'world' ? '世界观' : '伏笔清单' }}
          <span v-if="workGroupName && sidePanel !== 'material'" class="series-badge" :title="'系列：' + workGroupName">📚 {{ workGroupName }}</span>
        </span>
        <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">{{ sidebarCollapsed ? '>' : '&lt;' }}</button>
      </div>
      <template v-if="!sidebarCollapsed">
        <!-- 主 Tab：素材库 / 大纲 / 人物卡 / 世界观 / 伏笔 -->
        <div class="side-panel-tabs">
          <button :class="{ active: sidePanel === 'material' }" @click="sidePanel = 'material'">素材</button>
          <button :class="{ active: sidePanel === 'outline' }" @click="sidePanel = 'outline'">大纲</button>
          <button :class="{ active: sidePanel === 'chars' }" @click="sidePanel = 'chars'">人物</button>
          <button :class="{ active: sidePanel === 'world' }" @click="sidePanel = 'world'">世界</button>
          <button :class="{ active: sidePanel === 'hooks' }" @click="sidePanel = 'hooks'">伏笔</button>
        </div>

        <!-- 素材库面板 -->
        <template v-if="sidePanel === 'material'">
        <div class="source-tabs">
          <button :class="{ active: matSource === 'search' }" @click="matSource = 'search'">检索</button>
          <button :class="{ active: matSource === 'favorite' }" @click="matSource = 'favorite'; loadFavMats()">收藏</button>
        </div>
        <div v-if="matSource === 'search'" class="search-area">
          <select v-model="matCategory" class="mat-select" @change="applyMatSearch">
            <option value="">全部分类</option>
            <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
          </select>
          <input v-model="matKeyword" placeholder="搜索素材..." class="mat-search" @keyup.enter="applyMatSearch" />
          <button @click="applyMatSearch" class="btn-search-sm">搜索</button>
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
                  <button @click="bindChapterToAI(ch)" class="outline-act-btn" :class="{ active: pinnedOutline.includes(ch.title) }" title="绑定到 AI 大纲上下文">✦ AI</button>
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

        <!-- 人物卡面板 -->
        <template v-if="sidePanel === 'chars'">
          <div class="outline-toolbar">
            <button @click="addCharCard" class="outline-btn-add">＋ 新增人物</button>
            <button @click="bindAllCharsToAI" class="outline-btn-lock" title="将所有人物卡绑定到 AI">✦ 全绑 AI</button>
          </div>
          <div v-if="charCards.length === 0" class="outline-empty">
            <p>暂无人物卡</p>
            <p>点击「＋ 新增人物」创建第一张</p>
          </div>
          <div class="outline-list">
            <div v-for="ch in charCards" :key="ch.id" class="outline-card char-card">
              <div class="outline-card-header">
                <span class="char-card-name">{{ ch.name || '未命名' }}</span>
                <span class="char-card-identity">{{ ch.identity }}</span>
                <div class="outline-card-actions">
                  <button @click="editCharCard(ch)" class="outline-act-btn" title="编辑">✏️</button>
                  <button @click="deleteCharCard(ch.id)" class="outline-act-btn del" title="删除">✕</button>
                </div>
              </div>
              <div v-if="ch.personality" class="outline-summary">性格：{{ ch.personality }}</div>
              <div class="outline-status-row">
                <button @click="bindCharCardToAI(ch)" class="outline-act-btn" :class="{ active: charProfiles.includes(ch.name) }" title="绑定到 AI 人物设定">✦ AI</button>
              </div>
            </div>
          </div>
        </template>

        <!-- 世界观面板 -->
        <template v-if="sidePanel === 'world'">
          <div class="outline-toolbar">
            <button @click="bindWorldToAI" class="outline-btn-lock" title="将世界观绑定到 AI 上下文">✦ 绑定 AI</button>
            <button @click="clearWorldBinding" class="outline-btn-import" title="清除 AI 世界观绑定">✕ 解绑</button>
          </div>
          <div class="world-panel">
            <div v-for="mod in worldModules" :key="mod.key" class="world-module">
              <div class="world-module-hd" @click="toggleWorldModule(mod)">
                <span class="world-module-icon">{{ mod.icon }}</span>
                <span class="world-module-title">{{ mod.label }}</span>
                <span class="world-module-arrow">{{ mod.open ? '▾' : '▸' }}</span>
              </div>
              <transition name="world-expand">
                <div v-if="mod.open" class="world-module-body">
                  <textarea
                    v-model="worldSetting[mod.key]"
                    class="world-textarea"
                    :rows="5"
                    :placeholder="mod.placeholder"
                    @input="onWorldInput"
                  ></textarea>
                </div>
              </transition>
            </div>
          </div>
          <div class="world-footer">
            <span v-if="worldBound" class="world-bound-hint">✦ 已绑定到 AI</span>
            <span v-else class="world-unbound-hint">未绑定 AI</span>
          </div>
        </template>

        <!-- 伏笔面板 -->
        <template v-if="sidePanel === 'hooks'">
          <div class="outline-toolbar">
            <button @click="addHook" class="outline-btn-add">＋ 新增伏笔</button>
            <button @click="bindHooksToAI" class="outline-btn-lock" title="将未回收伏笔绑定到 AI">✦ 绑定 AI</button>
          </div>
          <div class="hooks-filter-row">
            <button class="status-btn" :class="{ active: hookFilter === 'all' }" @click="hookFilter = 'all'">全部</button>
            <button class="status-btn" :class="{ active: hookFilter === 'open' }" @click="hookFilter = 'open'">未回收</button>
            <button class="status-btn" :class="{ active: hookFilter === 'resolved' }" @click="hookFilter = 'resolved'">已回收</button>
          </div>
          <div v-if="filteredHooks.length === 0" class="outline-empty">
            <p>暂无伏笔</p>
            <p>点击「＋ 新增伏笔」创建第一条</p>
          </div>
          <div class="outline-list">
            <div v-for="h in filteredHooks" :key="h.id" class="outline-card hook-card">
              <div class="outline-card-header">
                <span class="hook-title">{{ h.title || '未命名伏笔' }}</span>
                <span class="hook-from">来源：{{ h.sourceChapter || '未标记章节' }}</span>
                <div class="outline-card-actions">
                  <button @click="insertHookToEditor(h)" class="outline-act-btn ins" title="插入到编辑器">↓写</button>
                  <button @click="toggleHookResolved(h.id)" class="outline-act-btn" :title="h.resolved ? '标记为未回收' : '标记为已回收'">
                    {{ h.resolved ? '↺' : '✓' }}
                  </button>
                  <button @click="deleteHook(h.id)" class="outline-act-btn del" title="删除">✕</button>
                </div>
              </div>
              <div v-if="h.note" class="outline-summary">{{ h.note }}</div>
              <div class="outline-status-row">
                <span class="hook-status" :class="h.resolved ? 'resolved' : 'open'">
                  {{ h.resolved ? '已回收' : '未回收' }}
                </span>
                <span v-if="h.resolvedChapter" class="hook-resolve-ch">回收章节：{{ h.resolvedChapter }}</span>
              </div>
            </div>
          </div>
        </template>

      </template>
    </aside>

    <div class="editor-area">
      <div class="editor-toolbar">
        <button @click="goWorks" class="btn-back">‹ 我的作品</button>
        <input v-model="docTitle" class="doc-title-input" placeholder="文章标题...">
        <div class="toolbar-actions">
          <span v-if="saveStatus" class="save-status" :class="saveStatus">{{ saveStatusText }}</span>
          <button @click="saveWork" :disabled="saving" class="btn-save">{{ saving ? '保存中...' : '保存' }}</button>
          <button @click="clearEditor" class="btn-tool">清空</button>
          <button @click="copyAll" class="btn-tool">复制</button>
          <button @click="showRichToolbar = !showRichToolbar" class="btn-tool" :class="{ active: showRichToolbar }">排版工具</button>
          <button @click="focusMode = !focusMode" class="btn-tool" :class="{ active: focusMode }" title="专注模式">{{ focusMode ? '退出专注' : '专注' }}</button>
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
              <button @click="removeBoundMat(mat)" class="ai-bound-remove">✕</button>
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

      <div v-if="showRichToolbar" class="rich-toolbar-wrap">
        <div class="rich-toolbar">
        <button @click="undo" class="rich-btn" title="撤销 Ctrl+Z" :disabled="!undoStack.length">↩</button>
        <button @click="redo" class="rich-btn" title="恢复 Ctrl+Y" :disabled="!redoStack.length">↪</button>
        <span class="rich-sep">|</span>
        <select v-model="fontFamily" class="rich-select">
          <option value="SimSun, STSong, serif">宋体</option>
          <option value="FangSong, STFangsong, serif">仿宋</option>
          <option value="KaiTi, STKaiti, serif">楷体</option>
          <option value="SimHei, STHeiti, sans-serif">黑体</option>
        </select>
        <select v-model="fontSize" class="rich-select">
          <option :value="12">12px</option>
          <option :value="14">14px</option>
          <option :value="16">16px</option>
          <option :value="18">18px</option>
        </select>
        <button @click="boldOn = !boldOn" class="rich-btn" :class="{ active: boldOn }"><b>B</b></button>
        <button @click="italicOn = !italicOn" class="rich-btn" :class="{ active: italicOn }"><i>I</i></button>
        <span class="rich-sep">|</span>
        <select v-model="lineHeightVal" class="rich-select">
          <option value="1.0">行距 1.0</option>
          <option value="1.5">行距 1.5</option>
          <option value="2.0">行距 2.0</option>
        </select>
        <select v-model="paragraphSpacing" class="rich-select">
          <option value="1.0">段距 1.0</option>
          <option value="1.5">段距 1.5</option>
          <option value="2.0">段距 2.0</option>
        </select>
        <span class="rich-sep">|</span>
        <button @click="textAlignVal = 'left'" class="rich-btn" :class="{ active: textAlignVal === 'left' }">左</button>
        <button @click="textAlignVal = 'center'" class="rich-btn" :class="{ active: textAlignVal === 'center' }">中</button>
        <button @click="textAlignVal = 'right'" class="rich-btn" :class="{ active: textAlignVal === 'right' }">右</button>
        <button @click="textAlignVal = 'justify'" class="rich-btn" :class="{ active: textAlignVal === 'justify' }">两端</button>
        <span class="rich-sep">|</span>
        <button @click="changeIndent(1)" class="rich-btn">缩进</button>
        <button @click="changeIndent(-1)" class="rich-btn">减缩</button>
        <button @click="insertCurrentDateTime" class="rich-btn">插入时间/日期</button>
        <button @click="saveEditorStyle" class="rich-btn">保存样式</button>
        <button @click="resetTypography" class="rich-btn">恢复默认排版</button>
        <button @click="exportTxt" class="rich-btn rich-btn-export" title="导出为 TXT">↓ TXT</button>
        <button @click="exportDocx" class="rich-btn rich-btn-export" title="导出为 Word">↓ Word</button>
      </div>
      </div>

      <textarea
        ref="editorRef"
        v-model="editorContent"
        class="editor-textarea"
        :style="editorStyle"
        placeholder="在此开始创作...&#10;&#10;Tab键首行缩进，Ctrl+B加粗，Ctrl+S保存，Ctrl+Z撤销"
        @keydown.tab.prevent="handleTab"
        @keydown.enter.prevent="handleEnter"
        @keydown="handleKeydown"
        @mouseup="handleEditorSelect"
        @keyup="handleEditorSelect"
      ></textarea>

      <div class="editor-word-count">{{ wordCount }} 字</div>

      <!-- 选中文本浮动工具栏（行内 AI 操作） -->
      <div v-if="selectionToolbar.show" class="selection-toolbar"
        :style="{ top: selectionToolbar.y + 'px', left: selectionToolbar.x + 'px' }">
        <button @click="inlineAction('polish')" class="sel-toolbar-btn polish" :disabled="inlineLoading" title="润色">✨ 润色</button>
        <div class="sel-toolbar-divider"></div>
        <button @click="inlineAction('expand')" class="sel-toolbar-btn expand" :disabled="inlineLoading" title="扩写">📝 扩写</button>
        <div class="sel-toolbar-divider"></div>
        <button @click="inlineAction('rewrite')" class="sel-toolbar-btn rewrite" :disabled="inlineLoading" title="改写">🔄 改写</button>
        <div class="sel-toolbar-divider"></div>
        <button @click="inlineAction('check')" class="sel-toolbar-btn check" :disabled="inlineLoading" title="检测">⚠️ 检测</button>
        <div class="sel-toolbar-divider"></div>
        <button @click="selectionToolbar.show = false" class="sel-toolbar-close">✕</button>
          </div>

      <!-- / 指令菜单 -->
      <div v-if="slashMenu.show" class="slash-menu"
        :style="{ top: slashMenu.y + 'px', left: slashMenu.x + 'px' }">
        <div class="slash-menu-header">⚡ AI 指令</div>
        <button v-for="(cmd, idx) in slashCommands" :key="cmd.key"
          class="slash-cmd"
          :class="{ active: slashMenu.activeIdx === idx }"
          @mousedown.prevent="runSlashCommand(cmd)">
          <span class="slash-cmd-icon">{{ cmd.icon }}</span>
          <div class="slash-cmd-info">
            <span class="slash-cmd-label">{{ cmd.label }}</span>
            <span class="slash-cmd-desc">{{ cmd.desc }}</span>
          </div>
        </button>
            </div>

      <!-- 行内 AI 生成结果预览条 -->
      <transition name="inline-ai">
      <div v-if="inlineResult.show" class="inline-ai-bar">
        <div class="inline-ai-bar-top">
          <span class="inline-ai-label">
            <span v-if="inlineLoading" class="inline-ai-dot loading"></span>
            <span v-else class="inline-ai-dot"></span>
            {{ inlineLoading ? 'AI 生成中...' : 'AI 结果预览' }}
          </span>
          <div class="inline-ai-actions" v-if="!inlineLoading">
            <button @click="acceptInline" class="inline-accept">✓ 接受</button>
            <button @click="retryInline" class="inline-retry">↺ 重试</button>
            <button @click="discardInline" class="inline-discard">✕ 放弃</button>
            </div>
            </div>
        <pre v-if="inlineOptions.length <= 1" class="inline-ai-text">{{ inlineResult.text }}</pre>
        <div v-else class="inline-options-list">
          <div v-for="(opt, idx) in inlineOptions" :key="idx" class="inline-option-item">
            <div class="inline-option-hd">
              <span class="inline-option-num">方案 {{ idx + 1 }}</span>
              <button @click="acceptOption(idx)" class="inline-accept">✓ 选用此方案</button>
            </div>
            <pre class="inline-option-text">{{ opt }}</pre>
            </div>
          </div>
        </div>
        </transition>

    </div><!-- /editor-area -->

  <!-- 右侧 AI 对话面板 -->
  <div class="ai-sidebar" :class="{ collapsed: !aiPanelOpen }" :style="aiPanelOpen ? { width: aiPanelWidth + 'px' } : {}">
    <div v-if="aiPanelOpen" class="ai-resize-handle" @mousedown="startAiResize"></div>
    <div v-if="!aiPanelOpen" class="ai-sidebar-tab" @click="aiPanelOpen = true" title="展开 AI 对话">
      <span class="ai-sidebar-tab-icon">✦</span>
      <span class="ai-sidebar-tab-text">AI</span>
      </div>
    <AIChatPanel
      v-if="aiPanelOpen"
      :bound-mats="boundMats"
      :outline="pinnedOutline"
      :chars="charProfiles"
      :world-setting="worldSettingBound"
      :plot-hooks="hooksBound"
      :editor-content="editorContent"
      :style-name="continueStyle"
      :words="continueWords"
      @update:outline="pinnedOutline = $event"
      @update:chars="charProfiles = $event"
      @update:bound-mats="boundMats = $event"
      @accept-msg="handleAcceptMsg"
      @collapse="aiPanelOpen = false"
    />
  </div><!-- /ai-sidebar -->
</div><!-- /workspace-shell -->

  <!-- 人物卡编辑弹窗 -->
  <transition name="modal-fade">
  <div v-if="charCardEditing" class="char-modal-mask" @click.self="charCardEditing = null">
    <div class="char-modal-dialog">
      <div class="char-modal-hd">
        <div class="char-modal-title">
          <span class="char-modal-icon">👤</span>
          <span>{{ charCardForm.id ? '编辑人物' : '新增人物' }}</span>
        </div>
        <button class="char-modal-close" @click="charCardEditing = null">✕</button>
      </div>
      <div class="char-modal-body">
        <div class="char-fields-grid">
          <div class="char-field">
            <label class="char-label">姓名 <span class="char-required">*</span></label>
            <input v-model="charCardForm.name" class="char-input" placeholder="如：李明远" />
          </div>
          <div class="char-field">
            <label class="char-label">别名 / 字号</label>
            <input v-model="charCardForm.alias" class="char-input" placeholder="如：字子谦、人称寒刀" />
          </div>
          <div class="char-field">
            <label class="char-label">性别</label>
            <select v-model="charCardForm.gender" class="char-input char-select">
              <option value="">不限</option>
              <option value="男">男</option>
              <option value="女">女</option>
              <option value="其他">其他</option>
            </select>
          </div>
          <div class="char-field">
            <label class="char-label">身份 / 职位</label>
            <input v-model="charCardForm.identity" class="char-input" placeholder="如：寒门举子、北境将领" />
          </div>
        </div>
        <div class="char-field char-field-full">
          <label class="char-label">性格特征</label>
          <textarea v-model="charCardForm.personality" class="char-textarea" rows="3" placeholder="如：沉稳内敛，遇事冷静，不轻易表露情感，逆境中愈发坚毅"></textarea>
        </div>
        <div class="char-field char-field-full">
          <label class="char-label">核心动机</label>
          <textarea v-model="charCardForm.motivation" class="char-textarea" rows="3" placeholder="如：为家族复仇，重振门楣；亦渴望在乱世中寻得一片安宁"></textarea>
        </div>
        <div class="char-field char-field-full">
          <label class="char-label">关系网络</label>
          <textarea v-model="charCardForm.relationships" class="char-textarea" rows="3" placeholder="如：与主角李明远为旧识，共历北境之役；与反派沈云有夺父之仇"></textarea>
        </div>
        <div class="char-field char-field-full">
          <label class="char-label">外貌 / 口头禅 / 备注</label>
          <textarea v-model="charCardForm.notes" class="char-textarea" rows="3" placeholder="如：身形颀长，眉眼清冷；惯用'无妨'二字；左手有一道旧伤"></textarea>
        </div>
      </div>
      <div class="char-modal-footer">
        <button @click="charCardEditing = null" class="char-btn-cancel">取消</button>
        <button @click="saveCharCard" class="char-btn-save">✓ 保存人物卡</button>
      </div>
    </div>
  </div>
  </transition>

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
import { worksApi, seriesApi } from '../api/works'
import AIChatPanel from '../components/AIChatPanel.vue'
import { useWorkspaceStore } from '../stores/workspace'

// ═══════════════════════════════════════════════════════════
// § 1. 路由 & Store
// ═══════════════════════════════════════════════════════════
const router = useRouter()
const route = useRoute()
const workspaceStore = useWorkspaceStore()

// ═══════════════════════════════════════════════════════════
// § 2. 作品 & 编辑器基础状态
// ═══════════════════════════════════════════════════════════
const workId = ref(null)
const workGroupName = ref('')   // 当前作品所属组名
const seriesLoaded = ref(false) // 是否已加载过系列档案
const saving = ref(false)
const saveStatus = ref('')
const saveStatusText = computed(() =>
  ({ saving: '保存中...', saved: '✓ 已保存', error: '✗ 保存失败' })[saveStatus.value] || ''
)
let autoSaveTimer = null
let countdownTimer = null
const autoSaveState = ref('idle')    // idle / counting / saving / saved
const autoSaveProgress = ref(0)
const autoSaveCountdown = ref(10)
const AUTO_SAVE_DELAY = 10           // 秒

const docTitle = ref('')
const editorContent = ref('')
const editorRef = ref(null)
const insertBanner = ref('')
const insertingId = ref(null)
const fontFamily = ref(localStorage.getItem('workspace_font_family') || 'KaiTi, STKaiti, serif')
const fontSize = ref(Number(localStorage.getItem('workspace_font_size')) || 16)
const lineHeightVal = ref(localStorage.getItem('workspace_line_height') || '1.5')
const paragraphSpacing = ref(localStorage.getItem('workspace_para_spacing') || '1.5')
const textAlignVal = ref(localStorage.getItem('workspace_text_align') || 'left')
const boldOn = ref(localStorage.getItem('workspace_bold') === 'true')
const italicOn = ref(localStorage.getItem('workspace_italic') === 'true')
const indentChars = ref(Number(localStorage.getItem('workspace_indent_chars') || 2))
const focusMode = ref(false)
const showRichToolbar = ref(false)

// ═══════════════════════════════════════════════════════════
// § 3. 左侧面板：素材库
// ═══════════════════════════════════════════════════════════
const sidebarCollapsed = ref(false)
const sidePanel = ref('material')   // 'material' | 'outline'
const matSource = ref('search')     // 'search' | 'favorite'
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
const matDetail = ref(null)

const displayMats = computed(() => {
  if (matSource.value === 'favorite') {
    const kw = favSearch.value.trim().toLowerCase()
    return kw ? favorites.value.filter(f =>
      f.title.toLowerCase().includes(kw) || f.content?.toLowerCase().includes(kw)
    ) : favorites.value
  }
  return searchResults.value
})

const wordCount = computed(() => editorContent.value.replace(/\s/g,'').length)

async function loadCategories() {
  try {
    const res = await import('../api/category').then(m => m.categoryApi.list())
    if (res.data?.code === 200) categories.value = (res.data.data || []).map(c => c.name)
  } catch(e) { console.error(e) }
}

// ═══════════════════════════════════════════════════════════
// § 4. 左侧面板：大纲模块
// ═══════════════════════════════════════════════════════════
const outlineChapters = ref([])  // { id, vol, title, summary, characters, status }
const outlineEditId = ref(null)  // 当前编辑中的章节 id
const outlineForm = ref({ vol: '', title: '', summary: '', characters: '' })
const outlineImporting = ref(false)

async function searchMats() {
  matLoading.value = true
  try {
    const res = await materialApi.search({
      category: matCategory.value?.trim() || undefined,
      keyword: matKeyword.value?.trim() || undefined,
      page: matPage.value, size: matPageSize,
    })
    if (res.data?.code === 200 && res.data?.data) {
      searchResults.value = res.data.data.records || []
      matTotal.value = res.data.data.total || 0
    }
  } catch(e) { console.error(e) } finally { matLoading.value = false }
}

function applyMatSearch() {
  matPage.value = 1
  searchMats()
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

// ═══════════════════════════════════════════════════════════
// § 13. 作品加载 & 保存 & 自动保存
// ═══════════════════════════════════════════════════════════
function normalizeGroupName(name) {
  const g = String(name ?? '').trim()
  return (!g || g === '未分组') ? '' : g
}

async function loadWork(id) {
  try {
    const res = await worksApi.get(id)
    if (res.data?.code === 200 && res.data?.data) {
      docTitle.value = res.data.data.title || '未命名'
      editorContent.value = res.data.data.content || ''
      workId.value = res.data.data.id
      workGroupName.value = normalizeGroupName(res.data.data.groupName)
      // 恢复大纲和人物设定
      pinnedOutline.value = res.data.data.pinnedOutline || ''
      charProfiles.value = res.data.data.charProfiles || ''
      // 恢复结构化大纲
      try {
        const od = res.data.data.outlineData
        outlineChapters.value = od ? JSON.parse(od) : []
      } catch(e) { outlineChapters.value = [] }
      // 恢复人物卡
      try {
        const cpj = res.data.data.charProfilesJson
        charCards.value = cpj ? JSON.parse(cpj) : []
      } catch(e) { charCards.value = [] }
      // 恢复世界观
      try {
        const ws = res.data.data.worldSetting
        const parsed = ws ? JSON.parse(ws) : {}
        worldSetting.value = { dynasty: '', geography: '', society: '', taboo: '', other: '', ...parsed }
        // 如果世界观有内容，自动恢复绑定状态
        worldSettingBound.value = buildWorldText()
      } catch(e) { worldSetting.value = { dynasty: '', geography: '', society: '', taboo: '', other: '' } }
      // 恢复伏笔
      try {
        const ph = res.data.data.plotHooks
        plotHooks.value = ph ? JSON.parse(ph) : []
      } catch(e) { plotHooks.value = [] }
      // 如果有分组，加载系列共享档案
      if (workGroupName.value && !seriesLoaded.value) {
        await loadSeries(workGroupName.value)
      }
    }
  } catch(e) { console.error(e) }
}

async function loadSeries(groupName) {
  groupName = normalizeGroupName(groupName)
  if (!groupName) return
  try {
    const res = await seriesApi.get(groupName)
    if (res.data?.code === 200 && res.data?.data) {
      const s = res.data.data
      // 系列档案：共享大纲 / 人设 / 世界观 / 伏笔
      if (s.pinnedOutline) pinnedOutline.value = s.pinnedOutline
      if (s.charProfiles) charProfiles.value = s.charProfiles
      try {
        const od = s.outlineData
        if (od) outlineChapters.value = JSON.parse(od)
      } catch(e) {}
      try {
        const cpj = s.charProfilesJson
        if (cpj) charCards.value = JSON.parse(cpj)
      } catch(e) {}
      try {
        const ws = s.worldSetting
        if (ws) {
          const parsed = JSON.parse(ws)
          worldSetting.value = { dynasty: '', geography: '', society: '', taboo: '', other: '', ...parsed }
          worldSettingBound.value = buildWorldText()
        }
      } catch(e) {}
      try {
        const ph = s.plotHooks
        if (ph) plotHooks.value = JSON.parse(ph)
      } catch(e) {}
      seriesLoaded.value = true
    }
  } catch(e) { console.error('系列档案加载失败', e) }
}

async function saveSeries() {
  const groupName = normalizeGroupName(workGroupName.value)
  if (!groupName) return
  try {
    await seriesApi.save({
      groupName,
      pinnedOutline: pinnedOutline.value,
      charProfiles: charProfiles.value,
      outlineData: JSON.stringify(outlineChapters.value),
      charProfilesJson: JSON.stringify(charCards.value),
      worldSetting: JSON.stringify(worldSetting.value),
      plotHooks: JSON.stringify(plotHooks.value),
    })
  } catch(e) { console.error('系列档案保存失败', e) }
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
        outlineData: JSON.stringify(outlineChapters.value),
        charProfilesJson: JSON.stringify(charCards.value),
        worldSetting: JSON.stringify(worldSetting.value),
        plotHooks: JSON.stringify(plotHooks.value),
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
  startAutoSave()
  insertBanner.value = item.title
  setTimeout(() => { insertBanner.value = '' }, 3000)
  const np = s + ins.length
  setTimeout(() => { ta.focus(); ta.setSelectionRange(np,np) }, 0)
}

const editorStyle = computed(() => ({
  fontFamily: fontFamily.value,
  fontSize: `${fontSize.value}px`,
  lineHeight: lineHeightVal.value,
  fontWeight: boldOn.value ? '700' : '400',
  fontStyle: italicOn.value ? 'italic' : 'normal',
  textAlign: textAlignVal.value,
}))

function formatText(type) {
  if (type === 'bold') boldOn.value = !boldOn.value
  if (type === 'italic') italicOn.value = !italicOn.value
}

function indentText() {
  return '　'.repeat(Math.max(0, indentChars.value))
}

function changeIndent(delta) {
  indentChars.value = Math.max(0, Math.min(8, indentChars.value + delta))
}

function insertCurrentDateTime() {
  const ta = editorRef.value; if (!ta) return
  const s = ta.selectionStart
  const now = new Date()
  const date = now.toLocaleDateString('zh-CN')
  const time = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  const ins = `【${date} ${time}】`
  editorContent.value = editorContent.value.slice(0, s) + ins + editorContent.value.slice(s)
  setTimeout(() => ta.setSelectionRange(s + ins.length, s + ins.length), 0)
}

function paragraphGap() {
  const map = { '1.0': '\n', '1.5': '\n\n', '2.0': '\n\n\n' }
  return map[paragraphSpacing.value] || '\n\n'
}

function insertIndent() {
  const ta = editorRef.value; if (!ta) return
  const s = ta.selectionStart
  const before = editorContent.value.slice(0, s)
  const lineStart = before.lastIndexOf('\n') + 1
  const t = indentText()
  editorContent.value = editorContent.value.slice(0, lineStart) + t + editorContent.value.slice(lineStart)
  setTimeout(() => ta.setSelectionRange(s + t.length, s + t.length), 0)
}

function insertNewPara() {
  const ta = editorRef.value; if (!ta) return
  const p = ta.selectionStart
  const insert = paragraphGap() + indentText()
  editorContent.value = editorContent.value.slice(0,p) + insert + editorContent.value.slice(p)
  setTimeout(() => ta.setSelectionRange(p + insert.length, p + insert.length), 0)
}

function resetTypography() {
  fontFamily.value = 'KaiTi, STKaiti, serif'
  fontSize.value = 16
  lineHeightVal.value = '1.5'
  paragraphSpacing.value = '1.5'
  textAlignVal.value = 'left'
  boldOn.value = false
  italicOn.value = false
  indentChars.value = 2
}

function saveEditorStyle() {
  localStorage.setItem('workspace_font_family', String(fontFamily.value))
  localStorage.setItem('workspace_font_size', String(fontSize.value))
  localStorage.setItem('workspace_line_height', String(lineHeightVal.value))
  localStorage.setItem('workspace_para_spacing', String(paragraphSpacing.value))
  localStorage.setItem('workspace_text_align', String(textAlignVal.value))
  localStorage.setItem('workspace_bold', String(boldOn.value))
  localStorage.setItem('workspace_italic', String(italicOn.value))
  localStorage.setItem('workspace_indent_chars', String(indentChars.value))
  insertBanner.value = '排版样式已保存'
  setTimeout(() => { insertBanner.value = '' }, 1600)
}

function handleTab() {
  const ta = editorRef.value; if (!ta) return
  const s = ta.selectionStart
  const t = indentText()
  editorContent.value = editorContent.value.slice(0,s) + t + editorContent.value.slice(s)
  setTimeout(() => ta.setSelectionRange(s + t.length, s + t.length), 0)
}

function handleEnter() {
  const ta = editorRef.value; if (!ta) return
  const s = ta.selectionStart, e = ta.selectionEnd
  const insert = paragraphGap() + indentText()
  editorContent.value = editorContent.value.slice(0, s) + insert + editorContent.value.slice(e)
  setTimeout(() => ta.setSelectionRange(s + insert.length, s + insert.length), 0)
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
  startAutoSave()
})

watch([fontFamily, fontSize, lineHeightVal, paragraphSpacing, textAlignVal, boldOn, italicOn, indentChars], () => {
  localStorage.setItem('workspace_font_family', String(fontFamily.value))
  localStorage.setItem('workspace_font_size', String(fontSize.value))
  localStorage.setItem('workspace_line_height', String(lineHeightVal.value))
  localStorage.setItem('workspace_para_spacing', String(paragraphSpacing.value))
  localStorage.setItem('workspace_text_align', String(textAlignVal.value))
  localStorage.setItem('workspace_bold', String(boldOn.value))
  localStorage.setItem('workspace_italic', String(italicOn.value))
  localStorage.setItem('workspace_indent_chars', String(indentChars.value))
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

// ═══════════════════════════════════════════════════════════
// § 9. 导出功能
// ═══════════════════════════════════════════════════════════
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

// ═══════════════════════════════════════════════════════════
// § 11. 全局键盘快捷键 + / 指令菜单处理
// ═══════════════════════════════════════════════════════════
// ── 全局键盘快捷键 ───────────────────────────────────────────
function handleKeydown(e) {
  // / 指令菜单快捷键
  if (e.key === '/' && !slashMenu.value.show) {
    const ta = editorRef.value
    if (!ta) return
    const pos = ta.selectionStart
    const before = editorContent.value.slice(0, pos)
    const lastNewline = before.lastIndexOf('\n')
    const lineStart = lastNewline + 1
    const lineContent = before.slice(lineStart).trim()
    
    // 只在空行或行首显示菜单
    if (lineContent === '') {
      e.preventDefault()
      const rect = ta.getBoundingClientRect()
      const containerRect = ta.parentElement.getBoundingClientRect()
      slashMenu.value = {
        show: true,
        x: Math.max(0, pos * 8 - containerRect.left),
        y: rect.top - containerRect.top + 30,
        activeIdx: 0
      }
      return
    }
  }
  
  // 菜单导航
  if (slashMenu.value.show) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      slashMenu.value.activeIdx = (slashMenu.value.activeIdx + 1) % slashCommands.length
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      slashMenu.value.activeIdx = (slashMenu.value.activeIdx - 1 + slashCommands.length) % slashCommands.length
    } else if (e.key === 'Enter') {
      e.preventDefault()
      runSlashCommand(slashCommands[slashMenu.value.activeIdx])
    } else if (e.key === 'Escape') {
      slashMenu.value.show = false
    }
    return
  }
  
  // 全局快捷键
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

function runQuickCommand(cmd) {
  // 右侧面板快捷按钮调用：不依赖编辑器光标位置
  inlineLoading.value = true
  inlineResult.value = { show: true, text: '', action: cmd.key, start: 0 }
  inlineSelStart.value = 0
  inlineSelEnd.value = 0

  let prompt = ''
  const editorText = editorContent.value
  if (cmd.key === 'continue') {
    const text = editorText.slice(-600).trim()
    if (!text) { inlineResult.value.text = '编辑器内容为空，无法续写'; inlineLoading.value = false; return }
    prompt = PROMPTS.continue(text, continueStyle.value, continueWords.value, pinnedOutline.value, charProfiles.value)
  } else if (cmd.key === 'scene') {
    prompt = PROMPTS.scene('盛唐长安，春日午后', continueStyle.value)
  } else if (cmd.key === 'dialogue') {
    prompt = PROMPTS.dialogue(charProfiles.value || '李白与杜甫', '长安酒肆，初次相遇', pinnedOutline.value)
  } else if (cmd.key === 'outline') {
    const text = editorText.slice(0, 400).trim()
    if (!text) { inlineResult.value.text = '编辑器内容为空，无法生成大纲'; inlineLoading.value = false; return }
    prompt = PROMPTS.outline(text)
  } else if (cmd.key === 'title') {
    const text = editorText.trim()
    if (!text) { inlineResult.value.text = '编辑器内容为空，无法生成标题'; inlineLoading.value = false; return }
    prompt = PROMPTS.title(text)
  }
  if (!prompt) { inlineLoading.value = false; return }
  callSpark(prompt, cmd.key)
}

function runSlashCommand(cmd) {
  slashMenu.value.show = false
  const ta = editorRef.value
  if (!ta) return
  
  const pos = ta.selectionStart
  const before = editorContent.value.slice(0, pos)
  const lastNewline = before.lastIndexOf('\n')
  const lineStart = lastNewline + 1
  
  // 删除 / 字符
  editorContent.value = editorContent.value.slice(0, lineStart) + editorContent.value.slice(pos)
  
  inlineLoading.value = true
  inlineResult.value = { show: true, text: '', action: cmd.key, start: lineStart }
  inlineSelStart.value = lineStart
  
  let prompt = ''
  if (cmd.key === 'continue') {
    const text = editorContent.value.slice(-600).trim()
    if (!text) {
      inlineResult.value.text = '编辑器内容为空，无法续写'
      inlineLoading.value = false
      return
    }
    prompt = PROMPTS.continue(text, continueStyle.value, continueWords.value, pinnedOutline.value, charProfiles.value)
  } else if (cmd.key === 'scene') {
    prompt = PROMPTS.scene('盛唐长安，春日午后', continueStyle.value)
  } else if (cmd.key === 'dialogue') {
    prompt = PROMPTS.dialogue('李白与杜甫', '长安酒肆，初次相遇', pinnedOutline.value)
  } else if (cmd.key === 'outline') {
    const text = editorContent.value.slice(0, 400).trim()
    if (!text) {
      inlineResult.value.text = '编辑器内容为空，无法生成大纲'
      inlineLoading.value = false
      return
    }
    prompt = PROMPTS.outline(text)
  } else if (cmd.key === 'title') {
    const content = editorContent.value.trim()
    if (!content) {
      inlineResult.value.text = '编辑器内容为空，无法生成标题'
      inlineLoading.value = false
      return
    }
    prompt = PROMPTS.title(content)
  }
  
  callSpark(prompt, cmd.key)
}

// ═══════════════════════════════════════════════════════════
// § 10. 大纲操作函数
// ═══════════════════════════════════════════════════════════

// ── 人物卡状态 ───────────────────────────────────────────────
const charCards = ref([])
const charCardEditing = ref(false)
const charCardForm = ref({ id: null, name: '', alias: '', gender: '', identity: '', personality: '', motivation: '', relationships: '', notes: '' })

function addCharCard() {
  charCardForm.value = { id: null, name: '', alias: '', gender: '', identity: '', personality: '', motivation: '', relationships: '', notes: '' }
  charCardEditing.value = true
}

function editCharCard(ch) {
  charCardForm.value = { ...ch }
  charCardEditing.value = true
}

function saveCharCard() {
  if (!charCardForm.value.name.trim()) return
  if (charCardForm.value.id) {
    const idx = charCards.value.findIndex(c => c.id === charCardForm.value.id)
    if (idx >= 0) charCards.value[idx] = { ...charCardForm.value }
  } else {
    charCards.value.push({ ...charCardForm.value, id: Date.now() })
  }
  charCardEditing.value = false
  saveOutline()
}

function deleteCharCard(id) {
  charCards.value = charCards.value.filter(c => c.id !== id)
  saveOutline()
}

function bindCharCardToAI(ch) {
  const cardText = `【${ch.name}${ch.alias ? '（' + ch.alias + '）' : ''}】${
    ch.gender ? ' ' + ch.gender : ''}${
    ch.identity ? '，' + ch.identity : ''}\n性格：${ch.personality || '未设定'}\n动机：${ch.motivation || '未设定'}${
    ch.relationships ? '\n关系：' + ch.relationships : ''}${
    ch.notes ? '\n备注：' + ch.notes : ''}`

  if (charProfiles.value.includes(ch.name)) {
    // 取消绑定：移除该人物段落
    const parts = charProfiles.value.split(/\n(?=【)/)
    charProfiles.value = parts.filter(p => !p.includes(ch.name)).join('\n').trim()
  } else {
    charProfiles.value = charProfiles.value
      ? charProfiles.value.trimEnd() + '\n\n' + cardText
      : cardText
    aiPanelOpen.value = true
  }
}

function bindAllCharsToAI() {
  if (!charCards.value.length) return
  const text = charCards.value.map(ch =>
    `【${ch.name}${ch.alias ? '（' + ch.alias + '）' : ''}】${
      ch.gender ? ' ' + ch.gender : ''}${
      ch.identity ? '，' + ch.identity : ''}\n性格：${ch.personality || '未设定'}\n动机：${ch.motivation || '未设定'}${
      ch.relationships ? '\n关系：' + ch.relationships : ''}${
      ch.notes ? '\n备注：' + ch.notes : ''}`
  ).join('\n\n')
  charProfiles.value = text
  aiPanelOpen.value = true
}

// ── 世界观状态 ───────────────────────────────────────────────
const worldSetting = ref({ dynasty: '', geography: '', society: '', taboo: '', other: '' })
const worldSettingBound = ref('')  // 独立绑定通道，不污染 pinnedOutline
const worldBound = computed(() => !!worldSettingBound.value.trim())

const worldModules = ref([
  { key: 'dynasty', icon: '🏯', label: '朝代 / 时代背景', placeholder: '如：武周盛世，神龙元年前后，女帝当政，百官朝圣...', open: true },
  { key: 'geography', icon: '🗺️', label: '地理 / 重要场所', placeholder: '如：神都洛阳，则天门、含元殿；北境云州，阴山隘口...', open: false },
  { key: 'society', icon: '⚖️', label: '社会制度 / 阶层', placeholder: '如：科举取士，寒门与世族之争；九品官制，品阶与权力...', open: false },
  { key: 'taboo', icon: '🚫', label: '禁忌 / 规则 / 设定', placeholder: '如：不得直呼圣名；女官制度特殊晋升规则；私藏兵器罪同谋逆...', open: false },
  { key: 'other', icon: '📝', label: '其他设定 / 备注', placeholder: '如：特殊武器/法术设定、文化习俗、方言用词规范...', open: false },
])

function onWorldInput() {
  saveOutline()
}

function toggleWorldModule(mod) {
  mod.open = !mod.open
}

function buildWorldText() {
  return worldModules.value
    .filter(m => worldSetting.value[m.key]?.trim())
    .map(m => `【${m.label}】\n${worldSetting.value[m.key].trim()}`)
    .join('\n\n')
}

function bindWorldToAI() {
  const text = buildWorldText()
  if (!text) return
  // 世界观走独立通道，直接存入 worldSettingBound，不污染 pinnedOutline
  worldSettingBound.value = text
  aiPanelOpen.value = true
}

function clearWorldBinding() {
  worldSettingBound.value = ''
}

// ── 伏笔状态（组级共享） ───────────────────────────────────────
const plotHooks = ref([])
const hookFilter = ref('open') // all | open | resolved
const hooksBound = computed(() => buildOpenHooksText())

const currentChapterLabel = computed(() => {
  const writing = outlineChapters.value.find(c => c.status === 'writing')
  if (writing) return writing.title || '当前章节'
  const last = outlineChapters.value[outlineChapters.value.length - 1]
  return last?.title || '当前章节'
})

const filteredHooks = computed(() => {
  if (hookFilter.value === 'all') return plotHooks.value
  if (hookFilter.value === 'open') return plotHooks.value.filter(h => !h.resolved)
  return plotHooks.value.filter(h => h.resolved)
})

function addHook() {
  const title = window.prompt('请输入伏笔标题（简短）')?.trim()
  if (!title) return
  const note = window.prompt('请输入伏笔说明（可选）')?.trim() || ''
  plotHooks.value.unshift({
    id: Date.now(),
    title,
    note,
    sourceChapter: currentChapterLabel.value,
    resolved: false,
    resolvedChapter: '',
    createdAt: new Date().toISOString(),
  })
  saveOutline()
}

function toggleHookResolved(id) {
  const h = plotHooks.value.find(x => x.id === id)
  if (!h) return
  h.resolved = !h.resolved
  h.resolvedChapter = h.resolved ? currentChapterLabel.value : ''
  saveOutline()
}

function deleteHook(id) {
  plotHooks.value = plotHooks.value.filter(h => h.id !== id)
  saveOutline()
}

function insertHookToEditor(hook) {
  const ta = editorRef.value
  if (!ta) return
  const p = ta.selectionStart
  const text = `\n【伏笔】${hook.title}\n${hook.note || ''}\n`
  editorContent.value = editorContent.value.slice(0, p) + text + editorContent.value.slice(p)
  startAutoSave()
  setTimeout(() => ta.setSelectionRange(p + text.length, p + text.length), 0)
  insertBanner.value = `伏笔：${hook.title}`
  setTimeout(() => { insertBanner.value = '' }, 2000)
}

function buildOpenHooksText() {
  const opens = plotHooks.value.filter(h => !h.resolved)
  if (!opens.length) return ''
  return opens.map((h, i) =>
    `${i + 1}. 【${h.title}】（来源：${h.sourceChapter || '未知章节'}）${h.note ? '\n   说明：' + h.note : ''}`
  ).join('\n')
}

function bindHooksToAI() {
  if (hooksBound.value) aiPanelOpen.value = true
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

function bindChapterToAI(ch) {
  // 把该章节内容追加（或设置）到 pinnedOutline，并同步到 AI 面板
  const chText = `第${outlineChapters.value.indexOf(ch) + 1}章：${ch.title}${
    ch.summary ? '\n  情节：' + ch.summary : ''}${
    ch.characters ? '\n  人物：' + ch.characters : ''}`

  if (pinnedOutline.value.includes(ch.title)) {
    // 已绑定则移除该章节
    const lines = pinnedOutline.value.split('\n')
    const startIdx = lines.findIndex(l => l.includes(ch.title))
    if (startIdx >= 0) {
      // 移除该章节（连续行直到下一章或结尾）
      let endIdx = startIdx + 1
      while (endIdx < lines.length && !lines[endIdx].startsWith('第')) endIdx++
      lines.splice(startIdx, endIdx - startIdx)
      pinnedOutline.value = lines.join('\n').trim()
    }
  } else {
    pinnedOutline.value = pinnedOutline.value
      ? pinnedOutline.value.trimEnd() + '\n\n' + chText
      : chText
    aiPanelOpen.value = true
  }
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
      outlineData: JSON.stringify(outlineChapters.value),
      charProfilesJson: JSON.stringify(charCards.value),
      worldSetting: JSON.stringify(worldSetting.value),
      plotHooks: JSON.stringify(plotHooks.value),
    })
  }).catch(e => console.error('大纲保存失败', e))
  // 同步到系列共享档案
  saveSeries()
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

// ═══════════════════════════════════════════════════════════
// § 5. AI 上下文配置
// ═══════════════════════════════════════════════════════════
const aiPanelOpen = ref(true)
const aiPanelWidth = ref(Number(localStorage.getItem('ai_panel_width')) || 300)
const boundMats = ref([])
const pinnedOutline = ref('')
const charProfiles = ref('')
const continueStyle = ref('典雅')
const continueWords = ref(2200)

function removeBoundMat(mat) {
  const idx = boundMats.value.indexOf(mat)
  if (idx >= 0) boundMats.value.splice(idx, 1)
}

function applyToAI(item) {
  const idx = boundMats.value.findIndex(m => m.id === item.id)
  if (idx >= 0) {
    boundMats.value.splice(idx, 1)
    return
  }
  if (boundMats.value.length >= 5) boundMats.value.shift()
  boundMats.value.push(item)
  aiPanelOpen.value = true
}
// 能实际发送给后端的素材数量（过滤 mine_ 前缀）
const validBoundMatsCount = computed(() =>
  boundMats.value.filter(m => /^\d+$/.test(String(m.id))).length
)

// 面板拖拽调宽
function startAiResize(e) {
  e.preventDefault()
  const startX = e.clientX
  const startW = aiPanelWidth.value
  function onMove(ev) {
    const delta = startX - ev.clientX
    aiPanelWidth.value = Math.max(220, Math.min(520, startW + delta))
  }
  function onUp() {
    localStorage.setItem('ai_panel_width', aiPanelWidth.value)
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

// ═══════════════════════════════════════════════════════════
// § 6. AI 调用：旧面板表单状态（待重构，暂保留）
// ═══════════════════════════════════════════════════════════
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
const ctxYear   = ref('')
const ctxPlace  = ref('')
const rewriteText   = ref('')
const rewriteReq    = ref('')
const dialogueChars = ref('')
const dialogueScene = ref('')
const outlineStory  = ref('')
const titleContent  = ref('')
const checkText     = ref('')
const detailQ       = ref('')
const sceneInput    = ref('')
const sceneStyle    = ref('典雅')
const polishText    = ref('')
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

function openMatDetail(item) {
  matDetail.value = item
}

function formatAiParagraphs(text) {
  const lines = String(text || '').replace(/\r/g, '').split('\n')
  const paragraphs = []
  let cur = []
  for (const line of lines) {
    if (line.trim()) {
      cur.push(line.trim())
    } else if (cur.length) {
      paragraphs.push(cur.join(''))
      cur = []
    }
  }
  if (cur.length) paragraphs.push(cur.join(''))
  const indent = indentText()
  const gap = paragraphGap()
  return paragraphs.map(p => `${indent}${p}`).join(gap)
}

// AI 对话面板：接受消息插入编辑器
function handleAcceptMsg({ content, mode }) {
  const formatted = formatAiParagraphs(content)
  if (mode === 'append') {
    editorContent.value = editorContent.value.trimEnd() + '\n\n' + formatted + '\n'
    insertBanner.value = '✓ 已追加到编辑器'
  } else {
    const ta = editorRef.value
    if (ta && ta.selectionStart !== ta.selectionEnd) {
      const s = ta.selectionStart, e = ta.selectionEnd
      editorContent.value = editorContent.value.slice(0, s) + formatted + editorContent.value.slice(e)
      insertBanner.value = '✓ 已替换选中区'
    } else {
      editorContent.value = editorContent.value.trimEnd() + '\n\n' + formatted + '\n'
      insertBanner.value = '✓ 已追加到编辑器'
    }
  }
  startAutoSave()
  setTimeout(() => { insertBanner.value = '' }, 2500)
}

// 选中文本浮动工具栏
// ═══════════════════════════════════════════════════════════
// § 7. 行内 AI：选中工具栏 + / 指令菜单
// ═══════════════════════════════════════════════════════════
const selectionToolbar = ref({ show: false, x: 0, y: 0, text: '', start: 0, end: 0 })
const inlineResult = ref({ show: false, text: '', action: '', start: 0 })
const inlineOptions = ref([])
const inlineLoading = ref(false)
const inlineSelStart = ref(0)
const inlineSelEnd = ref(0)

const slashMenu = ref({ show: false, x: 0, y: 0, activeIdx: 0 })
// AI 生成段落标记（记录每次 AI 插入的文本内容，用于侧边高亮）
const aiSegments = ref([]) // [{ text: string, label: string }]
const slashCommands = [
  { key: 'continue', icon: '➡️', label: '续写', desc: '基于末尾 600 字续写' },
  { key: 'scene',    icon: '✍️', label: '场景', desc: '生成环境描写' },
  { key: 'dialogue', icon: '💬', label: '对话', desc: '生成人物对话' },
  { key: 'outline',  icon: '📋', label: '大纲', desc: '生成分章大纲' },
  { key: 'title',    icon: '📌', label: '标题', desc: '生成文章标题' },
]

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
      x: Math.min(e.clientX - containerRect.left, containerRect.width - 200),
      y: e.clientY - containerRect.top - 50,
      text: selected,
      start,
      end
    }
  } else {
    selectionToolbar.value.show = false
  }
}

function inlineAction(action) {
  const text = selectionToolbar.value.text
  if (!text) return
  selectionToolbar.value.show = false
  inlineSelStart.value = selectionToolbar.value.start
  inlineSelEnd.value = selectionToolbar.value.end
  inlineLoading.value = true
  inlineResult.value = { show: true, text: '', action, start: inlineSelStart.value }
  
  let prompt = ''
  if (action === 'polish') {
    prompt = `你是专业的历史古风文学编辑。请对以下文段进行深度润色，保持原意，重点提升古言意境、潜台词、心理细节与节奏层次，减少直白说明，只返回润色后的正文：\n\n${text}`
  } else if (action === 'expand') {
    prompt = `你是擅长历史题材的文学作家。请将以下文段扩写至原文的 1.5-2 倍长度，增加环境氛围、动作细节与冲突阻力，避免简单顺利地化解矛盾，保持风格一致，直接返回扩写后的文本：\n\n${text}`
  } else if (action === 'rewrite') {
    prompt = `你是专业的历史文学编辑。请用不同的表达方式改写以下文段，保持原意但提升表现力，严格按以下格式返回3个方案，不要添加任何其他说明：
===方案1===
(第一种改写)
===方案2===
(第二种改写)
===方案3===
(第三种改写)

原文：\n\n${text}`
  } else if (action === 'check') {
    prompt = `你是历史考证专家。请检查以下文段中的历史错误（食材、器物、词汇、制度等），按【错误点】格式列出，若无错误则回复"未发现明显错误"：\n\n${text}`
  }
  
  callSpark(prompt, action)
}

function acceptInline() {
  const newText = inlineResult.value.text
  const action = inlineResult.value.action
  const start = inlineSelStart.value
  const end = inlineSelEnd.value
  
  if (action === 'check') {
    // 检测结果只显示，不替换
    inlineResult.value.show = false
    return
  }

  // 生成类指令（续写/场景/对话/大纲/标题）：追加到文章末尾
  const appendActions = ['continue', 'scene', 'dialogue', 'outline', 'title']
  if (appendActions.includes(action)) {
    const cur = editorContent.value
    const formatted = formatAiParagraphs(newText)
    editorContent.value = cur.trimEnd() + '\n\n' + formatted + '\n'
    inlineResult.value.show = false
    const labelMapA = { continue: '续写', scene: '场景', dialogue: '对话', outline: '大纲', title: '标题' }
    aiSegments.value.push({ text: newText.trim(), label: labelMapA[action] || 'AI' })
    startAutoSave()
    insertBanner.value = '✓ 已插入到文章末尾'
    setTimeout(() => { insertBanner.value = '' }, 2000)
    return
  }

  // 替换类操作（润色/扩写/改写）：替换选中文本
  editorContent.value = editorContent.value.slice(0, start) + formatAiParagraphs(newText) + editorContent.value.slice(end)
  // 记录 AI 修改段落
  const labelMap2 = { polish: '润色', expand: '扩写', rewrite: '改写' }
  aiSegments.value.push({ text: newText.trim(), label: labelMap2[action] || 'AI' })
  inlineResult.value.show = false
  startAutoSave()
  insertBanner.value = `✓ 已${action === 'polish' ? '润色' : action === 'expand' ? '扩写' : '改写'}`
  setTimeout(() => { insertBanner.value = '' }, 2000)
}

function acceptOption(idx) {
  const opt = inlineOptions.value[idx]
  if (!opt) return
  const start = inlineSelStart.value
  const end = inlineSelEnd.value
  editorContent.value = editorContent.value.slice(0, start) + formatAiParagraphs(opt) + editorContent.value.slice(end)
  startAutoSave()
  inlineOptions.value = []
  inlineResult.value.show = false
  insertBanner.value = '✓ 已选用方案 ' + (idx + 1)
  setTimeout(() => { insertBanner.value = '' }, 2000)
}


function retryInline() {
  inlineLoading.value = true
  inlineResult.value.text = ''
  const text = selectionToolbar.value.text
  const action = inlineResult.value.action
  
  let prompt = ''
  if (action === 'polish') {
    prompt = `你是专业的历史古风文学编辑。请对以下文段进行深度润色，保持原意，重点提升古言意境、潜台词、心理细节与节奏层次，减少直白说明，只返回润色后的正文：\n\n${text}`
  } else if (action === 'expand') {
    prompt = `你是擅长历史题材的文学作家。请将以下文段扩写至原文的 1.5-2 倍长度，增加环境氛围、动作细节与冲突阻力，避免简单顺利地化解矛盾，保持风格一致，直接返回扩写后的文本：\n\n${text}`
  } else if (action === 'rewrite') {
    prompt = `你是专业的历史文学编辑。请用不同的表达方式改写以下文段，保持原意但提升表现力，严格按以下格式返回3个方案，不要添加任何其他说明：
===方案1===
(第一种改写)
===方案2===
(第二种改写)
===方案3===
(第三种改写)

原文：\n\n${text}`
  } else if (action === 'check') {
    prompt = `你是历史考证专家。请检查以下文段中的历史错误（食材、器物、词汇、制度等），按【错误点】格式列出，若无错误则回复"未发现明显错误"：\n\n${text}`
  }
  
  callSpark(prompt, action)
}

function discardInline() {
  inlineOptions.value = []
  inlineResult.value.show = false
}

function parseSseDataLine(line) {
  if (!line.startsWith('data:')) return null
  let data = line.slice(5)
  if (data.startsWith(' ')) data = data.slice(1)
  if (data.endsWith('\r')) data = data.slice(0, -1)
  return data === '' ? '\n' : data
}

// 提示词模板
// ═══════════════════════════════════════════════════════════
// § 12. AI 调用核心（PROMPTS + callSpark + runXxx）
// ═══════════════════════════════════════════════════════════
function buildAiSystemPrompt() {
  const outline = pinnedOutline.value?.trim()
  const chars = charProfiles.value?.trim()
  const world = worldSettingBound.value?.trim()
  const hooks = hooksBound.value?.trim()
  const tail = editorContent.value?.trim().slice(-1200)
  const mats = (boundMats.value || []).slice(0, 5)

  let sys = `你是历史小说创作助手。你的目标不是堆砌素材，而是把素材转化为剧情因果、人物动作、环境细节和制度约束。
总原则：
1）素材融合必须“隐性渗透”，禁止逐条点名、逐条陈列素材；
2）若素材与时代设定冲突，优先服从世界观/朝代/大纲，不得跨朝代照搬官职、礼制、称谓；
3）对话要有潜台词与克制感，减少直白解释；
4）冲突推进要有阻力与代价，不能一招化解；
5）只在信息不足时做最小必要补全；
6）默认只输出可直接使用的内容，不要题外话。输出请使用自然分段，段间空一行；每段首行请使用两个全角空格缩进。\n\n`

  if (mats.length) {
    sys += '【绑定素材（需语义融合，不可堆砌）】\n'
    mats.forEach((m, i) => {
      const title = m?.title || `素材${i + 1}`
      const content = (m?.content || '').slice(0, 500)
      sys += `${i + 1}. ${title}\n${content}\n\n`
    })
    sys += '使用规则：优先提炼素材中的制度、器物、礼法、氛围、人物处境与叙事功能，只选择最契合当前段落的少数细节自然嵌入正文。\n\n'
  }
  if (outline) sys += `【故事大纲（最高优先级）】\n${outline}\n\n`
  if (chars) sys += `【人物设定（最高优先级）】\n${chars}\n\n`
  if (world) sys += `【世界观/设定（最高优先级）】\n${world}\n\n`
  if (hooks) sys += `【未回收伏笔（必须优先呼应）】\n${hooks}\n\n`
  if (tail) sys += `【编辑区末尾内容（最高优先级上下文）】\n${tail}\n\n`

  sys += '写作时请在内部先判断：当前时代边界、当前主冲突、可自然融合的素材细节、人物的隐性情绪表达方式。不要输出判断过程，只输出最终正文。'

  return sys
}

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
请根据以下场景要素，生成一段400-700字的环境与行动描写，要求：
- 历史细节准确（器物、植物、建筑、称谓符合时代）
- 调动视觉、听觉、嗅觉多种感官
- 场景细节必须服务人物情绪或后续冲突，不可堆砌
- 语言${style}，可直接嵌入小说正文
- 不出现现代词汇
- 末句保留叙事张力

场景要素：${scene}`,

  continue: (text, style, wordCount, outline, chars) =>
    `任务：长篇小说续写（高约束模式）

请严格按以下要求生成：
1）优先依据已绑定素材、大纲、人物设定、世界观、伏笔与编辑区末尾上下文；
2）与现有叙事保持同一人称、时态、语气和节奏；
3）围绕当前冲突持续加压，不跳时间线，不新增重大设定；
4）冲突不可被一句话或一个动作轻易化解，至少写出试探、受阻、反制、代价中的两项；
5）素材只能作为制度约束、器物细节、环境氛围、人物动作的来源自然渗透，禁止逐条罗列素材；
6）若素材存在跨朝代制度/称谓冲突，以当前世界观和大纲为准，只保留可迁移信息；
7）人物行为符合既有人设与动机；
8）对话要有潜台词和留白，避免现代口语和过直白心理描写；
9）生成长度约${Math.max(1800, Number(wordCount) || 2200)}-2600字，风格偏${style}。
${outline ? `
【当前故事大纲（最高优先级）】
${outline}
` : ''}
${chars ? `
【主要人物设定（最高优先级）】
${chars}
` : ''}
输出要求：
- 只输出可直接接在正文后的连续文本；
- 不要标题、不要说明、不要分点。

已有文段（以下为正文末尾，请从此处自然续写）：
${text}`,

  rewrite: (text, req) =>
    `任务：定向改写（保留信息、提升表达）

改写要求：${req}

请按以下约束执行：
1）优先参考绑定素材/大纲/人物/编辑区上下文，若有冲突以绑定内容为准；
2）保留原文关键信息、事件顺序与因果关系；
3）优化表达、节奏与可读性，避免空泛修辞；
4）只输出改写后的正文，不要解释。

原文：
${text}`,

  dialogue: (chars, scene, outline) =>
    `任务：人物对话生成（贴合人设）

请根据以下信息生成400-800字对话片段：
${outline ? `【故事大纲（最高优先级）】
${outline}
` : ''}
【人物设定（最高优先级）】
${chars}

场景：${scene}

要求：
1）对话语气、用词、行为符合人设与时代；
2）对话需推动情节或揭示关系，不空转；
3）多用潜台词、停顿、动作、视线、景物映衬，减少直白说理；
4）若存在权力压迫或利益博弈，必须体现试探与回锋；
5）只输出可直接入文的正文。`,

  outline: (story) =>
    `任务：连载分章大纲生成（可执行）

请基于以下梗概与绑定上下文，输出5-8章分章大纲：
${story}

每章包含：
- 章节标题
- 核心事件
- 冲突推进
- 人物变化
- 章节钩子

硬性要求：时间线连续、人物动机自洽、每章都推进主线。
输出要求：直接输出分章内容，不要前言。`,

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
  const isInline = ['polish', 'expand', 'rewrite', 'check', 'continue', 'scene', 'dialogue', 'outline', 'title'].includes(tabKey)
  
  if (isInline) {
    inlineLoading.value = true
    inlineResult.value.text = ''
  } else {
  aiLoading.value = true
  aiResult.value = ''
  aiError.value = ''
  }
  
    const token = localStorage.getItem('token')
    const payload = { prompt, systemPrompt: buildAiSystemPrompt() }
    // 过滤掉 mine_ 前缀的用户自建素材（id 非数字，后端 Long 类型无法解析）
    const validBoundMats = boundMats.value.filter(m => {
      const id = String(m.id)
      return /^\d+$/.test(id)
    })
    if (validBoundMats.length === 1) {
      payload.materialId = Number(validBoundMats[0].id)
    } else if (validBoundMats.length > 1) {
      payload.materialIds = validBoundMats.map(m => Number(m.id))
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
      let errMsg = '生成失败，请稍后重试'
      if (code === 429) errMsg = '⏰ AI 调用过于频繁，请稍后再试'
      else if (code === 403) errMsg = '📊 AI 调用额度已用完，请联系管理员'
      else if (code === 401) errMsg = '🔑 登录已过期，请重新登录'
      else errMsg = err?.message || errMsg
      
      if (isInline) {
        inlineLoading.value = false
        inlineResult.value.text = errMsg
    } else {
        aiError.value = errMsg
        aiLoading.value = false
      }
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
      buffer = lines.pop() ?? ''
      for (const line of lines) {
        const data = parseSseDataLine(line)
        if (data == null) continue
        if (data === '[DONE]') {
          if (isInline) {
            const raw = inlineResult.value.text
            const parts = raw.split(/===方案\d+===/).map(s => s.trim()).filter(Boolean)
            inlineOptions.value = parts.length > 1 ? parts : []
            inlineLoading.value = false
          } else {
            if (aiResult.value) saveToHistory(tabKey, prompt, aiResult.value)
            aiLoading.value = false
          }
          return
        }
        if (isInline) {
          inlineResult.value.text += data
        } else {
          aiResult.value += data
        }
      }
    }
    // 流读完但未收到 [DONE]（网关截断等情况）的兜底
    if (isInline) {
      const raw2 = inlineResult.value.text
      const parts2 = raw2.split(/===方案\d+===/).map(s => s.trim()).filter(Boolean)
      inlineOptions.value = parts2.length > 1 ? parts2 : []
      inlineLoading.value = false
    } else {
      if (aiResult.value) saveToHistory(tabKey, prompt, aiResult.value)
      aiLoading.value = false
    }
  } catch(e) {
    const errMsg = !navigator.onLine ? '📡 网络已断开' : '⚠️ 服务暂时不可用'
    if (isInline) {
      inlineLoading.value = false
      inlineResult.value.text = errMsg
      // 错误时保持 show=true 让用户看到错误信息，但停止 loading
    } else {
      aiError.value = errMsg
    aiLoading.value = false
    }
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
  const prompt = `你是专业的历史古风文学编辑，请对以下文段进行深度润色。
要求：
1）保持原意和事件顺序不变；
2）重点提升古言意境、潜台词、心理描写细腻度与节奏层次；
3）减少直白表述，增加含蓄、留白、景物映衬；
4）若涉及时代称谓、礼制、器物，请保持与已绑定设定一致；
5）只返回润色后的正文，不要解释。\n\n${polishText.value.trim()}`
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
  const formatted = formatAiParagraphs(para)
  editorContent.value = before + '\n\n' + formatted + '\n\n' + after
  startAutoSave()
  insertBanner.value = '已插入段落'
  setTimeout(() => insertBanner.value = '', 2000)
}

function insertAiResult() {
  if (!aiResult.value || !editorRef.value) return
  const el = editorRef.value
  const start = el.selectionStart
  const before = editorContent.value.slice(0, start)
  const after  = editorContent.value.slice(start)
  const formatted = formatAiParagraphs(aiResult.value)
  editorContent.value = before + '\n\n' + formatted + '\n\n' + after
  startAutoSave()
  insertBanner.value = 'AI助手结果'
  setTimeout(() => insertBanner.value = '', 2000)
}

// ═══════════════════════════════════════════════════════════
// § 14. 生命周期
// ═══════════════════════════════════════════════════════════
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
.workspace-shell { display: flex; flex-direction: row; height: calc(100vh - 60px); overflow: hidden; background: var(--bg-base); }
/* 右侧 AI 侧边栏：与 editor-area 并排 */
.editor-area { position: relative; flex: 1; min-width: 0; display: flex; flex-direction: column; overflow: hidden; }
.material-sidebar { width: 270px; flex-shrink: 0; background: var(--bg-sidebar); border-right: 1px solid var(--border); display: flex; flex-direction: column; transition: width 0.25s; overflow: hidden; }
.material-sidebar.collapsed { width: 36px; }
.sidebar-header { display: flex; align-items: center; justify-content: space-between; padding: 0.75rem; border-bottom: 1px solid var(--border); flex-shrink: 0; gap: 0.4rem; }
.series-badge { display: inline-block; font-size: 0.65rem; color: var(--primary); background: rgba(var(--primary-rgb), 0.1); border-radius: 4px; padding: 0.1rem 0.35rem; margin-left: 0.35rem; font-weight: 600; vertical-align: middle; max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sidebar-title { font-size: 0.8rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-muted); }
.collapse-btn { background: transparent; border: none; color: var(--text-muted); cursor: pointer; font-size: 1rem; padding: 0.1rem 0.3rem; border-radius: 4px; }
.collapse-btn:hover { color: var(--primary); }
.side-panel-tabs { display: flex; border-bottom: 2px solid var(--border); flex-shrink: 0; background: var(--bg-sidebar); }
.side-panel-tabs button { flex: 1; padding: 0.45rem 0.2rem; border: none; background: transparent; color: var(--text-muted); font-size: 0.72rem; font-weight: 600; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s; }
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
.char-card { border-left: 3px solid var(--primary); }
.char-card-name { font-weight: 700; font-size: 0.85rem; color: var(--text-main); }
.char-card-identity { font-size: 0.72rem; color: var(--text-muted); margin-left: 0.4rem; }

/* ── 人物卡弹窗 ── */
.char-modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.55); z-index: 1100; display: flex; align-items: center; justify-content: center; padding: 1.5rem; }
.char-modal-dialog { background: var(--bg-card); border: 1px solid var(--border); border-radius: 14px; width: min(660px, 92vw); max-height: 88vh; display: flex; flex-direction: column; box-shadow: 0 24px 64px rgba(0,0,0,0.4); overflow: hidden; }
.char-modal-hd { display: flex; align-items: center; justify-content: space-between; padding: 1rem 1.25rem; border-bottom: 1px solid var(--border); flex-shrink: 0; }
.char-modal-title { display: flex; align-items: center; gap: 0.5rem; font-size: 1rem; font-weight: 700; color: var(--text-main); }
.char-modal-icon { font-size: 1.1rem; }
.char-modal-close { background: none; border: none; color: var(--text-muted); font-size: 1.1rem; cursor: pointer; padding: 0.2rem 0.4rem; border-radius: 4px; transition: color 0.15s; }
.char-modal-close:hover { color: var(--text-main); }
.char-modal-body { flex: 1; overflow-y: auto; padding: 1.25rem; display: flex; flex-direction: column; gap: 1rem; }
.char-fields-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.85rem; }
.char-field { display: flex; flex-direction: column; gap: 0.3rem; }
.char-field-full { display: flex; flex-direction: column; gap: 0.3rem; }
.char-label { font-size: 0.72rem; font-weight: 600; color: var(--text-muted); letter-spacing: 0.03em; }
.char-required { color: #e05252; }
.char-input { background: var(--bg-main); border: 1px solid var(--border); border-radius: 7px; padding: 0.45rem 0.65rem; font-size: 0.82rem; color: var(--text-main); outline: none; transition: border-color 0.15s; width: 100%; box-sizing: border-box; }
.char-input:focus { border-color: var(--primary); }
.char-select { cursor: pointer; }
.char-textarea { background: var(--bg-main); border: 1px solid var(--border); border-radius: 7px; padding: 0.45rem 0.65rem; font-size: 0.82rem; color: var(--text-main); outline: none; resize: vertical; min-height: 72px; width: 100%; box-sizing: border-box; font-family: inherit; line-height: 1.6; transition: border-color 0.15s; }
.char-textarea:focus { border-color: var(--primary); }
.char-modal-footer { display: flex; justify-content: flex-end; gap: 0.75rem; padding: 0.9rem 1.25rem; border-top: 1px solid var(--border); flex-shrink: 0; background: var(--bg-card); }
.char-btn-cancel { background: transparent; border: 1px solid var(--border); color: var(--text-muted); border-radius: 7px; padding: 0.45rem 1.1rem; font-size: 0.82rem; cursor: pointer; transition: all 0.15s; }
.char-btn-cancel:hover { border-color: var(--text-muted); color: var(--text-main); }
.char-btn-save { background: var(--primary); border: none; color: #fff; border-radius: 7px; padding: 0.45rem 1.4rem; font-size: 0.82rem; font-weight: 600; cursor: pointer; transition: opacity 0.15s; }
.char-btn-save:hover { opacity: 0.88; }

/* ── 世界观面板 ── */
.world-panel { flex: 1; overflow-y: auto; padding: 0.5rem 0; }
.world-module { border-bottom: 1px solid var(--border); }
.world-module-hd { display: flex; align-items: center; gap: 0.4rem; padding: 0.6rem 0.75rem; cursor: pointer; user-select: none; transition: background 0.15s; }
.world-module-hd:hover { background: rgba(var(--primary-rgb), 0.06); }
.world-module-icon { font-size: 0.9rem; }
.world-module-title { flex: 1; font-size: 0.78rem; font-weight: 600; color: var(--text-main); }
.world-module-arrow { font-size: 0.7rem; color: var(--text-muted); }
.world-module-body { padding: 0 0.75rem 0.75rem; }
.world-textarea { width: 100%; box-sizing: border-box; background: var(--bg-main); border: 1px solid var(--border); border-radius: 7px; padding: 0.5rem 0.65rem; font-size: 0.78rem; color: var(--text-main); resize: vertical; min-height: 90px; font-family: inherit; line-height: 1.65; outline: none; transition: border-color 0.15s; }
.world-textarea:focus { border-color: var(--primary); }
.world-expand-enter-active, .world-expand-leave-active { transition: max-height 0.22s ease, opacity 0.18s; overflow: hidden; }
.world-expand-enter-from, .world-expand-leave-to { max-height: 0; opacity: 0; }
.world-expand-enter-to, .world-expand-leave-from { max-height: 400px; opacity: 1; }
.world-footer { padding: 0.45rem 0.75rem; border-top: 1px solid var(--border); text-align: center; flex-shrink: 0; }
.world-bound-hint { font-size: 0.72rem; color: var(--primary); font-weight: 600; }
.world-unbound-hint { font-size: 0.72rem; color: var(--text-muted); }

/* ── 伏笔面板 ── */
.hooks-filter-row { display: flex; gap: 0.4rem; padding: 0.45rem 0.7rem; border-bottom: 1px solid var(--border); }
.hook-card .hook-title { font-weight: 700; font-size: 0.78rem; color: var(--text-main); }
.hook-card .hook-from { margin-left: auto; font-size: 0.68rem; color: var(--text-muted); }
.hook-status { font-size: 0.68rem; padding: 0.12rem 0.38rem; border-radius: 999px; font-weight: 600; }
.hook-status.open { color: #c26b00; background: rgba(194, 107, 0, 0.12); }
.hook-status.resolved { color: #2c7a3f; background: rgba(44, 122, 63, 0.12); }
.hook-resolve-ch { font-size: 0.68rem; color: var(--text-muted); margin-left: 0.5rem; }

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
.editor-toolbar { display: flex; align-items: center; gap: 0.75rem; padding: 0.5rem 1rem; border-bottom: 1px solid var(--border); background: var(--bg-card); flex-shrink: 0; }
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
.autosave-bar { position: relative; height: 20px; background: var(--bg-input); border-bottom: 1px solid var(--border); display: flex; align-items: center; flex-shrink: 0; overflow: hidden; transition: opacity 0.3s; }
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
.rich-toolbar-wrap { border-bottom: 1px solid var(--border); background: var(--bg-input); }
.rich-toolbar { display: flex; align-items: center; gap: 0.25rem; padding: 0.35rem 1rem; background: var(--bg-input); flex-shrink: 0; flex-wrap: wrap; }
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
  padding: 0.8rem 4% 2rem;
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

/* 文字选中样式优化 - 柔和金色 */
.editor-textarea::selection {
  background-color: rgba(251,192,45,0.3);
  color: inherit;
}

.editor-textarea::-moz-selection {
  background-color: rgba(251,192,45,0.3);
  color: inherit;
}
.selection-toolbar {
  position: absolute;
  z-index: 200;
  display: flex;
  gap: 0.2rem;
  align-items: center;
  background: linear-gradient(135deg, #f5f0e8 0%, #ede6d8 100%);
  border: 1.5px solid #d8cfc0;
  border-radius: 10px;
  padding: 0.5rem 0.65rem;
  box-shadow: 0 12px 40px rgba(0,0,0,0.18), 0 1px 3px rgba(0,0,0,0.08);
  pointer-events: all;
  animation: toolbar-appear 0.2s cubic-bezier(0.34,1.56,0.64,1);
  backdrop-filter: blur(8px);
}

@keyframes toolbar-appear {
  from { opacity: 0; transform: translateY(8px) scale(0.88); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.sel-toolbar-divider { 
  width: 1px; 
  height: 20px; 
  background: linear-gradient(to bottom, transparent, #c8bfb0, transparent);
  margin: 0 0.15rem;
  opacity: 0.6;
}

.sel-toolbar-btn {
  display: flex; 
  align-items: center; 
  gap: 0.35rem;
  font-size: 0.8rem; 
  font-weight: 600;
  padding: 0.4rem 0.85rem;
  background: transparent;
  color: #3a2e1e;
  border: 1.5px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.18s cubic-bezier(0.4, 0, 0.2, 1);
  letter-spacing: 0.02em;
}

.sel-toolbar-btn:hover {
  background: rgba(255,255,255,0.6);
  border-color: #c8bfb0;
  color: #2c2010;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.sel-toolbar-btn:active {
  transform: translateY(0);
}

.sel-toolbar-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.sel-toolbar-btn.polish:hover { 
  background: linear-gradient(135deg, rgba(233,69,96,0.15), rgba(233,69,96,0.08));
  border-color: rgba(233,69,96,0.3);
  color: #c0392b;
}

.sel-toolbar-btn.expand:hover { 
  background: linear-gradient(135deg, rgba(52,152,219,0.15), rgba(52,152,219,0.08));
  border-color: rgba(52,152,219,0.3);
  color: #2980b9;
}

.sel-toolbar-btn.rewrite:hover { 
  background: linear-gradient(135deg, rgba(155,89,182,0.15), rgba(155,89,182,0.08));
  border-color: rgba(155,89,182,0.3);
  color: #8e44ad;
}

.sel-toolbar-btn.check:hover { 
  background: linear-gradient(135deg, rgba(241,196,15,0.15), rgba(241,196,15,0.08));
  border-color: rgba(241,196,15,0.3);
  color: #d68910;
}

.sel-toolbar-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  background: transparent;
  border: none;
  color: #8a7a65;
  font-size: 1rem;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.15s;
  margin-left: 0.2rem;
}

.sel-toolbar-close:hover {
  background: rgba(0,0,0,0.08);
  color: #c0392b;
}

/* / 指令菜单 */
.slash-menu { position: fixed; background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; box-shadow: 0 8px 32px var(--shadow); z-index: 2000; min-width: 280px; max-height: 360px; overflow-y: auto; }
.slash-menu-header { padding: 0.6rem 1rem; font-size: 0.8rem; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid var(--border); }
.slash-cmd { width: 100%; padding: 0.7rem 1rem; border: none; background: transparent; color: var(--text-main); text-align: left; cursor: pointer; display: flex; align-items: center; gap: 0.75rem; transition: all 0.15s; }
.slash-cmd:hover, .slash-cmd.active { background: var(--bg-hover); }
.slash-cmd-icon { font-size: 1.1rem; flex-shrink: 0; }
.slash-cmd-info { flex: 1; min-width: 0; }
.slash-cmd-label { display: block; font-size: 0.9rem; font-weight: 600; color: var(--text-main); }
.slash-cmd-desc { display: block; font-size: 0.75rem; color: var(--text-muted); margin-top: 0.15rem; }

/* 行内 AI 结果预览条 */
.inline-ai-bar { position: fixed; bottom: 2rem; left: 50%; transform: translateX(-50%); background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; box-shadow: 0 12px 40px var(--shadow); z-index: 1500; max-width: 90%; width: 600px; max-height: 280px; display: flex; flex-direction: column; }
.inline-ai-bar-top { display: flex; align-items: center; justify-content: space-between; padding: 0.75rem 1rem; border-bottom: 1px solid var(--border); flex-shrink: 0; }
.inline-ai-label { display: flex; align-items: center; gap: 0.5rem; font-size: 0.9rem; font-weight: 600; color: var(--text-main); }
.inline-ai-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--primary); }
.inline-ai-dot.loading { animation: pulse 1.5s ease-in-out infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
.inline-ai-actions { display: flex; gap: 0.5rem; }
.inline-accept { padding: 0.35rem 0.8rem; border-radius: 6px; border: none; background: #10b981; color: #fff; font-size: 0.8rem; font-weight: 600; cursor: pointer; }
.inline-accept:hover { background: #059669; }
.inline-retry { padding: 0.35rem 0.8rem; border-radius: 6px; border: 1px solid var(--border); background: transparent; color: var(--text-sub); font-size: 0.8rem; font-weight: 600; cursor: pointer; }
.inline-retry:hover { border-color: var(--primary); color: var(--primary); }
.inline-discard { padding: 0.35rem 0.8rem; border-radius: 6px; border: none; background: transparent; color: var(--text-muted); font-size: 0.8rem; cursor: pointer; }
.inline-discard:hover { color: #ef4444; }
.inline-ai-text { flex: 1; overflow-y: auto; padding: 0.75rem 1rem; font-size: 0.85rem; color: var(--text-main); line-height: 1.6; white-space: pre-wrap; word-break: break-word; margin: 0; }

.inline-ai-enter { animation: slideUp 0.3s ease; }
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
.inline-ai-leave-active { animation: slideDown 0.2s ease; }
@keyframes slideDown { from { opacity: 1; transform: translateY(0); } to { opacity: 0; transform: translateY(20px); } }.editor-word-count {
  position: absolute;
  right: 1rem;
  bottom: 0.8rem;
  z-index: 15;
  font-size: 0.78rem;
  color: var(--text-muted);
  background: rgba(0,0,0,0.18);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 0.18rem 0.55rem;
  user-select: none;
  pointer-events: none;
}

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
.focus-mode .ai-sidebar { display: none !important; }
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
  padding: 1rem 8% 2.2rem !important;
  font-size: 1.1rem !important;
  line-height: 2.4 !important;
  background: var(--bg-editor, var(--bg-card)) !important;
}
.focus-mode .editor-toolbar {
  padding: 0.45rem 8% !important;
  background: var(--bg-editor, var(--bg-card)) !important;
  border-bottom-color: transparent !important;
}
.focus-mode .rich-toolbar {
  justify-content: center !important;
  padding: 0.3rem 8% !important;
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

/* ── 右侧 AI 上下文配置侧边栏 ─────────────────────────────── */
.editor-area { position: relative; flex: 1; display: flex; flex-direction: column; overflow: hidden; }

/* 侧边栏容器 */
.ai-sidebar { flex-shrink: 0; width: 300px; background: var(--bg-sidebar); border-left: 1px solid var(--border); display: flex; flex-direction: row; position: relative; transition: width 0.22s cubic-bezier(0.4,0,0.2,1); overflow: hidden; }
.ai-sidebar.collapsed { width: 32px; }

/* 拖拽手柄 */
.ai-resize-handle { position: absolute; left: 0; top: 0; bottom: 0; width: 5px; cursor: col-resize; background: transparent; z-index: 10; transition: background 0.15s; }
.ai-resize-handle:hover { background: var(--primary); opacity: 0.5; }

/* 收起书签栏 */
.ai-sidebar-tab { width: 32px; display: flex; flex-direction: column; align-items: center; justify-content: flex-start; padding-top: 1.5rem; gap: 0.5rem; cursor: pointer; background: var(--bg-sidebar); transition: background 0.15s; user-select: none; }
.ai-sidebar-tab:hover { background: var(--bg-hover); }
.ai-sidebar-tab-icon { font-size: 0.9rem; color: var(--primary); }
.ai-sidebar-tab-text { writing-mode: vertical-rl; font-size: 0.7rem; font-weight: 700; color: var(--text-muted); letter-spacing: 2px; }

/* 展开面板 */
.ai-sidebar-inner { flex: 1; display: flex; flex-direction: column; overflow-y: auto; padding-left: 5px; }
.ai-sidebar-header { display: flex; align-items: center; justify-content: space-between; padding: 0.8rem 1rem 0.8rem 1.2rem; border-bottom: 1px solid var(--border); flex-shrink: 0; background: linear-gradient(135deg, var(--primary), var(--primary-light)); }
.ai-sidebar-title { color: #fff; font-weight: 700; font-size: 0.88rem; }
.ai-sidebar-collapse { background: rgba(255,255,255,0.2); border: none; color: #fff; border-radius: 50%; width: 24px; height: 24px; cursor: pointer; font-size: 1rem; display: flex; align-items: center; justify-content: center; transition: background 0.2s; font-weight: 700; }
.ai-sidebar-collapse:hover { background: rgba(255,255,255,0.35); }

/* 配置区块 */
.ai-cfg-section { padding: 0.85rem 1rem; border-bottom: 1px solid var(--border); flex-shrink: 0; }
.ai-cfg-section-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.55rem; font-size: 0.8rem; font-weight: 700; color: var(--text-main); }
.ai-cfg-count { font-size: 0.72rem; color: var(--text-muted); font-weight: 400; }
.ai-cfg-badge { font-size: 0.68rem; font-weight: 600; padding: 0.1rem 0.45rem; border-radius: 10px; }
.ai-cfg-badge.on  { background: rgba(16,185,129,0.15); color: #10b981; }
.ai-cfg-badge.off { background: var(--bg-hover); color: var(--text-muted); }
.ai-cfg-empty { font-size: 0.75rem; color: var(--text-muted); line-height: 1.6; }
.ai-cfg-chips { display: flex; flex-direction: column; gap: 0.35rem; }
.ai-cfg-chip { display: flex; align-items: center; justify-content: space-between; background: var(--bg-input); border: 1px solid var(--border); border-radius: 6px; padding: 0.3rem 0.6rem; font-size: 0.78rem; }
.ai-cfg-chip-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-main); }
.ai-cfg-chip-del { background: transparent; border: none; color: var(--text-muted); cursor: pointer; font-size: 0.7rem; padding: 0 0.2rem; transition: color 0.15s; }
.ai-cfg-chip-del:hover { color: #ef4444; }
.ai-cfg-clear { margin-top: 0.25rem; background: transparent; border: none; color: var(--text-muted); font-size: 0.72rem; cursor: pointer; padding: 0; text-decoration: underline; }
.ai-cfg-clear:hover { color: #ef4444; }
.ai-cfg-textarea { width: 100%; padding: 0.55rem 0.7rem; border: 1.5px solid var(--border); border-radius: 7px; background: var(--bg-input); color: var(--text-main); font-size: 0.8rem; line-height: 1.6; resize: vertical; box-sizing: border-box; transition: border-color 0.2s; font-family: inherit; }
.ai-cfg-textarea:focus { outline: none; border-color: var(--primary); }
.ai-cfg-row { display: flex; align-items: center; gap: 0.5rem; }
.ai-cfg-label { font-size: 0.75rem; color: var(--text-muted); white-space: nowrap; flex-shrink: 0; }
.ai-cfg-words { display: flex; gap: 0.3rem; flex-wrap: wrap; }
.ai-cfg-word-btn { padding: 0.2rem 0.55rem; border: 1px solid var(--border); border-radius: 5px; background: transparent; color: var(--text-sub); font-size: 0.75rem; cursor: pointer; transition: all 0.15s; }
.ai-cfg-word-btn.active { background: var(--primary); color: #fff; border-color: var(--primary); }
.ai-cfg-select { flex: 1; padding: 0.3rem 0.5rem; border: 1px solid var(--border); border-radius: 5px; background: var(--bg-input); color: var(--text-main); font-size: 0.78rem; cursor: pointer; }
.ai-cfg-select:focus { outline: none; border-color: var(--primary); }
.ai-cfg-quick-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.4rem; }
.ai-cfg-quick-btn { padding: 0.45rem 0.4rem; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-input); color: var(--text-main); font-size: 0.78rem; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 0.3rem; transition: all 0.15s; font-weight: 500; }
.ai-cfg-quick-btn:hover { border-color: var(--primary); color: var(--primary); background: var(--bg-hover); }
.ai-cfg-quick-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ai-cfg-tip { margin-top: 0.6rem; font-size: 0.72rem; color: var(--text-muted); line-height: 1.5; }
.ai-cfg-tip kbd { background: var(--bg-input); border: 1px solid var(--border); border-radius: 3px; padding: 0.05rem 0.3rem; font-size: 0.7rem; font-family: monospace; }

/* 上下文摘要面板 */
.ai-ctx-summary .ai-ctx-summary-body { display: flex; flex-direction: column; gap: 0.3rem; padding: 0.4rem 0.2rem; }
.ai-ctx-row { display: flex; align-items: center; gap: 0.4rem; font-size: 0.75rem; color: var(--text-muted, #888); padding: 0.25rem 0.4rem; border-radius: 5px; background: var(--bg-input, #f5f5f5); transition: background 0.2s, color 0.2s; }
.ai-ctx-row.active { color: var(--accent, #7c6af7); background: rgba(124,106,247,0.08); }
.ai-ctx-icon { font-size: 0.85rem; flex-shrink: 0; }
.ai-ctx-warn { font-size: 0.72rem; color: #e67e22; background: rgba(230,126,34,0.1); border-radius: 5px; padding: 0.2rem 0.4rem; line-height: 1.4; }

/* AI 生成内容标记面板 */
.ai-marker-panel { margin: 0.5rem 0.75rem; background: var(--bg-input); border: 1px solid var(--border); border-radius: 8px; overflow: hidden; flex-shrink: 0; }
.ai-marker-header { display: flex; align-items: center; justify-content: space-between; padding: 0.4rem 0.75rem; background: linear-gradient(90deg, rgba(99,102,241,0.12), transparent); border-bottom: 1px solid var(--border); font-size: 0.75rem; font-weight: 700; color: var(--primary); }
.ai-marker-clear { background: transparent; border: none; color: var(--text-muted); font-size: 0.7rem; cursor: pointer; padding: 0; }
.ai-marker-clear:hover { color: #ef4444; }
.ai-marker-list { display: flex; flex-direction: column; max-height: 160px; overflow-y: auto; }
.ai-marker-item { display: flex; align-items: center; gap: 0.5rem; padding: 0.35rem 0.75rem; border-bottom: 1px solid var(--border); transition: background 0.15s; }
.ai-marker-item:last-child { border-bottom: none; }
.ai-marker-item:hover { background: var(--bg-hover); }
.ai-marker-badge { flex-shrink: 0; font-size: 0.65rem; font-weight: 700; padding: 0.1rem 0.4rem; border-radius: 10px; background: linear-gradient(135deg, var(--primary), var(--primary-light)); color: #fff; white-space: nowrap; }
.ai-marker-preview { flex: 1; font-size: 0.75rem; color: var(--text-sub); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ai-marker-del { flex-shrink: 0; background: transparent; border: none; color: var(--text-muted); font-size: 0.65rem; cursor: pointer; padding: 0.1rem 0.25rem; border-radius: 3px; transition: color 0.15s; }
.ai-marker-del:hover { color: #ef4444; }

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