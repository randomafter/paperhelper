<template>
  <div class="ai-chat-panel">
    <div class="chat-header">
      <span class="chat-title">✦ AI 助手</span>
      <div class="chat-header-actions">
        <button class="chat-hdr-btn" @click="showCtx = !showCtx" :class="{ active: showCtx }" title="上下文配置">⚙</button>
        <button class="chat-hdr-btn" @click="newSession" title="新对话">＋</button>
        <button class="chat-hdr-btn" @click="showHistory = !showHistory" :class="{ active: showHistory }" title="历史会话">🕒</button>
        <button class="chat-hdr-btn chat-hdr-btn-collapse" @click="emit('collapse')" title="隐藏面板">›</button>
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
      <span :class="['ctx-tag', plotHooks ? 'ctx-tag--on' : 'ctx-tag--off']">
        🧷 {{ plotHooks ? '伏笔已设' : '无伏笔' }}
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
      <!-- 复核规则 -->
      <div class="ctx-row">
        <span class="ctx-label">🧪 复核</span>
        <div class="repair-rules">
          <label class="repair-rule"><input type="checkbox" v-model="repairRules.outline" /> 大纲一致</label>
          <label class="repair-rule"><input type="checkbox" v-model="repairRules.chars" /> 人设一致</label>
          <label class="repair-rule"><input type="checkbox" v-model="repairRules.timeline" /> 时间线连续</label>
          <label class="repair-rule"><input type="checkbox" v-model="repairRules.historyDetail" /> 历史细节</label>
          <label class="repair-rule"><input type="checkbox" v-model="repairRules.dualWhenConflict" /> 严重冲突双方案</label>
        </div>
      </div>
    </div>

    <div class="chat-messages" ref="messagesRef">
      <div v-if="messages.length === 0" class="chat-welcome">
        <div class="welcome-icon">✦</div>
        <div class="welcome-title">AI 创作助手</div>
        <div class="welcome-desc">直接描述你的需求，我会参考绑定的素材、大纲和编辑区内容来回答。</div>
        <div class="welcome-hints">
          <span @click="input = '请根据大纲续写下一段'" class="hint-tag">续写下一段</span>
          <span @click="input = '根据素材生成一段历史场景'" class="hint-tag">生成场景</span>
        </div>
      </div>
      <template v-for="msg in messages" :key="msg.id">
        <div v-if="msg.role === 'user'" class="msg msg-user">
          <div class="bubble bubble-user">{{ msg.content }}</div>
        </div>
        <div v-else-if="msg.role === 'assistant'" class="msg msg-ai">
          <div class="bubble bubble-ai">
            <div v-if="msg.streaming && !msg.content" class="ai-thinking">
              正在思考中<span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </div>
            <pre class="ai-text">{{ msg.content }}<span v-if="msg.streaming" class="cursor">▋</span></pre>
            <div v-if="!msg.streaming" class="msg-actions">
              <button @click="accept(msg, 'append')" class="act-btn">↓ 插入</button>
              <button @click="accept(msg, 'replace')" class="act-btn">⇄ 替换</button>
              <button @click="copy(msg)" class="act-btn">📋</button>
              <button @click="retry(msg)" class="act-btn">↺</button>
            </div>
            <div v-if="!msg.streaming && msg.repairOptions?.length" class="repair-options">
              <span class="repair-options-hd">检测到潜在冲突，选择修正版：</span>
              <button v-for="(opt, idx) in msg.repairOptions" :key="idx" class="act-btn" @click="useRepairOption(msg, idx)">方案{{ idx + 1 }}</button>
            </div>
          </div>
        </div>
      </template>
      <div v-if="loading && !messages.some(m => m.streaming)" class="msg msg-ai">
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

    <div v-if="showKickoff" class="kickoff-mask" @click.self="showKickoff = false">
      <div class="kickoff-dialog">
        <div class="kickoff-hd">🚀 5问立项</div>
        <div class="kickoff-grid">
          <label>题材风格<input v-model="kickoffForm.genre" class="kickoff-input" placeholder="如：历史权谋+群像" /></label>
          <label>主角结构<input v-model="kickoffForm.protagonist" class="kickoff-input" placeholder="如：双主角" /></label>
          <label>核心性格<input v-model="kickoffForm.coreTrait" class="kickoff-input" placeholder="如：克制、冷静" /></label>
          <label>核心冲突<input v-model="kickoffForm.coreConflict" class="kickoff-input" placeholder="如：家国与私情" /></label>
          <label>章节规模<input v-model="kickoffForm.chapterScale" class="kickoff-input" placeholder="如：120章，每章3500字" /></label>
        </div>
        <div class="kickoff-actions">
          <button class="quick-btn" @click="showKickoff = false">取消</button>
          <button class="send-btn" @click="applyKickoff">生成立项提示词</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'

