<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>患者信息</h2>
        </div>
      </template>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="患者档案ID">{{ patientId }}</el-descriptions-item>
      </el-descriptions>

      <div class="action-block">
        <h3 class="block-title">诊疗操作</h3>
        <el-button-group>
          <el-button type="primary" @click="router.push(`/patient/${patientId}/condition`)">
            病情录入
          </el-button>
          <el-button type="primary" @click="router.push(`/patient/${patientId}/medical-records`)">
            病历列表
          </el-button>
          <el-button type="primary" @click="router.push(`/patient/${patientId}/medical-records/new`)">
            新建病历
          </el-button>
          <el-button type="primary" @click="router.push(`/patient/${patientId}/prescriptions/new`)">
            开具处方
          </el-button>
        </el-button-group>
      </div>

      <div class="action-block">
        <h3 class="block-title">AI辅助</h3>
        <el-button-group>
          <el-button type="success" @click="router.push(`/ai/diagnosis?patient_id=${patientId}`)">
            AI辅助诊断
          </el-button>
          <el-button type="success" @click="router.push(`/ai/examination?patientId=${patientId}`)">
            AI开立检查
          </el-button>
          <el-button
            type="success"
            @click="router.push(`/ai/prescription-assist?patientId=${patientId}`)"
          >
            AI辅助开方
          </el-button>
          <el-button
            type="success"
            @click="router.push(`/ai/medical-record-gen?patient_id=${patientId}`)"
          >
            AI病历生成
          </el-button>
        </el-button-group>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const patientId = Number(route.params.patientId)
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
}

.action-block {
  margin-top: 24px;
}

.block-title {
  font-size: 15px;
  margin: 0 0 12px 0;
  color: #303133;
}

.el-button-group {
  display: flex;
  flex-wrap: wrap;
}
</style>
