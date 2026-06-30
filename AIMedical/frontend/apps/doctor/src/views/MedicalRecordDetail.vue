<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <h2>病历详情</h2>
          <div class="header-actions">
            <el-button
              v-if="record && record.status === 'DRAFT'"
              type="success"
              @click="handlePublish"
            >发布为正式版本</el-button>
            <el-button @click="router.push(`/patient/${patientId}/medical-records`)">返回列表</el-button>
            <el-button
              v-if="record && record.status === 'DRAFT'"
              @click="router.push({ name: 'MedicalRecordEdit', params: { patientId, recordId: record.id } })"
            >编辑</el-button>
          </div>
        </div>
      </template>

      <el-descriptions v-if="record" :column="2" border>
        <el-descriptions-item label="版本号">{{ record.version_no }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="record.status === 'OFFICIAL' ? 'success' : 'info'">
            {{ record.status === 'OFFICIAL' ? '正式' : '草稿' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="科室">{{ record.department }}</el-descriptions-item>
        <el-descriptions-item label="AI生成">{{ record.ai_generated ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="主诉" :span="2">{{ record.chief_complaint || '—' }}</el-descriptions-item>
        <el-descriptions-item label="现病史" :span="2">{{ record.present_illness || '—' }}</el-descriptions-item>
        <el-descriptions-item label="既往史" :span="2">{{ record.past_history || '—' }}</el-descriptions-item>
        <el-descriptions-item label="诊断" :span="2">{{ record.diagnosis || '—' }}</el-descriptions-item>
        <el-descriptions-item label="治疗方案" :span="2">{{ record.treatment_plan || '—' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ record.remark || '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(record.created_at) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(record.updated_at) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { doctorApi, isBusinessError } from '@aimedical/shared'
import type { MedicalRecordResponse } from '@aimedical/shared'

const route = useRoute()
const router = useRouter()
const patientId = Number(route.params.patientId)
const recordId = Number(route.params.recordId)
const loading = ref(false)
const record = ref<MedicalRecordResponse | null>(null)

const formatDateTime = (iso: string | null): string =>
  iso ? new Date(iso).toLocaleString('zh-CN') : '—'

async function loadRecord() {
  loading.value = true
  try {
    const result = await doctorApi.getMedicalRecord(recordId)
    if (isBusinessError(result)) {
      ElMessage.error(result.message)
      return
    }
    record.value = result
  } finally {
    loading.value = false
  }
}

async function handlePublish() {
  loading.value = true
  try {
    const result = await doctorApi.publishMedicalRecord(recordId)
    if (isBusinessError(result)) {
      ElMessage.error(result.message)
      return
    }
    ElMessage.success('病历已发布为正式版本')
    await loadRecord()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRecord()
})
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

.header-actions {
  display: flex;
  gap: 8px;
}
</style>
