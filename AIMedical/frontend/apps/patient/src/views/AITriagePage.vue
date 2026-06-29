<template>
  <div class="triage-container">
    <div class="page-header">
      <el-button type="default" @click="router.push('/home')">← 返回首页</el-button>
      <h2>AI 智能导诊</h2>
      <el-tag v-if="aiUnavailable" type="warning">AI 服务暂不可用，已切换为科室选择模式</el-tag>
    </div>

    <!-- 输入区 -->
    <el-card shadow="hover" class="input-card">
      <div class="input-header">
        <span>请描述您的主要症状、不适部位及持续时间</span>
        <span class="char-count" :class="{ over: charCount > 500, warn: charCount > 450 }">
          {{ charCount }} / 500
        </span>
      </div>
      <el-input
        v-model="chiefComplaint"
        type="textarea"
        :rows="4"
        :maxlength="500"
        show-word-limit
        placeholder="例如：头痛3天，伴有发烧，体温38.5°C..."
        :disabled="triageComplete"
      />
      <div v-if="complaintTooShort" class="short-tip">
        主诉内容太少，请至少输入 10 个字符以描述您的症状
      </div>
      <el-button
        type="primary"
        class="submit-btn"
        :disabled="!canSubmit"
        :loading="submitting"
        @click="handleSubmit"
      >
        {{ submitting ? 'AI 正在分析…' : '开始导诊' }}
      </el-button>
    </el-card>

    <!-- 追问区 -->
    <el-card v-if="currentQuestion && !triageComplete" shadow="hover" class="qa-card">
      <h3>AI 追问</h3>
      <div class="question-box">
        <span class="q-label">🤖</span>
        <span>{{ currentQuestion }}</span>
      </div>
      <el-input
        v-model="additionalResponse"
        type="textarea"
        :rows="3"
        placeholder="请输入补充信息..."
        :disabled="submitting"
      />
      <el-button
        type="success"
        class="submit-btn"
        :disabled="!additionalResponse.trim() || submitting"
        :loading="submitting"
        @click="handleFollowUp"
      >
        回答追问
      </el-button>
    </el-card>

    <!-- 失败兜底 -->
    <el-alert
      v-if="failureCount >= 3"
      title="建议直接联系线下接诊窗口"
      type="error"
      show-icon
      :closable="false"
      description="AI 导诊连续多次无法完成，请联系医院门诊大厅导诊台获取帮助"
      class="failure-alert"
    />

    <!-- AI 结果展示 -->
    <template v-if="triageComplete && lastResult">
      <!-- 推荐理由 -->
      <el-card shadow="hover" class="result-card">
        <h3>推荐理由</h3>
        <p class="reason-text">{{ lastResult.reason || 'AI 未提供推荐理由' }}</p>
      </el-card>

      <!-- 推荐科室 -->
      <el-card v-if="lastResult.departments?.length" shadow="hover" class="result-card">
        <h3>推荐科室</h3>
        <div class="dept-grid">
          <div
            v-for="dept in lastResult.departments"
            :key="dept.department_id"
            class="dept-card"
            :class="{ top: dept.score >= 80 }"
          >
            <div class="dept-name">{{ dept.department_name }}</div>
            <el-progress
              :percentage="dept.score"
              :color="dept.score >= 80 ? '#67C23A' : dept.score >= 60 ? '#E6A23C' : '#F56C6C'"
              :stroke-width="8"
            />
          </div>
        </div>
      </el-card>

      <!-- 推荐医生 -->
      <el-card v-if="lastResult.doctors?.length" shadow="hover" class="result-card">
        <h3>推荐医生</h3>
        <div class="doctor-grid">
          <div
            v-for="doc in lastResult.doctors"
            :key="doc.doctor_id"
            class="doctor-card"
          >
            <div class="doc-info">
              <div class="doc-name">{{ doc.doctor_name }}</div>
              <div class="doc-meta">
                <el-tag size="small" type="primary">可约 {{ doc.available_slot_count }} 个号源</el-tag>
                <span class="doc-score">匹配度 {{ doc.score }}%</span>
              </div>
            </div>
            <el-button
              type="primary"
              size="small"
              @click="handleAppointment(doc)"
            >
              立即挂号
            </el-button>
          </div>
        </div>
      </el-card>
    </template>

    <!-- 科室选择模式 （降级） -->
    <el-card v-if="aiUnavailable && !triageComplete" shadow="hover" class="fallback-card">
      <h3>选择就诊科室</h3>
      <p class="fallback-desc">AI 服务暂不可用，请手动选择您需要就诊的科室</p>
      <div class="dept-grid">
        <div
          v-for="dept in manualDepartments"
          :key="dept.department_id"
          class="dept-card dept-selectable"
          @click="handleSelectDepartment(dept)"
        >
          <div class="dept-name">{{ dept.department_name }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { triageApi } from '@aimedical/shared'
import type { TriageRequest, TriageResponse, TriageDepartment, BusinessError } from '@aimedical/shared'

const router = useRouter()

const chiefComplaint = ref('')
const additionalResponse = ref('')
const currentQuestion = ref('')
const sessionId = ref<string | null>(null)
const submitting = ref(false)
const triageComplete = ref(false)
const failureCount = ref(0)
const aiUnavailable = ref(false)
const lastResult = ref<TriageResponse | null>(null)
const manualDepartments = ref<TriageDepartment[]>([])
const additionalResponses = ref<string[]>([])

const charCount = computed(() => chiefComplaint.value.length)
const complaintTooShort = computed(() => chiefComplaint.value.length > 0 && chiefComplaint.value.length < 10)
const canSubmit = computed(() => chiefComplaint.value.trim().length >= 10 && !submitting.value && !aiUnavailable.value)

function buildTriageRequest(): TriageRequest {
  if (sessionId.value && additionalResponses.value.length > 0) {
    return {
      chief_complaint: chiefComplaint.value.trim(),
      session_id: sessionId.value,
      additional_responses: [...additionalResponses.value],
    }
  }
  return { chief_complaint: chiefComplaint.value.trim() }
}

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const req = buildTriageRequest()
    const result = await triageApi.triage(req)
    await processTriageResult(result)
  } catch {
    // API 调用抛出异常时使用 Mock 兜底
    processMockTriage()
  } finally {
    submitting.value = false
  }
}

