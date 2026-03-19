<template>
  <div class="favorites-page">
    <div class="page-header">
      <h1>我的收藏</h1>
      <p class="subtitle">管理收藏素材，或创建自己的素材提交审核</p>
    </div>

    <!-- 主Tab切换 -->
    <div class="main-tabs">
      <button class="main-tab" :class="{ active: mainTab === 'favorites' }" @click="mainTab = 'favorites'">收藏素材</button>
      <button class="main-tab" :class="{ active: mainTab === 'mine' }" @click="mainTab = 'mine'; loadMyMaterials()">我的素材</button>
    </div>

    <!-- 收藏素材面板 -->
    <template v-if="mainTab === 'favorites'">

    <!-- 分组切换 -->
    <div class="group-bar" v-if="groupList.length">
      <button
        v-for="g in groupList"
        :key="g"
        class="group-chip"
        :class="{ active: currentGroup === g }"
        @click="currentGroup = g"
      >
        {{ g }}
      </button>
      <button class="group-chip add" @click="createGroup">＋ 新建分组</button>
    </div>

    <!-- ECharts 饼图统计 -->
    <div v-if="!loading && favorites.length > 0" class="stat-row">
      <div ref="chartEl" class="pie-chart"></div>
      <div class="stat-info">
        <div class="stat-num">{{ favorites.length }}</div>
        <div class="stat-label">条收藏素材</div>
        <div class="stat-cats">
          <span v-for="(count, cat) in categoryMap" :key="cat" class="stat-cat-item">{{ cat }} {{ count }} 条</span>
        </div>
      </div>
    </div>

    <div class="table-wrap">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="favorites.length === 0" class="empty">
        暂无收藏，默认已为你预置
        <span class="tag">我的灵感</span>
        和
        <span class="tag">未定义</span>
        两个分组，去素材检索页面收藏一些素材吧
      </div>
      <table v-else class="material-table">
        <thead><tr>
          <th>分组</th><th>分类</th><th>标题</th><th>标签</th><th>操作</th>
        </tr></thead>
        <tbody>
          <tr
            v-for="item in filteredFavorites"
            :key="item.id"
            :class="{ 'row-active': activeDetail && activeDetail.id === item.id }"
            @click="openDetail(item.id)"
            style="cursor: pointer;"
          >
            <td>
              <select
                class="group-select"
                :value="item.favoriteGroup"
                @change="onGroupSelect(item, $event.target.value)"
                @click.stop
              >
                <option v-for="g in groupList" :key="g" :value="g">{{ g }}</option>
              </select>
            </td>
            <td>{{ item.category }}</td>
            <td class="title-cell">{{ item.title }}</td>
            <td><span v-for="tag in (item.tags||[]).slice(0,3)" :key="tag" class="tag">{{ tag }}</span></td>
            <td class="action-cell">
              <span v-if="item._source === 'mine'" class="mine-badge">我创作</span>
              <button @click.stop="applyToAI(item)" class="btn-action ai">✦ AI</button>
              <button @click.stop="useInWorkspace(item)" class="btn-action use">引用</button>
              <button v-if="item._source !== 'mine'" @click.stop="removeFavorite(item)" class="btn-action del">取消收藏</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="copyMsg" class="toast">{{ copyMsg }}</div>

    </template><!-- end favorites tab -->

    <!-- 我的素材面板 -->
    <template v-if="mainTab === 'mine'">
      <div class="mine-toolbar">
        <button @click="openMineModal(null)" class="btn-create-mine">＋ 新建素材</button>
      </div>
      <div v-if="mineLoading" class="loading">加载中...</div>
      <div v-else-if="myMaterials.length === 0" class="empty">暂无自建素材，点击「新建素材」创建并提交审核</div>
      <div v-else class="mine-list">
        <div v-for="item in myMaterials" :key="item.id" class="mine-card">
          <div class="mine-card-header">
            <span class="cat-badge">{{ item.category }}</span>
            <span class="mine-title">{{ item.title }}</span>
            <span class="status-badge" :class="item.status">{{ statusLabel(item.status) }}</span>
          </div>
          <div class="mine-preview">{{ item.content?.slice(0, 100) }}{{ (item.content?.length||0) > 100 ? '...' : '' }}</div>
          <div v-if="item.adminComment" class="admin-comment">审核意见：{{ item.adminComment }}</div>
          <div class="mine-actions">
            <!-- draft/rejected/approved 都可编辑 -->
            <button
              v-if="item.status !== 'pending'"
              @click="openMineModal(item)"
              class="btn-action edit">编辑</button>
            <!-- draft/rejected/approved 都可提交审核 -->
            <button
              v-if="item.status !== 'pending'"
              @click="submitMine(item)"
              class="btn-action submit">提交审核</button>
            <!-- 审核中不可删除，其余状态都可删除 -->
            <button
              v-if="item.status !== 'pending'"
              @click="deleteMine(item)"
              class="btn-action del">删除</button>
            <!-- 审核中提示 -->
            <span v-if="item.status === 'pending'" class="pending-hint">审核中，请等待管理员处理...</span>
          </div>
        </div>
      </div>
    </template><!-- end mine tab -->

    <!-- 新建/编辑素材弹窗 -->
    <div v-if="showMineModal" class="modal-overlay" @click.self="showMineModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ editingMine ? '编辑素材' : '新建素材' }}</h2>
          <button class="close-btn" @click="showMineModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>分类 *</label>
            <select v-model="mineForm.category" class="form-select">
              <option value="">请选择</option>
              <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>标题 *</label>
            <input v-model="mineForm.title" type="text" placeholder="素材标题" class="form-input" />
          </div>
          <div class="form-group">
            <label>内容 *</label>
            <textarea v-model="mineForm.content" rows="8" placeholder="素材内容..." class="form-input" />
          </div>
          <div class="form-group">
            <label>标签（逗号分隔）</label>
            <input v-model="mineForm.tags" type="text" placeholder="如：战役,将领" class="form-input" />
          </div>
          <p v-if="mineError" class="error">{{ mineError }}</p>
        </div>
        <div class="modal-footer">
          <button @click="showMineModal = false" class="btn-secondary">取消</button>
          <button @click="saveMine('draft')" :disabled="mineSaving" class="btn-secondary">保存草稿</button>
          <button @click="saveMine('submit')" :disabled="mineSaving" class="btn-primary">{{ mineSaving ? '提交中...' : '保存并提交审核' }}</button>
        </div>
      </div>
    </div>

    <!-- 右侧详情抽屉，与素材检索保持一致 -->
    <div v-if="detailOpen && !detailCollapsed" class="detail-drawer" :style="{ width: drawerWidth + 'px' }">
      <div class="resize-handle" @mousedown="startResize"></div>
      <div class="drawer-tabs">
        <button
          v-for="tab in tabs.tabs"
          :key="tab.id"
          class="drawer-tab"
          :class="{ active: activeDetail && activeDetail.id === tab.id }"
          @click="switchTab(tab.id)"
        >
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
            <button
              @click="toggleFavorite"
              :disabled="favLoading"
              class="btn-fav"
              :class="{ favorited: activeDetail.isFavorite }"
            >
              {{ activeDetail.isFavorite ? '❤️ 取消收藏' : '🤍 收藏' }}
            </button>
            <button @click="useInWorkspace(activeDetail)" class="btn-use-ws">🖊 引用到工作台</button>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { materialApi } from '../api/material'
