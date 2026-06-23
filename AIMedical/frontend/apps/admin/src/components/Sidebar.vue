<template>
  <div class="sidebar-container">
    <div class="sidebar-header">
      <h2>管理员端</h2>
    </div>
    <nav class="sidebar-nav">
      <router-link
        to="/dashboard"
        class="nav-item"
        :class="{ active: isActive('/dashboard') }"
      >
        仪表盘
      </router-link>
      <div v-if="menuStore.hasMenus" class="menu-section">
        <div v-for="menu in menuStore.menus" :key="menu.id" class="menu-item">
          <router-link
            :to="menu.path"
            class="nav-item"
            :class="{ active: isActive(menu.path) }"
          >
            {{ menu.name }}
          </router-link>
        </div>
      </div>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { useMenuStore } from '../stores/menu'
import { useRoute } from 'vue-router'

const menuStore = useMenuStore()
const route = useRoute()

function isActive(path: string): boolean {
  return route.path === path
}
</script>

<style scoped>
.sidebar-container {
  height: 100vh;
  color: white;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-header h2 {
  font-size: 18px;
  font-weight: 500;
}

.sidebar-nav {
  padding: 10px 0;
}

.nav-item {
  display: block;
  padding: 12px 20px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  transition: all 0.3s;
}

.nav-item:hover {
  color: white;
  background: rgba(255, 255, 255, 0.1);
}

.nav-item.active {
  color: white;
  background: rgba(255, 255, 255, 0.2);
}

.menu-section {
  margin-top: 10px;
}
</style>