const props = defineProps({
  boundMats: { type: Array, default: () => [] },
  outline: { type: String, default: '' },
  chars: { type: String, default: '' },
  worldSetting: { type: String, default: '' },
  plotHooks: { type: String, default: '' },
  editorContent: { type: String, default: '' },
  styleName: { type: String, default: '典雅' },
  words: { type: Number, default: 2200 },
})

const emit = defineEmits(['update:outline', 'update:chars', 'accept-msg', 'update:boundMats', 'collapse'])

const messages = ref([])
const input = ref('')
const loading = ref(false)
const showCtx = ref(false)
const showHistory = ref(false)
const sessions = ref(JSON.parse(localStorage.getItem('chat_sessions') || '[]'))
const currentId = ref(null)
const messagesRef = ref(null)
const inputRef = ref(null)
const pendingDisplayText = ref('')
const showKickoff = ref(false)
const kickoffForm = ref({
  genre: '',
  protagonist: '',
  coreTrait: '',
  coreConflict: '',
  chapterScale: ''
})

const defaultRepairRules = {
  outline: false,
  chars: false,
  timeline: false,
  historyDetail: false,
  dualWhenConflict: false,
}
const savedRepairRules = JSON.parse(localStorage.getItem('ai_repair_rules') || 'null')
const repairRules = ref({ ...defaultRepairRules, ...(savedRepairRules || {}) })

const quickCmds = [
  { key: 'continue', icon: '➡️', label: '续写' },
  { key: 'generate', icon: '🪄', label: '生成' },
  { key: 'scene',    icon: '✍️', label: '场景' },
  { key: 'kickoff',  icon: '🚀', label: '5问立项' },
]

function targetWordRange(baseWords) {
  const base = Number(baseWords) || 2000
  const min = Math.max(1200, Math.round(base * 0.9))
  const max = Math.max(min + 200, Math.round(base * 1.15))
  return `${min}-${max}`
}

function buildSystemPrompt() {
  const hasOutline = !!props.outline?.trim()
  const hasChars = !!props.chars?.trim()
  const hasWorld = !!props.worldSetting?.trim()
  const hasHooks = !!props.plotHooks?.trim()
  const tail = props.editorContent?.trim().slice(-1000) || ''
  const mats = (props.boundMats || []).slice(0, 5)

  let sys = `你是历史小说创作助手，必须把“素材理解、时代考据、人物动机、冲突递进、文学表达”同时做好，而不是只把素材名称堆进正文。
写作总原则：
1. 素材只能在“人物行为、场景细节、制度约束、情节因果”里自然消化，禁止把素材做成展品式罗列；
2. 若多条素材存在时代、制度、称谓冲突，必须先判断哪条与当前世界观/朝代更一致，冲突素材只保留其可迁移的抽象信息，不可直接照搬名词；
3. 对话要含蓄、有潜台词，少直白解释，多通过动作、停顿、神态、景物映衬人物心绪；
4. 冲突推进必须有博弈过程，不能一招化解，至少体现试探、压迫、反制或代价；
5. 若信息不足，只做最小必要补全，不新增重大设定；
6. 最终回复只输出可直接使用的正文，不要解释、不要前言、不要题外话。
输出要求：自然分段，段间空一行；每段首行两个全角空格缩进。

`

  if (mats.length) {
    sys += '【绑定素材（需语义融合，不可堆砌）】\n'
    mats.forEach((m, i) => {
      const title = m?.title || `素材${i + 1}`
      const content = (m?.content || '').slice(0, 500)
      sys += `${i + 1}. ${title}\n${content}\n\n`
    })
    sys += '使用规则：优先提炼素材中的“制度、器物、礼法、氛围、人物处境、叙事功能”，只选最适合当前情节的1-3个细节点自然渗透到正文，不要逐条点名使用。\n\n'
  }
  if (hasOutline) sys += `【故事大纲（最高优先级）】\n${props.outline.trim()}\n\n`
  if (hasChars) sys += `【人物设定（最高优先级）】\n${props.chars.trim()}\n\n`
  if (hasWorld) sys += `【世界观/时代设定（最高优先级）】\n${props.worldSetting.trim()}\n\n`
  if (hasHooks) sys += `【未回收伏笔（必须优先呼应）】\n${props.plotHooks.trim()}\n\n`
  if (tail) sys += `【编辑区末尾内容（最高优先级上下文）】\n${tail}\n\n`

  sys += '写作时请先在内部完成四步判断：① 当前朝代/制度边界；② 本段主冲突与阻力；③ 哪些素材值得隐性融合；④ 人物情绪应如何通过含蓄表达显现。不要输出这四步，只输出最终正文。'

  return sys
}

