<template>
  <div class="works-page">
    <div class="page-header">
      <div class="header-left">
        <h1>我的作品</h1>
        <p class="subtitle">管理你的所有创作作品</p>
      </div>
      <div class="header-actions">
        <div class="view-toggle">
          <button :class="{ active: viewMode === 'table' }" @click="viewMode = 'table'" title="表格视图">📊</button>
          <button :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'" title="网格视图">🔲</button>
        </div>
      </div>
    </div>

    <div class="tab-nav-wrapper">
      <div class="tab-nav">
        <button v-for="g in groupList" :key="g" class="tab-item" :class="{ active: currentGroup === g }" @click="currentGroup = g">
          <span class="tab-label">{{ g }}</span>
          <span class="tab-count">{{ groupCount(g) }}</span>
          <div v-if="g !== '全部' && g !== '未分组'" class="group-actions" @click.stop>
            <button class="group-btn edit" @click="editGroupName(g)">✏️</button>
            <button class="group-btn del" @click="deleteGroup(g)">✕</button>
          </div>
        </button>
      </div>
      <button class="btn-add-group" @click="createGroup">＋ 新建分组</button>
    </div>

    <div v-if="loading" class="loading"><div class="loading-spinner"></div><span>加载中...</span></div>
    <div v-else-if="filteredWorks.length === 0" class="empty">
      <div class="empty-icon">📝</div>
      <p>{{ works.length === 0 ? '还没有任何作品' : '该分组暂无作品' }}</p>
      <button @click="createWorkInGroup" class="btn-new">✦ 在此分组新建作品</button>
    </div>

    <div v-else-if="viewMode === 'table'" class="works-table-wrap">
      <table class="works-table">
        <thead><tr><th>标题</th><th style="width:160px">创建时间</th><th style="width:160px">最后修改</th><th style="width:140px">操作</th></tr></thead>
        <tbody>
          <tr v-for="work in filteredWorks" :key="work.id" class="work-row" :class="{ pinned: work.lastOpenedAt }">
            <td class="title-cell"><span v-if="work.lastOpenedAt" class="pin-badge">📌</span><span class="work-title">{{ work.title || '未命名' }}</span></td>
            <td class="date-cell">{{ formatDate(work.createdAt) }}</td>
            <td class="date-cell">{{ formatDate(work.updatedAt) }}</td>
            <td class="action-cell"><button @click="openWork(work.id)" class="btn-action view">打开</button><button @click="confirmDelete(work)" class="btn-action del">删除</button></td>
          </tr>
        </tbody>
      </table>
      <button @click="createWorkInGroup" class="btn-add-work">＋ 在此分组新建作品</button>
    </div>

    <div v-else class="works-grid">
      <div v-for="work in filteredWorks" :key="work.id" class="work-card" :class="{ pinned: work.lastOpenedAt }" @click="openWork(work.id)">
        <div class="card-top"><span v-if="work.lastOpenedAt" class="pin-dot"></span><h3 class="card-title">{{ work.title || '未命名' }}</h3></div>
        <div class="card-preview">{{ work.content ? work.content.slice(0,80) + (work.content.length > 80 ? '...' : '') : '暂无内容' }}</div>
        <div class="card-footer"><span class="date-label">🕐 {{ formatDate(work.updatedAt) }}</span><div class="card-actions" @click.stop><button @click="openWork(work.id)" class="btn-card open">打开</button><button @click="confirmDelete(work)" class="btn-card del">🗑</button></div></div>
      </div>
      <button @click="createWorkInGroup" class="btn-add-work-card">＋ 新建作品</button>
    </div>

    <div v-if="totalPages > 1" class="pagination">
      <button @click="goPage(page-1)" :disabled="page<=1" class="page-btn">‹ 上一页</button>
      <span class="page-info">第 {{ page }} / {{ totalPages }} 页，共 {{ total }} 篇</span>
      <button @click="goPage(page+1)" :disabled="page>=totalPages" class="page-btn">下一页 ›</button>
    </div>

    <!-- 删除作品确认 -->
    <div v-if="deletingWork" class="modal-overlay" @click.self="deletingWork = null">
      <div class="confirm-modal"><div class="confirm-icon">🗑</div><h3>确认删除？</h3><p>即将删除作品「{{ deletingWork.title || '未命名' }}」，此操作不可撤销。</p>
        <div class="confirm-actions"><button @click="deletingWork = null" class="btn-cancel">取消</button><button @click="doDelete" :disabled="deleting" class="btn-confirm-del">{{ deleting ? '删除中...' : '确认删除' }}</button></div>
      </div>
    </div>

    <!-- 删除分组确认 -->
    <div v-if="deletingGroup" class="modal-overlay" @click.self="deletingGroup = null">
      <div class="confirm-modal"><div class="confirm-icon">⚠️</div><h3>确认删除分组？</h3>
        <p>分组「{{ deletingGroup }}」及其中的所有作品将被<strong>永久删除</strong>。此操作不可撤销。</p>
        <div class="confirm-actions"><button @click="deletingGroup = null" class="btn-cancel">取消</button><button @click="doDeleteGroup" :disabled="deleting" class="btn-confirm-del">{{ deleting ? '删除中...' : '确认删除' }}</button></div>
      </div>
    </div>

    <!-- 重命名分组 -->
    <div v-if="editingGroup" class="modal-overlay" @click.self="editingGroup = null">
      <div class="confirm-modal"><h3>重命名分组</h3>
        <input v-model="editingGroupNewName" type="text" class="group-input" placeholder="新分组名称" @keyup.enter="doEditGroup" />
        <p v-if="groupOpError" class="group-op-error">{{ groupOpError }}</p>
        <div class="confirm-actions"><button @click="editingGroup = null" class="btn-cancel">取消</button><button @click="doEditGroup" :disabled="groupOpLoading" class="btn-confirm-del">{{ groupOpLoading ? '保存中...' : '确认' }}</button></div>
      </div>
    </div>

    <!-- 新建分组 -->
    <div v-if="showCreateGroup" class="modal-overlay" @click.self="showCreateGroup = false">
      <div class="confirm-modal"><h3>新建分组</h3>
        <input v-model="newGroupName" type="text" class="group-input" placeholder="分组名称" @keyup.enter="doCreateGroup" />
        <p v-if="groupOpError" class="group-op-error">{{ groupOpError }}</p>
        <div class="confirm-actions"><button @click="showCreateGroup = false" class="btn-cancel">取消</button><button @click="doCreateGroup" :disabled="groupOpLoading" class="btn-confirm-del">{{ groupOpLoading ? '创建中...' : '确认' }}</button></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { worksApi, workGroupsApi } from '../api/works'