import { categoryApi } from '../api/category'
import { useWorkspaceStore } from '../stores/workspace'
import { useMaterialTabsStore } from '../stores/materialTabs'
import { userMaterialApi } from '../api/userMaterial'

const router = useRouter()
const workspaceStore = useWorkspaceStore()
const tabs = useMaterialTabsStore()
const favorites = ref([])
const loading = ref(false)
const copyMsg = ref('')
const chartEl = ref(null)
let chartInstance = null

// 主Tab
const mainTab = ref('favorites')

// 我的素材
const myMaterials = ref([])
const mineLoading = ref(false)
const showMineModal = ref(false)
const editingMine = ref(null)
const mineSaving = ref(false)
const mineError = ref('')
const mineForm = ref({ category: '', title: '', content: '', tags: '' })

const categories = ref([])

async function loadCategories() {
  try {
    const res = await categoryApi.list()
    if (res.data?.code === 200) categories.value = (res.data.data || []).map(c => c.name)
  } catch(e) { console.error(e) }
}

function statusLabel(s) {
  return { draft: '自用', pending: '待审核', approved: '已通过审核', rejected: '已拒绝' }[s] || s
}

async function loadMyMaterials() {
  mineLoading.value = true
  try {
    const res = await userMaterialApi.list()
    if (res.data?.code === 200) myMaterials.value = res.data.data || []
  } catch(e) { console.error(e) } finally { mineLoading.value = false }
}

