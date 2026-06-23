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
        @click="menuStore.setActiveMenu('/dashboard')"
      >
        仪表盘
      </router-link>
      <div v-if="menuStore.hasMenus" class="menu-section">
        <template v-for="menu in menuStore.menus" :key="menu.id">
          <div class="menu-item" :class="{ 'has-children': menu.children && menu.children.length > 0 }">
            <router-link
              :to="menu.path"
              class="nav-item"
              :class="{ active: isActive(menu.path) }"
              @click="menuStore.setActiveMenu(menu.path)"
            >
              <span v-if="menu.icon" class="menu-icon">{{ menu.icon }}</span>
              <span class="menu-name">{{ menu.name }}</span>
              <span v-if="menu.children && menu.children.length > 0" class="expand-icon">▶</span>
            </router-link>
            <!-- 子菜单递归渲染 -->
            <div v-if="menu.children && menu.children.length > 0" class="submenu">
              <router-link
                v-for="child in menu.children"
                :key="child.id"
                :to="child.path"
                class="nav-item submenu-item"
                :class="{ active: isActive(child.path) }"
                @click="menuStore.setActiveMenu(child.path)"
              >
                <span class="menu-name">{{ child.name }}</span>
              </router-link>
            </div>
          </div>
        </template>
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
  // 优先使用store中的activeMenu，否则使用route.path
  return menuStore.activeMenu === path || route.path === path
}
</script>

<style scoped>
.sidebar-container {
  height: 100vh;
  color: white;
  display: flex;
  flex-direction: column;
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
  flex: 1;
  overflow-y: auto;
  padding: 10px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
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

.menu-item {
  position: relative;
}

.menu-item .nav-item {
  padding-left: 30px;
}

.menu-item.has-children > .nav-item {
  padding-right: 30px;
}

.menu-icon {
  font-size: 14px;
}

.menu-name {
  flex: 1;
}

.expand-icon {
  font-size: 10px;
  transition: transform 0.3s;
}

.submenu {
  background: rgba(0, 0, 0, 0.1);
}

.submenu-item {
  padding-left: 50px !important;
  font-size: 14px;
}
</style>