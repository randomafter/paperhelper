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
    <div class="group-bar">
      <button
        v-for="g in groupList"
        :key="g"
        class="group-chip"
        :class="{ active: currentGroup === g, system: SYSTEM_GROUPS.includes(g) }"
        @click="currentGroup = g"
      >
        {{ g }}
        <span v-if="!SYSTEM_GROUPS.includes(g)" class="group-actions" @click.stop>
          <button class="group-btn edit" @click="editGroupName(g)" title="重命名">✏️</button>
          <button class="group-btn del" @click="deleteGroup(g)" title="删除">✕</button>
        </span>
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
                <option v-for="g in assignableGroups" :key="g" :value="g">{{ g }}</option>
              </select>
            </td>
            <td>{{ item.category }}</td>
            <td class="title-cell">{{ item.title }}</td>
            <td><span v-for="tag in (item.tags||[]).slice(0,3)" :key="tag" class="tag">{{ tag }}</span></td>
            <td class="action-cell">
              <span v-if="item._source === 'mine'" class="mine-badge">我创作</span>
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
            <button
              v-if="item.status === 'draft' || item.status === 'rejected'"
              @click="openMineModal(item)"
              class="btn-action edit">编辑</button>
            <button
              v-if="item.status === 'draft' || item.status === 'rejected'"
              @click="submitMine(item)"
              class="btn-action submit">提交审核</button>
            <button
              v-if="item.status !== 'approved'"
              @click="deleteMine(item)"
              class="btn-action del">删除</button>
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

    <!-- 新建分组模态框 -->
    <div v-if="showCreateGroup" class="modal-overlay" @click.self="showCreateGroup = false">
      <div class="confirm-modal">
        <h3>新建分组</h3>
        <input v-model="newGroupName" type="text" class="group-input" placeholder="分组名称" @keyup.enter="doCreateGroup" />
        <p v-if="createGroupError" class="error" style="margin-bottom:1rem;text-align:left">{{ createGroupError }}</p>
        <div class="confirm-actions">
          <button @click="showCreateGroup = false" class="btn-cancel">取消</button>
          <button @click="doCreateGroup" :disabled="creatingGroup" class="btn-confirm">{{ creatingGroup ? '创建中...' : '确认' }}</button>
        </div>
      </div>
    </div>

    <!-- 编辑分组名模态框 -->
    <div v-if="editingGroup" class="modal-overlay" @click.self="editingGroup = null">
      <div class="confirm-modal">
        <h3>重命名分组</h3>
        <input v-model="editingGroupNewName" type="text" class="group-input" placeholder="新分组名称" @keyup.enter="doEditGroup" />
        <div class="confirm-actions">
          <button @click="editingGroup = null" class="btn-cancel">取消</button>
          <button @click="doEditGroup" class="btn-confirm">确认</button>
        </div>
      </div>
    </div>

    <!-- 删除分组确认模态框 -->
    <div v-if="deletingGroup" class="modal-overlay" @click.self="deletingGroup = null">
      <div class="confirm-modal">
        <div class="confirm-icon">⚠️</div>
        <h3>确认删除分组？</h3>
        <p>分组「{{ deletingGroup }}」中的所有收藏将被移至「未定义」。此操作不可撤销。</p>
        <div class="confirm-actions">
          <button @click="deletingGroup = null" class="btn-cancel">取消</button>
          <button @click="doDeleteGroup" :disabled="deleting" class="btn-confirm-del">{{ deleting ? '删除中...' : '确认删除' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { materialApi } from '../api/material'
import { favoriteGroupsApi } from '../api/works'
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

const categories = [
  '历史沉淀', '传统民俗', '服饰装扮', '行业手艺', '宗教信仰',
  '兵器武林', '饮食文化', '玉石珍宝', '传说典故', '科技文明', '五行异象', '其他',
]

function statusLabel(s) {
  return { draft: '草稿', pending: '待审核', approved: '已通过', rejected: '已拒绝' }[s] || s
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
    // 同步刷新收藏列表（已通过的自建素材会出现在收藏中），独立 try 避免污染错误提示
    loadFavorites().catch(e => console.error('刷新收藏列表失败', e))
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
  if (!confirm(`确认删除素材「${item.title}」？`)) return
  try {
    await userMaterialApi.delete(item.id)
    myMaterials.value = myMaterials.value.filter(m => m.id !== item.id)
  } catch(e) { console.error(e) }
}

const currentGroup = ref('全部')
const extraGroups = ref([])
const editingGroup = ref(null)
const editingGroupNewName = ref('')
const deletingGroup = ref(null)
const deleting = ref(false)
const showCreateGroup = ref(false)
const newGroupName = ref('')
const createGroupError = ref('')
const creatingGroup = ref(false)

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

const SYSTEM_GROUPS = ['全部', '我的灵感', '未定义']

const groupList = computed(() => {
  // System groups always first in fixed order
  // Also include any group names that exist on favorites but aren't in extraGroups yet
  const fromFavorites = favorites.value
    .map(f => f.favoriteGroup)
    .filter(g => g && !SYSTEM_GROUPS.includes(g))
  const allUserGroups = [...new Set([...extraGroups.value, ...fromFavorites].filter(g => !SYSTEM_GROUPS.includes(g)))]
  return [...SYSTEM_GROUPS, ...allUserGroups]
})

// Groups available for assigning a material (excludes '全部')
const assignableGroups = computed(() => groupList.value.filter(g => g !== '全部'))

const filteredFavorites = computed(() => {
  if (currentGroup.value === '全部') return favorites.value
  return favorites.value.filter(f => (f.favoriteGroup || '未定义') === currentGroup.value)
})

async function loadFavorites() {
  loading.value = true
  try {
    const [favRes, mineRes, groupsRes] = await Promise.all([
      materialApi.getFavorites(),
      userMaterialApi.list(),
      favoriteGroupsApi.list().catch(e => { console.error('加载分组失败', e); return { data: { code: 200, data: [] } } }),
    ])
    const favList = (favRes.data?.code === 200 && favRes.data?.data)
      ? favRes.data.data.map(it => ({
          ...it,
          favoriteGroup: it.favoriteGroup || '未定义',
          _source: 'public',
        }))
      : []

    // 将用户自建（已通过）的素材合并进收藏列表，默认归入「我的灵感」分组
    const approvedMine = (mineRes.data?.code === 200 && mineRes.data?.data)
      ? mineRes.data.data
          .filter(m => m.status === 'approved')
          .map(m => ({
            id: `mine_${m.id}`,
            category: m.category,
            title: m.title,
            content: m.content,
            tags: m.tags ? m.tags.split(',').map(t => t.trim()).filter(Boolean) : [],
            favoriteGroup: '我的灵感',
            isFavorite: true,
            _source: 'mine',
            _mineId: m.id,
          }))
      : []

    // 合并，避免重复（公共收藏中已有的不重复添加）
    const existingTitles = new Set(favList.map(f => f.title))
    const uniqueMine = approvedMine.filter(m => !existingTitles.has(m.title))
    favorites.value = [...favList, ...uniqueMine]
    
    // 加载分组，过滤掉与系统分组同名的项
    if (groupsRes.data?.code === 200 && groupsRes.data?.data) {
      const SYSTEM = ['全部', '我的灵感', '未定义']
      extraGroups.value = (groupsRes.data.data.map(g => g.name) || []).filter(n => !SYSTEM.includes(n))
    }
    
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

async function createGroup() {
  newGroupName.value = ''
  createGroupError.value = ''
  showCreateGroup.value = true
}

async function doCreateGroup() {
  const trimmed = newGroupName.value.trim()
  if (!trimmed) { createGroupError.value = '分组名称不能为空'; return }
  if (SYSTEM_GROUPS.includes(trimmed)) { createGroupError.value = `「${trimmed}」是系统保留分组名，请使用其他名称`; return }
  creatingGroup.value = true; createGroupError.value = ''
  try {
    const res = await favoriteGroupsApi.create(trimmed)
    if (res.data?.code === 200 && res.data?.data) {
      if (!extraGroups.value.includes(res.data.data.name)) {
        extraGroups.value.push(res.data.data.name)
      }
      currentGroup.value = res.data.data.name
      showCreateGroup.value = false
    } else {
      createGroupError.value = res.data?.message || '创建失败'
    }
  } catch(e) {
    createGroupError.value = e.response?.data?.message || '创建失败，请检查是否已登录'
    console.error(e)
  } finally {
    creatingGroup.value = false
  }
}

function editGroupName(g) {
  editingGroup.value = g
  editingGroupNewName.value = g
}

async function doEditGroup() {
  if (!editingGroupNewName.value?.trim()) return
  const newName = editingGroupNewName.value.trim()
  if (newName === editingGroup.value) { editingGroup.value = null; return }
  if (SYSTEM_GROUPS.includes(newName)) {
    alert(`「${newName}」是系统保留分组名，请使用其他名称`)
    return
  }
  try {
    const groupsRes = await favoriteGroupsApi.list()
    let group = groupsRes.data?.data?.find(g => g.name === editingGroup.value)
    if (!group) {
      // 该分组名在 favorite_group 表中不存在（可能是从收藏记录里来的），先创建再重命名
      const createRes = await favoriteGroupsApi.create(editingGroup.value)
      group = createRes.data?.data
    }
    if (!group) { alert('操作失败：无法找到或创建分组'); editingGroup.value = null; return }
    await favoriteGroupsApi.rename(group.id, newName)
    // 同步更新 extraGroups
    const idx = extraGroups.value.indexOf(editingGroup.value)
    if (idx >= 0) extraGroups.value[idx] = newName
    else if (!extraGroups.value.includes(newName)) extraGroups.value.push(newName)
    // 同步更新收藏列表中的分组名
    favorites.value.forEach(f => { if (f.favoriteGroup === editingGroup.value) f.favoriteGroup = newName })
    if (currentGroup.value === editingGroup.value) currentGroup.value = newName
    editingGroup.value = null
  } catch(e) {
    alert(e.response?.data?.message || '重命名失败')
    console.error(e)
  }
}

function deleteGroup(g) { deletingGroup.value = g }

async function doDeleteGroup() {
  if (!deletingGroup.value) return
  deleting.value = true
  try {
    const groupsRes = await favoriteGroupsApi.list()
    let group = groupsRes.data?.data?.find(g => g.name === deletingGroup.value)
    if (!group) {
      // 分组记录不在 favorite_group 表中（如通过素材行下拉直接设置的分组名），
      // 只在前端和收藏记录中清除，把受影响的公共收藏的 group_name 改为「未定义」
      const affectedItems = favorites.value.filter(f => f.favoriteGroup === deletingGroup.value && f._source === 'public')
      await Promise.all(affectedItems.map(f => materialApi.updateFavoriteGroup(f.id, '未定义').catch(() => {})))
      favorites.value.forEach(f => { if (f.favoriteGroup === deletingGroup.value) f.favoriteGroup = '未定义' })
      const idx = extraGroups.value.indexOf(deletingGroup.value)
      if (idx >= 0) extraGroups.value.splice(idx, 1)
      if (currentGroup.value === deletingGroup.value) currentGroup.value = '全部'
      deletingGroup.value = null
      return
    }
    await favoriteGroupsApi.delete(group.id)
    const idx = extraGroups.value.indexOf(deletingGroup.value)
    if (idx >= 0) extraGroups.value.splice(idx, 1)
    favorites.value.forEach(f => { if (f.favoriteGroup === deletingGroup.value) f.favoriteGroup = '未定义' })
    if (currentGroup.value === deletingGroup.value) currentGroup.value = '全部'
    deletingGroup.value = null
  } catch(e) {
    alert(e.response?.data?.message || '删除失败')
    console.error(e)
  } finally { deleting.value = false }
}

onMounted(loadFavorites)
</script>

<style scoped>
.favorites-page { padding: 2rem; max-width: 1100px; margin: 0 auto; }
.page-header { margin-bottom: 1.5rem; }
.page-header h1 { font-size: 1.75rem; font-weight: 700; background: linear-gradient(90deg, var(--primary), var(--primary-light)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; margin: 0 0 0.25rem; }
.subtitle { color: var(--text-sub); font-size: 0.9rem; margin: 0; }
.group-bar { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-bottom: 1rem; }
.group-chip { border-radius: 999px; padding: 0.25rem 0.9rem; border: 1px solid var(--border); background: var(--bg-card); color: var(--text-sub); font-size: 0.8rem; cursor: pointer; transition: all 0.2s; position: relative; }
.group-chip.active { border-color: var(--primary); background: var(--bg-hover); color: var(--primary); font-weight: 600; }
.group-chip.add { border-style: dashed; }
.group-chip.add:hover { border-color: var(--primary); color: var(--primary); }
.group-chip.system { opacity: 0.85; }
.group-chip.system.active { opacity: 1; }
.group-actions { display: none; position: absolute; right: 0.3rem; top: 50%; transform: translateY(-50%); gap: 0.2rem; }
.group-chip:hover .group-actions { display: flex; }
.group-btn { padding: 0.2rem 0.3rem; border: none; background: transparent; color: var(--text-muted); font-size: 0.7rem; cursor: pointer; border-radius: 3px; transition: all 0.2s; }
.group-btn:hover { color: var(--primary); }
.group-btn.del:hover { color: #e53935; }
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
.btn-action.use { border-color: var(--accent); color: var(--accent); }
.btn-action.use:hover { background: rgba(251,192,45,0.12); }
.btn-action.del { border-color: #e53935; color: #e53935; }
.btn-action.del:hover { background: rgba(229,57,53,0.1); }
.mine-badge { display: inline-block; padding: 0.15rem 0.5rem; background: linear-gradient(90deg, #43a047, #66bb6a); color: #fff; border-radius: 20px; font-size: 0.72rem; font-weight: 600; margin-right: 0.3rem; }
.loading, .empty { text-align: center; padding: 3rem; color: var(--text-muted); }
.toast { position: fixed; bottom: 2rem; left: 50%; transform: translateX(-50%); background: var(--primary); color: #fff; padding: 0.7rem 1.5rem; border-radius: 999px; font-size: 0.9rem; font-weight: 600; box-shadow: 0 4px 16px var(--shadow); z-index: 9999; pointer-events: none; animation: fadeup 0.3s ease; }
@keyframes fadeup { from { opacity: 0; transform: translateX(-50%) translateY(10px); } to { opacity: 1; transform: translateX(-50%) translateY(0); } }

/* 模态框样式 */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.55); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.confirm-modal { background: var(--bg-card); border: 1px solid var(--border); border-radius: 14px; padding: 2.5rem 2rem; width: 90%; max-width: 380px; text-align: center; box-shadow: 0 16px 48px var(--shadow); }
.confirm-icon { font-size: 2.5rem; margin-bottom: 0.75rem; }
.confirm-modal h3 { margin: 0 0 0.75rem; font-size: 1.15rem; color: var(--text-main); }
.confirm-modal p { color: var(--text-sub); font-size: 0.9rem; margin: 0 0 1.5rem; line-height: 1.6; }
.group-input { width: 100%; padding: 0.6rem 0.9rem; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-input); color: var(--text-main); font-size: 0.9rem; margin-bottom: 1.5rem; box-sizing: border-box; }
.group-input:focus { outline: none; border-color: var(--primary); }
.confirm-actions { display: flex; gap: 0.75rem; }
.btn-cancel { flex: 1; padding: 0.6rem; border-radius: 7px; border: 1px solid var(--border); background: transparent; color: var(--text-sub); font-size: 0.9rem; cursor: pointer; transition: all 0.2s; }
.btn-cancel:hover { border-color: var(--primary); color: var(--primary); }
.btn-confirm { flex: 1; padding: 0.6rem; border-radius: 7px; border: none; background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; font-weight: 700; font-size: 0.9rem; cursor: pointer; transition: all 0.2s; }
.btn-confirm:hover { opacity: 0.88; }
.btn-confirm-del { flex: 1; padding: 0.6rem; border-radius: 7px; border: none; background: linear-gradient(90deg, #e53935, #ef5350); color: #fff; font-weight: 700; font-size: 0.9rem; cursor: pointer; }
.btn-confirm-del:disabled { opacity: 0.5; cursor: not-allowed; }

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
.mine-actions { display: flex; gap: 0.5rem; }
.btn-action.edit { border-color: var(--primary); color: var(--primary); }
.btn-action.edit:hover { background: var(--bg-hover); }
.btn-action.submit { border-color: #43a047; color: #43a047; }
.btn-action.submit:hover { background: rgba(67,160,71,0.1); }

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