function openMineModal(item) {
  editingMine.value = item
  mineError.value = ''
  if (item) {
    mineForm.value = { category: item.category, title: item.title, content: item.content, tags: item.tags || '' }
  } else {
    mineForm.value = { category: '', title: '', content: '', tags: '' }
  }
  showMineModal.value = true
}

async function saveMine(action) {
  if (!mineForm.value.category || !mineForm.value.title || !mineForm.value.content) {
    mineError.value = '请填写分类、标题和内容'
    return
  }
  mineSaving.value = true; mineError.value = ''
  try {
    let saved
    if (editingMine.value) {
      const res = await userMaterialApi.update(editingMine.value.id, mineForm.value)
      saved = res.data?.data
    } else {
      const res = await userMaterialApi.create(mineForm.value)
      saved = res.data?.data
    }
    if (action === 'submit' && saved?.id) {
      await userMaterialApi.submit(saved.id)
    }
    showMineModal.value = false
    await loadMyMaterials()
    // 同步刷新收藏列表（已通过的自建素材会出现在收藏中）
    await loadFavorites()
  } catch(e) { mineError.value = e.response?.data?.message || '保存失败' }
  finally { mineSaving.value = false }
}

async function submitMine(item) {
  try {
    await userMaterialApi.submit(item.id)
    await loadMyMaterials()
  } catch(e) { console.error(e) }
}

async function deleteMine(item) {
  const isApproved = item.status === 'approved'
  const confirmMsg = isApproved
    ? `确认删除素材「${item.title}」？\n\n⚠️ 该素材已通过审核并收录至公共素材库，删除后公共库中对应记录也将一并删除。`
    : `确认删除素材「${item.title}」？`
  if (!confirm(confirmMsg)) return
  try {
    await userMaterialApi.delete(item.id)
    myMaterials.value = myMaterials.value.filter(m => m.id !== item.id)
    copyMsg.value = `「${item.title}」已删除${isApproved ? '（公共库同步删除）' : ''}`
    setTimeout(() => { copyMsg.value = '' }, 2500)
  } catch(e) {
    copyMsg.value = '删除失败：' + (e.response?.data?.message || e.message)
    setTimeout(() => { copyMsg.value = '' }, 2500)
  }
}

const currentGroup = ref('全部')
const extraGroups = ref([])

const activeDetail = ref(null)
const detailOpen = ref(false)
const detailCollapsed = ref(false)
const detailLoading = ref(false)
const drawerWidth = ref(440)
const favLoading = ref(false)

const categoryMap = computed(() => {
  const map = {}
  favorites.value.forEach(f => { map[f.category] = (map[f.category] || 0) + 1 })
  return map
})

const groupList = computed(() => {
  const set = new Set()
  set.add('全部')
  set.add('我的灵感')
  set.add('未定义')
  extraGroups.value.forEach(g => set.add(g))
  favorites.value.forEach(f => {
    if (f.favoriteGroup) set.add(f.favoriteGroup)
  })
  return Array.from(set)
})