function parseSseDataLine(line) {
  if (!line.startsWith('data:')) return null
  let data = line.slice(5)
  if (data.startsWith(' ')) data = data.slice(1)
  if (data.endsWith('\r')) data = data.slice(0, -1)
  return data === '' ? '\n' : data
}

function scroll() {
  const el = messagesRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function runConsistencyRepair(draft, userDemand, validIds) {
  const content = (draft || '').trim()
  if (!content) return { content: draft, options: [] }

  const enabled = []
  if (repairRules.value.outline) enabled.push('大纲一致')
  if (repairRules.value.chars) enabled.push('人物一致')
  if (repairRules.value.timeline) enabled.push('时间线连续')
  if (repairRules.value.historyDetail) enabled.push('历史细节准确')
  const checkScope = enabled.length ? enabled.join('、') : '基础一致性'

  const hasCtx = props.boundMats.length > 0 || !!props.outline?.trim() || !!props.chars?.trim() || !!props.editorContent?.trim()
  if (!hasCtx) return { content: draft, options: [] }

  const token = localStorage.getItem('token')
  const dualMode = repairRules.value.dualWhenConflict
  const reviewPrompt = dualMode
    ? `任务：一致性复核与修正（冲突时给双方案）\n\n请按以下范围复核候选正文：${checkScope}。\n要求：\n1）优先遵循绑定上下文；\n2）保持用户原始意图；\n3）若无明显冲突，直接输出最终正文；\n4）若存在明显冲突，按如下格式输出两种修正版（只输出正文，不要解释）：\n===方案1===\n（严格遵循绑定上下文）\n===方案2===\n（尽量保留原候选表达但修正关键冲突）\n\n用户需求：${userDemand}\n\n候选正文：\n${content}`
    : `任务：一致性复核与最小修正\n\n请按以下范围复核候选正文：${checkScope}。\n要求：\n1）若与绑定上下文冲突，做最小必要修正；\n2）保持用户原始意图不变；\n3）保持文风连续；\n4）只输出修正后的最终正文，不要解释。\n\n用户需求：${userDemand}\n\n候选正文：\n${content}`

  const response = await fetch('/api/spark/stream/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
    body: JSON.stringify({
      systemPrompt: buildSystemPrompt(),
      messages: [{ role: 'user', content: reviewPrompt }],
      materialIds: validIds.length > 0 ? validIds : undefined
    })
  })

  if (!response.ok || !response.body) return { content: draft, options: [] }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let fixed = ''

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
        const all = fixed.trim() || draft
        const options = all.split(/===方案\d+===/).map(s => s.trim()).filter(Boolean)
        if (options.length > 1) return { content: options[0], options }
        return { content: all, options: [] }
      }
      fixed += data
    }
  }

  const all = fixed.trim() || draft
  const options = all.split(/===方案\d+===/).map(s => s.trim()).filter(Boolean)
  if (options.length > 1) return { content: options[0], options }
  return { content: all, options: [] }
}

function cleanAssistantText(text) {
  let t = String(text || '')
  t = t.replace(/^\s*(每段首行请使用两个全角空格缩进\.?|两个全角空格[：:，,。]?)/, '')
  return t.trimStart()
}

