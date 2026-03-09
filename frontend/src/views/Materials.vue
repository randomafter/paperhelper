<template>
  <div class="materials-page">
    <div class="search-bar">
      <select v-model="searchParams.dynasty" @change="searchMaterials">
        <option value="">全部朝代</option>
        <option value="汉">汉</option>
        <option value="唐">唐</option>
        <option value="宋">宋</option>
        <option value="明">明</option>
      </select>
      <select v-model="searchParams.category" @change="searchMaterials">
        <option value="">全部分类</option>
        <option value="制度">制度</option>
        <option value="文化">文化</option>
        <option value="服饰">服饰</option>
        <option value="语言">语言</option>
        <option value="战争">战争</option>
      </select>
      <select v-model="searchParams.tag" @change="searchMaterials">
        <option value="">全部标签</option>
        <option v-for="tag in tags" :key="tag" :value="tag">{{ tag }}</option>
      </select>
      <input v-model="searchParams.keyword" type="text" placeholder="搜索关键词" @keyup.enter="searchMaterials" />
      <button @click="searchMaterials">搜索</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="materials.length === 0" class="empty">暂无素材</div>
    <div v-else class="materials-list">
      <div v-for="material in materials" :key="material.id" class="material-card" @click="viewDetail(material.id)">
        <div class="material-header">
          <span class="dynasty-tag">{{ material.dynasty }}</span>
          <span class="category-tag">{{ material.category }}</span>
          <button class="favorite-btn" @click.stop="toggleFavorite(material)">
            {{ material.isFavorite ? '★' : '☆' }}
          </button>
        </div>
        <h3 class="material-title">{{ material.title }}</h3>
        <p class="material-content">{{ material.content.substring(0, 100) }}...</p>
        <div class="material-tags">
          <span v-for="tag in material.tags" :key="tag" class="tag">{{ tag }}</span>
        </div>
        <div class="material-footer">
          <span class="date">{{ formatDate(material.createdAt) }}</span>
        </div>
      </div>
    </div>

    <div v-if="totalPages > 1" class="pagination">
      <button @click="prevPage" :disabled="currentPage === 1">上一页</button>
      <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
      <button @click="nextPage" :disabled="currentPage === totalPages">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { materialApi } from '../api/material'

const router = useRouter()
const loading = ref(false)
const materials = ref([])
const tags = ref([])
const currentPage = ref(1)
const totalPages = ref(1)
const searchParams = reactive({
  dynasty: '',
  category: '',
  tag: '',
  keyword: '',
  page: 1,
  size: 20
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

async function loadTags() {
  try {
    const res = await materialApi.getAllTags()
    if (res.data?.code === 200 && res.data?.data) {
      tags.value = res.data.data || []
    }
  } catch (e) {
    console.error(e)
  }
}

function searchMaterials() {
  currentPage.value = 1
  searchParams.page = 1
  loadMaterials()
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

function viewDetail(id) {
  router.push(`/materials/${id}`)
}

async function toggleFavorite(material) {
  try {
    const res = await materialApi.toggleFavorite(material.id)
    if (res.data?.code === 200) {
      material.isFavorite = !material.isFavorite
    }
  } catch (e) {
    console.error(e)
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadMaterials()
  loadTags()
})
</script>

<style scoped>
.materials-page {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
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

.materials-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 1.5rem;
}

.material-card {
  background: rgba(255,255,255,0.06);
  border-radius: 12px;
  padding: 1.5rem;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.material-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.3);
}

.material-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
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

.favorite-btn {
  background: none;
  border: none;
  color: #e94560;
  font-size: 1.5rem;
  cursor: pointer;
  padding: 0;
  transition: transform 0.2s;
}

.favorite-btn:hover {
  transform: scale(1.2);
}

.material-title {
  margin: 0 0 0.75rem;
  font-size: 1.25rem;
  color: #e8e8e8;
}

.material-content {
  color: #aaa;
  margin-bottom: 1rem;
  line-height: 1.6;
}

.material-tags {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.tag {
  padding: 0.25rem 0.75rem;
  background: rgba(233, 69, 96, 0.1);
  color: #e94560;
  border-radius: 4px;
  font-size: 0.85rem;
}

.material-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #888;
  font-size: 0.9rem;
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

.loading,
.empty {
  text-align: center;
  padding: 3rem;
  color: #aaa;
  font-size: 1.1rem;
}
</style>
