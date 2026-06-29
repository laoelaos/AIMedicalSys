<template>
  <div class="fee-container">
    <div class="page-header">
      <el-button type="default" @click="router.push('/home')">← 返回首页</el-button>
      <h2>费用查询</h2>
    </div>

    <el-card shadow="hover" class="fee-card">
      <el-alert title="功能建设中" type="info" show-icon :closable="false" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<style scoped>
.fee-container {
  max-width: 780px;
  margin: 0 auto;
  padding: 24px 16px;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.page-header h2 { margin: 0; }
.fee-card { margin-bottom: 16px; }
</style>
