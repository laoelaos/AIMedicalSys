<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <h2>AI 辅助开方</h2>
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
        <el-form-item label="诊断（必填）">
          <el-input
            v-model="form.diagnosis"
            type="textarea"
            :rows="2"
            placeholder="已确定的诊断"
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
            请求 AI 用药推荐
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>

      <el-divider v-if="result" />

      <div v-if="result" class="result-section">
        <h3>AI 推荐药品</h3>
        <el-table :data="result.drugs" border stripe>
          <el-table-column prop="drug_name" label="药品名称" min-width="140" />
          <el-table-column prop="specification" label="规格" width="120" />
          <el-table-column prop="dosage" label="用量" width="100" />
          <el-table-column prop="frequency" label="频次" width="100" />
          <el-table-column prop="reason" label="推荐理由" min-width="200" />
        </el-table>
        <el-empty v-if="!result.drugs.length" description="暂无推荐药品" />
        <p class="summary-text" v-if="result.summary">
          <strong>综合建议：</strong>{{ result.summary }}
        </p>
        <p v-if="degraded" class="degraded-note">
          注：以上为降级兜底建议（通用药品参考），AI 服务当前不可用。
          降级路径：请医生手动开具处方，参考药品说明书与临床指南。
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
import type { AiPrescriptionAssistRequest, AiPrescriptionAssistResponse, BusinessError } from '@aimedical/shared'
import AiDegradedBanner from '../../components/AiDegradedBanner.vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const degraded = ref(false)
const fallbackReason = ref<string | null>(null)
const result = ref<AiPrescriptionAssistResponse | null>(null)

const form = reactive<AiPrescriptionAssistRequest>({
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
  if (!form.diagnosis.trim()) {
    ElMessage.warning('请输入诊断')
    return
  }

  loading.value = true
  degraded.value = false
  fallbackReason.value = null
  result.value = null

  const payload: AiPrescriptionAssistRequest = {
    ...form,
    patient_id: form.patient_id ? Number(form.patient_id) : null,
  }

  const res = await doctorApi.aiPrescriptionAssist(payload)
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
      ElMessage.success('AI 用药推荐已生成')
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

.summary-text {
  margin-top: 12px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
}

.degraded-note {
  margin-top: 12px;
  color: #92400e;
  font-size: 13px;
}
</style>
