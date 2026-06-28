<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <h2>病历列表</h2>
          <div class="header-actions">
            <el-button type="primary" @click="router.push(`/patient/${patientId}/medical-records/new`)">
              新建病历
            </el-button>
            <el-button @click="router.push(`/patient/${patientId}`)">返回患者</el-button>
          </div>
        </div>
      </template>

      <el-table :data="records" border style="width: 100%">
        <el-table-column prop="version_no" label="版本号" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OFFICIAL' ? 'success' : 'info'">
              {{ row.status === 'OFFICIAL' ? '正式' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="主诉" min-width="200">
          <template #default="{ row }">{{ truncate(row.chief_complaint) }}</template>
        </el-table-column>
        <el-table-column label="诊断" min-width="200">
          <template #default="{ row }">{{ truncate(row.diagnosis) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="200">
          <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="200">
          <template #default="{ row }">{{ formatDateTime(row.updated_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="goDetail(row.id)">查看</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无病历记录" />
        </template>
      </el-table>
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
const loading = ref(false)
const records = ref<MedicalRecordResponse[]>([])

const formatDateTime = (iso: string | null): string =>
  iso ? new Date(iso).toLocaleString('zh-CN') : '—'

const truncate = (text: string | null, max = 30): string => {
  if (!text) return '—'
  return text.length > max ? text.slice(0, max) + '...' : text
}

async function loadRecords() {
  loading.value = true
  try {
    const result = await doctorApi.listMedicalRecordsByPatient(patientId)
    if (isBusinessError(result)) {
      ElMessage.error(result.message)
      return
    }
    records.value = result
  } finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  router.push(`/patient/${patientId}/medical-records/${id}`)
}

onMounted(() => {
  loadRecords()
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