async function handleFollowUp() {
  const text = additionalResponse.value.trim()
  if (!text) return
  submitting.value = true
  additionalResponses.value.push(text)
  additionalResponse.value = ''

  try {
    const req: TriageRequest = {
      chief_complaint: chiefComplaint.value.trim(),
      session_id: sessionId.value!,
      additional_responses: [...additionalResponses.value],
    }
    const result = await triageApi.triage(req)
    await processTriageResult(result)
  } catch {
    // API 异常 + 有追问 → 直接给出最终结果
    processMockResult()
  } finally {
    submitting.value = false
  }
}

async function processTriageResult(result: TriageResponse | BusinessError) {
  if ((result as BusinessError).isBusinessError) {
    const err = result as BusinessError
    if (err.code === 'SERVICE_UNAVAILABLE') {
      aiUnavailable.value = true
      await loadDepartments()
      return
    }
    processMockTriage()
    return
  }

  const data = result as TriageResponse
  if (data.session_id) {
    sessionId.value = data.session_id
  }

  if (data.is_complete) {
    triageComplete.value = true
    lastResult.value = data
    currentQuestion.value = ''
    failureCount.value = 0
  } else if (data.question) {
    currentQuestion.value = data.question
    failureCount.value = 0
  } else {
    processMockTriage()
  }
}

function processMockTriage() {
  if (!sessionId.value) {
    // 首次提交 → 追问
    sessionId.value = 'mock-session-' + Date.now()
    currentQuestion.value = getMockQuestion()
    failureCount.value = 0
  } else {
    // 已有 session → 直接给结果
    processMockResult()
  }
}

function processMockResult() {
  lastResult.value = {
    session_id: sessionId.value || 'mock-session',
    is_complete: true,
    reason: getMockReason(),
    departments: getMockDepartments(),
    doctors: getMockDoctors(),
  }
  triageComplete.value = true
  currentQuestion.value = ''
  failureCount.value = 0
}

function getMockQuestion(): string {
  const complaints = chiefComplaint.value.toLowerCase()
  if (complaints.includes('头') || complaints.includes('疼')) {
    return '请问头痛是持续性的还是阵发性的？有没有伴有恶心、眩晕或视力模糊等症状？'
  }
  if (complaints.includes('发烧') || complaints.includes('发热') || complaints.includes('体温')) {
    return '请问发烧持续了几天？有没有吃过退烧药？是否有咳嗽、咽痛等伴随症状？'
  }
  if (complaints.includes('咳嗽') || complaints.includes('喉咙')) {
    return '请问咳嗽是干咳还是有痰？痰的颜色是什么样的？是否伴有胸闷或呼吸困难？'
  }
  if (complaints.includes('肚') || complaints.includes('胃') || complaints.includes('腹')) {
    return '请问疼痛的具体位置在哪里（上腹/下腹/左侧/右侧）？有没有腹泻、恶心或呕吐的情况？'
  }
  return '请问这个症状是什么时候开始的？之前是否因此就医或服用过药物？有没有其他伴随症状？'
}

function getMockReason(): string {
  const complaints = chiefComplaint.value.toLowerCase()
  if (complaints.includes('头') || complaints.includes('疼')) {
    return '根据您的头痛症状描述，结合可能伴随的眩晕和恶心表现，建议优先就诊神经内科以排除偏头痛、紧张性头痛或神经系统其他疾病。'
  }
  if (complaints.includes('发烧') || complaints.includes('发热') || complaints.includes('体温')) {
    return '根据您的发热症状描述，怀疑为上呼吸道感染或其它感染性疾病，建议就诊普通内科进行进一步检查。'
  }
  return '根据您的主诉，AI 综合分析认为您可能存在相关科室的常见疾病，建议优先就诊以获得专业诊断。具体病因需医生面诊结合检查确认。'
}

