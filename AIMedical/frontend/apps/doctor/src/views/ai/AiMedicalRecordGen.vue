<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <h2>AI 病历生成</h2>
      </template>

      <AiDegradedBanner :visible="degraded" :reason="fallbackReason" />

      <el-form :model="form" label-position="top" class="ai-form">
        <el-form-item label="患者 ID（必填）">
          <el-input
            v-model="patientIdInput"
            placeholder="输入患者档案ID"
            clearable
          />
        </el-form-item>
        <el-form-item label="主诉">
          <el-input
            v-model="form.chief_complaint"
            type="textarea"
            :rows="2"
            placeholder="患者主要症状描述"
          />
        </el-form-item>
        <el-form-item label="现病史">
          <el-input
            v-model="form.present_illness"
            type="textarea"
            :rows="4"
            placeholder="本次发病经过"
          />
        </el-form-item>
        <el-form-item label="既往史">
          <el-input
            v-model="form.past_history"
            type="textarea"
            :rows="3"
            placeholder="既往疾病史"
          />
        </el-form-item>
        <el-form-item label="初步诊断">
          <el-input
            v-model="form.diagnosis"
            type="textarea"
            :rows="2"
            placeholder="初步诊断"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            请求 AI 生成病历
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>

      <el-divider v-if="result" />

      <div v-if="result" class="result-section">
        <h3>AI 生成的病历内容</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="主诉">
            {{ result.chief_complaint || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="现病史">
            <pre class="pre-text">{{ result.present_illness || '—' }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="既往史">
            <pre class="pre-text">{{ result.past_history || '—' }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="诊断">
            {{ result.diagnosis || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="治疗方案">
            <pre class="pre-text">{{ result.treatment_plan || '—' }}</pre>
          </el-descriptions-item>
        </el-descriptions>
        <div class="action-bar" v-if="result">
          <el-button
            type="success"
            @click="goToMedicalRecordForm"
          >
            使用此内容填写病历
          </el-button>
        </div>
        <p v-if="degraded" class="degraded-note">
          注：AI 服务当前不可用，以上为降级兜底内容（基于模板填充）。
          降级路径：请医生手动录入病历，或选择病历模板进行填充。
        </p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { doctorApi, isBusinessError } from '@aimedical/shared'
import type { AiMedicalRecordGenRequest, AiMedicalRecordGenResponse, BusinessError } from '@aimedical/shared'
import AiDegradedBanner from '../../components/AiDegradedBanner.vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const degraded = ref(false)
const fallbackReason = ref<string | null>(null)
const result = ref<AiMedicalRecordGenResponse | null>(null)

const patientIdInput = ref('')

const form = reactive<Omit<AiMedicalRecordGenRequest, 'patient_id'>>({
  template_id: null,
  chief_complaint: '',
  present_illness: '',
  past_history: '',
  diagnosis: '',
})

onMounted(() => {
  // 优先从 sessionStorage 读取长文本（避免 URL 静默截断），兼容 query 回退
  const raw = sessionStorage.getItem('condition_entry_draft')
  if (raw) {
    try {
      const data = JSON.parse(raw)
      if (data.chief_complaint) form.chief_complaint = data.chief_complaint
      if (data.present_illness) form.present_illness = data.present_illness
      if (data.past_history) form.past_history = data.past_history
      if (data.diagnosis) form.diagnosis = data.diagnosis
    } catch {
      // 忽略损坏的 JSON
    }
    sessionStorage.removeItem('condition_entry_draft')
  }
  const pid = route.query.patient_id
  if (pid) patientIdInput.value = String(pid)
  if (route.query.chief_complaint)
    form.chief_complaint = String(route.query.chief_complaint)
  if (route.query.present_illness)
    form.present_illness = String(route.query.present_illness)
  if (route.query.past_history)
    form.past_history = String(route.query.past_history)
  if (route.query.diagnosis) form.diagnosis = String(route.query.diagnosis)
})

async function handleSubmit() {
  const pid = Number(patientIdInput.value)
  if (!pid) {
    ElMessage.warning('请输入患者ID')
    return
  }

  loading.value = true
  degraded.value = false
  fallbackReason.value = null
  result.value = null

  const payload: AiMedicalRecordGenRequest = {
    ...form,
    patient_id: pid,
  }

  const res = await doctorApi.aiMedicalRecordGen(payload)
  loading.value = false

  if (isBusinessError(res)) {
    ElMessage.error((res as BusinessError).message)
    return
  }

  degraded.value = res.degraded
  fallbackReason.value = res.fallback_reason

  if (!res.success && !res.degraded) {
    ElMessage.error(`AI 调用失败：${res.error_code ?? '未知错误'}`)
    return
  }

  if (res.data) {
    result.value = res.data
    if (res.degraded) {
      ElMessage.warning('AI 不可用，已返回降级兜底内容')
    } else {
      ElMessage.success('AI 病历内容已生成')
    }
  }
}

function goToMedicalRecordForm() {
  if (!result.value || !patientIdInput.value) return
  const pid = patientIdInput.value
  // 长文本通过 sessionStorage 传递，避免 URL 静默截断（目标页读取后清除）
  sessionStorage.setItem(
    'ai_medical_record_gen',
    JSON.stringify({
      chief_complaint: result.value.chief_complaint,
      present_illness: result.value.present_illness,
      past_history: result.value.past_history,
      diagnosis: result.value.diagnosis,
      treatment_plan: result.value.treatment_plan,
    })
  )
  router.push(`/patient/${pid}/medical-records/new`)
}

function handleReset() {
  patientIdInput.value = ''
  form.chief_complaint = ''
  form.present_illness = ''
  form.past_history = ''
  form.diagnosis = ''
  result.value = null
  degraded.value = false
  fallbackReason.value = null
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.ai-form {
  max-width: 700px;
}

.result-section {
  margin-top: 16px;
}

.pre-text {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
}

.action-bar {
  margin-top: 16px;
  text-align: right;
}

.degraded-note {
  margin-top: 12px;
  color: #92400e;
  font-size: 13px;
}
</style>
