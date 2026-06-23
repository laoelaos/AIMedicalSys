<template>
  <div class="dynamic-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ pageTitle }}</span>
        </div>
      </template>
      <div class="card-content">
        <p>页面路径: {{ currentPath }}</p>
        <p>此页面功能正在开发中...</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const pageTitle = computed(() => {
  return route.name || '动态页面'
})

const currentPath = computed(() => {
  return route.path
})
</script>

<style scoped>
.dynamic-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-content {
  padding: 20px;
  text-align: center;
}
</style>