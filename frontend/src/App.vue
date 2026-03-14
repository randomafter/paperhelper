<template>
  <router-view />
</template>

<script setup>
import { useThemeStore } from './stores/theme'
import { onMounted } from 'vue'

const theme = useThemeStore()

onMounted(() => {
  theme.applyTheme(theme.theme)
})
</script>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif; transition: background 0.3s, color 0.3s; }
#app { min-height: 100vh; }

/* ===== 深色主题：靛蓝配红 ===== */
[data-theme="dark"] {
  --primary: #e94560;
  --primary-light: #ff6b9d;
  --accent: #3d5af1;
  --bg-base: #0d0d1a;
  --bg-card: #1a1a2e;
  --bg-sidebar: #141428;
  --bg-header: #16162a;
  --bg-input: rgba(255,255,255,0.08);
  --bg-hover: rgba(233,69,96,0.12);
  --text-main: #e8e8f0;
  --text-sub: #9090b0;
  --text-muted: #606080;
  --border: #2a2a4a;
  --border-accent: rgba(233,69,96,0.3);
  --shadow: rgba(0,0,0,0.4);
  --tag-bg: #2a2a4a;
}

/* ===== 浅色主题：青配焦黄 ===== */
[data-theme="light"] {
  --primary: #00897b;
  --primary-light: #4db6ac;
  --accent: #fbc02d;
  --bg-base: #f0f4f4;
  --bg-card: #ffffff;
  --bg-sidebar: #e8f0ef;
  --bg-header: #ffffff;
  --bg-input: rgba(0,0,0,0.05);
  --bg-hover: rgba(0,137,123,0.08);
  --text-main: #1a2e2c;
  --text-sub: #4a6462;
  --text-muted: #8aabaa;
  --border: #d0e0de;
  --border-accent: rgba(0,137,123,0.3);
  --shadow: rgba(0,0,0,0.1);
  --tag-bg: #e0f2f1;
}

body {
  background: var(--bg-base);
  color: var(--text-main);
}

/* 全局 select 适配主题 */
select {
  background: var(--bg-input);
  color: var(--text-main);
  border: 1px solid var(--border);
}
select option {
  background: var(--bg-card);
  color: var(--text-main);
}
</style>
