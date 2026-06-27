<template>
  <div class="layout-container">
    <div class="sidebar" :style="sidebarStyle">
      <slot name="sidebar" />
    </div>
    <div class="main-container">
      <div class="header">
        <slot name="header" />
      </div>
      <main class="content">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 通用布局组件（T5 抽取到 shared 包）
 *
 * <p>两端 Layout 结构完全相同，仅 sidebar 背景色不同。
 * sidebar 背景色通过 sidebarBg prop 传入。
 * Sidebar 和 Header 组件通过具名插槽注入，保持 shared 组件的纯 UI 特性。
 *
 * <p>使用示例：
 * <pre>
 * &lt;LayoutBase sidebar-bg="#1e3c72"&gt;
 *   &lt;template #sidebar&gt;&lt;Sidebar /&gt;&lt;/template&gt;
 *   &lt;template #header&gt;&lt;Header /&gt;&lt;/template&gt;
 *   &lt;router-view /&gt;
 * &lt;/LayoutBase&gt;
 * </pre>
 */
import { computed } from 'vue'

const props = defineProps<{
  /** 侧边栏背景色，默认 #1e3c72 */
  sidebarBg?: string
}>()

const sidebarStyle = computed(() => ({
  background: props.sidebarBg || '#1e3c72',
}))
</script>

<style scoped>
.layout-container {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 200px;
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
