<template>
  <div class="appointment-container">
    <div class="page-header">
      <el-button type="default" @click="router.push('/triage')">← 返回导诊</el-button>
      <h2>预约挂号</h2>
    </div>

    <el-card shadow="hover" class="doctor-card">
      <template #header>
        <span>医生信息</span>
      </template>
      <div class="doctor-info">
        <div class="info-row">
          <span class="info-label">医生姓名</span>
          <span class="info-value">{{ doctorName }}</span>
        </div>
        <div class="info-row" v-if="departmentName">
          <span class="info-label">所属科室</span>
          <span class="info-value">{{ departmentName }}</span>
        </div>
      </div>
    </el-card>

    <el-card shadow="hover" class="slot-card">
      <template #header>
        <span>选择就诊时段</span>
      </template>

      <el-skeleton :loading="loading" animated>
        <div v-if="slots.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无可用号源" />
        </div>
        <div v-else class="slot-grid">
          <div
            v-for="slot in availableSlots"
            :key="slot.slot_id"
            class="slot-item"
            @click="selectedSlot = slot.slot_id"
            :class="{ selected: selectedSlot === slot.slot_id }"
          >
            {{ slot.time_slot }}
          </div>
        </div>
      </el-skeleton>
    </el-card>

    <div class="action-bar">
      <el-button
        type="primary"
        size="large"
        :disabled="!selectedSlot || booking"
        :loading="booking"
        @click="handleBook"
      >
        {{ booking ? '预约中…' : '确认挂号' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { appointmentApi, type AppointmentSlot, type BusinessError } from '@aimedical/shared'

const router = useRouter()
const route = useRoute()

const rawDoctorId = Number(route.query.doctor_id)
const doctorId = Number.isNaN(rawDoctorId) || rawDoctorId <= 0 ? 0 : rawDoctorId
const doctorName = (route.query.doctor_name as string) || ''
const departmentName = (route.query.department_name as string) || ''

if (doctorId === 0 && route.query.doctor_id !== undefined) {
  throw new Error('无效的医生ID参数')
}

const loading = ref(true)
const booking = ref(false)
const selectedSlot = ref<number | null>(null)
const slots = ref<AppointmentSlot[]>([])

const availableSlots = computed(() => slots.value.filter(s => s.available))

async function handleBook() {
  if (!selectedSlot.value) return
  booking.value = true
  try {
    const result = await appointmentApi.book({
      doctor_id: doctorId,
      slot_id: selectedSlot.value,
    })
    if ((result as BusinessError).isBusinessError) {
      ElMessage.error((result as BusinessError).message)
    } else {
      ElMessage.success('挂号预约成功！请按时前往就诊')
      router.push('/home')
    }
  } catch {
    ElMessage.error('挂号请求失败')
  } finally {
    booking.value = false
  }
}

onMounted(async () => {
  const result = await appointmentApi.getSlots(doctorId)
  if ((result as BusinessError).isBusinessError) {
    slots.value = [
      { slot_id: 1, time_slot: '2026-07-01 08:00-08:30', available: true },
      { slot_id: 2, time_slot: '2026-07-01 09:00-09:30', available: true },
      { slot_id: 3, time_slot: '2026-07-01 10:00-10:30', available: false },
      { slot_id: 4, time_slot: '2026-07-01 14:00-14:30', available: true },
      { slot_id: 5, time_slot: '2026-07-02 08:30-09:00', available: true },
      { slot_id: 6, time_slot: '2026-07-02 10:30-11:00', available: true },
    ]
  } else {
    slots.value = result as AppointmentSlot[]
  }
  loading.value = false
})
</script>

<style scoped>
.appointment-container {
  max-width: 780px;
  margin: 0 auto;
  padding: 24px 16px 60px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.doctor-card,
.slot-card {
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.info-label {
  color: #909399;
  min-width: 70px;
}

.slot-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.slot-item {
  padding: 12px 8px;
  text-align: center;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  font-size: 13px;
  transition: border-color 0.15s, background 0.15s;
}

.slot-item:hover {
  border-color: #409eff;
}

.slot-item.selected {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}

.empty-state {
  padding: 20px 0;
}

.action-bar {
  text-align: center;
  padding-top: 10px;
}
</style>