function formatNovelParagraphs(text) {
  const raw = cleanAssistantText(text)
  let parts = raw.split(/\n{2,}/).map(s => s.trim()).filter(Boolean)

  if (parts.length <= 1) {
    const compact = raw.replace(/\n+/g, '').trim()
    if (compact) {
      const segs = compact.split(/(?<=[。！？!?])/).map(s => s.trim()).filter(Boolean)
      const rebuilt = []
      let acc = ''
      for (const s of segs) {
        if ((acc + s).length > 120 && acc) {
          rebuilt.push(acc)
          acc = s
        } else {
          acc += s
        }
      }
      if (acc) rebuilt.push(acc)
      if (rebuilt.length > 1) parts = rebuilt
      else parts = [compact]
    }
  }

  if (!parts.length) return ''
  const normalized = parts.map(p => {
    const body = p.replace(/^[\u3000\s]+/, '')
    return `　　${body}`
  })
  return normalized.join('\n\n')
}

async function send(payload = '') {
  const text = typeof payload === 'string' ? (payload || input.value).trim() : input.value.trim()
  if (!text || loading.value) return
  input.value = ''
  if (!currentId.value) currentId.value = Date.now()

  const shown = (pendingDisplayText.value || text).trim()
  pendingDisplayText.value = ''
  const userMsg = { id: Date.now(), role: 'user', content: shown }
  if (shown !== text) userMsg._prompt = text
  messages.value.push(userMsg)
  await nextTick(); scroll()

  loading.value = true
  const history = messages.value
    .filter(m => m.role === 'user' || m.role === 'assistant')
    .map(m => ({ role: m.role, content: m._prompt || m.content }))

  const aiMsg = { id: Date.now() + 1, role: 'assistant', content: '', streaming: true }
  messages.value.push(aiMsg)
  await nextTick(); scroll()

  const token = localStorage.getItem('token')
  const validIds = props.boundMats
    .filter(m => /^\d+$/.test(String(m.id)))
    .map(m => Number(m.id))

  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 180000)

  try {
    const response = await fetch('/api/spark/stream/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
      body: JSON.stringify({
        systemPrompt: buildSystemPrompt(),
        messages: history,
        materialIds: validIds.length > 0 ? validIds : undefined
      }),
      signal: controller.signal,
    })

    if (!response.ok || !response.body) {
      const err = await response.json().catch(() => ({}))
      aiMsg.content = err?.message || '请求失败，请稍后重试'
      return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let doneReceived = false

    while (!doneReceived) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() ?? ''
      for (const line of lines) {
        const data = parseSseDataLine(line)
        if (data == null) continue
        if (data === '[DONE]') {
          doneReceived = true
          break
        }
        aiMsg.content += data
      }
      await nextTick(); scroll()
    }

    aiMsg.content = formatNovelParagraphs(aiMsg.content)
    saveSession()
    await nextTick(); scroll()
  } catch (e) {
    aiMsg.content = e.name === 'AbortError'
      ? '⏱️ 生成超时，请重试'
      : (!navigator.onLine ? '📡 网络已断开' : '⚠️ 服务暂时不可用')
  } finally {
    clearTimeout(timeout)
    aiMsg.streaming = false
    loading.value = false
  }
}

