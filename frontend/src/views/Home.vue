<template>
  <div class="home">
    <div class="home-hero">
      <h1 class="home-title">历史特色创作平台</h1>
      <p class="home-sub">检索历史素材，AI 辅助创作，沉浸历史世界</p>
    </div>
    <div class="home-cards">
      <router-link to="/materials" class="home-card">
        <span class="card-icon">📚</span>
        <span class="card-label">素材检索</span>
        <span class="card-desc">浏览和搜索历史特色素材库</span>
      </router-link>
      <router-link to="/workspace" class="home-card">
        <span class="card-icon">🎨</span>
        <span class="card-label">创作工作台</span>
        <span class="card-desc">在线写作，AI 辅助，自动保存</span>
      </router-link>
      <router-link to="/works" class="home-card">
        <span class="card-icon">📝</span>
        <span class="card-label">我的作品</span>
        <span class="card-desc">管理所有创作作品与分组</span>
      </router-link>
      <router-link to="/favorites" class="home-card">
        <span class="card-icon">❤️</span>
        <span class="card-label">我的收藏</span>
        <span class="card-desc">收藏素材与自建素材管理</span>
      </router-link>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home { padding: 2rem; max-width: 900px; margin: 0 auto; }
.home-hero { text-align: center; padding: 2.5rem 0 2rem; }
.home-title {
  font-size: 2rem; font-weight: 800;
  background: linear-gradient(90deg, var(--primary), var(--primary-light));
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
  margin: 0 0 0.6rem;
}
.home-sub { color: var(--text-sub); font-size: 1rem; margin: 0; }
.home-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 1.25rem; }
.home-card {
  display: flex; flex-direction: column; gap: 0.5rem;
  padding: 1.5rem; background: var(--bg-card); border: 1px solid var(--border);
  border-radius: 12px; text-decoration: none; color: var(--text-main); transition: all 0.2s;
}
.home-card:hover { border-color: var(--primary); box-shadow: 0 6px 20px var(--shadow); transform: translateY(-3px); }
.card-icon { font-size: 1.8rem; }
.card-label { font-size: 1rem; font-weight: 700; color: var(--text-main); }
.card-desc { font-size: 0.82rem; color: var(--text-muted); line-height: 1.5; }
</style>
