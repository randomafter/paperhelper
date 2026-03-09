<template>
  <div class="material-detail">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="material" class="detail-container">
      <div class="detail-header">
        <span class="dynasty-tag">{{ material.dynasty }}</span>
        <span class="category-tag">{{ material.category }}</span>
        <button class="favorite-btn" @click="toggleFavorite">
          {{ material.isFavorite ? '★ 已收藏' : '☆ 收藏' }}
        </button>
      </div>

      <h1 class="detail-title">{{ material.title }}</h1>

      <div class="detail-meta">
        <span class="date">创建时间：{{ formatDate(material.createdAt) }}</span>
        <span v-if="material.updatedAt !== material.createdAt" class="date">
          更新时间：{{ formatDate(material.updatedAt) }}
        </span>
      </div>

      <div class="detail-content">
        <h3>素材内容</h3>
        <div class="content-text">{{ material.content }}</div>
      </div>

      <div v-if="material.sourceUrl" class="detail-source">
        <h3>来源链接</h3>
        <a :href="material.sourceUrl" target="_blank" class="source-link">{{ material.sourceUrl }}</a>
      </div>

      <div v-if="material.tags && material.tags.length > 0" class="detail-tags">
        <h3>标签</h3>
        <div class="tags-list">
          <span v-for="tag in material.tags" :key="tag" class="tag">{{ tag }}</span>
        </div>
      </div>

      <div class="detail-actions">
        <button class="back-btn" @click="goBack">返回列表</button>
        <button v-if="isAdmin" class="edit-btn" @click="editMaterial">编辑素材</button>
      </div>
    </div>
    <div v-else class="empty">素材不存在</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { materialApi } from '../api/material'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const material = ref(null)
const isAdmin = ref(false)

async function loadMaterial() {
  loading.value = true
  try {
    const id = route.params.id
    const res = await materialApi.getMaterial(id)
    if (res.data?.code === 200 && res.data?.data) {
      material.value = res.data.data
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function toggleFavorite() {
  if (!material.value) return
  try {
    const res = await materialApi.toggleFavorite(material.value.id)
    if (res.data?.code === 200) {
      material.value.isFavorite = !material.value.isFavorite
    }
  } catch (e) {
    console.error(e)
  }
}

function goBack() {
  router.push('/materials')
}

function editMaterial() {
  if (!material.value) return
  router.push(`/admin/materials/${material.value.id}/edit`)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadMaterial()
  isAdmin.value = auth.user?.role === 'ADMIN'
})
</script>

<style scoped>
.material-detail {
  padding: 2rem;
  max-width: 1000px;
  margin: 0 auto;
}

.loading,
.empty {
  text-align: center;
  padding: 3rem;
  color: #aaa;
  font-size: 1.1rem;
}

.detail-container {
  background: rgba(255,255,255,0.06);
  border-radius: 12px;
  padding: 2rem;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.dynasty-tag,
.category-tag {
  padding: 0.5rem 1rem;
  border-radius: 6px;
  font-size: 0.9rem;
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
  padding: 0.5rem 1.5rem;
  border: 1px solid #e94560;
  border-radius: 6px;
  background: transparent;
  color: #e94560;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.favorite-btn:hover {
  background: rgba(233, 69, 96, 0.1);
}

.detail-title {
  margin: 0 0 1.5rem;
  font-size: 2rem;
  color: #e8e8e8;
  line-height: 1.4;
}

.detail-meta {
  display: flex;
  gap: 2rem;
  margin-bottom: 2rem;
  color: #888;
  font-size: 0.9rem;
}

.detail-content h3,
.detail-source h3,
.detail-tags h3 {
  margin: 0 0 1rem;
  font-size: 1.2rem;
  color: #e8e8e8;
}

.content-text {
  color: #ccc;
  line-height: 1.8;
  white-space: pre-wrap;
  margin-bottom: 2rem;
}

.source-link {
  color: #e94560;
  text-decoration: none;
  word-break: break-all;
}

.source-link:hover {
  text-decoration: underline;
}

.tags-list {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.tag {
  padding: 0.5rem 1rem;
  background: rgba(233, 69, 96, 0.1);
  color: #e94560;
  border-radius: 6px;
  font-size: 0.9rem;
}

.detail-actions {
  display: flex;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(255,255,255,0.1);
}

.back-btn,
.edit-btn {
  padding: 0.75rem 2rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn {
  background: rgba(255,255,255,0.1);
  color: #fff;
}

.back-btn:hover {
  background: rgba(255,255,255,0.2);
}

.edit-btn {
  background: #e94560;
  color: #fff;
}

.edit-btn:hover {
  background: #d6364e;
}
</style>
