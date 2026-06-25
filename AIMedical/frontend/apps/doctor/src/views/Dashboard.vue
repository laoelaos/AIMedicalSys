<template>
  <div class="dashboard-container">
    <h1>医生端仪表盘</h1>
    <p class="welcome-text">欢迎，{{ authStore.user?.realName || '医生' }}</p>
    <p class="placeholder-hint">Phase 1 占位页面 — 功能开发中</p>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
</script>

<style scoped>
.dashboard-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  font-family: sans-serif;
}

.dashboard-container h1 {
  font-size: 24px;
  color: #333;
  margin-bottom: 16px;
}

.welcome-text {
  font-size: 18px;
  color: #666;
  margin-bottom: 8px;
}

.placeholder-hint {
  color: #999;
  font-size: 14px;
}
</style>