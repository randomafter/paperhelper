<template>
  <div class="ai-chat-panel">
    <div class="chat-header">
      <span class="chat-title">✦ AI 助手</span>
      <div class="chat-header-actions">
        <button class="chat-hdr-btn" @click="showCtx = !showCtx" :class="{ active: showCtx }" title="上下文配置">⚙</button>
        <button class="chat-hdr-btn" @click="newSession" title="新对话">＋</button>
        <button class="chat-hdr-btn" @click="showHistory = !showHistory" :class="{ active: showHistory }" title="历史会话">🕒</button>
      </div>
    </div>

    <!-- 常驻上下文状态条 -->
    <div class="ctx-statusbar" @click="showCtx = !showCtx" title="点击展开/收起上下文配置">
      <span :class="['ctx-tag', boundMats.length > 0 ? 'ctx-tag--on' : 'ctx-tag--off']">
        📎 {{ boundMats.length > 0 ? boundMats.length + '条素材' : '无素材' }}
      </span>
      <span :class="['ctx-tag', outline ? 'ctx-tag--on' : 'ctx-tag--off']">
        📋 {{ outline ? '大纲已设' : '无大纲' }}
      </span>
      <span :class="['ctx-tag', chars ? 'ctx-tag--on' : 'ctx-tag--off']">
        👤 {{ chars ? '人物已设' : '无人物' }}
      </span>
      <span :class="['ctx-tag', editorContent?.trim() ? 'ctx-tag--on' : 'ctx-tag--off']">
        📝 {{ editorContent?.trim() ? '编辑区有内容' : '编辑区为空' }}
      </span>
    </div>

    <div v-if="showHistory" class="chat-history">
      <div class="chat-history-hd">历史会话</div>
      <div v-if="sessions.length === 0" class="chat-history-empty">暂无历史</div>
      <div v-for="s in sessions" :key="s.id" class="chat-history-item" :class="{ active: s.id === currentId }"
        @click="loadSession(s.id)">
        <span class="chat-history-title">{{ s.title }}</span>
        <button class="chat-history-del" @click.stop="deleteSession(s.id)">✕</button>
      </div>
    </div>

    <div v-if="showCtx" class="chat-ctx">
      <!-- 素材 -->
      <div class="ctx-row">
        <span class="ctx-label">📎 素材</span>
        <div class="ctx-chips">
          <span v-if="boundMats.length === 0" class="ctx-empty">从左侧点击「✦ AI」绑定</span>
          <span v-for="m in boundMats" :key="m.id" class="ctx-chip">
            <span class="ctx-chip-name">{{ m.title }}</span>
            <button class="ctx-chip-del" @click="removeMat(m.id)" title="移除">✕</button>
          </span>
          <button v-if="boundMats.length > 0" class="ctx-clear-btn" @click="emit('update:boundMats', [])">全部清除</button>
        </div>
      </div>
      <!-- 大纲 -->
      <div class="ctx-row">
        <div class="ctx-label-row">
          <span class="ctx-label">📋 大纲</span>
          <button v-if="outline" class="ctx-clear-btn" @click="emit('update:outline', '')">清除</button>
        </div>
        <textarea :value="outline" @input="emit('update:outline', $event.target.value)"
          class="ctx-textarea" rows="3" placeholder="粘贴大纲，AI 续写时将严格遵循..."></textarea>
      </div>
      <!-- 人物 -->
      <div class="ctx-row">
        <div class="ctx-label-row">
          <span class="ctx-label">👤 人物</span>
          <button v-if="chars" class="ctx-clear-btn" @click="emit('update:chars', '')">清除</button>
        </div>
        <textarea :value="chars" @input="emit('update:chars', $event.target.value)"
          class="ctx-textarea" rows="2" placeholder="人物设定，如：李明远，寒门举子..."></textarea>
      </div>
    </div>

    <div class="chat-messages" ref="messagesRef">
      <div v-if="messages.length === 0" class="chat-welcome">
        <div class="welcome-icon">✦</div>
        <div class="welcome-title">AI 创作助手</div>
        <div class="welcome-desc">直接描述你的需求，我会参考绑定的素材、大纲和编辑区内容来回答。</div>
        <div class="welcome-hints">
          <span @click="input = '请根据大纲续写下一段'" class="hint-tag">续写下一段</span>
          <span @click="input = '润色最后一段，语言更典雅'" class="hint-tag">润色最后一段</span>
          <span @click="input = '根据素材生成一段历史场景'" class="hint-tag">生成场景</span>
          <span @click="input = '检查是否有历史错误'" class="hint-tag">检查错误</span>
        </div>
      </div>
      <template v-for="msg in messages" :key="msg.id">
        <div v-if="msg.role === 'user'" class="msg msg-user">
          <div class="bubble bubble-user">{{ msg.content }}</div>
        </div>
        <div v-else-if="msg.role === 'assistant'" class="msg msg-ai">
          <div class="bubble bubble-ai">
            <pre class="ai-text">{{ msg.content }}<span v-if="msg.streaming" class="cursor">▋</span></pre>
            <div v-if="!msg.streaming" class="msg-actions">
              <button @click="accept(msg, 'append')" class="act-btn">↓ 插入</button>
              <button @click="accept(msg, 'replace')" class="act-btn">⇄ 替换</button>
              <button @click="copy(msg)" class="act-btn">📋</button>
              <button @click="retry(msg)" class="act-btn">↺</button>
            </div>
          </div>
        </div>
      </template>
      <div v-if="loading" class="msg msg-ai">
        <div class="bubble bubble-ai loading">
          <span class="dot"></span><span class="dot"></span><span class="dot"></span>
        </div>
      </div>
    </div>

    <div class="chat-input-area">
      <div class="quick-cmds">
        <button v-for="cmd in quickCmds" :key="cmd.key" @click="quickCmd(cmd)" :disabled="loading" class="quick-btn">{{ cmd.icon }} {{ cmd.label }}</button>
      </div>
      <div class="input-row">
        <textarea v-model="input" ref="inputRef" class="input" rows="3"
          placeholder="输入你的需求，如：续写下一段、润色第二段、根据素材写一场战役..."
          @keydown.enter.exact.prevent="send"
          @keydown.enter.shift.exact="input += '\n'"></textarea>
        <button @click="send" :disabled="loading || !input.trim()" class="send-btn">
          {{ loading ? '⏳' : '发送' }}
        </button>
      </div>
      <div class="input-hint">Enter 发送 · Shift+Enter 换行</div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'

