<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <h2>挂号管理 — 叫号队列</h2>
          <div class="header-actions">
            <el-button type="primary" :loading="loading" @click="handleCallNext">叫下一位</el-button>
            <el-button :loading="loading" @click="loadQueue">刷新</el-button>
          </div>
        </div>
      </template>

      <!-- 候诊队列 -->
      <div class="section">
        <h3 class="section-title">候诊队列</h3>
        <el-table :data="waitingList" border style="width: 100%">
          <el-table-column prop="queue_no" label="排队号" width="120" />
          <el-table-column label="患者姓名" width="160">
            <template #default="{ row }">
              <el-link type="primary" :underline="false" @click="goPatient(row.patient_id)">
                {{ row.patient_name }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column label="挂号时间" width="200">
            <template #default="{ row }">{{ formatDateTime(row.registered_at) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无候诊患者" />
          </template>
        </el-table>
      </div>

      <!-- 当前接诊 -->
      <div class="section">
        <h3 class="section-title">当前接诊</h3>
        <el-table :data="currentConsultations" border style="width: 100%">
          <el-table-column prop="queue_no" label="排队号" width="120" />
          <el-table-column label="患者姓名" width="160">
            <template #default="{ row }">
              <el-link type="primary" :underline="false" @click="goPatient(row.patient_id)">
                {{ row.patient_name }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="140">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="叫号时间" width="200">
            <template #default="{ row }">{{ formatDateTime(row.called_at) }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="240">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'CALLED'"
                type="success"
                size="small"
                @click="handleStart(row.id)"
              >开始接诊</el-button>
              <el-button
                v-if="row.status === 'IN_CONSULTATION'"
                type="success"
                size="small"
                @click="handleFinish(row.id)"
              >完成接诊</el-button>
              <el-button
                v-if="row.status === 'IN_CONSULTATION'"
                type="warning"
                size="small"
                @click="handleSkip(row.id)"
              >过号</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无接诊中患者" />
          </template>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { doctorApi, isBusinessError } from '@aimedical/shared'
import type { ConsultationQueueResponse, ConsultationStatus } from '@aimedical/shared'

const router = useRouter()
const loading = ref(false)
const queue = ref<ConsultationQueueResponse[]>([])

const waitingList = computed(() => queue.value.filter((q) => q.status === 'WAITING'))
const currentConsultations = computed(() =>
  queue.value.filter((q) => q.status === 'CALLED' || q.status === 'IN_CONSULTATION'),
)

const formatDateTime = (iso: string | null): string =>
  iso ? new Date(iso).toLocaleString('zh-CN') : '—'

const statusLabel = (status: ConsultationStatus): string => {
  const map: Record<ConsultationStatus, string> = {
    WAITING: '候诊',
    CALLED: '已叫号',
    IN_CONSULTATION: '接诊中',
    FINISHED: '已完成',
    SKIPPED: '已过号',
  }
  return map[status] || status
}

const statusTagType = (
  status: ConsultationStatus,
): 'primary' | 'success' | 'info' | 'warning' | 'danger' => {
  const map: Record<ConsultationStatus, 'primary' | 'success' | 'info' | 'warning' | 'danger'> = {
    WAITING: 'info',
    CALLED: 'warning',
    IN_CONSULTATION: 'danger',
    FINISHED: 'success',
    SKIPPED: 'info',
  }
  return map[status] || 'info'
}

function goPatient(patientId: number) {
  router.push(`/patient/${patientId}`)
}

async function loadQueue() {
  loading.value = true
  try {
    const result = await doctorApi.listMyQueue()
    if (isBusinessError(result)) {
      ElMessage.error(result.message)
      return
    }
    queue.value = result
  } finally {
    loading.value = false
  }
}

async function handleCallNext() {
  loading.value = true
  try {
    const result = await doctorApi.callNext()
    if (isBusinessError(result)) {
      ElMessage.error(result.message)
      return
    }
    ElMessage.success(`已叫号：${result.patient_name}`)
  } finally {
    await loadQueue()
  }
}

async function handleStart(id: number) {
  loading.value = true
  try {
    const result = await doctorApi.startConsultation(id)
    if (isBusinessError(result)) {
      ElMessage.error(result.message)
      return
    }
    ElMessage.success('已开始接诊')
  } finally {
    await loadQueue()
  }
}

async function handleFinish(id: number) {
  loading.value = true
  try {
    const result = await doctorApi.finishConsultation(id)
    if (isBusinessError(result)) {
      ElMessage.error(result.message)
      return
    }
    ElMessage.success('已完成接诊')
  } finally {
    await loadQueue()
  }
}

async function handleSkip(id: number) {
  loading.value = true
  try {
    const result = await doctorApi.skipQueue(id)
    if (isBusinessError(result)) {
      ElMessage.error(result.message)
      return
    }
    ElMessage.success('已过号')
  } finally {
    await loadQueue()
  }
}

onMounted(() => {
  loadQueue()
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

.section {
  margin-bottom: 24px;
}

.section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 16px;
  margin: 0 0 12px 0;
  color: #303133;
  border-left: 3px solid #409eff;
  padding-left: 8px;
}
</style>
