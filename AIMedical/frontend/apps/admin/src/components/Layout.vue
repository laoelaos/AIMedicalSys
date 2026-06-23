<template>
  <div class="layout-container">
    <Sidebar class="sidebar" />
    <div class="main-container">
      <Header class="header" />
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
</script>

<style scoped>
.layout-container {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 200px;
  background: #1e3c72;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.header {
  height: 60px;
  background: white;
  border-bottom: 1px solid #e6e6e6;
}

.content {
  flex: 1;
  padding: 20px;
  background: #f5f5f5;
}
</style>