const props = defineProps({
  boundMats: { type: Array, default: () => [] },
  outline: { type: String, default: '' },
  chars: { type: String, default: '' },
  editorContent: { type: String, default: '' },
  styleName: { type: String, default: '典雅' },
  words: { type: Number, default: 200 },
})

const emit = defineEmits(['update:outline', 'update:chars', 'accept-msg', 'update:boundMats'])

const messages = ref([])
const input = ref('')
const loading = ref(false)
const showCtx = ref(false)
const showHistory = ref(false)
const sessions = ref(JSON.parse(localStorage.getItem('chat_sessions') || '[]'))
const currentId = ref(null)
const messagesRef = ref(null)
const inputRef = ref(null)

const quickCmds = [
  { key: 'continue', icon: '➡️', label: '续写' },
  { key: 'polish',   icon: '✨', label: '润色' },
  { key: 'outline',  icon: '📋', label: '大纲' },
  { key: 'check',    icon: '⚠️', label: '检错' },
  { key: 'scene',    icon: '✍️', label: '场景' },
]

function buildSystemPrompt() {
  let sys = ''
  if (props.outline?.trim()) sys += `【故事大纲（必须遵循）】\n${props.outline.trim()}\n\n`
  if (props.chars?.trim()) sys += `【人物设定】\n${props.chars.trim()}\n\n`
  const tail = props.editorContent?.trim().slice(-600)
  if (tail) sys += `【编辑区末尾内容（供续写/润色参考）】\n${tail}\n\n`
  return sys
}

