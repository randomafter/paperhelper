<template>
  <div class="admin-materials">
    <div class="page-header">
      <h1>素材管理</h1>
      <div class="header-actions">
        <button class="tab-btn" :class="{ active: adminTab === 'materials' }" @click="adminTab = 'materials'">素材列表</button>
        <button class="tab-btn" :class="{ active: adminTab === 'categories' }" @click="adminTab = 'categories'">分类管理</button>
        <template v-if="adminTab === 'materials'">
          <button class="create-btn" @click="showCreateModal = true">新增素材</button>
          <button class="import-btn" @click="showImportModal = true">批量导入</button>
        </template>
        <template v-else>
          <button class="create-btn" @click="openNewCatModal">新增分类</button>
        </template>
      </div>
    </div>

    <div class="search-bar" v-if="adminTab === 'materials'">
      <select v-model="searchParams.category" @change="loadMaterials" class="filter-select">
        <option value="">全部类型</option>
        <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
      </select>
      <input v-model="searchParams.keyword" type="text" placeholder="搜索关键词" @keyup.enter="loadMaterials" />
      <button @click="loadMaterials">搜索</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="adminTab === 'materials' && materials.length === 0" class="empty">暂无素材</div>
    <div v-else-if="adminTab === 'materials'" class="materials-table">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>分类</th>
            <th>标题</th>
            <th>标签</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="material in materials" :key="material.id">
            <td>{{ material.id }}</td>
            <td><span class="category-tag">{{ material.category }}</span></td>
            <td class="title-cell">{{ material.title }}</td>
            <td>
              <span v-for="tag in material.tags" :key="tag" class="tag">{{ tag }}</span>
            </td>
            <td>{{ formatDate(material.createdAt) }}</td>
            <td>
              <button class="edit-btn" @click="editMaterial(material)">编辑</button>
              <button class="delete-btn" @click="deleteMaterial(material.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="adminTab === 'materials' && totalPages > 1" class="pagination">
      <button @click="prevPage" :disabled="currentPage === 1">上一页</button>
      <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
      <button @click="nextPage" :disabled="currentPage === totalPages">下一页</button>
    </div>

    <!-- 分类管理面板 -->
    <div v-if="adminTab === 'categories'" class="categories-panel">
      <div v-if="catLoading" class="loading">加载中...</div>
      <div v-else class="materials-table">
        <table>
          <thead><tr><th>ID</th><th>分类名称</th><th>排序权重</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="cat in categoryList" :key="cat.id">
              <td>{{ cat.id }}</td>
              <td><span class="category-tag">{{ cat.name }}</span></td>
              <td>{{ cat.sortOrder }}</td>
              <td>
                <button class="edit-btn" @click="editCategory(cat)">编辑</button>
                <button class="delete-btn" @click="deleteCategory(cat.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 新增/编辑素材弹窗 -->
    <div v-if="showCreateModal || showEditModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <h2>{{ showEditModal ? '编辑素材' : '新增素材' }}</h2>
        <form @submit.prevent="saveMaterial">
          <div class="form-group">
            <label>分类 *</label>
            <select v-model="form.category" required>
              <option value="">请选择分类</option>
              <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>标题 *</label>
            <input v-model="form.title" type="text" placeholder="请输入标题" required />
          </div>
          <div class="form-group">
            <label>内容 *</label>
            <textarea v-model="form.content" rows="10" placeholder="请输入素材内容" required></textarea>
          </div>
          <div class="form-group">
            <label>标签（多个标签用逗号分隔）</label>
            <input v-model="tagsInput" type="text" placeholder="例如：宫廷,礼仪,服饰" />
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="closeModal">取消</button>
            <button type="submit" class="submit-btn" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 批量导入弹窗 -->
    <div v-if="showImportModal" class="modal-overlay" @click.self="showImportModal = false">
      <div class="modal">
        <h2>批量导入素材</h2>
        <div class="import-info">
          <p>请上传Excel文件，格式如下：</p>
          <div class="import-format">
            <p>朝代 | 分类 | 标题 | 内容 | 来源链接 | 标签</p>
            <p>汉 | 制度 | 汉代官制 | 汉代的官制体系... | http://... | 官制,政治</p>
          </div>
        </div>
        <div class="form-group">
          <label>选择Excel文件</label>
          <input type="file" accept=".xlsx,.xls" @change="handleFileChange" />
        </div>
        <div class="form-actions">
          <button type="button" class="cancel-btn" @click="showImportModal = false">取消</button>
          <button type="button" class="submit-btn" @click="importMaterials" :disabled="importing">
            {{ importing ? '导入中...' : '导入' }}
          </button>
        </div>
      </div>
    </div>
    <!-- 新增/编辑分类弹窗 -->
    <div v-if="showCatModal" class="modal-overlay" @click.self="showCatModal = false">
      <div class="modal">
        <h2>{{ editCatId ? '编辑分类' : '新增分类' }}</h2>
        <form @submit.prevent="saveCategory">
          <div class="form-group">
            <label>分类名称 *</label>
            <input v-model="catForm.name" type="text" placeholder="请输入分类名称" required />
          </div>
          <div class="form-group">
            <label>排序权重（越小越靠前）</label>
            <input v-model.number="catForm.sortOrder" type="number" min="0" />
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="showCatModal = false">取消</button>
            <button type="submit" class="submit-btn" :disabled="catSaving">{{ catSaving ? '保存中...' : '保存' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { materialApi } from '../api/material'
import { categoryApi } from '../api/category'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const importing = ref(false)
const materials = ref([])
const currentPage = ref(1)
const totalPages = ref(1)
const showCreateModal = ref(false)
const showEditModal = ref(false)
const showImportModal = ref(false)
const editId = ref(null)
const tagsInput = ref('')
const importFile = ref(null)
const adminTab = ref('materials')

// 分类管理
const categoryList = ref([])
const catLoading = ref(false)
const showCatModal = ref(false)
const editCatId = ref(null)
const catSaving = ref(false)
const catForm = reactive({ name: '', sortOrder: 100 })

// 动态分类列表（用于素材新增/编辑下拉）
const categories = ref([])

const searchParams = reactive({
  category: '',
  keyword: '',
  page: 1,
  size: 20,
  showAll: true  // 管理端展示所有状态素材
})

const form = reactive({
  category: '',
  title: '',
  content: '',
})

async function loadCategories() {
  catLoading.value = true
  try {
    const res = await categoryApi.list()
    if (res.data?.code === 200) {
      categoryList.value = res.data.data || []
      categories.value = categoryList.value.map(c => c.name)
    }
  } catch(e) { console.error(e) } finally { catLoading.value = false }
}

function openNewCatModal() {
  editCatId.value = null
  catForm.name = ''
  catForm.sortOrder = 100
  showCatModal.value = true
}

function editCategory(cat) {
  editCatId.value = cat.id
  catForm.name = cat.name
  catForm.sortOrder = cat.sortOrder
  showCatModal.value = true
}

async function saveCategory() {
  catSaving.value = true
  try {
    if (editCatId.value) {
      await categoryApi.update(editCatId.value, { name: catForm.name, sortOrder: catForm.sortOrder })
    } else {
      await categoryApi.create({ name: catForm.name, sortOrder: catForm.sortOrder })
    }
    showCatModal.value = false
    editCatId.value = null
    catForm.name = ''
    catForm.sortOrder = 100
    await loadCategories()
  } catch(e) {
    alert('保存失败：' + (e.response?.data?.message || e.message))
  } finally { catSaving.value = false }
}

async function deleteCategory(id) {
  if (!confirm('确定要删除该分类吗？删除后不影响已有素材的分类字段。')) return
  try {
    await categoryApi.remove(id)
    await loadCategories()
  } catch(e) {
    alert('删除失败：' + (e.response?.data?.message || e.message))
  }
}

async function loadMaterials() {
  loading.value = true
  try {
    const res = await materialApi.searchMaterials(searchParams)
    if (res.data?.code === 200 && res.data?.data) {
      materials.value = res.data.data.records || []
      totalPages.value = Math.ceil((res.data.data.total || 0) / searchParams.size)
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
    searchParams.page = currentPage.value
    loadMaterials()
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    searchParams.page = currentPage.value
    loadMaterials()
  }
}

function editMaterial(material) {
  editId.value = material.id
  form.category = material.category
  form.title = material.title
  form.content = material.content
  tagsInput.value = material.tags ? material.tags.join(',') : ''
  showEditModal.value = true
}

async function saveMaterial() {
  saving.value = true
  try {
    const tags = tagsInput.value.split(',').map(t => t.trim()).filter(t => t)
    const data = { ...form, tags }
    
    if (showEditModal.value) {
      data.id = editId.value
      await materialApi.updateMaterial(data)
    } else {
      await materialApi.createMaterial(data)
    }
    
    closeModal()
    loadMaterials()
  } catch (e) {
    console.error(e)
    alert('保存失败：' + (e.response?.data?.message || e.message))
  } finally {
    saving.value = false
  }
}

async function deleteMaterial(id) {
  if (!confirm('确定要删除这个素材吗？')) return
  
  try {
    await materialApi.deleteMaterial(id)
    loadMaterials()
  } catch (e) {
    console.error(e)
    alert('删除失败：' + (e.response?.data?.message || e.message))
  }
}

function closeModal() {
  showCreateModal.value = false
  showEditModal.value = false
  editId.value = null
  form.category = ''
  form.title = ''
  form.content = ''
  tagsInput.value = ''
}

function handleFileChange(e) {
  importFile.value = e.target.files[0]
}

async function importMaterials() {
  if (!importFile.value) {
    alert('请选择文件')
    return
  }
  
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    
    const res = await materialApi.importMaterials(formData)
    if (res.data?.code === 200 && res.data?.data) {
      const result = res.data.data
      let message = `导入完成！\n成功：${result.successCount}条\n失败：${result.failCount}条`
      if (result.errors && result.errors.length > 0) {
        message += '\n\n错误信息：\n' + result.errors.join('\n')
      }
      alert(message)
      showImportModal.value = false
      loadMaterials()
    }
  } catch (e) {
    console.error(e)
    alert('导入失败：' + (e.response?.data?.message || e.message))
  } finally {
    importing.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadCategories()
  loadMaterials()
})
</script>

<style scoped>
.admin-materials {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.page-header h1 {
  margin: 0;
  font-size: 2rem;
  color: #e8e8e8;
}

.header-actions {
  display: flex;
  gap: 1rem;
}

.create-btn,
.import-btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn {
  padding: 0.6rem 1.2rem;
  border: 1px solid #444;
  border-radius: 8px;
  font-size: 0.95rem;
  cursor: pointer;
  background: transparent;
  color: #aaa;
  transition: all 0.2s;
}
.tab-btn.active {
  background: #e94560;
  color: #fff;
  border-color: #e94560;
}
.tab-btn:hover:not(.active) {
  border-color: #e94560;
  color: #e94560;
}

.create-btn {
  background: #e94560;
  color: #fff;
}

.create-btn:hover {
  background: #d6364e;
}

.import-btn {
  background: #0f3460;
  color: #fff;
}

.import-btn:hover {
  background: #16213e;
}

.search-bar {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  flex-wrap: wrap;
}

.search-bar select,
.search-bar input {
  padding: 0.75rem 1rem;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-input);
  color: var(--text-main);
  font-size: 1rem;
}

.search-bar select {
  min-width: 160px;
}

.search-bar input {
  flex: 1;
  min-width: 200px;
}

.search-bar button {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  background: linear-gradient(90deg, var(--primary), var(--primary-light));
  color: #fff;
  font-size: 1rem;
  cursor: pointer;
}

.materials-table {
  background: var(--bg-card);
  border-radius: 12px;
  overflow: hidden;
}

.materials-table table {
  width: 100%;
  border-collapse: collapse;
}

.materials-table th,
.materials-table td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid var(--border);
}

.materials-table th {
  background: var(--bg-input);
  color: var(--text-muted);
  font-weight: 600;
}

.materials-table td {
  color: var(--text-main);
}

.title-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-tag {
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: 500;
}

.category-tag {
  background: linear-gradient(90deg, var(--primary), var(--primary-light));
  color: #fff;
}

.tag {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background: var(--tag-bg);
  color: var(--text-sub);
  border-radius: 4px;
  font-size: 0.85rem;
  margin-right: 0.5rem;
}

.edit-btn,
.delete-btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
  margin-right: 0.5rem;
}

