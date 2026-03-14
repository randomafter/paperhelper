<template>
  <div class="review-page">
    <div class="page-header">
      <h1>素材审核</h1>
      <p class="subtitle">审核用户提交的自建素材，通过后自动收录至素材检索库</p>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="items.length === 0" class="empty">暂无待审核素材 ✅</div>
    <div v-else class="review-list">
      <div v-for="item in items" :key="item.id" class="review-card">
        <div class="review-header">
          <span class="cat-badge">{{ item.category }}</span>
          <h3 class="review-title">{{ item.title }}</h3>
          <span class="review-meta">用户ID {{ item.userId }} · {{ formatDate(item.createdAt) }}</span>
        </div>
        <div class="review-tags" v-if="item.tags">
          <span v-for="tag in item.tags.split(',').filter(Boolean)" :key="tag" class="tag">{{ tag.trim() }}</span>
        </div>
        <pre class="review-content">{{ item.content }}</pre>
        <div class="review-actions">
          <input
            v-model="commentMap[item.id]"
            type="text"
            placeholder="审核意见（可选）"
            class="comment-input"
          />
          <button @click="approve(item)" :disabled="processingId === item.id" class="btn-approve">
            {{ processingId === item.id ? '处理中...' : '✅ 通过' }}
          </button>
          <button @click="reject(item)" :disabled="processingId === item.id" class="btn-reject">
            {{ processingId === item.id ? '处理中...' : '❌ 拒绝' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="toastMsg" class="toast">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { userMaterialApi } from '../api/userMaterial'

const items = ref([])
const loading = ref(false)
const processingId = ref(null)
const commentMap = reactive({})
const toastMsg = ref('')

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function loadPending() {
  loading.value = true
  try {
    const res = await userMaterialApi.listPending()
    if (res.data?.code === 200) items.value = res.data.data || []
  } catch(e) { console.error(e) } finally { loading.value = false }
}

async function approve(item) {
  processingId.value = item.id
  try {
    await userMaterialApi.approve(item.id, commentMap[item.id] || '')
    items.value = items.value.filter(i => i.id !== item.id)
    showToast(`「${item.title}」已通过并收录至素材库`)
  } catch(e) { showToast('操作失败：' + (e.response?.data?.message || e.message)) }
  finally { processingId.value = null }
}

async function reject(item) {
  if (!commentMap[item.id]?.trim()) {
    showToast('请填写拒绝原因')
    return
  }
  processingId.value = item.id
  try {
    await userMaterialApi.reject(item.id, commentMap[item.id])
    items.value = items.value.filter(i => i.id !== item.id)
    showToast(`「${item.title}」已拒绝`)
  } catch(e) { showToast('操作失败：' + (e.response?.data?.message || e.message)) }
  finally { processingId.value = null }
}

function showToast(msg) {
  toastMsg.value = msg
  setTimeout(() => { toastMsg.value = '' }, 3000)
}

onMounted(loadPending)
</script>

<style scoped>
.review-page { padding: 2rem; max-width: 900px; margin: 0 auto; }
.page-header { margin-bottom: 1.75rem; }
.page-header h1 { font-size: 1.75rem; font-weight: 700; background: linear-gradient(90deg, var(--primary), var(--primary-light)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; margin: 0 0 0.25rem; }
.subtitle { color: var(--text-sub); font-size: 0.9rem; margin: 0; }
.loading, .empty { text-align: center; padding: 4rem; color: var(--text-muted); font-size: 1rem; }
.review-list { display: flex; flex-direction: column; gap: 1.25rem; }
.review-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 1.25rem 1.5rem; }
.review-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.75rem; flex-wrap: wrap; }
.cat-badge { display: inline-block; padding: 0.18rem 0.6rem; background: linear-gradient(90deg, var(--primary), var(--primary-light)); color: #fff; border-radius: 20px; font-size: 0.72rem; font-weight: 600; flex-shrink: 0; }
.review-title { flex: 1; font-size: 1.05rem; font-weight: 700; color: var(--text-main); margin: 0; }
.review-meta { font-size: 0.78rem; color: var(--text-muted); white-space: nowrap; }
.review-tags { display: flex; flex-wrap: wrap; gap: 0.35rem; margin-bottom: 0.75rem; }
.tag { display: inline-block; padding: 0.12rem 0.45rem; background: var(--tag-bg); color: var(--text-sub); border-radius: 4px; font-size: 0.75rem; }
.review-content { background: var(--bg-input); border-radius: 8px; padding: 1rem; font-size: 0.88rem; color: var(--text-main); line-height: 1.75; white-space: pre-wrap; word-break: break-all; margin: 0 0 1rem; max-height: 300px; overflow-y: auto; }
.review-actions { display: flex; gap: 0.75rem; align-items: center; }
.comment-input { flex: 1; padding: 0.5rem 0.85rem; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-input); color: var(--text-main); font-size: 0.88rem; font-family: inherit; }
.comment-input:focus { outline: none; border-color: var(--primary); }
.btn-approve { padding: 0.5rem 1.2rem; border-radius: 7px; border: none; background: linear-gradient(90deg, #43a047, #66bb6a); color: #fff; font-weight: 700; font-size: 0.88rem; cursor: pointer; transition: opacity 0.2s; white-space: nowrap; }
.btn-approve:hover:not(:disabled) { opacity: 0.85; }
.btn-approve:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-reject { padding: 0.5rem 1.2rem; border-radius: 7px; border: none; background: linear-gradient(90deg, #e53935, #ef5350); color: #fff; font-weight: 700; font-size: 0.88rem; cursor: pointer; transition: opacity 0.2s; white-space: nowrap; }
.btn-reject:hover:not(:disabled) { opacity: 0.85; }
.btn-reject:disabled { opacity: 0.5; cursor: not-allowed; }
.toast { position: fixed; bottom: 2rem; left: 50%; transform: translateX(-50%); background: var(--primary); color: #fff; padding: 0.7rem 1.5rem; border-radius: 999px; font-size: 0.9rem; font-weight: 600; box-shadow: 0 4px 16px var(--shadow); z-index: 9999; pointer-events: none; animation: fadeup 0.3s ease; }
@keyframes fadeup { from { opacity: 0; transform: translateX(-50%) translateY(10px); } to { opacity: 1; transform: translateX(-50%) translateY(0); } }
</style>