function scroll() {
  const el = messagesRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function send() {
  const text = input.value.trim()
  if (!text || loading.value) return
  input.value = ''
  if (!currentId.value) currentId.value = Date.now()

  messages.value.push({ id: Date.now(), role: 'user', content: text })
  await nextTick(); scroll()

  loading.value = true
  const history = messages.value
    .filter(m => m.role === 'user' || m.role === 'assistant')
    .map(m => ({ role: m.role, content: m.content }))

  const aiMsg = { id: Date.now() + 1, role: 'assistant', content: '', streaming: true }
  messages.value.push(aiMsg)
  await nextTick(); scroll()

  const token = localStorage.getItem('token')
  const validIds = props.boundMats
    .filter(m => /^\d+$/.test(String(m.id)))
    .map(m => Number(m.id))

  try {
    const response = await fetch('/api/spark/stream/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
      body: JSON.stringify({
        systemPrompt: buildSystemPrompt(),
        messages: history,
        materialIds: validIds.length > 0 ? validIds : undefined
      })
    })

    if (!response.ok) {
      const err = await response.json().catch(() => ({}))
      aiMsg.content = err?.message || '请求失败，请稍后重试'
      aiMsg.streaming = false; loading.value = false; return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n'); buffer = lines.pop() ?? ''
      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed.startsWith('data:')) continue
        const data = trimmed.slice(5).trim()
        if (data === '[DONE]') {
          aiMsg.streaming = false; loading.value = false
          saveSession(); await nextTick(); scroll(); return
        }
        aiMsg.content += data
        await nextTick(); scroll()
      }
    }
    aiMsg.streaming = false; loading.value = false; saveSession()
  } catch (e) {
    aiMsg.content = !navigator.onLine ? '📡 网络已断开' : '⚠️ 服务暂时不可用'
    aiMsg.streaming = false; loading.value = false
  }
}

function quickCmd(cmd) {
  const map = {
    continue: `请根据大纲和编辑区末尾内容，以${props.styleName}风格续写约${props.words}字，末尾标注参考章节。`,
    polish:   '请润色编辑区最后一段，保持原意，提升文学性，直接返回润色后文本。',
    outline:  '请根据故事梗概和素材，生成完整分章大纲（5-8章），每章含标题、核心情节、关键人物。',
    check:    '请检查编辑区末尾内容中的历史错误（食材、器物、词汇、制度等），按【错误点N】格式列出。',
    scene:    `请生成一段150-250字的历史场景描写，风格${props.styleName}，历史细节准确，调动多种感官。`,
  }
  input.value = map[cmd.key] || ''
  inputRef.value?.focus()
}

function accept(msg, mode) {
  emit('accept-msg', { content: msg.content, mode })
}

async function copy(msg) {
  try { await navigator.clipboard.writeText(msg.content) } catch(e) {}
}

function retry(msg) {
  const idx = messages.value.indexOf(msg)
  if (idx < 0) return
  const prevUser = [...messages.value].slice(0, idx).reverse().find(m => m.role === 'user')
  if (!prevUser) return
  messages.value.splice(idx, 1)
  input.value = prevUser.content
  send()
}

function newSession() {
  if (messages.value.length > 0) saveSession()
  messages.value = []; currentId.value = null; showHistory.value = false
}

