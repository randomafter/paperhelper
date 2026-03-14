<template>
  <div class="page-shell">
    <div class="materials-main">
      <div class="page-header">
        <h1>素材检索</h1>
        <p class="subtitle">搜索和浏览历史特色素材库</p>
      </div>
      <div class="filter-bar">
        <select v-model="filters.category" class="filter-select">
          <option value="">全部分类</option>
          <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
        </select>
        <input v-model="filters.keyword" type="text" placeholder="搜索标题或内容关键词..." class="filter-input" @keyup.enter="onSearch" />
        <button @click="onSearch" class="btn-primary">检索</button>
        <button @click="onReset" class="btn-secondary">重置</button>
        <button v-if="isAdmin" @click="openCreateModal" class="btn-create">+ 新增素材</button>
      </div>
      <div class="table-wrap">
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="materials.length === 0" class="empty">暂无素材，请调整筛选条件</div>
        <table v-else class="material-table">
          <thead><tr>
            <th>分类</th><th>标题 / 内容</th><th>标签</th><th v-if="isAdmin">操作</th>
          </tr></thead>
          <tbody>
            <tr v-for="item in materials" :key="item.id" 
                :class="{ 'row-active': activeDetail && activeDetail.id === item.id }"
                @click="openDetail(item.id)"
                style="cursor: pointer;">
              <td>{{ item.category }}</td>
              <td class="title-cell">
                <div class="title-text" v-html="highlight(item.title)"></div>
                <div v-if="getSnippet(item)" class="snippet" v-html="getSnippet(item)"></div>
              </td>
              <td><span v-for="tag in (item.tags||[]).slice(0,3)" :key="tag" class="tag">{{ tag }}</span></td>
              <td v-if="isAdmin" class="action-cell" @click.stop>
                <button @click="openEditModal(item)" class="btn-action edit">编辑</button>
                <button @click="confirmDelete(item)" class="btn-action del">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="total > 0" class="pagination">
        <button :disabled="page <= 1" @click="changePage(page-1)" class="page-btn">‹ 上一页</button>
        <span class="page-info">第 {{ page }} / {{ totalPages }} 页，共 {{ total }} 条</span>
        <button :disabled="page >= totalPages" @click="changePage(page+1)" class="page-btn">下一页 ›</button>
        <select v-model.number="pageSize" @change="onSearch" class="filter-select">
          <option :value="10">10条/页</option><option :value="20">20条/页</option><option :value="50">50条/页</option>
        </select>
      </div>
    </div>

    <!-- 右侧详情抽屉 -->
    <div v-if="detailOpen && !detailCollapsed" class="detail-drawer" :style="{ width: drawerWidth + 'px' }">
      <div class="resize-handle" @mousedown="startResize"></div>
      <div class="drawer-tabs">
        <button v-for="tab in tabs.tabs" :key="tab.id"
          class="drawer-tab" :class="{ active: activeDetail && activeDetail.id === tab.id }"
          @click="switchTab(tab.id)">
          <span class="tab-title">{{ tab.title }}</span>
          <span class="tab-close" @click.stop="closeTab(tab.id)">✕</span>
        </button>
        <span class="drawer-ctrl" @click="collapseDrawer" title="收缩为书签">⇥</span>
        <span class="drawer-ctrl" @click="closeAllDrawer" title="关闭">✕</span>
      </div>
      <div class="drawer-body">
        <div v-if="detailLoading" class="loading">加载中...</div>
        <div v-else-if="activeDetail">
          <div class="drawer-meta">
            <span class="category-tag">{{ activeDetail.category }}</span>
          </div>
          <h2 class="drawer-title">{{ activeDetail.title }}</h2>
          <div v-if="activeDetail.tags && activeDetail.tags.length" class="drawer-tags">
            <span v-for="tag in activeDetail.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
          <pre class="drawer-content">{{ activeDetail.content }}</pre>
          <div class="drawer-actions">
            <button @click="toggleFavorite" :disabled="favLoading" class="btn-fav" :class="{ favorited: activeDetail.isFavorite }">
              {{ activeDetail.isFavorite ? '❤️ 取消收藏' : '🤍 收藏' }}
            </button>
            <button @click="useInWorkspace(activeDetail)" class="btn-use-ws">🖊 引用到工作台</button>
          </div>
          <!-- 相似素材推荐 -->
          <div v-if="similarItems.length" class="similar-section">
            <div class="similar-heading">相似素材推荐</div>
            <div v-for="s in similarItems" :key="s.id" class="similar-card" @click="openDetail(s.id)">
              <span class="category-tag small">{{ s.category }}</span>
              <span class="similar-title">{{ s.title }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 书签栏 -->
    <div v-if="detailCollapsed && tabs.tabs.length" class="bookmark-bar">
      <div v-for="tab in tabs.tabs" :key="tab.id" class="bookmark" @click="expandTab(tab.id)">
        <span class="bookmark-title">{{ tab.title }}</span>
        <span class="bookmark-close" @click.stop="closeTab(tab.id)">✕</span>
      </div>
    </div>

    <!-- 新增/编辑 -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal-content wide">
        <div class="modal-header">
          <h2>{{ editingItem ? '编辑素材' : '新增素材' }}</h2>
          <button class="close-btn" @click="showModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>分类 *</label>
            <select v-model="form.category" class="filter-select full">
              <option value="">请选择</option>
              <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>标题 *</label>
            <input v-model="form.title" type="text" placeholder="素材标题" class="filter-input full" />
          </div>
          <div class="form-group">
            <label>内容 *</label>
            <textarea v-model="form.content" rows="10" placeholder="素材内容" class="filter-input full" />
          </div>
          <div class="form-group">
            <label>标签（逗号分隔）</label>
            <input v-model="form.tagsStr" type="text" placeholder="如：战役,将领" class="filter-input full" />
          </div>
          <p v-if="modalError" class="error">{{ modalError }}</p>
        </div>
        <div class="modal-footer">
          <button @click="showModal = false" class="btn-secondary">取消</button>
          <button @click="onSave" :disabled="saving" class="btn-primary">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- 删除确认 -->
    <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="showDeleteConfirm = false">
      <div class="modal-content small">
        <div class="modal-header"><h2>确认删除</h2><button class="close-btn" @click="showDeleteConfirm = false">✕</button></div>
        <div class="modal-body"><p style="color:var(--text-main)">确认删除「{{ deletingItem?.title }}」？此操作不可撤销。</p></div>
        <div class="modal-footer">
          <button @click="showDeleteConfirm = false" class="btn-secondary">取消</button>
          <button @click="doDelete" :disabled="deleting" class="btn-danger">{{ deleting ? '删除中...' : '确认删除' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useAuthStore } from '../stores/auth'
import { materialApi } from '../api/material'
import { useMaterialTabsStore } from '../stores/materialTabs'
import { useWorkspaceStore } from '../stores/workspace'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const tabs = useMaterialTabsStore()
const workspaceStore = useWorkspaceStore()
const router = useRouter()
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
const categories = [
  '历史沉淀',
  '传统民俗',
  '服饰装扮',
  '行业手艺',
  '宗教信仰',
  '兵器武林',
  '饮食文化',
  '玉石珍宝',
  '传说典故',
  '科技文明',
  '五行异象',
  '其他',
]
const filters = reactive({ category: '', keyword: '' })
const materials = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)
const activeDetail = ref(null)
const detailOpen = ref(false)
const detailCollapsed = ref(false)
const detailLoading = ref(false)
const drawerWidth = ref(440)
const favLoading = ref(false)
const similarItems = ref([])
const showModal = ref(false)
const editingItem = ref(null)
const saving = ref(false)
const modalError = ref('')
const form = reactive({ category: '', title: '', content: '', tagsStr: '' })
const showDeleteConfirm = ref(false)
const deletingItem = ref(null)
const deleting = ref(false)

async function loadMaterials() {
  loading.value = true
  try {
    const res = await materialApi.search({
      category: filters.category || undefined,
      keyword: filters.keyword || undefined,
      page: page.value,
      size: pageSize.value,
    })
    if (res.data?.code === 200 && res.data?.data) {
      materials.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
  } catch(e) { console.error(e) } finally { loading.value = false }
}
function onSearch() { page.value = 1; loadMaterials() }
function onReset() { filters.category = ''; filters.keyword = ''; page.value = 1; loadMaterials() }
function changePage(p) { page.value = p; loadMaterials() }

async function openDetail(id) {
  detailLoading.value = true; detailOpen.value = true; detailCollapsed.value = false
  try {
    const res = await materialApi.getById(id)
    if (res.data?.code === 200 && res.data?.data) {
      activeDetail.value = res.data.data
      tabs.openTab(res.data.data)
      loadSimilar(res.data.data)
    }
  } catch(e) { console.error(e) } finally { detailLoading.value = false }
}

async function loadSimilar(item) {
  similarItems.value = []
  try {
    const res = await materialApi.search({ category: item.category, page: 1, size: 6 })
    if (res.data?.code === 200 && res.data?.data) {
      similarItems.value = (res.data.data.records || []).filter(s => s.id !== item.id).slice(0, 5)
    }
  } catch(e) { console.error(e) }
}
async function switchTab(id) {
  if (activeDetail.value?.id === id) return
  detailLoading.value = true; detailCollapsed.value = false; detailOpen.value = true
  try {
    const res = await materialApi.getById(id)
    if (res.data?.code === 200 && res.data?.data) {
      activeDetail.value = res.data.data
      loadSimilar(res.data.data)
    }
  } catch(e) { console.error(e) } finally { detailLoading.value = false }
}

function useInWorkspace(item) {
  workspaceStore.insertMaterial(item)
  router.push('/workspace')
}
function closeTab(id) {
  tabs.closeTab(id)
  if (activeDetail.value?.id === id) {
    if (tabs.tabs.length) switchTab(tabs.tabs[tabs.tabs.length - 1].id)
    else { activeDetail.value = null; detailOpen.value = false }
  }
}
function collapseDrawer() { detailCollapsed.value = true }
function closeAllDrawer() { tabs.clearTabs(); detailOpen.value = false; activeDetail.value = null }
function expandTab(id) { detailCollapsed.value = false; switchTab(id) }
async function toggleFavorite() {
  if (!activeDetail.value) return
  favLoading.value = true
  try { await materialApi.toggleFavorite(activeDetail.value.id); activeDetail.value.isFavorite = !activeDetail.value.isFavorite }
  catch(e) { console.error(e) } finally { favLoading.value = false }
}
let resizing = false, startX = 0, startW = 0
function startResize(e) {
  resizing = true; startX = e.clientX; startW = drawerWidth.value
  document.addEventListener('mousemove', onResize)
  document.addEventListener('mouseup', stopResize)
}
function onResize(e) {
  if (!resizing) return
  drawerWidth.value = Math.max(280, Math.min(window.innerWidth * 0.7, startW + (startX - e.clientX)))
}
function stopResize() {
  resizing = false
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
}
onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
})
function esc(s) { return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') }
function highlight(text) {
  const kw = (filters.keyword || '').trim()
  if (!kw) return text
  try { return String(text || '').replace(new RegExp(esc(kw), 'gi'), m => `<mark class="hl">${m}</mark>`) }
  catch { return text }
}
function getSnippet(item) {
  const kw = (filters.keyword || '').trim()
  if (!kw || !item.content) return ''
  try {
    const idx = item.content.toLowerCase().indexOf(kw.toLowerCase())
    if (idx === -1) return ''
    const s = Math.max(0, idx - 30)
    const e = Math.min(item.content.length, idx + kw.length + 60)
    const raw = (s > 0 ? '...' : '') + item.content.slice(s, e) + (e < item.content.length ? '...' : '')
    return raw.replace(new RegExp(esc(kw), 'gi'), m => `<mark class="hl">${m}</mark>`)
  } catch { return '' }
}
function openCreateModal() {
  editingItem.value = null
  Object.assign(form, { category: '', title: '', content: '', tagsStr: '' })
  modalError.value = ''; showModal.value = true
}
function openEditModal(item) {
  editingItem.value = item
  Object.assign(form, { category: item.category, title: item.title, content: item.content, tagsStr: (item.tags || []).join(',') })
  modalError.value = ''; showModal.value = true
}
async function onSave() {
  if (!form.category || !form.title || !form.content) { modalError.value = '请填写必填字段'; return }
  saving.value = true; modalError.value = ''
  const tags = form.tagsStr ? form.tagsStr.split(',').map(t => t.trim()).filter(Boolean) : []
  try {
    const payload = { category: form.category, title: form.title, content: form.content, tags }
    if (editingItem.value) await materialApi.update({ ...payload, id: editingItem.value.id })
    else await materialApi.create(payload)
    showModal.value = false; loadMaterials()
  } catch(e) { modalError.value = e.response?.data?.message || '保存失败' }
  finally { saving.value = false }
}
function confirmDelete(item) { deletingItem.value = item; showDeleteConfirm.value = true }
async function doDelete() {
  deleting.value = true
  try { await materialApi.remove(deletingItem.value.id); showDeleteConfirm.value = false; loadMaterials() }
  catch(e) { console.error(e) } finally { deleting.value = false }
}
onMounted(loadMaterials)
</script>

<style scoped>
.page-shell { display: flex; min-height: 100%; position: relative; }
.materials-main { flex: 1; padding: 2rem; max-width: 100%; min-width: 0; }
.page-header { margin-bottom: 1.5rem; }
.page-header h1 { font-size: 1.75rem; font-weight: 700; background: linear-gradient(90deg, var(--primary), var(--primary-light)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; margin: 0 0 0.25rem; }
.subtitle { color: var(--text-sub); font-size: 0.9rem; margin: 0; }
.filter-bar { display: flex; flex-wrap: wrap; gap: 0.75rem; align-items: center; margin-bottom: 1.5rem; padding: 1.25rem; background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; }
.filter-select {
  padding: 0.55rem 0.9rem;
  border: 1px solid var(--border);
  border-radius: 7px;
  background: var(--bg-card);
  color: var(--text-main);
  font-size: 0.9rem;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'%3E%3Cpath d='M1 1l5 5 5-5' stroke='%23888' stroke-width='1.5' fill='none' stroke-linecap='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  padding-right: 2rem;
}
.filter-input { flex: 1; min-width: 160px; padding: 0.55rem 0.9rem; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-input); color: var(--text-main); font-size: 0.9rem; font-family: inherit; }
.filter-input:focus, .filter-select:focus { outline: none; border-color: var(--primary); }
.full { width: 100%; }
textarea.filter-input { resize: vertical; }
.btn-primary, .btn-secondary, .btn-create, .btn-danger { padding: 0.55rem 1.2rem; border-radius: 7px; font-weight: 600; cursor: pointer; font-size: 0.9rem; border: none; transition: all 0.2s; }
.btn-primary { background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; }
.btn-primary:hover:not(:disabled) { opacity: 0.85; transform: translateY(-1px); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { background: transparent; border: 1px solid var(--border); color: var(--text-sub); }
.btn-secondary:hover { border-color: var(--primary); color: var(--primary); background: var(--bg-hover); }
.btn-create { background: linear-gradient(90deg, var(--accent), #f9a825); color: #1a1a1a; font-weight: 700; }
.btn-create:hover { opacity: 0.85; }
.btn-danger { background: #e53935; color: #fff; }
.btn-danger:hover:not(:disabled) { opacity: 0.85; }
.btn-danger:disabled { opacity: 0.5; cursor: not-allowed; }
.table-wrap { background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; overflow: hidden; }
.material-table { width: 100%; border-collapse: collapse; }
.material-table th { padding: 0.9rem 1rem; text-align: left; font-size: 0.78rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-muted); background: var(--bg-input); border-bottom: 1px solid var(--border); }
.material-table td { padding: 0.85rem 1rem; border-bottom: 1px solid var(--border); color: var(--text-main); font-size: 0.9rem; vertical-align: top; }
.material-table tr:last-child td { border-bottom: none; }
.material-table tr:hover td { background: var(--bg-hover); }
.row-active td { background: var(--bg-hover) !important; }
.dynasty-tag { display: inline-block; padding: 0.18rem 0.6rem; background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; border-radius: 20px; font-size: 0.75rem; font-weight: 600; white-space: nowrap; }
.category-tag { display: inline-block; padding: 0.18rem 0.6rem; background: var(--tag-bg); color: var(--text-sub); border-radius: 20px; font-size: 0.75rem; }
.title-cell { max-width: 320px; }
.title-text { font-weight: 600; color: var(--text-main); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.snippet { font-size: 0.8rem; color: var(--text-muted); margin-top: 0.3rem; line-height: 1.4; word-break: break-all; }
.tag { display: inline-block; padding: 0.15rem 0.5rem; background: var(--tag-bg); color: var(--text-sub); border-radius: 4px; font-size: 0.75rem; margin-right: 0.3rem; margin-bottom: 0.2rem; }
.action-cell { white-space: nowrap; }
.btn-action { padding: 0.28rem 0.7rem; border-radius: 5px; font-size: 0.8rem; font-weight: 600; cursor: pointer; border: 1px solid var(--border); background: transparent; color: var(--text-sub); transition: all 0.2s; margin-right: 0.25rem; }
.btn-action:hover, .btn-action.active { border-color: var(--primary); color: var(--primary); background: var(--bg-hover); }
.btn-action.edit { border-color: var(--accent); color: var(--accent); }
.btn-action.del { border-color: #e53935; color: #e53935; }
.btn-action.del:hover { background: rgba(229,57,53,0.1); }
.loading, .empty { text-align: center; padding: 3rem; color: var(--text-muted); }
.pagination { display: flex; align-items: center; gap: 1rem; margin-top: 1.5rem; flex-wrap: wrap; }
.page-btn { padding: 0.48rem 1rem; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-card); color: var(--text-sub); cursor: pointer; font-size: 0.9rem; transition: all 0.2s; }
.page-btn:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { color: var(--text-sub); font-size: 0.9rem; }
.hl { background: var(--accent); color: #1a1a1a; padding: 0 2px; border-radius: 3px; font-weight: 700; }
.detail-drawer { position: fixed; top: 60px; right: 0; bottom: 0; background: var(--bg-card); border-left: 1px solid var(--border); box-shadow: -4px 0 20px var(--shadow); display: flex; flex-direction: column; z-index: 200; min-width: 280px; }
.resize-handle { position: absolute; left: 0; top: 0; bottom: 0; width: 6px; cursor: col-resize; z-index: 201; }
.resize-handle:hover, .resize-handle:active { background: var(--primary); opacity: 0.4; }
.drawer-tabs { display: flex; align-items: center; gap: 0.3rem; padding: 0.6rem 0.75rem; border-bottom: 1px solid var(--border); background: var(--bg-input); overflow-x: auto; flex-shrink: 0; }
.drawer-tab { display: inline-flex; align-items: center; gap: 0.3rem; padding: 0.25rem 0.7rem; border-radius: 999px; border: 1px solid var(--border); background: var(--bg-card); color: var(--text-sub); font-size: 0.8rem; cursor: pointer; white-space: nowrap; transition: all 0.2s; }
.drawer-tab.active { background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; border-color: transparent; }
.tab-title { max-width: 120px; overflow: hidden; text-overflow: ellipsis; }
.tab-close { opacity: 0.6; cursor: pointer; font-size: 0.75rem; padding: 0 2px; }
.tab-close:hover { opacity: 1; }
.drawer-ctrl { padding: 0.2rem 0.5rem; cursor: pointer; color: var(--text-muted); font-size: 1rem; border-radius: 4px; flex-shrink: 0; user-select: none; }
.drawer-ctrl:hover { color: var(--primary); }
.drawer-body { flex: 1; overflow-y: auto; padding: 1.25rem; }
.drawer-meta { display: flex; gap: 0.5rem; margin-bottom: 0.75rem; flex-wrap: wrap; }
.drawer-title { font-size: 1.3rem; font-weight: 700; color: var(--text-main); margin: 0 0 0.75rem; line-height: 1.4; }
.drawer-tags { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-bottom: 1rem; }
.drawer-content { background: var(--bg-input); border-radius: 8px; padding: 1rem; font-size: 0.9rem; color: var(--text-main); line-height: 1.8; white-space: pre-wrap; word-break: break-all; margin-bottom: 1rem; }
.drawer-source { font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1rem; }
.drawer-source a { color: var(--primary); }
.drawer-actions { display: flex; gap: 0.75rem; }
.btn-fav { padding: 0.55rem 1.2rem; border-radius: 7px; border: 1px solid var(--border); background: transparent; color: var(--text-sub); cursor: pointer; font-size: 0.9rem; font-weight: 600; transition: all 0.2s; }
.btn-fav:hover { border-color: var(--primary); color: var(--primary); background: var(--bg-hover); }
.btn-fav.favorited { background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; border-color: transparent; }
.btn-fav:disabled { opacity: 0.5; cursor: not-allowed; }
.bookmark-bar { position: fixed; right: 0; bottom: 2rem; display: flex; flex-direction: column; gap: 0.5rem; z-index: 200; }
.bookmark { display: flex; align-items: center; gap: 0.5rem; background: var(--bg-card); border: 1px solid var(--border); border-right: none; border-radius: 8px 0 0 8px; padding: 0.45rem 0.9rem; cursor: pointer; box-shadow: -2px 2px 8px var(--shadow); max-width: 220px; transition: all 0.2s; }
.bookmark:hover { border-color: var(--primary); }
.bookmark-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 0.85rem; color: var(--text-main); }
.bookmark-close { color: var(--text-muted); font-size: 0.75rem; cursor: pointer; padding: 0 2px; flex-shrink: 0; }
.bookmark-close:hover { color: var(--primary); }
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: var(--bg-card); border: 1px solid var(--border-accent); border-radius: 14px; width: 90%; max-width: 760px; box-shadow: 0 16px 48px var(--shadow); }
.modal-content.wide { max-width: 820px; }
.modal-content.small { max-width: 380px; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 1.25rem 1.5rem; border-bottom: 1px solid var(--border); }
.modal-header h2 { margin: 0; color: var(--text-main); font-size: 1.1rem; }
.close-btn { background: transparent; border: none; color: var(--text-muted); font-size: 1.4rem; cursor: pointer; transition: color 0.2s; }
.close-btn:hover { color: var(--primary); }
.modal-body { padding: 1.5rem; max-height: 70vh; overflow-y: auto; }
.modal-footer { display: flex; gap: 1rem; padding: 1.25rem 1.5rem; border-top: 1px solid var(--border); }
.modal-footer button { flex: 1; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.form-group { margin-bottom: 1.1rem; }
.form-group label { display: block; margin-bottom: 0.4rem; color: var(--text-main); font-weight: 600; font-size: 0.85rem; }
.error { color: #e53935; font-size: 0.875rem; margin: 0.5rem 0; padding: 0.65rem; background: rgba(229,57,53,0.1); border-left: 3px solid #e53935; border-radius: 4px; }
.btn-use-ws { padding: 0.55rem 1.2rem; border-radius: 7px; border: 1px solid var(--accent); background: transparent; color: var(--accent); font-size: 0.9rem; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.btn-use-ws:hover { background: var(--accent); color: #1a1a1a; }
.similar-section { margin-top: 1.5rem; padding-top: 1.5rem; border-top: 1px solid var(--border); }
.similar-heading { font-size: 0.8rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-muted); margin-bottom: 0.75rem; }
.similar-card { display: flex; align-items: center; gap: 0.5rem; padding: 0.6rem 0.75rem; border-radius: 7px; cursor: pointer; transition: background 0.2s; margin-bottom: 0.4rem; }
.similar-card:hover { background: var(--bg-hover); }
.similar-title { flex: 1; font-size: 0.85rem; color: var(--text-main); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.category-tag.small { font-size: 0.68rem; padding: 0.1rem 0.45rem; }
@media (max-width: 768px) { .materials-main { padding: 1rem; } .filter-bar { gap: 0.5rem; } .filter-input { min-width: 120px; } .form-row { grid-template-columns: 1fr; } .detail-drawer { top: 0; } }
</style>