.edit-btn {
  background: #0f3460;
  color: #fff;
}

.edit-btn:hover {
  background: #16213e;
}

.delete-btn {
  background: #e94560;
  color: #fff;
}

.delete-btn:hover {
  background: #d6364e;
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-top: 2rem;
  align-items: center;
}

.pagination button {
  padding: 0.5rem 1rem;
  border: 1px solid #444;
  border-radius: 6px;
  background: rgba(255,255,255,0.08);
  color: #fff;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: #1a1a2e;
  border-radius: 12px;
  padding: 2rem;
  width: 90%;
  max-width: 700px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal h2 {
  margin: 0 0 1.5rem;
  font-size: 1.5rem;
  color: #e8e8e8;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #e8e8e8;
  font-size: 0.95rem;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #444;
  border-radius: 8px;
  background: rgba(255,255,255,0.08);
  color: #fff;
  font-size: 1rem;
  font-family: inherit;
}

.form-group textarea {
  resize: vertical;
  min-height: 150px;
}

.import-info {
  background: rgba(233, 69, 96, 0.1);
  border: 1px solid #e94560;
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 1.5rem;
}

.import-info p {
  margin: 0.5rem 0;
  color: #e8e8e8;
}

.import-format {
  background: rgba(255,255,255,0.05);
  padding: 1rem;
  border-radius: 6px;
  margin-top: 1rem;
}

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2rem;
}

.cancel-btn,
.submit-btn {
  padding: 0.75rem 2rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn {
  background: rgba(255,255,255,0.1);
  color: #fff;
}

.cancel-btn:hover {
  background: rgba(255,255,255,0.2);
}

.submit-btn {
  background: #e94560;
  color: #fff;
}

.submit-btn:hover {
  background: #d6364e;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading,
.empty {
  text-align: center;
  padding: 3rem;
  color: #aaa;
  font-size: 1.1rem;
}
</style>