const router = useRouter()
const works = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 20
const total = ref(0)
const totalPages = computed(() => Math.ceil(total.value / pageSize) || 1)
const viewMode = ref('grid')
const currentGroup = ref('全部')
const extraGroups = ref([])
const deletingWork = ref(null)
const deletingGroup = ref(null)
const editingGroup = ref(null)
const editingGroupNewName = ref('')
const deleting = ref(false)
const showCreateGroup = ref(false)
const newGroupName = ref('')
const groupOpLoading = ref(false)
const groupOpError = ref('')

const groupList = computed(() => {
  const set = new Set(['全部', '未分组'])
  extraGroups.value.forEach(g => set.add(g))
  works.value.forEach(w => { if (w.groupName) set.add(w.groupName) })
  return Array.from(set)
})

const filteredWorks = computed(() => {
  if (currentGroup.value === '全部') return works.value
  if (currentGroup.value === '未分组') return works.value.filter(w => !w.groupName)
  return works.value.filter(w => w.groupName === currentGroup.value)
})

function groupCount(g) {
  if (g === '全部') return works.value.length
  if (g === '未分组') return works.value.filter(w => !w.groupName).length
  return works.value.filter(w => w.groupName === g).length
}

function formatDate(d) {
  if (!d) return '-'
  const now = new Date(), date = new Date(d), diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff/60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff/3600000) + ' 小时前'
  if (diff < 604800000) return Math.floor(diff/86400000) + ' 天前'
  return date.toLocaleDateString('zh-CN', { month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' })
}

async function loadWorks() {
  loading.value = true
  try {
    const [worksRes, groupsRes] = await Promise.all([
      worksApi.list(page.value, pageSize),
      workGroupsApi.list().catch(e => { console.error('加载分组失败', e); return { data: { code: 200, data: [] } } }),
    ])
    if (worksRes.data?.code === 200 && worksRes.data?.data) {
      works.value = worksRes.data.data.records || []
      total.value = worksRes.data.data.total || 0
    }
    if (groupsRes.data?.code === 200 && groupsRes.data?.data) {
      extraGroups.value = groupsRes.data.data.map(g => g.name) || []
    }
  } catch(e) { console.error(e) } finally { loading.value = false }
}

async function createWorkInGroup() {
  const groupName = currentGroup.value === '全部' || currentGroup.value === '未分组' ? null : currentGroup.value
  try {
    const res = await worksApi.create({ title: '未命名', content: '', groupName })
    if (res.data?.code === 200 && res.data?.data?.id) router.push(`/workspace?workId=${res.data.data.id}`)
  } catch(e) { console.error(e) }
}

function openWork(id) { router.push(`/workspace?workId=${id}`) }
function confirmDelete(work) { deletingWork.value = work }

async function doDelete() {
  if (!deletingWork.value) return
  deleting.value = true
  try {
    await worksApi.delete(deletingWork.value.id)
    deletingWork.value = null
    await loadWorks()
  } catch(e) { console.error(e) } finally { deleting.value = false }
}

function goPage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p; loadWorks()
}

