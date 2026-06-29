<template>
  <div class="page-container">
    <div class="header-row">
      <h2>健康档案</h2>
      <el-button text @click="$router.push('/profile')">返回个人中心</el-button>
    </div>
    <HealthRecordSection />
  </div>
</template>

<script setup lang="ts">
import HealthRecordSection from '../components/HealthRecordSection.vue'
</script>

<style scoped>
.page-container {
  max-width: 900px;
  margin: 20px auto;
  padding: 0 20px;
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>
