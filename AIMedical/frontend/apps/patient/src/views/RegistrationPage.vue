<template>
  <div class="registration-container">
    <div class="page-header">
      <el-button type="default" @click="router.push('/home')">← 返回首页</el-button>
      <h2>门诊挂号</h2>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- 在线挂号 -->
      <el-tab-pane label="在线挂号" name="register">
        <el-radio-group v-model="regBranch" class="branch-switch">
          <el-radio-button value="outpatient">门诊预约</el-radio-button>
          <el-radio-button value="exam">检查预约</el-radio-button>
        </el-radio-group>

        <!-- 门诊预约分支 -->
        <template v-if="regBranch === 'outpatient'">
          <el-card class="step-card">
            <template #header><span>选择科室</span></template>
            <div class="dept-grid">
              <el-radio-group v-model="outpatient.deptId" @change="onDeptChange">
                <div v-for="d in mockDepts" :key="d.department_id" class="dept-radio">
                  <el-radio :value="d.department_id">{{ d.department_name }}</el-radio>
                </div>
              </el-radio-group>
            </div>
          </el-card>

          <el-card v-if="outpatient.deptId" class="step-card">
            <template #header><span>选择医生</span></template>
            <div class="doctor-list">
              <div
                v-for="d in mockDoctors"
                :key="d.doctor_id"
                class="doctor-row"
                :class="{ selected: outpatient.doctorId === d.doctor_id }"
                @click="outpatient.doctorId = d.doctor_id; outpatient.doctorName = d.doctor_name"
              >
                <span class="doc-name">{{ d.doctor_name }}</span>
                <el-tag size="small" type="primary">可约 {{ d.available_slot_count }} 号</el-tag>
              </div>
            </div>
          </el-card>

          <el-card v-if="outpatient.doctorId" class="step-card">
            <template #header><span>选择时段</span></template>
            <div class="slot-grid">
              <div
                v-for="s in availableSlots"
                :key="s.slot_id"
                class="slot-cell"
                :class="{ selected: outpatient.slotId === s.slot_id, full: !s.available }"
                @click="outpatient.slotId = s.slot_id; outpatient.slotTime = s.time_slot"
              >
                {{ s.time_slot }}
              </div>
            </div>
          </el-card>

          <el-button
            v-if="outpatient.slotId"
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitting"
            @click="submitOutpatient"
          >
            确认挂号
          </el-button>
        </template>

        <!-- 检查预约分支 -->
        <template v-if="regBranch === 'exam'">
          <el-card class="step-card">
            <template #header><span>检查类别</span></template>
            <el-radio-group v-model="exam.category" @change="exam.itemId = null; exam.slotId = null">
              <el-radio value="影像">影像检查 (CT/MRI/X光)</el-radio>
              <el-radio value="超声">超声检查</el-radio>
              <el-radio value="心电">心电检查</el-radio>
              <el-radio value="检验">检验检查 (血常规/尿常规等)</el-radio>
            </el-radio-group>
          </el-card>

          <el-card v-if="exam.category" class="step-card">
            <template #header><span>检查项目</span></template>
            <div
              v-for="item in getExamItems(exam.category)"
              :key="item.id"
              class="exam-row"
              :class="{ selected: exam.itemId === item.id }"
              @click="exam.itemId = item.id; exam.itemName = item.name"
            >
              <div>
                <div class="exam-name">{{ item.name }}</div>
                <div class="exam-price">¥{{ item.price }}</div>
              </div>
              <el-checkbox :model-value="exam.itemId === item.id" />
            </div>
          </el-card>

          <el-card v-if="exam.itemId" class="step-card">
            <template #header><span>选择时段</span></template>
            <div class="slot-grid">
              <div
                v-for="s in availableSlots"
                :key="s.slot_id"
                class="slot-cell"
                :class="{ selected: exam.slotId === s.slot_id, full: !s.available }"
                @click="exam.slotId = s.slot_id; exam.slotTime = s.time_slot"
              >
                {{ s.time_slot }}
              </div>
            </div>
          </el-card>

          <el-button
            v-if="exam.slotId && exam.itemId"
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitting"
            @click="submitExam"
          >
            确认预约
          </el-button>
        </template>
      </el-tab-pane>

      <!-- 我的挂号 -->
      <el-tab-pane label="我的挂号" name="records" lazy>

        <!-- 待确认 -->
        <template v-if="pendingRegs.length">
          <h4 class="section-title">待确认</h4>
          <div v-for="r in pendingRegs" :key="r.id" class="record-card">
            <template v-if="cancellingId === r.id">
              <div class="cancel-progress">
                <div v-if="cancelResult">
                  <el-alert
                    :type="cancelResult.success ? 'success' : 'error'"
                    :title="cancelResult.success ? '取消成功' : '取消失败'"
                    :description="cancelResult.message"
                    show-icon
                    :closable="false"
                  />
                  <div v-if="cancelResult.refund_amount" class="refund-info">
                    💳 已申请退费，待窗口处理  ¥{{ cancelResult.refund_amount }}
                  </div>
                  <el-button size="small" class="back-btn" @click="dismissCancel">返回</el-button>
                </div>
                <div v-else class="cancel-confirm">
                  <div v-if="cancelOverWindow" class="cancel-over">
                    <el-alert type="error" show-icon :closable="false"
                      title="已超过自助取消时间窗"
                      description="距就诊时间不足 2 小时，请联系线下窗口办理取消"
                    />
                    <el-button size="small" class="back-btn" @click="dismissCancel">返回</el-button>
                  </div>
                  <div v-else class="cancel-warn">
                    <p>确认取消「{{ r.doctor_name || r.exam_item_name }}」的挂号？</p>
                    <p class="cancel-slot-time">{{ r.time_slot }}</p>
                    <div class="cancel-actions">
                      <el-button size="small" @click="dismissCancel">不取消</el-button>
                      <el-button size="small" type="danger" :loading="cancellingActive" @click="doCancel(r)">确认取消</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="record-header">
                <el-tag :type="r.registration_type === 'OUTPATIENT' ? 'primary' : 'success'" size="small">
                  {{ r.registration_type === 'OUTPATIENT' ? '门诊' : '检查' }}
                </el-tag>
                <el-tag type="warning" size="small">待确认</el-tag>
              </div>
              <div class="record-body">
                <p v-if="r.doctor_name"><strong>医生：</strong>{{ r.doctor_name }}（{{ r.department_name }}）</p>
                <p v-if="r.exam_item_name"><strong>检查项目：</strong>{{ r.exam_item_name }}</p>
                <p><strong>时段：</strong>{{ r.time_slot }}</p>
                <p class="record-time">创建于 {{ r.created_at }}</p>
              </div>
              <div class="record-actions">
                <el-button type="danger" text size="small" :disabled="cancellingId !== null" @click="startCancel(r)">
                  取消挂号
                </el-button>
              </div>
            </template>
          </div>
        </template>

        <!-- 已挂号 -->
        <template v-if="confirmedRegs.length">
          <h4 class="section-title section-confirmed">已挂号</h4>
          <div v-for="r in confirmedRegs" :key="r.id" class="record-card confirmed">
            <div class="record-header">
              <el-tag :type="r.registration_type === 'OUTPATIENT' ? 'primary' : 'success'" size="small">
                {{ r.registration_type === 'OUTPATIENT' ? '门诊' : '检查' }}
              </el-tag>
              <el-tag type="success" size="small">已确认</el-tag>
            </div>
            <div class="record-body">
              <p v-if="r.doctor_name"><strong>医生：</strong>{{ r.doctor_name }}（{{ r.department_name }}）</p>
              <p v-if="r.exam_item_name"><strong>检查项目：</strong>{{ r.exam_item_name }}</p>
              <p><strong>时段：</strong>{{ r.time_slot }}</p>
              <p class="record-time">创建于 {{ r.created_at }}</p>
            </div>
          </div>
        </template>

        <!-- 挂号历史 -->
        <template v-if="historyRegs.length">
          <h4 class="section-title">挂号历史</h4>
          <div v-for="r in historyRegs" :key="r.id" class="record-card history">
            <div class="record-header">
              <el-tag :type="r.registration_type === 'OUTPATIENT' ? 'primary' : 'success'" size="small">
                {{ r.registration_type === 'OUTPATIENT' ? '门诊' : '检查' }}
              </el-tag>
              <el-tag :type="r.status === 'CANCELLED' ? 'info' : ''" size="small">{{ r.status === 'CANCELLED' ? '已取消' : '已发药' }}</el-tag>
            </div>
            <div class="record-body">
              <p v-if="r.doctor_name"><strong>医生：</strong>{{ r.doctor_name }}（{{ r.department_name }}）</p>
              <p v-if="r.exam_item_name"><strong>检查项目：</strong>{{ r.exam_item_name }}</p>
              <p><strong>时段：</strong>{{ r.time_slot }}</p>
              <p class="record-time">创建于 {{ r.created_at }}</p>
            </div>
            <div v-if="r.status === 'DISPENSED' && r.registration_type === 'OUTPATIENT'" class="record-hint">
              已发药完成，如需退费请携带处方至线下窗口办理
            </div>
          </div>
        </template>

        <el-empty v-if="registrations.length === 0" description="暂无挂号记录" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registrationApi, type RegistrationRecord, type AppointmentSlot, type BusinessError } from '@aimedical/shared'