function createGroup() {
  newGroupName.value = ''
  groupOpError.value = ''
  showCreateGroup.value = true
}

async function doCreateGroup() {
  const trimmed = newGroupName.value.trim()
  if (!trimmed) { groupOpError.value = '分组名称不能为空'; return }
  groupOpLoading.value = true; groupOpError.value = ''
  try {
    const res = await workGroupsApi.create(trimmed)
    if (res.data?.code === 200 && res.data?.data) {
      if (!extraGroups.value.includes(res.data.data.name)) {
        extraGroups.value.push(res.data.data.name)
      }
      currentGroup.value = res.data.data.name
      showCreateGroup.value = false
    } else {
      groupOpError.value = res.data?.message || '创建失败'
    }
  } catch(e) {
    groupOpError.value = e.response?.data?.message || '创建失败，请检查是否已登录'
    console.error(e)
  } finally { groupOpLoading.value = false }
}

function editGroupName(g) {
  editingGroup.value = g
  editingGroupNewName.value = g
  groupOpError.value = ''
}

async function doEditGroup() {
  if (!editingGroupNewName.value?.trim()) return
  const newName = editingGroupNewName.value.trim()
  if (newName === editingGroup.value) { editingGroup.value = null; return }
  groupOpLoading.value = true; groupOpError.value = ''
  try {
    const groupsRes = await workGroupsApi.list()
    const group = groupsRes.data?.data?.find(g => g.name === editingGroup.value)
    if (!group) { groupOpError.value = '分组不存在'; groupOpLoading.value = false; return }
    await workGroupsApi.rename(group.id, newName)
    const idx = extraGroups.value.indexOf(editingGroup.value)
    if (idx >= 0) extraGroups.value[idx] = newName
    works.value.forEach(w => { if (w.groupName === editingGroup.value) w.groupName = newName })
    if (currentGroup.value === editingGroup.value) currentGroup.value = newName
    editingGroup.value = null
  } catch(e) {
    groupOpError.value = e.response?.data?.message || '重命名失败'
    console.error(e)
  } finally { groupOpLoading.value = false }
}

function deleteGroup(g) { deletingGroup.value = g }

async function doDeleteGroup() {
  if (!deletingGroup.value) return
  deleting.value = true
  try {
    const groupsRes = await workGroupsApi.list()
    const group = groupsRes.data?.data?.find(g => g.name === deletingGroup.value)
    if (!group) {
      // 分组在DB中不存在，只清理前端状态
      works.value = works.value.filter(w => w.groupName !== deletingGroup.value)
      const idx = extraGroups.value.indexOf(deletingGroup.value)
      if (idx >= 0) extraGroups.value.splice(idx, 1)
      if (currentGroup.value === deletingGroup.value) currentGroup.value = '全部'
      deletingGroup.value = null
      return
    }
    await workGroupsApi.delete(group.id)
    const idx = extraGroups.value.indexOf(deletingGroup.value)
    if (idx >= 0) extraGroups.value.splice(idx, 1)
    if (currentGroup.value === deletingGroup.value) currentGroup.value = '全部'
    deletingGroup.value = null
    await loadWorks()
  } catch(e) {
    alert(e.response?.data?.message || '删除失败')
    console.error(e)
  } finally { deleting.value = false }
}

onMounted(loadWorks)
</script>