const filteredFavorites = computed(() => {
  if (currentGroup.value === '全部') return favorites.value
  return favorites.value.filter(f => (f.favoriteGroup || '未定义') === currentGroup.value)
})

async function loadFavorites() {
  loading.value = true
  try {
    const [favRes, mineRes] = await Promise.all([
      materialApi.getFavorites(),
      userMaterialApi.list(),
    ])
    const favList = (favRes.data?.code === 200 && favRes.data?.data)
      ? favRes.data.data.map(it => ({
          ...it,
          favoriteGroup: it.favoriteGroup || '未定义',
          _source: 'public',
        }))
      : []

    // 将用户所有自建素材（包括 draft/pending/approved/rejected）合并进收藏列表
    const allMine = (mineRes.data?.code === 200 && mineRes.data?.data)
      ? mineRes.data.data
          .map(m => ({
            id: `mine_${m.id}`,
            category: m.category,
            title: m.title,
            content: m.content,
            tags: m.tags ? m.tags.split(',').map(t => t.trim()).filter(Boolean) : [],
            favoriteGroup: m.favoriteGroup || '我的灵感',
            isFavorite: true,
            _source: 'mine',
            _mineId: m.id,
            _status: m.status,
          }))
      : []

    // 合并，公共收藏已有的同名自建素材不重复添加
    const existingTitles = new Set(favList.map(f => f.title))
    const uniqueMine = allMine.filter(m => !existingTitles.has(m.title))
    favorites.value = [...favList, ...uniqueMine]
    await nextTick()
    renderChart()
  } catch(e) { console.error(e) } finally { loading.value = false }
}

function renderChart() {
  if (!chartEl.value || !favorites.value.length) return
  import('echarts').then(({ init }) => {
    if (chartInstance) chartInstance.dispose()
    chartInstance = init(chartEl.value)
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark'
    const data = Object.entries(categoryMap.value).map(([name, value]) => ({ name, value }))
    chartInstance.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'item', formatter: '{b}: {c} 条 ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 5, borderWidth: 2, borderColor: isDark ? '#1a1a2e' : '#f0f4f4' },
        label: { show: true, formatter: '{b}\n{d}%', fontSize: 11, color: isDark ? '#9090b0' : '#4a6462' },
        labelLine: { length: 10, length2: 8 },
        data,
      }],
    })
    const onResize = () => chartInstance?.resize()
    window.removeEventListener('resize', onResize)
    window.addEventListener('resize', onResize)
  })
}

function useInWorkspace(item) {
  workspaceStore.insertMaterial(item)
  copyMsg.value = `「${item.title}」已引用，正在跳转工作台...`
  setTimeout(() => {
    copyMsg.value = ''
    router.push('/workspace')
  }, 1000)
}

function applyToAI(item) {
  workspaceStore.applyToAI(item)
  copyMsg.value = `已为 AI 绑定素材：「${item.title}」，正在跳转工作台...`
  setTimeout(() => {
    copyMsg.value = ''
    router.push('/workspace')
  }, 1200)
}

async function removeFavorite(item) {
  try {
    if (item._source === 'mine') {
      // 自建素材不能取消收藏，只能在「我的素材」Tab删除
      return
    }
    await materialApi.toggleFavorite(item.id)
    favorites.value = favorites.value.filter(f => f.id !== item.id)
    await nextTick()
    renderChart()
  } catch(e) { console.error(e) }
}

async function onGroupSelect(item, groupName) {
  const name = (groupName || '').trim()
  if (!name || name === item.favoriteGroup) return
  try {
    await materialApi.updateFavoriteGroup(item.id, name)
    item.favoriteGroup = name
  } catch (e) {
    console.error(e)
  }
}

function createGroup() {
  const name = window.prompt('新建分组名称，例如：我的灵感', '')
  if (!name) return
  const groupName = name.trim()
  if (!groupName) return
  if (!extraGroups.value.includes(groupName)) {
    extraGroups.value.push(groupName)
  }
  currentGroup.value = groupName
}

