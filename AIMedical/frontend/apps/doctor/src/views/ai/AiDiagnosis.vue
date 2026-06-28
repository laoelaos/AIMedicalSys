<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <h2>AI 辅助诊断</h2>
      </template>

      <AiDegradedBanner :visible="degraded" :reason="fallbackReason" />

      <el-form :model="form" label-position="top" class="ai-form">
        <el-form-item label="患者 ID">
          <el-input
            v-model="form.patient_id"
            placeholder="可选，输入患者档案ID"
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
            placeholder="本次发病经过、症状演变等"
          />
        </el-form-item>
        <el-form-item label="既往史">
          <el-input
            v-model="form.past_history"
            type="textarea"
            :rows="3"
            placeholder="既往疾病、手术史、过敏史等"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            请求 AI 诊断建议
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>

      <el-divider v-if="result" />

      <div v-if="result" class="result-section">
        <h3>AI 诊断建议</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="可能诊断">
            <el-tag
              v-for="d in result.possible_diagnoses"
              :key="d"
              class="diag-tag"
              type="warning"
            >
              {{ d }}
            </el-tag>
            <span v-if="!result.possible_diagnoses.length" class="empty-text">暂无建议</span>
          </el-descriptions-item>
          <el-descriptions-item label="综合分析">
            {{ result.summary || '—' }}
          </el-descriptions-item>
        </el-descriptions>
        <p v-if="degraded" class="degraded-note">
          注：以上为降级兜底建议，AI 服务当前不可用。请医生结合临床判断手动录入诊断。
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
import type { AiDiagnosisRequest, AiDiagnosisResponse, BusinessError } from '@aimedical/shared'
import AiDegradedBanner from '../../components/AiDegradedBanner.vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const degraded = ref(false)
const fallbackReason = ref<string | null>(null)
const result = ref<AiDiagnosisResponse | null>(null)

const form = reactive<AiDiagnosisRequest>({
  patient_id: null,
  chief_complaint: '',
  present_illness: '',
  past_history: '',
})

onMounted(() => {
  // 优先从 sessionStorage 读取长文本（避免 URL 静默截断），读取后清除
  const raw = sessionStorage.getItem('condition_entry_draft')
  if (raw) {
    try {
      const data = JSON.parse(raw)
      if (data.chief_complaint) form.chief_complaint = data.chief_complaint
      if (data.present_illness) form.present_illness = data.present_illness
      if (data.past_history) form.past_history = data.past_history
    } catch {
      // 忽略损坏的 JSON
    }
    sessionStorage.removeItem('condition_entry_draft')
  }
  // patient_id 从 query 读取（短数值，统一 snake_case）
  const pid = route.query.patient_id
  if (pid) form.patient_id = Number(pid)
  // query 回退（兼容旧入口）
  if (route.query.chief_complaint)
    form.chief_complaint = String(route.query.chief_complaint)
  if (route.query.present_illness)
    form.present_illness = String(route.query.present_illness)
  if (route.query.past_history)
    form.past_history = String(route.query.past_history)
})

async function handleSubmit() {
  loading.value = true
  degraded.value = false
  fallbackReason.value = null
  result.value = null

  const payload: AiDiagnosisRequest = {
    ...form,
    patient_id: form.patient_id ? Number(form.patient_id) : null,
  }

  const res = await doctorApi.aiDiagnosis(payload)
  loading.value = false

  if (isBusinessError(res)) {
    ElMessage.error((res as BusinessError).message)
    return
  }

  // res 是 AiResultResponse<AiDiagnosisResponse>
  degraded.value = res.degraded
  fallbackReason.value = res.fallback_reason

  if (!res.success && !res.degraded) {
    ElMessage.error(`AI 调用失败：${res.error_code ?? '未知错误'}`)
    return
  }

  if (res.data) {
    result.value = res.data
    if (res.degraded) {
      ElMessage.warning('AI 不可用，已返回降级兜底建议')
    } else {
      ElMessage.success('AI 诊断建议已生成')
    }
  }
}

function handleReset() {
  form.patient_id = null
  form.chief_complaint = ''
  form.present_illness = ''
  form.past_history = ''
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

.diag-tag {
  margin-right: 8px;
  margin-bottom: 4px;
}

.empty-text {
  color: #999;
}

.degraded-note {
  margin-top: 12px;
  color: #92400e;
  font-size: 13px;
}
</style>