<style scoped>
.works-page { padding: 2rem; max-width: 1200px; margin: 0 auto; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 1.75rem; }
.header-left h1 { font-size: 1.9rem; font-weight: 800; background: linear-gradient(90deg, var(--primary), var(--primary-light)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; margin: 0 0 0.25rem; }
.subtitle { color: var(--text-sub); font-size: 0.9rem; margin: 0; }
.header-actions { display: flex; gap: 1rem; }
.view-toggle { display: flex; gap: 0.3rem; border: 1px solid var(--border); border-radius: 7px; padding: 0.3rem; background: var(--bg-input); }
.view-toggle button { padding: 0.35rem 0.7rem; border: none; background: transparent; color: var(--text-muted); font-size: 1rem; cursor: pointer; border-radius: 5px; transition: all 0.2s; }
.view-toggle button.active { background: var(--primary); color: #fff; }

.tab-nav-wrapper { display: flex; align-items: center; justify-content: space-between; border-bottom: 2px solid var(--border); margin-bottom: 1.75rem; gap: 1rem; }
.tab-nav { display: flex; overflow-x: auto; gap: 0; flex: 1; scrollbar-width: none; }
.tab-nav::-webkit-scrollbar { display: none; }
.tab-item { display: inline-flex; align-items: center; gap: 0.45rem; padding: 0.7rem 1.35rem; border: none; background: transparent; color: var(--text-muted); font-size: 0.88rem; font-weight: 500; cursor: pointer; border-bottom: 3px solid transparent; margin-bottom: -2px; transition: all 0.18s; white-space: nowrap; border-radius: 6px 6px 0 0; position: relative; }
.tab-item:hover { color: var(--primary); background: var(--bg-hover); }
.tab-item.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 700; background: var(--bg-hover); }
.tab-count { display: inline-flex; align-items: center; justify-content: center; min-width: 20px; height: 18px; padding: 0 5px; background: var(--bg-input); color: var(--text-muted); border-radius: 999px; font-size: 0.7rem; font-weight: 700; }
.tab-item.active .tab-count { background: var(--primary); color: #fff; }
.group-actions { display: none; position: absolute; right: 0.5rem; top: 50%; transform: translateY(-50%); gap: 0.2rem; }
.tab-item:hover .group-actions { display: flex; }
.group-btn { padding: 0.2rem 0.4rem; border: none; background: transparent; color: var(--text-muted); font-size: 0.75rem; cursor: pointer; border-radius: 3px; transition: all 0.2s; }
.group-btn:hover { color: var(--primary); }
.group-btn.del:hover { color: #e53935; }
.btn-add-group { padding: 0.45rem 1rem; border: 1px dashed var(--border); border-bottom: none; border-radius: 7px 7px 0 0; background: transparent; color: var(--text-muted); font-size: 0.82rem; cursor: pointer; transition: all 0.2s; white-space: nowrap; flex-shrink: 0; }
.btn-add-group:hover { border-color: var(--primary); color: var(--primary); }

.loading { display: flex; flex-direction: column; align-items: center; gap: 1rem; padding: 5rem; color: var(--text-muted); }
.loading-spinner { width: 32px; height: 32px; border: 3px solid var(--border); border-top-color: var(--primary); border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.empty { text-align: center; padding: 5rem 2rem; color: var(--text-muted); }
.empty-icon { font-size: 3.5rem; margin-bottom: 1rem; }
.empty p { margin: 0 0 1.5rem; }
.btn-new { padding: 0.65rem 1.5rem; border-radius: 9px; border: none; background: linear-gradient(135deg, var(--primary), var(--primary-light)); color: #fff; font-weight: 700; font-size: 0.95rem; cursor: pointer; transition: all 0.2s; }
.btn-new:hover { opacity: 0.88; transform: translateY(-2px); }

.works-table-wrap { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
.works-table { width: 100%; border-collapse: collapse; }
.works-table th { padding: 0.85rem 1rem; text-align: left; font-size: 0.78rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-muted); background: var(--bg-input); border-bottom: 1px solid var(--border); }
.works-table td { padding: 0.85rem 1rem; border-bottom: 1px solid var(--border); color: var(--text-main); font-size: 0.9rem; }
.works-table tr:last-child td { border-bottom: none; }
.work-row { cursor: pointer; transition: background 0.15s; }
.work-row:hover td { background: var(--bg-hover); }
.work-row.pinned > td:nth-child(1) { border-left: 3px solid var(--primary); padding-left: calc(1rem - 3px); }
.title-cell { display: flex; align-items: center; gap: 0.4rem; max-width: 380px; }
.pin-badge { font-size: 0.82rem; flex-shrink: 0; }
.work-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 600; }
.date-cell { font-size: 0.82rem; color: var(--text-sub); white-space: nowrap; }
.action-cell { white-space: nowrap; }
.btn-action { padding: 0.28rem 0.75rem; border-radius: 5px; font-size: 0.8rem; font-weight: 600; cursor: pointer; border: 1px solid var(--border); background: transparent; color: var(--text-sub); transition: all 0.2s; margin-right: 0.3rem; }
.btn-action.view { border-color: var(--primary); color: var(--primary); }
.btn-action.view:hover { background: var(--primary); color: #fff; }
.btn-action.del { border-color: #e53935; color: #e53935; }
.btn-action.del:hover { background: rgba(229,57,53,0.1); }
.btn-add-work { width: 100%; padding: 0.75rem; border: 1px dashed var(--border); border-top: none; background: transparent; color: var(--text-muted); font-size: 0.85rem; cursor: pointer; transition: all 0.2s; }
.btn-add-work:hover { border-color: var(--primary); color: var(--primary); }

.works-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(290px, 1fr)); gap: 1.25rem; }
.work-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 1.25rem 1.35rem; cursor: pointer; transition: all 0.22s; display: flex; flex-direction: column; gap: 0.8rem; position: relative; overflow: hidden; }
.work-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px; background: linear-gradient(90deg, var(--primary), var(--primary-light)); opacity: 0; transition: opacity 0.2s; }
.work-card:hover { border-color: var(--primary); box-shadow: 0 8px 28px var(--shadow); transform: translateY(-3px); }
.work-card:hover::before { opacity: 1; }
.work-card.pinned { border-left: 3px solid var(--primary); }
.work-card.pinned::before { opacity: 1; }
.card-top { display: flex; align-items: flex-start; gap: 0.5rem; }
.pin-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--primary); flex-shrink: 0; margin-top: 5px; animation: pulse-dot 2s ease-in-out infinite; }
@keyframes pulse-dot { 0%,100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.5; transform: scale(0.8); } }
.card-title { flex: 1; font-size: 1rem; font-weight: 700; color: var(--text-main); margin: 0; line-height: 1.4; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.card-preview { font-size: 0.82rem; color: var(--text-muted); line-height: 1.65; flex: 1; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; min-height: 40px; }
.card-footer { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; flex-wrap: wrap; border-top: 1px solid var(--border); padding-top: 0.75rem; margin-top: auto; }
.date-label { font-size: 0.75rem; color: var(--text-muted); white-space: nowrap; }
.card-actions { display: flex; align-items: center; gap: 0.4rem; }
.btn-card { padding: 0.25rem 0.65rem; border-radius: 5px; font-size: 0.78rem; font-weight: 600; cursor: pointer; border: 1px solid var(--border); background: transparent; transition: all 0.2s; }
.btn-card.open { border-color: var(--primary); color: var(--primary); }
.btn-card.open:hover { background: var(--primary); color: #fff; }
.btn-card.del { border-color: #e53935; color: #e53935; padding: 0.25rem 0.5rem; }
.btn-card.del:hover { background: rgba(229,57,53,0.1); }
.btn-add-work-card { grid-column: 1 / -1; padding: 2rem; border: 2px dashed var(--border); border-radius: 12px; background: transparent; color: var(--text-muted); font-size: 0.9rem; cursor: pointer; transition: all 0.2s; }
.btn-add-work-card:hover { border-color: var(--primary); color: var(--primary); }

.pagination { display: flex; align-items: center; justify-content: center; gap: 1rem; padding: 1.5rem 0 0; }
.page-btn { padding: 0.4rem 1rem; border-radius: 6px; border: 1px solid var(--border); background: transparent; color: var(--text-sub); cursor: pointer; font-size: 0.88rem; transition: all 0.2s; }
.page-btn:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 0.85rem; color: var(--text-muted); }

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.55); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.confirm-modal { background: var(--bg-card); border: 1px solid var(--border); border-radius: 14px; padding: 2.5rem 2rem; width: 90%; max-width: 380px; text-align: center; box-shadow: 0 16px 48px var(--shadow); }
.confirm-icon { font-size: 2.5rem; margin-bottom: 0.75rem; }
.confirm-modal h3 { margin: 0 0 0.75rem; font-size: 1.15rem; color: var(--text-main); }
.confirm-modal p { color: var(--text-sub); font-size: 0.9rem; margin: 0 0 1.5rem; line-height: 1.6; }
.group-input { width: 100%; padding: 0.6rem 0.9rem; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-input); color: var(--text-main); font-size: 0.9rem; margin-bottom: 1rem; box-sizing: border-box; }
.group-input:focus { outline: none; border-color: var(--primary); }
.group-op-error { color: #e53935; font-size: 0.82rem; margin: 0 0 1rem; text-align: left; }
.confirm-actions { display: flex; gap: 0.75rem; }
.btn-cancel { flex: 1; padding: 0.6rem; border-radius: 7px; border: 1px solid var(--border); background: transparent; color: var(--text-sub); font-size: 0.9rem; cursor: pointer; }
.btn-cancel:hover { border-color: var(--primary); color: var(--primary); }
.btn-confirm-del { flex: 1; padding: 0.6rem; border-radius: 7px; border: none; background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; font-weight: 700; font-size: 0.9rem; cursor: pointer; }
.btn-confirm-del:disabled { opacity: 0.5; cursor: not-allowed; }
</style> 