function quickCmd(cmd) {
  if (cmd.key === 'kickoff') {
    showKickoff.value = true
    return
  }

  const map = {
    continue: {
      display: '请续写下一段',
      prompt: `任务：长篇小说续写（高约束）\n\n请严格依据已绑定素材、大纲、人物、世界观、伏笔与编辑区末尾上下文续写。\n要求：\n1）保持同一人称/时态/语气；\n2）围绕当前主冲突持续加压，不能一步化解危机，至少写出一次试探、一次受阻、一次反制或代价；\n3）素材只能作为细节与因果来源自然渗透，禁止逐条罗列素材名词；\n4）若出现跨朝代制度/称谓冲突，以当前世界观和朝代设定为准，只保留可迁移信息；\n5）对话要含蓄克制，增加潜台词、动作、神态、景物映衬；\n6）生成长度约${targetWordRange(props.words)}字，风格偏${props.styleName}；\n7）输出按自然段组织，段间空一行；每段首行两个全角空格缩进；\n8）仅输出可直接接在正文后的连续文本。`
    },
    generate: {
      display: '请生成剧情',
      prompt: `任务：小说剧情生成（高约束）\n\n请基于当前已绑定上下文（素材、大纲、人物、世界观、伏笔）生成一段可直接入文的剧情正文。\n要求：\n1）先判断时代边界与制度边界，避免跨朝代官职、礼制、称谓混用；\n2）优先呼应未回收伏笔并推动主线；\n3）人物行为必须符合人设与动机，不能为了推进剧情强行转折；\n4）冲突解决不能过快，需体现博弈、阻力与代价；\n5）素材融合采用“渗透式写法”，最多选择最适合情节的少数细节自然嵌入；\n6）生成长度${targetWordRange(props.words)}字；\n7）输出按自然段组织，段间空一行；每段首行两个全角空格缩进；\n8）只输出正文，不要标题、不要说明。`
    },
    scene: {
      display: '请生成历史场景',
      prompt: `任务：场景生成（可直接入文）\n\n请基于已绑定素材和世界观，生成一段400-700字历史场景：\n- 风格：${props.styleName}\n- 至少包含两种感官描写\n- 细节符合时代背景，避免跨朝代器物/称谓误用\n- 场景中的器物、礼制、氛围要自然服务人物情绪或剧情，不可堆砌\n- 末句保留叙事张力\n\n只输出场景正文。`
    },
  }
  const payload = map[cmd.key]
  if (!payload) return
  pendingDisplayText.value = payload.display
  send(payload.prompt)
}

async function applyKickoff() {
  const f = kickoffForm.value
  const prompt = `请基于以下5问立项信息，直接输出：1）故事总纲；2）8章分章大纲；3）核心人物档案；4）时间线初稿；5）伏笔清单初稿。\n\n题材风格：${f.genre || '未提供'}\n主角结构：${f.protagonist || '未提供'}\n核心性格：${f.coreTrait || '未提供'}\n核心冲突：${f.coreConflict || '未提供'}\n章节规模：${f.chapterScale || '未提供'}\n\n输出要求：只输出可直接使用内容，不要解释。`
  showKickoff.value = false
  pendingDisplayText.value = '请生成立项方案'
  await send(prompt)
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
  pendingDisplayText.value = ''
  send(prevUser._prompt || prevUser.content)
}

