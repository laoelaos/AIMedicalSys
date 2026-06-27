<template>
  <div class="header-container">
    <div class="header-left">
      <span class="app-title">{{ title }}</span>
    </div>
    <div class="header-right">
      <span class="user-name">{{ user?.real_name || fallbackName }}</span>
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 通用顶部栏组件（T4 抽取到 shared 包）
 *
 * <p>两端 Header 结构完全相同，仅标题和用户名 fallback 文字不同。
 * 通过 props 传入 title 和 fallbackName，由各 app 薄包装组件提供。
 *
 * <p>依赖各 app 的 authStore 和 menuStore（通过 createAuthStore/createMenuStore 工厂创建），
 * 因此 HeaderBase 不直接导入 store，而是由薄包装层注入 logout 回调。
 * 为简化实现，此处直接使用 useRouter 和传入的 onLogout 回调。
 */
import { useRouter } from 'vue-router'

const props = defineProps<{
  /** 应用标题（如 "智慧云脑诊疗平台 - 管理员端"） */
  title: string
  /** 未登录用户名时的 fallback 文字（如 "管理员"） */
  fallbackName: string
  /** 用户信息对象（real_name 字段用于显示） */
  user?: { real_name?: string } | null
}>()

const emit = defineEmits<{
  (e: 'logout'): void
}>()

const router = useRouter()

async function handleLogout() {
  emit('logout')
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
