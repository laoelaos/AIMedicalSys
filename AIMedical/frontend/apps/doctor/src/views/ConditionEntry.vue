<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>病情录入</h2>
          <span class="patient-hint">患者ID: {{ patientId }}</span>
        </div>
      </template>

      <el-form :model="form" label-position="top">
        <el-form-item label="主诉">
          <el-input
            v-model="form.chief_complaint"
            type="textarea"
            :rows="2"
            placeholder="请输入主诉"
          />
        </el-form-item>
        <el-form-item label="现病史">
          <el-input
            v-model="form.present_illness"
            type="textarea"
            :rows="4"
            placeholder="请输入现病史"
          />
        </el-form-item>
        <el-form-item label="既往史">
          <el-input
            v-model="form.past_history"
            type="textarea"
            :rows="4"
            placeholder="请输入既往史"
          />
        </el-form-item>
        <el-form-item label="初步诊断">
          <el-input
            v-model="form.diagnosis"
            type="textarea"
            :rows="2"
            placeholder="请输入初步诊断"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveAsDraft">保存为病历草稿</el-button>
          <el-button type="success" @click="goAiDiagnosis">AI辅助诊断</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const patientId = Number(route.params.patientId)

const form = reactive({
  chief_complaint: '',
  present_illness: '',
  past_history: '',
  diagnosis: '',
})

function saveAsDraft() {
  router.push({
    path: `/patient/${patientId}/medical-records/new`,
    query: {
      chief_complaint: form.chief_complaint,
      present_illness: form.present_illness,
      past_history: form.past_history,
      diagnosis: form.diagnosis,
    },
  })
}

function goAiDiagnosis() {
  router.push({
    path: '/ai/diagnosis',
    query: {
      patientId: String(patientId),
      chief_complaint: form.chief_complaint,
      present_illness: form.present_illness,
      past_history: form.past_history,
    },
  })
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
}

.patient-hint {
  color: #909399;
  font-size: 14px;
}
</style>