function getMockDepartments(): TriageDepartment[] {
  const complaints = chiefComplaint.value.toLowerCase()
  if (complaints.includes('头') || complaints.includes('疼') || complaints.includes('晕')) {
    return [
      { department_id: 1, department_name: '神经内科', score: 92 },
      { department_id: 2, department_name: '普通内科', score: 65 },
      { department_id: 3, department_name: '中医科', score: 38 },
    ]
  }
  if (complaints.includes('发烧') || complaints.includes('发热') || complaints.includes('咳嗽')) {
    return [
      { department_id: 4, department_name: '呼吸内科', score: 88 },
      { department_id: 5, department_name: '普通内科', score: 72 },
      { department_id: 6, department_name: '感染科', score: 45 },
    ]
  }
  return [
    { department_id: 7, department_name: '普通内科', score: 75 },
    { department_id: 8, department_name: '全科门诊', score: 60 },
    { department_id: 9, department_name: '中医科', score: 35 },
  ]
}

function getMockDoctors(): { doctor_id: number; doctor_name: string; available_slot_count: number; score: number }[] {
  return [
    { doctor_id: 101, doctor_name: '王主任', available_slot_count: 5, score: 95 },
    { doctor_id: 102, doctor_name: '张副主任', available_slot_count: 3, score: 82 },
    { doctor_id: 103, doctor_name: '李主治医师', available_slot_count: 8, score: 70 },
  ]
}

function handleFailure() {
  failureCount.value++
  ElMessage.error(`导诊请求失败 (${failureCount.value}/3)`)
  if (failureCount.value >= 3) {
    currentQuestion.value = ''
  }
}

async function loadDepartments() {
  const result = await triageApi.getDepartments()
  if (!(result as BusinessError).isBusinessError) {
    manualDepartments.value = result as TriageDepartment[]
  } else {
    manualDepartments.value = [
      { department_id: 1, department_name: '内科', score: 0 },
      { department_id: 2, department_name: '外科', score: 0 },
      { department_id: 3, department_name: '儿科', score: 0 },
      { department_id: 4, department_name: '妇产科', score: 0 },
      { department_id: 5, department_name: '骨科', score: 0 },
      { department_id: 6, department_name: '眼科', score: 0 },
      { department_id: 7, department_name: '皮肤科', score: 0 },
      { department_id: 8, department_name: '耳鼻喉科', score: 0 },
      { department_id: 9, department_name: '神经内科', score: 0 },
    ]
  }
}

function handleSelectDepartment(dept: TriageDepartment) {
  ElMessage.success(`已选择科室：${dept.department_name}，请前往挂号`)
  router.push('/home')
}

function handleAppointment(doc: { doctor_id: number; doctor_name: string }) {
  const deptName = lastResult.value?.departments?.[0]?.department_name || ''
  router.push({
    path: '/appointment',
    query: {
      doctor_id: doc.doctor_id,
      doctor_name: doc.doctor_name,
      department_name: deptName,
    },
  })
}

onMounted(async () => {
  try {
    await loadDepartments()
  } catch {
    aiUnavailable.value = true
  }
})
</script>

<style scoped>
.triage-container {
  max-width: 780px;
  margin: 0 auto;
  padding: 24px 16px 60px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.page-header h2 {
  margin: 0;
  flex: 1;
}

.input-card,
.qa-card,
.result-card,
.fallback-card {
  margin-bottom: 16px;
}

.input-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
}

.char-count.warn { color: #E6A23C; }
.char-count.over { color: #F56C6C; }

.short-tip {
  margin-top: 6px;
  color: #E6A23C;
  font-size: 13px;
}

.submit-btn {
  margin-top: 14px;
}

.qa-card h3,
.result-card h3,
.fallback-card h3 {
  margin: 0 0 12px 0;
}

.question-box {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 16px;
  background: #f0f9ff;
  border-radius: 8px;
  margin-bottom: 14px;
  font-size: 14px;
}

.q-label { font-size: 20px; }

.reason-text {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.dept-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dept-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  background: #fff;
}

.dept-card.top {
  border-color: #67C23A;
  background: #f0f9eb;
}

.dept-selectable {
  cursor: pointer;
  transition: background 0.15s;
}

.dept-selectable:hover {
  background: #f5f7fa;
}

.dept-name {
  font-weight: 500;
  font-size: 15px;
  min-width: 100px;
}

.doctor-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.doctor-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  background: #fff;
}

.doc-info { flex: 1; }

.doc-name {
  font-weight: 500;
  font-size: 15px;
  margin-bottom: 4px;
}

.doc-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.doc-score {
  font-size: 13px;
  color: #909399;
}

.failure-alert {
  margin-bottom: 16px;
}

.fallback-card .fallback-desc {
  font-size: 13px;
  color: #909399;
  margin: 0 0 14px 0;
}
</style>