async function openDetail(id) {
  detailLoading.value = true
  detailOpen.value = true
  detailCollapsed.value = false
  try {
    const res = await materialApi.getById(id)
    if (res.data?.code === 200 && res.data?.data) {
      activeDetail.value = res.data.data
      tabs.openTab(res.data.data)
    }
  } catch (e) {
    console.error(e)
  } finally {
    detailLoading.value = false
  }
}

async function switchTab(id) {
  if (activeDetail.value?.id === id) return
  detailLoading.value = true
  detailCollapsed.value = false
  detailOpen.value = true
  try {
    const res = await materialApi.getById(id)
    if (res.data?.code === 200 && res.data?.data) {
      activeDetail.value = res.data.data
    }
  } catch (e) {
    console.error(e)
  } finally {
    detailLoading.value = false
  }
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
  try {
    await materialApi.toggleFavorite(activeDetail.value.id)
    activeDetail.value.isFavorite = !activeDetail.value.isFavorite
    if (!activeDetail.value.isFavorite) {
      favorites.value = favorites.value.filter(f => f.id !== activeDetail.value.id)
    }
  } catch (e) {
    console.error(e)
  } finally {
    favLoading.value = false
  }
}

let resizing = false; let startX = 0; let startW = 0
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

onMounted(() => { loadCategories(); loadFavorites() })
</script>