function saveSession() {
  if (!currentId.value || messages.value.length === 0) return
  const firstUser = messages.value.find(m => m.role === 'user')
  const title = firstUser
    ? firstUser.content.slice(0, 20) + (firstUser.content.length > 20 ? '...' : '')
    : '对话'
  const session = {
    id: currentId.value, title,
    time: new Date().toLocaleString('zh-CN', { month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' }),
    messages: messages.value.filter(m => !m.streaming).map(m => ({ role: m.role, content: m.content }))
  }
  const idx = sessions.value.findIndex(s => s.id === currentId.value)
  if (idx >= 0) sessions.value[idx] = session
  else sessions.value.unshift(session)
  if (sessions.value.length > 20) sessions.value = sessions.value.slice(0, 20)
  localStorage.setItem('chat_sessions', JSON.stringify(sessions.value))
}

function loadSession(id) {
  const s = sessions.value.find(x => x.id === id)
  if (!s) return
  currentId.value = id
  messages.value = s.messages.map((m, i) => ({ ...m, id: id + i, streaming: false }))
  showHistory.value = false
  nextTick(() => scroll())
}

function deleteSession(id) {
  sessions.value = sessions.value.filter(s => s.id !== id)
  localStorage.setItem('chat_sessions', JSON.stringify(sessions.value))
  if (currentId.value === id) { messages.value = []; currentId.value = null }
}

function removeMat(id) {
  emit('update:boundMats', props.boundMats.filter(m => m.id !== id))
}
</script>

<style scoped>
.ai-chat-panel { display: flex; flex-direction: column; height: 100%; background: #fff; }
.ctx-statusbar { display: flex; flex-wrap: wrap; gap: 0.3rem; padding: 0.4rem 0.8rem; background: #f7f7fb; border-bottom: 1px solid #ebebf5; cursor: pointer; transition: background 0.15s; }
.ctx-statusbar:hover { background: #f0f0fa; }
.ctx-tag { font-size: 0.7rem; padding: 0.15rem 0.5rem; border-radius: 8px; font-weight: 500; }
.ctx-tag--on { background: rgba(124,106,247,0.12); color: #7c6af7; }
.ctx-tag--off { background: #f0f0f0; color: #bbb; }
.ctx-label-row { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.ctx-chip-name { max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ctx-chip-del { background: none; border: none; color: #7c6af7; cursor: pointer; font-size: 0.65rem; padding: 0 0 0 0.2rem; opacity: 0.7; }
.ctx-chip-del:hover { opacity: 1; color: #e53935; }
.ctx-clear-btn { background: none; border: 1px solid #ddd; border-radius: 6px; color: #aaa; font-size: 0.68rem; padding: 0.1rem 0.4rem; cursor: pointer; white-space: nowrap; flex-shrink: 0; transition: all 0.15s; }
.ctx-clear-btn:hover { border-color: #e53935; color: #e53935; }
.chat-header { display: flex; align-items: center; justify-content: space-between; padding: 0.6rem 0.8rem; border-bottom: 1px solid #e0e0e0; background: linear-gradient(135deg, #7c6af7 0%, #6c5ce7 100%); color: #fff; }
.chat-title { font-weight: 600; font-size: 0.95rem; }
.chat-header-actions { display: flex; gap: 0.3rem; }
.chat-hdr-btn { background: rgba(255,255,255,0.2); border: none; color: #fff; border-radius: 6px; width: 28px; height: 28px; cursor: pointer; font-size: 0.85rem; display: flex; align-items: center; justify-content: center; transition: background 0.2s; }
.chat-hdr-btn:hover, .chat-hdr-btn.active { background: rgba(255,255,255,0.35); }
.chat-history { background: #f9f9f9; border-bottom: 1px solid #e0e0e0; padding: 0.5rem; max-height: 180px; overflow-y: auto; }
.chat-history-hd { font-size: 0.75rem; font-weight: 600; color: #888; margin-bottom: 0.3rem; }
.chat-history-empty { font-size: 0.75rem; color: #bbb; text-align: center; padding: 0.5rem; }
.chat-history-item { display: flex; align-items: center; gap: 0.3rem; padding: 0.3rem 0.4rem; border-radius: 6px; cursor: pointer; font-size: 0.75rem; transition: background 0.15s; }
.chat-history-item:hover, .chat-history-item.active { background: rgba(124,106,247,0.12); }
.chat-history-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chat-history-del { background: none; border: none; color: #bbb; cursor: pointer; padding: 0 0.2rem; font-size: 0.75rem; }
.chat-history-del:hover { color: #e53935; }
.chat-ctx { padding: 0.6rem 0.8rem; border-bottom: 1px solid #e0e0e0; background: #f9f9f9; display: flex; flex-direction: column; gap: 0.5rem; }
.ctx-row { display: flex; align-items: flex-start; gap: 0.5rem; }
.ctx-label { font-size: 0.72rem; color: #888; flex-shrink: 0; padding-top: 0.2rem; min-width: 40px; }
.ctx-chips { display: flex; flex-wrap: wrap; gap: 0.25rem; flex: 1; }
.ctx-empty { font-size: 0.72rem; color: #bbb; }
.ctx-chip { background: rgba(124,106,247,0.12); color: #7c6af7; border-radius: 10px; padding: 0.1rem 0.5rem; font-size: 0.72rem; display: flex; align-items: center; gap: 0.2rem; }
.ctx-chip button { background: none; border: none; color: inherit; cursor: pointer; font-size: 0.65rem; padding: 0; }
.ctx-textarea { flex: 1; background: #fff; border: 1px solid #ddd; border-radius: 6px; padding: 0.3rem 0.5rem; font-size: 0.75rem; resize: vertical; color: #333; }
.chat-messages { flex: 1; overflow-y: auto; padding: 0.8rem; display: flex; flex-direction: column; gap: 0.8rem; scroll-behavior: smooth; }
.chat-welcome { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; gap: 0.6rem; color: #aaa; text-align: center; padding: 1rem; }
.welcome-icon { font-size: 2rem; opacity: 0.4; }
.welcome-title { font-size: 1rem; font-weight: 600; color: #555; }
.welcome-desc { font-size: 0.78rem; line-height: 1.6; }
.welcome-hints { display: flex; flex-wrap: wrap; gap: 0.4rem; justify-content: center; margin-top: 0.4rem; }
.hint-tag { background: rgba(124,106,247,0.1); color: #7c6af7; border-radius: 12px; padding: 0.2rem 0.7rem; font-size: 0.75rem; cursor: pointer; transition: background 0.15s; }
.hint-tag:hover { background: rgba(124,106,247,0.2); }
.msg { display: flex; margin-bottom: 0.2rem; }
.msg-user { justify-content: flex-end; }
.msg-ai { justify-content: flex-start; }
.bubble { max-width: 90%; border-radius: 12px; padding: 0.6rem 0.8rem; font-size: 0.82rem; line-height: 1.65; word-break: break-word; }
.bubble-user { background: #7c6af7; color: #fff; border-bottom-right-radius: 3px; }
.bubble-ai { background: #f3f3f3; color: #333; border-bottom-left-radius: 3px; border: 1px solid #e8e8e8; }
.bubble-ai.loading { display: flex; gap: 0.3rem; align-items: center; padding: 0.8rem; }
.ai-text { white-space: pre-wrap; font-family: inherit; font-size: 0.82rem; margin: 0; line-height: 1.65; }
.cursor { animation: blink 1s infinite; }
@keyframes blink { 0%, 49% { opacity: 1; } 50%, 100% { opacity: 0; } }
.dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #aaa; animation: bounce 1.4s infinite; }
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce { 0%, 80%, 100% { opacity: 0.3; } 40% { opacity: 1; } }
.msg-actions { display: flex; gap: 0.3rem; margin-top: 0.4rem; flex-wrap: wrap; }
.act-btn { background: rgba(124,106,247,0.1); border: none; color: #7c6af7; border-radius: 6px; padding: 0.2rem 0.5rem; font-size: 0.7rem; cursor: pointer; transition: background 0.15s; }
.act-btn:hover { background: rgba(124,106,247,0.22); }
.chat-input-area { padding: 0.6rem 0.8rem; border-top: 1px solid #e0e0e0; background: #fafafa; display: flex; flex-direction: column; gap: 0.4rem; flex-shrink: 0; }
.quick-cmds { display: flex; gap: 0.3rem; flex-wrap: wrap; }
.quick-btn { background: #fff; border: 1px solid #ddd; border-radius: 6px; padding: 0.25rem 0.55rem; font-size: 0.72rem; cursor: pointer; transition: all 0.15s; }
.quick-btn:hover:not(:disabled) { background: #f0f0f0; border-color: #7c6af7; color: #7c6af7; }
.quick-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.input-row { display: flex; gap: 0.4rem; }
.input { flex: 1; background: #fff; border: 1px solid #ddd; border-radius: 8px; padding: 0.45rem 0.65rem; font-size: 0.82rem; resize: none; color: #333; font-family: inherit; line-height: 1.5; outline: none; transition: border-color 0.15s; }
.input:focus { border-color: #7c6af7; }
.send-btn { background: #7c6af7; border: none; color: #fff; border-radius: 8px; padding: 0 1rem; font-size: 0.82rem; cursor: pointer; transition: background 0.2s; flex-shrink: 0; min-width: 56px; }
.send-btn:hover:not(:disabled) { background: #6c5ce7; }
.send-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.input-hint { font-size: 0.68rem; color: #ccc; text-align: center; }
</style>