const router = useRouter()
const activeTab = ref('register')
const regBranch = ref<'outpatient' | 'exam'>('outpatient')
const submitting = ref(false)
const cancellingId = ref<number | null>(null)
const cancellingActive = ref(false)
const cancelOverWindow = ref(false)
const cancelResult = ref<{ success: boolean; message: string; refund_amount?: number } | null>(null)

// Module-level state: persists across tab switches within the same session
const sessionRegistrations = ref<RegistrationRecord[]>(getMocks())

function getMocks(): RegistrationRecord[] {
  return [
    {
      id: 1001, registration_type: 'OUTPATIENT', doctor_name: '王主任', department_name: '神经内科',
      time_slot: '07-01 08:00-08:30', status: 'PENDING', created_at: '2026-06-29 10:30', can_cancel: true,
    },
    {
      id: 1002, registration_type: 'EXAMINATION', exam_item_name: '头颅 CT',
      time_slot: '07-02 10:30-11:00', status: 'PENDING', created_at: '2026-06-29 14:00', can_cancel: true,
    },
    {
      id: 1003, registration_type: 'OUTPATIENT', doctor_name: '李主治医师', department_name: '普通内科',
      time_slot: '07-01 15:00-15:30', status: 'DISPENSED', created_at: '2026-06-28 09:00', can_cancel: false,
    },
  ]
}

