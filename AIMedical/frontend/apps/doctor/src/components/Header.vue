<template>
  <div class="header-container">
    <div class="header-left">
      <span class="app-title">智慧云脑诊疗平台 - 医生端</span>
    </div>
    <div class="header-right">
      <span class="user-name">{{ authStore.user?.realName || '医生' }}</span>
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useMenuStore } from '../stores/menu'

const router = useRouter()
const authStore = useAuthStore()
const menuStore = useMenuStore()

async function handleLogout() {
  await authStore.logout()
  menuStore.clearMenus()
  router.push('/login')
}
</script>

<style scoped>
.header-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
}

.app-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-name {
  font-size: 14px;
  color: #666;
}

.logout-btn {
  padding: 8px 16px;
  background: #f56c6c;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.logout-btn:hover {
  opacity: 0.9;
}
</style>