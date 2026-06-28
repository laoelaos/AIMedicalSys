<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <h2>AI 处方审核</h2>
      </template>

      <AiDegradedBanner :visible="degraded" :reason="fallbackReason" />

      <el-form :model="form" label-position="top" class="ai-form">
        <el-form-item label="处方 ID（必填）">
          <el-input
            v-model="prescriptionIdInput"
            placeholder="输入待审核的处方ID"
            clearable
          />
        </el-form-item>
        <el-form-item label="诊断">
          <el-input
            v-model="form.diagnosis"
            type="textarea"
            :rows="2"
            placeholder="处方对应的诊断"
          />
        </el-form-item>
        <el-form-item label="药品名称列表（每行一个）">
          <el-input
            v-model="drugNamesInput"
            type="textarea"
            :rows="5"
            placeholder="阿莫西林胶囊&#10;布洛芬片&#10;..."
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            请求 AI 处方审核
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>

      <el-divider v-if="result" />

      <div v-if="result" class="result-section">
        <h3>AI 审核结果</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="审核结论">
            <el-tag :type="result.passed ? 'success' : 'danger'" size="large">
              {{ result.passed ? '通过' : '不通过' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="riskTagType(result.risk_level)">
              {{ riskLabel(result.risk_level) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="风险提示" v-if="result.warnings.length">
            <ul class="warning-list">
              <li v-for="(w, i) in result.warnings" :key="i">{{ w }}</li>
            </ul>
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-if="!result.warnings.length" description="无风险提示" />
        <p v-if="degraded" class="degraded-note">
          注：AI 服务当前不可用，以上为降级提示。降级路径：请药师或上级医生进行人工审核，
          核对药品相互作用、禁忌症与剂量后手动确认。
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
import type { AiPrescriptionAuditRequest, AiPrescriptionAuditResponse, BusinessError } from '@aimedical/shared'
import AiDegradedBanner from '../../components/AiDegradedBanner.vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const degraded = ref(false)
const fallbackReason = ref<string | null>(null)
const result = ref<AiPrescriptionAuditResponse | null>(null)

const prescriptionIdInput = ref('')
const drugNamesInput = ref('')

const form = reactive<Omit<AiPrescriptionAuditRequest, 'prescription_id' | 'drug_names'>>({
  diagnosis: '',
})

onMounted(() => {
  const pid = route.query.prescriptionId
  if (pid) prescriptionIdInput.value = String(pid)
})

function riskTagType(level: string): 'success' | 'warning' | 'danger' | 'info' {
  const l = level.toLowerCase()
  if (l.includes('high') || l.includes('高')) return 'danger'
  if (l.includes('medium') || l.includes('中')) return 'warning'
  if (l.includes('low') || l.includes('低')) return 'success'
  return 'info'
}

function riskLabel(level: string): string {
  if (!level) return '—'
  return level
}

async function handleSubmit() {
  const pid = Number(prescriptionIdInput.value)
  if (!pid) {
    ElMessage.warning('请输入处方ID')
    return
  }

  const drugNames = drugNamesInput.value
    .split('\n')
    .map((s) => s.trim())
    .filter((s) => s.length > 0)

  if (drugNames.length === 0) {
    ElMessage.warning('请至少输入一个药品名称')
    return
  }

  loading.value = true
  degraded.value = false
  fallbackReason.value = null
  result.value = null

  const payload: AiPrescriptionAuditRequest = {
    prescription_id: pid,
    diagnosis: form.diagnosis,
    drug_names: drugNames,
  }

  const res = await doctorApi.aiPrescriptionAudit(payload)
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
      ElMessage.warning('AI 不可用，已返回降级提示')
    } else {
      ElMessage.success('AI 审核结果已生成')
    }
  }
}

function handleReset() {
  prescriptionIdInput.value = ''
  drugNamesInput.value = ''
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

.warning-list {
  margin: 0;
  padding-left: 20px;
}

.warning-list li {
  margin-bottom: 4px;
  color: #c45627;
}

.degraded-note {
  margin-top: 12px;
  color: #92400e;
  font-size: 13px;
}
</style>