const registrations = sessionRegistrations

const pendingRegs = computed(() =>
  registrations.value.filter(r =>
    r.status === 'PENDING' || r.id === cancellingId.value
  )
)
const confirmedRegs = computed(() =>
  registrations.value.filter(r =>
    r.status === 'CONFIRMED' && r.id !== cancellingId.value
  )
)
const historyRegs = computed(() =>
  registrations.value.filter(r =>
    (r.status === 'CANCELLED' || r.status === 'DISPENSED') && r.id !== cancellingId.value
  )
)

const mockDepts = [
  { department_id: 1, department_name: '神经内科', score: 0 },
  { department_id: 2, department_name: '心内科', score: 0 },
  { department_id: 3, department_name: '消化内科', score: 0 },
  { department_id: 4, department_name: '骨科', score: 0 },
  { department_id: 5, department_name: '眼科', score: 0 },
  { department_id: 6, department_name: '皮肤科', score: 0 },
]

const mockDoctors = [
  { doctor_id: 201, doctor_name: '王主任', available_slot_count: 5, score: 95 },
  { doctor_id: 202, doctor_name: '张副主任', available_slot_count: 3, score: 82 },
  { doctor_id: 203, doctor_name: '李主治医师', available_slot_count: 8, score: 70 },
]

const mockSlots: AppointmentSlot[] = [
  { slot_id: 1, time_slot: '07-01 08:00-08:30', available: true },
  { slot_id: 2, time_slot: '07-01 09:00-09:30', available: true },
  { slot_id: 3, time_slot: '07-01 10:00-10:30', available: true },
  { slot_id: 4, time_slot: '07-01 14:00-14:30', available: false },
  { slot_id: 5, time_slot: '07-02 08:30-09:00', available: true },
  { slot_id: 6, time_slot: '07-02 10:30-11:00', available: true },
]

