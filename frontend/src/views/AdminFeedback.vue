<template>
  <div class="feedback-page">
    <div class="page-header">
      <h1>用户反馈管理</h1>
      <p class="subtitle">查看和管理用户提交的反馈意见</p>
    </div>

    <div class="filters">
      <button
        v-for="status in ['all', 'pending', 'read', 'resolved']"
        :key="status"
        :class="{ active: currentStatus === status }"
        @click="currentStatus = status"
        class="filter-btn"
      >
        {{ statusLabel(status) }}
      </button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="filteredFeedbacks.length === 0" class="empty">
      <p>暂无反馈</p>
    </div>
    <div v-else class="feedback-list">
      <div v-for="feedback in filteredFeedbacks" :key="feedback.id" class="feedback-item">
        <div class="feedback-header">
          <div class="user-info">
            <span class="username">{{ feedback.username }}</span>
            <span class="status" :class="feedback.status">{{ statusLabel(feedback.status) }}</span>
          </div>
          <span class="time">{{ formatDate(feedback.createdAt) }}</span>
        </div>
        <div class="feedback-content">
          {{ feedback.content }}
        </div>
        <div class="feedback-actions">
          <button
            v-if="feedback.status === 'pending'"
            @click="markAsRead(feedback.id)"
            class="btn-action"
          >
            标记为已读
          </button>
          <button
            v-if="feedback.status !== 'resolved'"
            @click="markAsResolved(feedback.id)"
            class="btn-action resolve"
          >
            标记为已解决
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { feedbackApi } from '../api/feedback'

const currentStatus = ref('all')
const feedbacks = ref([])
const loading = ref(false)

const filteredFeedbacks = computed(() => {
  if (currentStatus.value === 'all') {
    return feedbacks.value
  }
  return feedbacks.value.filter(f => f.status === currentStatus.value)
})

function statusLabel(status) {
  const labels = {
    all: '全部',
    pending: '待处理',
    read: '已读',
    resolved: '已解决'
  }
  return labels[status] || status
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function loadFeedbacks() {
  loading.value = true
  try {
    const res = await feedbackApi.getAllFeedback()
    if (res.data?.code === 200 && res.data?.data) {
      feedbacks.value = res.data.data
    }
  } catch (e) {
    console.error('加载反馈失败:', e)
  } finally {
    loading.value = false
  }
}

async function markAsRead(id) {
  try {
    const res = await feedbackApi.markAsRead(id)
    if (res.data?.code === 200) {
      const feedback = feedbacks.value.find(f => f.id === id)
      if (feedback) {
        feedback.status = 'read'
      }
    }
  } catch (e) {
    console.error('标记失败:', e)
  }
}

async function markAsResolved(id) {
  try {
    const res = await feedbackApi.markAsResolved(id)
    if (res.data?.code === 200) {
      const feedback = feedbacks.value.find(f => f.id === id)
      if (feedback) {
        feedback.status = 'resolved'
      }
    }
  } catch (e) {
    console.error('标记失败:', e)
  }
}

onMounted(() => {
  loadFeedbacks()
})
</script>

<style scoped>
.feedback-page {
  padding: 2rem;
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 2px solid rgba(233, 69, 96, 0.2);
}

.page-header h1 {
  margin: 0 0 0.5rem;
  font-size: 2rem;
  background: linear-gradient(90deg, #e94560, #ff6b9d);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  margin: 0;
  color: #b8b8d0;
  font-size: 0.95rem;
}

.filters {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  flex-wrap: wrap;
}

.filter-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #444;
  border-radius: 6px;
  background: transparent;
  color: #b8b8d0;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 600;
}

.filter-btn:hover {
  border-color: #e94560;
  color: #e94560;
}

.filter-btn.active {
  background: linear-gradient(90deg, #e94560, #ff6b9d);
  border-color: #e94560;
  color: #fff;
}

.loading,
.empty {
  text-align: center;
  padding: 2rem;
  color: #888;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.feedback-item {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.8), rgba(22, 33, 62, 0.8));
  border: 1px solid rgba(233, 69, 96, 0.2);
  border-radius: 12px;
  padding: 1.5rem;
  transition: all 0.3s ease;
}

.feedback-item:hover {
  border-color: rgba(233, 69, 96, 0.5);
  box-shadow: 0 4px 12px rgba(233, 69, 96, 0.1);
}

.feedback-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid rgba(233, 69, 96, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.username {
  font-weight: 600;
  color: #e8e8e8;
}

.status {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
}

.status.pending {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
}

.status.read {
  background: rgba(76, 175, 80, 0.2);
  color: #4caf50;
}

.status.resolved {
  background: rgba(33, 150, 243, 0.2);
  color: #2196f3;
}

.time {
  color: #888;
  font-size: 0.9rem;
}

.feedback-content {
  color: #b8b8d0;
  line-height: 1.6;
  margin-bottom: 1rem;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  word-break: break-word;
}

.feedback-actions {
  display: flex;
  gap: 0.75rem;
}

.btn-action {
  padding: 0.5rem 1rem;
  border: 1px solid #444;
  border-radius: 6px;
  background: transparent;
  color: #b8b8d0;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.9rem;
  font-weight: 600;
}

.btn-action:hover {
  border-color: #e94560;
  color: #e94560;
  background: rgba(233, 69, 96, 0.1);
}

.btn-action.resolve {
  border-color: #2196f3;
  color: #2196f3;
}

.btn-action.resolve:hover {
  background: rgba(33, 150, 243, 0.1);
}

@media (max-width: 768px) {
  .feedback-page {
    padding: 1rem;
  }

  .page-header h1 {
    font-size: 1.5rem;
  }

  .feedback-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .filters {
    gap: 0.5rem;
  }

  .filter-btn {
    padding: 0.4rem 0.8rem;
    font-size: 0.9rem;
  }
}
</style>