<style scoped>
.favorites-page { padding: 2rem; max-width: 1100px; margin: 0 auto; }
.page-header { margin-bottom: 1.5rem; }
.page-header h1 { font-size: 1.75rem; font-weight: 700; background: linear-gradient(90deg, var(--primary), var(--primary-light)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; margin: 0 0 0.25rem; }
.subtitle { color: var(--text-sub); font-size: 0.9rem; margin: 0; }
.group-bar { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-bottom: 1rem; }
.group-chip { border-radius: 999px; padding: 0.25rem 0.9rem; border: 1px solid var(--border); background: var(--bg-card); color: var(--text-sub); font-size: 0.8rem; cursor: pointer; transition: all 0.2s; }
.group-chip.active { border-color: var(--primary); background: var(--bg-hover); color: var(--primary); font-weight: 600; }
.group-chip.add { border-style: dashed; }
.stat-row { display: flex; align-items: center; gap: 1.5rem; background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 1rem 1.5rem; margin-bottom: 1.5rem; }
.pie-chart { width: 220px; height: 160px; flex-shrink: 0; }
.stat-info { display: flex; flex-direction: column; gap: 0.4rem; }
.stat-num { font-size: 2.5rem; font-weight: 800; background: linear-gradient(90deg, var(--primary), var(--primary-light)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; line-height: 1; }
.stat-label { font-size: 0.85rem; color: var(--text-sub); }
.stat-cats { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-top: 0.25rem; }
.stat-cat-item { font-size: 0.75rem; padding: 0.15rem 0.55rem; background: var(--tag-bg); color: var(--text-sub); border-radius: 20px; }
.table-wrap { background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; overflow: hidden; }
.material-table { width: 100%; border-collapse: collapse; }
.material-table th { padding: 0.9rem 1rem; text-align: left; font-size: 0.78rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-muted); background: var(--bg-input); border-bottom: 1px solid var(--border); }
.material-table td { padding: 0.85rem 1rem; border-bottom: 1px solid var(--border); color: var(--text-main); font-size: 0.9rem; }
.material-table tr:last-child td { border-bottom: none; }
.material-table tr:hover td { background: var(--bg-hover); }
.row-active td { background: var(--bg-hover) !important; }
.title-cell { max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 600; }
.tag { display: inline-block; padding: 0.15rem 0.5rem; background: var(--tag-bg); color: var(--text-sub); border-radius: 4px; font-size: 0.75rem; margin-right: 0.3rem; }
.group-select { padding: 0.25rem 0.6rem; border-radius: 999px; border: 1px solid var(--border); background: var(--bg-card); color: var(--text-sub); font-size: 0.78rem; cursor: pointer; }
.action-cell { white-space: nowrap; }
.btn-action { padding: 0.28rem 0.7rem; border-radius: 5px; font-size: 0.8rem; font-weight: 600; cursor: pointer; border: 1px solid var(--border); background: transparent; color: var(--text-sub); transition: all 0.2s; margin-right: 0.25rem; }
.btn-action:hover { border-color: var(--primary); color: var(--primary); background: var(--bg-hover); }
.btn-action.ai { border-color: var(--primary); color: var(--primary); font-weight: 700; }
.btn-action.ai:hover { background: var(--primary); color: #fff; }
.btn-action.use { border-color: var(--accent); color: var(--accent); }
.btn-action.use:hover { background: rgba(251,192,45,0.12); }
.btn-action.del { border-color: #e53935; color: #e53935; }
.btn-action.del:hover { background: rgba(229,57,53,0.1); }
.mine-badge { display: inline-block; padding: 0.15rem 0.5rem; background: linear-gradient(90deg, #43a047, #66bb6a); color: #fff; border-radius: 20px; font-size: 0.72rem; font-weight: 600; margin-right: 0.3rem; }
.loading, .empty { text-align: center; padding: 3rem; color: var(--text-muted); }
.toast { position: fixed; bottom: 2rem; left: 50%; transform: translateX(-50%); background: var(--primary); color: #fff; padding: 0.7rem 1.5rem; border-radius: 999px; font-size: 0.9rem; font-weight: 600; box-shadow: 0 4px 16px var(--shadow); z-index: 9999; pointer-events: none; animation: fadeup 0.3s ease; }
@keyframes fadeup { from { opacity: 0; transform: translateX(-50%) translateY(10px); } to { opacity: 1; transform: translateX(-50%) translateY(0); } }

/* 右侧详情抽屉样式（与素材检索一致） */
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
.drawer-actions { display: flex; gap: 0.75rem; }
.btn-fav { padding: 0.55rem 1.2rem; border-radius: 7px; border: 1px solid var(--border); background: transparent; color: var(--text-sub); cursor: pointer; font-size: 0.9rem; font-weight: 600; transition: all 0.2s; }
.btn-fav:hover { border-color: var(--primary); color: var(--primary); background: var(--bg-hover); }
.btn-fav.favorited { background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; border-color: transparent; }
.btn-fav:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-use-ws { padding: 0.55rem 1.2rem; border-radius: 7px; border: none; background: linear-gradient(90deg, var(--accent), #f9a825); color: #1a1a1a; font-weight: 700; cursor: pointer; font-size: 0.9rem; }
.btn-use-ws:hover { opacity: 0.9; }
.bookmark-bar { position: fixed; right: 0; bottom: 2rem; display: flex; flex-direction: column; gap: 0.5rem; z-index: 200; }
.bookmark { display: flex; align-items: center; gap: 0.5rem; background: var(--bg-card); border: 1px solid var(--border); border-right: none; border-radius: 8px 0 0 8px; padding: 0.45rem 0.9rem; cursor: pointer; box-shadow: -2px 2px 8px var(--shadow); max-width: 220px; transition: all 0.2s; }
.bookmark:hover { border-color: var(--primary); }
.bookmark-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 0.85rem; color: var(--text-main); }
.bookmark-close { color: var(--text-muted); font-size: 0.75rem; cursor: pointer; padding: 0 2px; flex-shrink: 0; }
.bookmark-close:hover { color: var(--primary); }

/* 主Tab */
.main-tabs { display: flex; gap: 0; margin-bottom: 1.5rem; border-bottom: 2px solid var(--border); }
.main-tab { padding: 0.6rem 1.5rem; border: none; background: transparent; color: var(--text-sub); font-size: 0.95rem; font-weight: 600; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s; }
.main-tab.active { color: var(--primary); border-bottom-color: var(--primary); }
.main-tab:hover { color: var(--primary); }

/* 我的素材 */
.mine-toolbar { display: flex; justify-content: flex-end; margin-bottom: 1rem; }
.btn-create-mine { padding: 0.55rem 1.25rem; border-radius: 7px; border: none; background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; font-weight: 700; font-size: 0.9rem; cursor: pointer; transition: opacity 0.2s; }
.btn-create-mine:hover { opacity: 0.85; }
.mine-list { display: flex; flex-direction: column; gap: 0.75rem; }
.mine-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; padding: 1rem 1.25rem; }
.mine-card-header { display: flex; align-items: center; gap: 0.6rem; margin-bottom: 0.5rem; flex-wrap: wrap; }
.cat-badge { display: inline-block; padding: 0.15rem 0.55rem; background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; border-radius: 20px; font-size: 0.72rem; font-weight: 600; white-space: nowrap; flex-shrink: 0; }
.mine-title { flex: 1; font-weight: 700; color: var(--text-main); font-size: 0.95rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.status-badge { font-size: 0.75rem; font-weight: 700; padding: 0.18rem 0.6rem; border-radius: 20px; flex-shrink: 0; }
.status-badge.draft { background: var(--tag-bg); color: var(--text-muted); }
.status-badge.pending { background: rgba(251,192,45,0.15); color: #f9a825; }
.status-badge.approved { background: rgba(67,160,71,0.15); color: #43a047; }
.status-badge.rejected { background: rgba(229,57,53,0.12); color: #e53935; }
.mine-preview { font-size: 0.85rem; color: var(--text-muted); line-height: 1.6; margin-bottom: 0.5rem; }
.admin-comment { font-size: 0.82rem; color: #e53935; background: rgba(229,57,53,0.08); border-left: 3px solid #e53935; padding: 0.4rem 0.75rem; border-radius: 4px; margin-bottom: 0.5rem; }
.mine-actions { display: flex; gap: 0.5rem; align-items: center; flex-wrap: wrap; }
.btn-action.edit { border-color: var(--primary); color: var(--primary); }
.btn-action.edit:hover { background: var(--bg-hover); }
.btn-action.submit { border-color: #43a047; color: #43a047; }
.btn-action.submit:hover { background: rgba(67,160,71,0.1); }
.pending-hint { font-size: 0.75rem; color: var(--text-muted); font-style: italic; }

/* 弹窗 */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: var(--bg-card); border: 1px solid var(--border); border-radius: 14px; width: 90%; max-width: 640px; box-shadow: 0 16px 48px var(--shadow); }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 1.25rem 1.5rem; border-bottom: 1px solid var(--border); }
.modal-header h2 { margin: 0; color: var(--text-main); font-size: 1.1rem; }
.close-btn { background: transparent; border: none; color: var(--text-muted); font-size: 1.4rem; cursor: pointer; }
.close-btn:hover { color: var(--primary); }
.modal-body { padding: 1.5rem; max-height: 60vh; overflow-y: auto; }
.modal-footer { display: flex; gap: 0.75rem; padding: 1.25rem 1.5rem; border-top: 1px solid var(--border); justify-content: flex-end; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; margin-bottom: 0.35rem; font-size: 0.85rem; font-weight: 600; color: var(--text-main); }
.form-select { width: 100%; padding: 0.55rem 0.9rem; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-input); color: var(--text-main); font-size: 0.9rem; }
.form-select:focus { outline: none; border-color: var(--primary); }
.form-input { width: 100%; padding: 0.55rem 0.9rem; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-input); color: var(--text-main); font-size: 0.9rem; font-family: inherit; box-sizing: border-box; }
.form-input:focus { outline: none; border-color: var(--primary); }
textarea.form-input { resize: vertical; }
.error { color: #e53935; font-size: 0.85rem; margin-top: 0.5rem; }
.btn-primary { padding: 0.55rem 1.2rem; border-radius: 7px; border: none; background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; font-weight: 700; font-size: 0.9rem; cursor: pointer; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { padding: 0.55rem 1.2rem; border-radius: 7px; border: 1px solid var(--border); background: transparent; color: var(--text-sub); font-size: 0.9rem; cursor: pointer; }
.btn-secondary:hover { border-color: var(--primary); color: var(--primary); }
</style>
