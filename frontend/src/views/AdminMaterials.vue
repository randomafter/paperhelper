<template>
  <div class="admin-materials">
    <div class="page-header">
      <h1>素材管理</h1>
      <div class="header-actions">
        <button class="create-btn" @click="showCreateModal = true">新增素材</button>
        <button class="import-btn" @click="showImportModal = true">批量导入</button>
      </div>
    </div>

    <div class="search-bar">
      <select v-model="searchParams.dynasty" @change="loadMaterials">
        <option value="">全部朝代</option>
        <option value="汉">汉</option>
        <option value="唐">唐</option>
        <option value="宋">宋</option>
        <option value="明">明</option>
      </select>
      <select v-model="searchParams.category" @change="loadMaterials">
        <option value="">全部分类</option>
        <option value="制度">制度</option>
        <option value="文化">文化</option>
        <option value="服饰">服饰</option>
        <option value="语言">语言</option>
        <option value="战争">战争</option>
      </select>
      <input v-model="searchParams.keyword" type="text" placeholder="搜索关键词" @keyup.enter="loadMaterials" />
      <button @click="loadMaterials">搜索</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="materials.length === 0" class="empty">暂无素材</div>
    <div v-else class="materials-table">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>朝代</th>
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
            <td><span class="dynasty-tag">{{ material.dynasty }}</span></td>
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

    <div v-if="totalPages > 1" class="pagination">
      <button @click="prevPage" :disabled="currentPage === 1">上一页</button>
      <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
      <button @click="nextPage" :disabled="currentPage === totalPages">下一页</button>
    </div>

    <!-- 新增/编辑素材弹窗 -->
    <div v-if="showCreateModal || showEditModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <h2>{{ showEditModal ? '编辑素材' : '新增素材' }}</h2>
        <form @submit.prevent="saveMaterial">
          <div class="form-group">
            <label>朝代 *</label>
            <select v-model="form.dynasty" required>
              <option value="">请选择朝代</option>
              <option value="汉">汉</option>
              <option value="唐">唐</option>
              <option value="宋">宋</option>
              <option value="明">明</option>
            </select>
          </div>
          <div class="form-group">
            <label>分类 *</label>
            <select v-model="form.category" required>
              <option value="">请选择分类</option>
              <option value="制度">制度</option>
              <option value="文化">文化</option>
              <option value="服饰">服饰</option>
              <option value="语言">语言</option>
              <option value="战争">战争</option>
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
            <label>来源链接</label>
            <input v-model="form.sourceUrl" type="url" placeholder="请输入来源链接" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { materialApi } from '../api/material'

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

const searchParams = reactive({
  dynasty: '',
  category: '',
  keyword: '',
  page: 1,
  size: 20
})

const form = reactive({
  dynasty: '',
  category: '',
  title: '',
  content: '',
  sourceUrl: ''
})

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
  form.dynasty = material.dynasty
  form.category = material.category
  form.title = material.title
  form.content = material.content
  form.sourceUrl = material.sourceUrl || ''
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
  form.dynasty = ''
  form.category = ''
  form.title = ''
  form.content = ''
  form.sourceUrl = ''
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
  border: 1px solid #444;
  border-radius: 8px;
  background: rgba(255,255,255,0.08);
  color: #fff;
  font-size: 1rem;
}

.search-bar select {
  min-width: 120px;
}

.search-bar input {
  flex: 1;
  min-width: 200px;
}

.search-bar button {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  background: #e94560;
  color: #fff;
  font-size: 1rem;
  cursor: pointer;
}

.materials-table {
  background: rgba(255,255,255,0.06);
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
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.materials-table th {
  background: rgba(255,255,255,0.1);
  color: #e8e8e8;
  font-weight: 600;
}

.materials-table td {
  color: #ccc;
}

.title-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dynasty-tag,
.category-tag {
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: 500;
}

.dynasty-tag {
  background: #e94560;
  color: #fff;
}

.category-tag {
  background: #0f3460;
  color: #fff;
}

.tag {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background: rgba(233, 69, 96, 0.1);
  color: #e94560;
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
