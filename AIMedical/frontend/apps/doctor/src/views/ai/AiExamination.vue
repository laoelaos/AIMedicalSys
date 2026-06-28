<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <h2>AI 开立检查推荐</h2>
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
        <el-form-item label="初步诊断">
          <el-input
            v-model="form.diagnosis"
            type="textarea"
            :rows="2"
            placeholder="已确定的诊断或疑似诊断"
          />
        </el-form-item>
        <el-form-item label="主诉">
          <el-input
            v-model="form.chief_complaint"
            type="textarea"
            :rows="2"
            placeholder="患者主诉"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            请求 AI 检查推荐
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>

      <el-divider v-if="result" />

      <div v-if="result" class="result-section">
        <h3>AI 推荐检查项目</h3>
        <el-table :data="result.items" border stripe>
          <el-table-column prop="name" label="检查项目" min-width="160" />
          <el-table-column prop="category" label="类别" width="140" />
          <el-table-column prop="reason" label="推荐理由" min-width="240" />
        </el-table>
        <el-empty v-if="!result.items.length" description="暂无推荐" />
        <p v-if="degraded" class="degraded-note">
          注：以上为降级兜底建议（通用检查项），AI 服务当前不可用。请医生根据临床需要手动开立检查。
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
import type { AiExaminationRequest, AiExaminationResponse, BusinessError } from '@aimedical/shared'
import AiDegradedBanner from '../../components/AiDegradedBanner.vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const degraded = ref(false)
const fallbackReason = ref<string | null>(null)
const result = ref<AiExaminationResponse | null>(null)

const form = reactive<AiExaminationRequest>({
  patient_id: null,
  diagnosis: '',
  chief_complaint: '',
})

onMounted(() => {
  const pid = route.query.patientId
  if (pid) form.patient_id = Number(pid)
  if (route.query.diagnosis) form.diagnosis = String(route.query.diagnosis)
  if (route.query.chief_complaint)
    form.chief_complaint = String(route.query.chief_complaint)
})

async function handleSubmit() {
  loading.value = true
  degraded.value = false
  fallbackReason.value = null
  result.value = null

  const payload: AiExaminationRequest = {
    ...form,
    patient_id: form.patient_id ? Number(form.patient_id) : null,
  }

  const res = await doctorApi.aiExamination(payload)
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
      ElMessage.warning('AI 不可用，已返回降级兜底建议')
    } else {
      ElMessage.success('AI 检查推荐已生成')
    }
  }
}

function handleReset() {
  form.patient_id = null
  form.diagnosis = ''
  form.chief_complaint = ''
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

.degraded-note {
  margin-top: 12px;
  color: #92400e;
  font-size: 13px;
}
</style>