const mockExamItems: Record<string, { id: number; name: string; price: number }[]> = {
  '影像': [
    { id: 301, name: '头颅 CT', price: 260 },
    { id: 302, name: '胸部 X 光', price: 120 },
    { id: 303, name: '腰椎 MRI', price: 580 },
  ],
  '超声': [
    { id: 304, name: '腹部彩超', price: 180 },
    { id: 305, name: '甲状腺超声', price: 150 },
    { id: 306, name: '心脏彩超', price: 300 },
  ],
  '心电': [
    { id: 307, name: '常规心电图', price: 50 },
    { id: 308, name: '24h 动态心电图', price: 200 },
  ],
  '检验': [
    { id: 309, name: '血常规', price: 30 },
    { id: 310, name: '尿常规', price: 20 },
    { id: 311, name: '生化全套', price: 280 },
  ],
}

const outpatient = reactive({
  deptId: null as number | null,
  deptName: '',
  doctorId: null as number | null,
  doctorName: '',
  slotId: null as number | null,
  slotTime: '',
})

const exam = reactive({
  category: null as string | null,
  itemId: null as number | null,
  itemName: '',
  slotId: null as number | null,
  slotTime: '',
})

const availableSlots = computed(() => mockSlots)

function onDeptChange(val: number | null) {
  const dept = mockDepts.find(d => d.department_id === val)
  outpatient.deptName = dept?.department_name || ''
  outpatient.doctorId = null
  outpatient.slotId = null
}

function getExamItems(cat: string) {
  return mockExamItems[cat] || []
}

async function submitOutpatient() {
  if (!outpatient.doctorId || !outpatient.slotId) {
    ElMessage.warning('请完整选择科室、医生和时段')
    return
  }
  submitting.value = true
  try {
    const result = await registrationApi.create({
      registration_type: 'OUTPATIENT',
      doctor_id: outpatient.doctorId ?? undefined,
      doctor_name: outpatient.doctorName,
      department_id: outpatient.deptId ?? undefined,
      department_name: outpatient.deptName,
      time_slot_id: outpatient.slotId ?? undefined,
      time_slot: outpatient.slotTime,
    })
    if ((result as BusinessError).isBusinessError) {
      ElMessage.error((result as BusinessError).message)
    } else {
      ElMessage.success('门诊挂号成功！')
      router.push('/home')
    }
  } catch {
    ElMessage.error('挂号请求失败')
  } finally {
    submitting.value = false
  }
}

async function submitExam() {
  if (!exam.itemId) {
    ElMessage.error('预约检查失败：未选择检查项目 (exam_item_id 缺失)')
    return
  }
  submitting.value = true
  try {
    const result = await registrationApi.create({
      registration_type: 'EXAMINATION',
      exam_item_id: exam.itemId ?? undefined,
      exam_item_name: exam.itemName,
      exam_category: exam.category ?? undefined,
      time_slot_id: exam.slotId ?? undefined,
      time_slot: exam.slotTime,
    })
    if ((result as BusinessError).isBusinessError) {
      ElMessage.error((result as BusinessError).message)
    } else {
      ElMessage.success('检查预约成功！')
      router.push('/home')
    }
  } catch {
    ElMessage.error('挂号请求失败')
  } finally {
    submitting.value = false
  }
}

function statusType(status: string): '' | 'success' | 'info' | 'warning' | 'danger' {
  const map: Record<string, '' | 'success' | 'info' | 'warning' | 'danger'> = {
    PENDING: 'warning', CONFIRMED: 'success', CANCELLED: 'info', DISPENSED: '',
  }
  return map[status] || 'info'
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待确认', CONFIRMED: '已确认', CANCELLED: '已取消', DISPENSED: '已发药',
  }
  return map[status] || status
}

function startCancel(r: RegistrationRecord) {
  cancelResult.value = null
  cancelOverWindow.value = false
  cancellingId.value = r.id
}

function dismissCancel() {
  cancellingId.value = null
  cancelResult.value = null
  cancelOverWindow.value = false
}

async function doCancel(r: RegistrationRecord) {
  const slotTime = new Date('2026-' + r.time_slot.substring(0, 11))
  const now = new Date()
  const diffHours = (slotTime.getTime() - now.getTime()) / 3600000

  if (diffHours < 2) {
    cancelOverWindow.value = true
    return
  }

  cancellingActive.value = true
  const result = await registrationApi.cancel(r.id)
  if (!(result as BusinessError).isBusinessError) {
    const data = result as { success: boolean; message: string; refund_amount?: number; over_window?: boolean }
    if (data.success) {
      r.status = 'CANCELLED'
      r.can_cancel = false
    }
    cancelResult.value = { success: data.success, message: data.message, refund_amount: data.refund_amount }
  } else {
    cancelResult.value = { success: false, message: (result as BusinessError).message }
  }
  cancellingActive.value = false
}

