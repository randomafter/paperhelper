<template>
  <div class="home">
    <h1>欢迎使用历史特色创作平台</h1>
    <p>当前版本仅保留「用户管理」相关功能，包括注册、登录与个人中心。</p>
    <div class="cards">
      <router-link to="/profile" class="card">进入个人中心</router-link>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home { padding: 1rem 0; }
.home h1 { margin-bottom: 0.5rem; font-size: 1.75rem; }
.home p { color: #aaa; margin-bottom: 2rem; }
.cards { display: flex; gap: 1rem; flex-wrap: wrap; }
.card {
  display: block;
  padding: 1.5rem 2rem;
  background: #1a1a2e;
  border-radius: 10px;
  color: #e8e8e8;
  text-decoration: none;
  border: 1px solid #2a2a4a;
  min-width: 160px;
}
.card:hover { border-color: #e94560; color: #e94560; }
</style>