function useRepairOption(msg, idx) {
  const options = msg?.repairOptions || []
  const picked = options[idx]
  if (!picked) return
  msg.content = picked
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

watch(repairRules, (val) => {
  localStorage.setItem('ai_repair_rules', JSON.stringify(val))
}, { deep: true })
</script>

<style scoped>
.ai-chat-panel { display: flex; flex-direction: column; height: 100%; background: var(--bg-card); color: var(--text-main); }
.ctx-statusbar { display: flex; flex-wrap: wrap; gap: 0.3rem; padding: 0.4rem 0.8rem; background: var(--bg-input); border-bottom: 1px solid var(--border); cursor: pointer; transition: background 0.15s; }
.ctx-statusbar:hover { background: var(--bg-hover); }
.ctx-tag { font-size: 0.7rem; padding: 0.15rem 0.5rem; border-radius: 8px; font-weight: 500; }
.ctx-tag--on { background: rgba(var(--primary-rgb), 0.12); color: var(--primary); }
.ctx-tag--off { background: var(--bg-hover); color: var(--text-muted); }
.ctx-label-row { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.ctx-chip-name { max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ctx-chip-del { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 0.65rem; padding: 0 0 0 0.2rem; opacity: 0.75; }
.ctx-chip-del:hover { opacity: 1; color: #e53935; }
.ctx-clear-btn { background: none; border: 1px solid var(--border); border-radius: 6px; color: var(--text-muted); font-size: 0.68rem; padding: 0.1rem 0.4rem; cursor: pointer; white-space: nowrap; flex-shrink: 0; transition: all 0.15s; }
.ctx-clear-btn:hover { border-color: #e53935; color: #e53935; }
.chat-header { display: flex; align-items: center; justify-content: space-between; padding: 0.6rem 0.8rem; border-bottom: 1px solid var(--border); background: linear-gradient(135deg, var(--primary), var(--primary-light)); color: #fff; }
.chat-title { font-weight: 600; font-size: 0.95rem; }
.chat-header-actions { display: flex; gap: 0.3rem; }
.chat-hdr-btn { background: rgba(255,255,255,0.2); border: none; color: #fff; border-radius: 6px; width: 28px; height: 28px; cursor: pointer; font-size: 0.85rem; display: flex; align-items: center; justify-content: center; transition: background 0.2s; }
.chat-hdr-btn:hover, .chat-hdr-btn.active { background: rgba(255,255,255,0.35); }
.chat-hdr-btn-collapse { font-weight: 700; }
.chat-history { background: var(--bg-input); border-bottom: 1px solid var(--border); padding: 0.5rem; max-height: 180px; overflow-y: auto; }
.chat-history-hd { font-size: 0.75rem; font-weight: 600; color: var(--text-muted); margin-bottom: 0.3rem; }
.chat-history-empty { font-size: 0.75rem; color: var(--text-muted); text-align: center; padding: 0.5rem; opacity: 0.75; }
.chat-history-item { display: flex; align-items: center; gap: 0.3rem; padding: 0.3rem 0.4rem; border-radius: 6px; cursor: pointer; font-size: 0.75rem; transition: background 0.15s; }
.chat-history-item:hover, .chat-history-item.active { background: rgba(var(--primary-rgb), 0.12); }
.chat-history-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chat-history-del { background: none; border: none; color: var(--text-muted); cursor: pointer; padding: 0 0.2rem; font-size: 0.75rem; }
.chat-history-del:hover { color: #e53935; }
.chat-ctx { padding: 0.6rem 0.8rem; border-bottom: 1px solid var(--border); background: var(--bg-input); display: flex; flex-direction: column; gap: 0.5rem; }
.ctx-row { display: flex; align-items: flex-start; gap: 0.5rem; }
.ctx-label { font-size: 0.72rem; color: var(--text-muted); flex-shrink: 0; padding-top: 0.2rem; min-width: 40px; }
.ctx-chips { display: flex; flex-wrap: wrap; gap: 0.25rem; flex: 1; }
.ctx-empty { font-size: 0.72rem; color: var(--text-muted); opacity: 0.8; }
.ctx-chip { background: rgba(var(--primary-rgb), 0.12); color: var(--primary); border-radius: 10px; padding: 0.1rem 0.5rem; font-size: 0.72rem; display: flex; align-items: center; gap: 0.2rem; }
.ctx-chip button { background: none; border: none; color: inherit; cursor: pointer; font-size: 0.65rem; padding: 0; }
.repair-rules { display: flex; flex-wrap: wrap; gap: 0.35rem 0.5rem; }
.repair-rule { font-size: 0.7rem; color: var(--text-sub); display: inline-flex; align-items: center; gap: 0.2rem; }
.repair-rule input { accent-color: var(--primary); }
.ctx-textarea { flex: 1; background: var(--bg-card); border: 1px solid var(--border); border-radius: 6px; padding: 0.3rem 0.5rem; font-size: 0.75rem; resize: vertical; color: var(--text-main); }
.chat-messages { flex: 1; overflow-y: auto; padding: 0.8rem; display: flex; flex-direction: column; gap: 0.8rem; scroll-behavior: smooth; }
.chat-welcome { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; gap: 0.6rem; color: var(--text-muted); text-align: center; padding: 1rem; }
.welcome-icon { font-size: 2rem; opacity: 0.4; }
.welcome-title { font-size: 1rem; font-weight: 600; color: var(--text-main); }
.welcome-desc { font-size: 0.78rem; line-height: 1.6; color: var(--text-sub); }
.welcome-hints { display: flex; flex-wrap: wrap; gap: 0.4rem; justify-content: center; margin-top: 0.4rem; }
.hint-tag { background: rgba(var(--primary-rgb), 0.1); color: var(--primary); border-radius: 12px; padding: 0.2rem 0.7rem; font-size: 0.75rem; cursor: pointer; transition: background 0.15s; }
.hint-tag:hover { background: rgba(var(--primary-rgb), 0.2); }
.msg { display: flex; margin-bottom: 0.2rem; }
.msg-user { justify-content: flex-end; }
.msg-ai { justify-content: flex-start; }
.bubble { max-width: 90%; border-radius: 12px; padding: 0.6rem 0.8rem; font-size: 0.82rem; line-height: 1.65; word-break: break-word; }
.bubble-user { background: var(--primary); color: #fff; border-bottom-right-radius: 3px; }
.bubble-ai { background: var(--bg-input); color: var(--text-main); border-bottom-left-radius: 3px; border: 1px solid var(--border); }
.ai-thinking { font-size: 0.76rem; color: var(--text-muted); display: flex; align-items: center; gap: 0.3rem; margin-bottom: 0.25rem; }
.bubble-ai.loading { display: flex; gap: 0.3rem; align-items: center; padding: 0.8rem; }
.ai-text { white-space: pre-wrap; font-family: inherit; font-size: 0.82rem; margin: 0; line-height: 1.65; }
.cursor { animation: blink 1s infinite; }
@keyframes blink { 0%, 49% { opacity: 1; } 50%, 100% { opacity: 0; } }
.dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: var(--text-muted); animation: bounce 1.4s infinite; }
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce { 0%, 80%, 100% { opacity: 0.3; } 40% { opacity: 1; } }
.msg-actions { display: flex; gap: 0.3rem; margin-top: 0.4rem; flex-wrap: wrap; }
.repair-options { margin-top: 0.45rem; display: flex; align-items: center; gap: 0.35rem; flex-wrap: wrap; }
.repair-options-hd { font-size: 0.68rem; color: var(--text-muted); }
.act-btn { background: rgba(var(--primary-rgb), 0.1); border: none; color: var(--primary); border-radius: 6px; padding: 0.2rem 0.5rem; font-size: 0.7rem; cursor: pointer; transition: background 0.15s; }
.act-btn:hover { background: rgba(var(--primary-rgb), 0.22); }
.chat-input-area { padding: 0.6rem 0.8rem; border-top: 1px solid var(--border); background: var(--bg-input); display: flex; flex-direction: column; gap: 0.4rem; flex-shrink: 0; }
.quick-cmds { display: flex; gap: 0.3rem; flex-wrap: wrap; }
.quick-btn { background: var(--bg-card); border: 1px solid var(--border); border-radius: 6px; padding: 0.25rem 0.55rem; font-size: 0.72rem; color: var(--text-sub); cursor: pointer; transition: all 0.15s; }
.quick-btn:hover:not(:disabled) { background: var(--bg-hover); border-color: var(--primary); color: var(--primary); }
.quick-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.input-row { display: flex; gap: 0.4rem; }
.input { flex: 1; background: var(--bg-card); border: 1px solid var(--border); border-radius: 8px; padding: 0.45rem 0.65rem; font-size: 0.82rem; resize: none; color: var(--text-main); font-family: inherit; line-height: 1.5; outline: none; transition: border-color 0.15s; }
.input:focus { border-color: var(--primary); }
.send-btn { background: linear-gradient(90deg, var(--primary), var(--primary-light)); border: none; color: #fff; border-radius: 8px; padding: 0 1rem; font-size: 0.82rem; cursor: pointer; transition: opacity 0.2s; flex-shrink: 0; min-width: 56px; }
.send-btn:hover:not(:disabled) { opacity: 0.85; }
.send-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.input-hint { font-size: 0.68rem; color: var(--text-muted); text-align: center; opacity: 0.8; }
.kickoff-mask { position: absolute; inset: 0; background: rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; z-index: 30; }
.kickoff-dialog { width: min(520px, calc(100% - 24px)); background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; padding: 0.9rem; box-shadow: 0 12px 36px rgba(0,0,0,0.22); }
.kickoff-hd { font-size: 0.9rem; font-weight: 700; color: var(--text-main); margin-bottom: 0.7rem; }
.kickoff-grid { display: grid; gap: 0.5rem; }
.kickoff-grid label { display: grid; gap: 0.2rem; font-size: 0.74rem; color: var(--text-muted); }
.kickoff-input { width: 100%; box-sizing: border-box; background: var(--bg-input); border: 1px solid var(--border); border-radius: 7px; color: var(--text-main); padding: 0.35rem 0.5rem; font-size: 0.8rem; }
.kickoff-input:focus { outline: none; border-color: var(--primary); }
.kickoff-actions { margin-top: 0.75rem; display: flex; justify-content: flex-end; gap: 0.5rem; }
</style>