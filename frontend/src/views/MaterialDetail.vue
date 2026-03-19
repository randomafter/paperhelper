<template>
  <div class="detail-page">
    <div class="back-bar">
      <button @click="$router.back()" class="btn-back">← 返回</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!material" class="empty">素材不存在</div>
    <div v-else class="detail-card">
      <div class="detail-header">
        <div class="meta">
          <span class="category-tag">{{ material.category }}</span>
        </div>
        <h1 class="detail-title">{{ material.title }}</h1>
        <div v-if="material.tags && material.tags.length" class="tags">
          <span v-for="tag in material.tags" :key="tag" class="tag">{{ tag }}</span>
        </div>
      </div>

      <div class="detail-body">
        <div class="content-section">
          <h3>素材内容</h3>
          <textarea class="content-view" readonly :value="material.content" rows="16" />
        </div>
      </div>

      <div class="action-bar">
        <button @click="toggleFavorite" :disabled="favLoading" class="btn-fav" :class="{ favorited: material.isFavorite }">
          {{ material.isFavorite ? '❤️ 取消收藏' : '🤍 收藏' }}
        </button>
        <template v-if="isAdmin">
          <button @click="openEdit" class="btn-edit">✏️ 编辑</button>
          <button @click="showDeleteConfirm = true" class="btn-delete">🗑️ 删除</button>
        </template>
      </div>

      <p v-if="actionMsg" class="action-msg" :class="actionMsgType">{{ actionMsg }}</p>
    </div>

    <!-- 编辑模态框 -->
    <div v-if="showEdit" class="modal-overlay" @click.self="showEdit = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2>编辑素材</h2><button class="close-btn" @click="showEdit = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>分类 *</label>
            <select v-model="form.category" class="field full">
              <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>
          <div class="form-group"><label>标题 *</label><input v-model="form.title" class="field full" /></div>
          <div class="form-group"><label>内容 *</label><textarea v-model="form.content" rows="6" class="field full" /></div>
          <div class="form-group"><label>标签（逗号分隔）</label><input v-model="form.tagsStr" class="field full" /></div>
          <p v-if="editError" class="error">{{ editError }}</p>
        </div>
        <div class="modal-footer">
          <button @click="showEdit = false" class="btn-sec">取消</button>
          <button @click="doEdit" :disabled="saving" class="btn-pri">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- 删除确认 -->
    <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="showDeleteConfirm = false">
      <div class="modal-content small">
        <div class="modal-header"><h2>确认删除</h2><button class="close-btn" @click="showDeleteConfirm = false">✕</button></div>
        <div class="modal-body"><p style="color:var(--text-main)">确认删除此素材？此操作不可撤销。</p></div>
        <div class="modal-footer">
          <button @click="showDeleteConfirm = false" class="btn-sec">取消</button>
          <button @click="doDelete" :disabled="deleting" class="btn-danger">{{ deleting ? '删除中...' : '确认删除' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { materialApi } from '../api/material'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
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

const material = ref(null)
const loading = ref(false)
const favLoading = ref(false)
const actionMsg = ref('')
const actionMsgType = ref('success')

const showEdit = ref(false)
const saving = ref(false)
const editError = ref('')
const form = reactive({ category: '', title: '', content: '', tagsStr: '' })

const showDeleteConfirm = ref(false)
const deleting = ref(false)

async function loadMaterial() {
  loading.value = true
  try {
    const res = await materialApi.getById(route.params.id)
    if (res.data?.code === 200) material.value = res.data.data
  } catch (e) { console.error(e) } finally { loading.value = false }
}

async function toggleFavorite() {
  favLoading.value = true
  try {
    await materialApi.toggleFavorite(material.value.id)
    material.value.isFavorite = !material.value.isFavorite
    actionMsg.value = material.value.isFavorite ? '收藏成功' : '已取消收藏'
    actionMsgType.value = 'success'
    setTimeout(() => { actionMsg.value = '' }, 2000)
  } catch (e) {
    actionMsg.value = '操作失败'
    actionMsgType.value = 'error'
  } finally { favLoading.value = false }
}

function openEdit() {
  Object.assign(form, { category: material.value.category, title: material.value.title, content: material.value.content, tagsStr: (material.value.tags || []).join(',') })
  editError.value = ''; showEdit.value = true
}

async function doEdit() {
  if (!form.category || !form.title || !form.content) { editError.value = '请填写必填字段'; return }
  saving.value = true; editError.value = ''
  const tags = form.tagsStr ? form.tagsStr.split(',').map(t => t.trim()).filter(Boolean) : []
  try {
    const res = await materialApi.update({ id: material.value.id, category: form.category, title: form.title, content: form.content, tags })
    if (res.data?.code === 200) { material.value = res.data.data; showEdit.value = false }
    else editError.value = res.data?.message || '保存失败'
  } catch (e) { editError.value = e.response?.data?.message || '保存失败' }
  finally { saving.value = false }
}

async function doDelete() {
  deleting.value = true
  try { await materialApi.remove(material.value.id); router.push('/materials') }
  catch (e) { console.error(e) } finally { deleting.value = false }
}

onMounted(loadMaterial)
</script>