onMounted(async () => {
  try {
    const result = await registrationApi.list()
    if (!(result as BusinessError).isBusinessError) {
      const raw = result as { id: number; registration_type: string; doctor_name?: string; department_name?: string; exam_item_name?: string; time_slot: string; status: string; can_cancel: boolean; created_at: string }[]
      sessionRegistrations.value = raw.map(r => ({
        id: r.id,
        registration_type: r.registration_type as 'OUTPATIENT' | 'EXAMINATION',
        doctor_name: r.doctor_name,
        department_name: r.department_name,
        exam_item_name: r.exam_item_name,
        time_slot: r.time_slot,
        status: r.status as 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'DISPENSED',
        can_cancel: r.can_cancel,
        created_at: r.created_at,
      }))
    }
  } catch { /* keep mock defaults */ }
})
</script>

<style scoped>
.registration-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 16px 60px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h2 { margin: 0; }

.branch-switch { margin-bottom: 16px; display: block; }

.step-card { margin-bottom: 14px; }

.dept-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.dept-radio { margin-right: 20px; }

.doctor-list { display: flex; flex-direction: column; gap: 8px; }

.doctor-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px; border: 1px solid #ebeef5; border-radius: 8px; cursor: pointer;
  transition: border-color 0.15s;
}
.doctor-row:hover, .doctor-row.selected { border-color: #409eff; background: #ecf5ff; }
.doc-name { font-size: 15px; }

.slot-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;
}

.slot-cell {
  padding: 10px 6px; text-align: center; border-radius: 6px;
  border: 1px solid #dcdfe6; cursor: pointer; font-size: 12px;
  transition: border-color 0.15s;
}
.slot-cell:hover:not(.full) { border-color: #409eff; }
.slot-cell.selected { border-color: #409eff; background: #ecf5ff; color: #409eff; font-weight: 500; }
.slot-cell.full { background: #f5f5f5; color: #c0c4cc; cursor: not-allowed; }

.exam-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px; border: 1px solid #ebeef5; border-radius: 8px; cursor: pointer;
  margin-bottom: 6px; transition: border-color 0.15s;
}
.exam-row:hover, .exam-row.selected { border-color: #409eff; background: #ecf5ff; }
.exam-name { font-size: 15px; }
.exam-price { font-size: 13px; color: #f56c6c; margin-top: 2px; }

.submit-btn { margin-top: 12px; width: 100%; }

.record-card {
  border: 1px solid #ebeef5; border-radius: 8px; padding: 14px; margin-bottom: 10px;
}
.record-card.history { opacity: 0.75; }
.record-card.confirmed { border-left: 3px solid #67C23A; }
.record-header { display: flex; justify-content: space-between; margin-bottom: 8px; }
.record-body p { margin: 4px 0; font-size: 14px; }
.record-time { color: #909399; font-size: 12px; }
.record-hint { margin-top: 8px; padding: 8px; background: #fdf6ec; border-radius: 6px; font-size: 13px; color: #b88230; }
.record-actions { margin-top: 8px; text-align: right; }

.section-title {
  font-size: 16px; margin: 6px 0 12px; padding-bottom: 6px; border-bottom: 2px solid #409eff;
}
.section-confirmed { border-bottom-color: #67C23A; }

.cancel-progress { padding: 4px 0; }
.cancel-active { border-color: #e6a23c; background: #fdf6ec; }
.cancel-slot-time { font-size: 13px; color: #909399; margin-bottom: 10px; }
.cancel-over { margin-bottom: 8px; }
.cancel-actions { display: flex; gap: 8px; }
.back-btn { margin-top: 8px; }

.refund-info {
  display: flex; align-items: center; gap: 8px;
  margin-top: 10px; padding: 10px; background: #f0f9eb; border-radius: 8px;
  font-size: 14px;
}

.back-btn { margin-top: 8px; }
.refund-icon { font-size: 20px; }
</style>
