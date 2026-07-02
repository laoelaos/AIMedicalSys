<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>病情录入</h2>
          <span class="patient-hint">患者ID: {{ patientId }}</span>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="主诉" prop="chief_complaint">
          <el-input
            v-model="form.chief_complaint"
            type="textarea"
            :rows="2"
            placeholder="请输入主诉"
          />
        </el-form-item>
        <el-form-item label="现病史">
          <el-input
            v-model="form.present_illness"
            type="textarea"
            :rows="4"
            placeholder="请输入现病史"
          />
        </el-form-item>
        <el-form-item label="既往史">
          <el-input
            v-model="form.past_history"
            type="textarea"
            :rows="4"
            placeholder="请输入既往史"
          />
        </el-form-item>
        <el-form-item label="初步诊断">
          <el-input
            v-model="form.diagnosis"
            type="textarea"
            :rows="2"
            placeholder="请输入初步诊断"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveAsDraft">保存为病历草稿</el-button>
          <el-button type="success" @click="goAiDiagnosis">AI辅助诊断</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const route = useRoute()
const router = useRouter()
const patientId = Number(route.params.patientId)

const formRef = ref<FormInstance>()

const form = reactive({
  chief_complaint: '',
  present_illness: '',
  past_history: '',
  diagnosis: '',
})

const rules: FormRules = {
  chief_complaint: [
    { required: true, message: '主诉不能为空', trigger: 'blur' },
  ],
}

async function saveAsDraft() {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (!valid) {
      ElMessage.warning('请填写主诉后再保存')
      return
    }
    // 长文本通过 sessionStorage 传递，避免 URL 静默截断（目标页读取后清除）
    sessionStorage.setItem('condition_entry_draft', JSON.stringify(buildDraftSnapshot()))
    router.push(`/patient/${patientId}/medical-records/new`)
  })
}

async function goAiDiagnosis() {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (!valid) {
      ElMessage.warning('请填写主诉后再进行 AI 诊断')
      return
    }
    // 长文本通过 sessionStorage 传递，避免 URL 静默截断（目标页读取后清除）
    sessionStorage.setItem('condition_entry_draft', JSON.stringify(buildDraftSnapshot()))
    router.push({
      path: '/ai/diagnosis',
      query: {
        patient_id: String(patientId),
      },
    })
  })
}

/**
 * 构建病情录入草稿快照，供 saveAsDraft 与 goAiDiagnosis 共享字段定义，
 * 避免两条路径字段不一致（如遗漏 diagnosis 导致 AI 诊断页拿不到初步诊断）。
 */
function buildDraftSnapshot() {
  return {
    chief_complaint: form.chief_complaint,
    present_illness: form.present_illness,
    past_history: form.past_history,
    diagnosis: form.diagnosis,
  }
}
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

.patient-hint {
  color: #909399;
  font-size: 14px;
}
</style>
