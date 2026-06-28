<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <h2>{{ isEditing ? '病历编辑' : '病历录入' }}</h2>
          <span class="patient-hint">患者ID: {{ patientId }}</span>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="模板">
          <el-select
            v-model="selectedTemplateId"
            placeholder="选择模板（可选）"
            clearable
            style="width: 100%"
            @change="applyTemplate"
          >
            <el-option
              v-for="tpl in templates"
              :key="tpl.id"
              :label="tpl.name"
              :value="tpl.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="主诉" prop="chief_complaint">
          <el-input v-model="form.chief_complaint" type="textarea" :rows="2" placeholder="请输入主诉" />
        </el-form-item>

        <el-form-item label="现病史">
          <el-input v-model="form.present_illness" type="textarea" :rows="4" placeholder="请输入现病史" />
        </el-form-item>

        <el-form-item label="既往史">
          <el-input v-model="form.past_history" type="textarea" :rows="4" placeholder="请输入既往史" />
        </el-form-item>

        <el-form-item label="诊断">
          <el-input v-model="form.diagnosis" type="textarea" :rows="2" placeholder="请输入诊断" />
        </el-form-item>

        <el-form-item label="治疗方案">
          <el-input v-model="form.treatment_plan" type="textarea" :rows="3" placeholder="请输入治疗方案" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit(false)">保存草稿</el-button>
          <el-button type="success" :loading="loading" @click="handleSubmit(true)">保存并发布</el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { doctorApi, isBusinessError } from '@aimedical/shared'
import type { MedicalRecordTemplateResponse } from '@aimedical/shared'

const route = useRoute()
const router = useRouter()
const patientId = Number(route.params.patientId)
const editingId = route.query.id ? Number(route.query.id) : null
const isEditing = computed(() => editingId !== null)

const loading = ref(false)
const formRef = ref<FormInstance>()
const templates = ref<MedicalRecordTemplateResponse[]>([])
const selectedTemplateId = ref<number | '' | null>(null)

const form = reactive({
  chief_complaint: '',
  present_illness: '',
  past_history: '',
  diagnosis: '',
  treatment_plan: '',
  remark: '',
})

const rules: FormRules = {
  chief_complaint: [{ required: true, message: '请输入主诉', trigger: 'blur' }],
}

function prefillFromQuery() {
  if (route.query.chief_complaint) form.chief_complaint = String(route.query.chief_complaint)
  if (route.query.present_illness) form.present_illness = String(route.query.present_illness)
  if (route.query.past_history) form.past_history = String(route.query.past_history)
  if (route.query.diagnosis) form.diagnosis = String(route.query.diagnosis)
}

function applyTemplate(templateId: number | '' | null) {
  if (templateId === null || templateId === '') return
  const tpl = templates.value.find((t) => t.id === templateId)
  if (!tpl) return
  form.chief_complaint = tpl.chief_complaint_tpl
  form.present_illness = tpl.present_illness_tpl
  form.past_history = tpl.past_history_tpl
  form.diagnosis = tpl.diagnosis_tpl
  form.treatment_plan = tpl.treatment_plan_tpl
}

async function loadRecordForEdit(id: number) {
  const result = await doctorApi.getMedicalRecord(id)
  if (isBusinessError(result)) {
    ElMessage.error(result.message)
    return
  }
  form.chief_complaint = result.chief_complaint
  form.present_illness = result.present_illness
  form.past_history = result.past_history
  form.diagnosis = result.diagnosis
  form.treatment_plan = result.treatment_plan
  form.remark = result.remark || ''
  if (result.template_id !== null) {
    selectedTemplateId.value = result.template_id
  }
}

async function loadTemplates() {
  // 传空字符串获取所有启用模板（后端在 department 为空时返回全量）
  const result = await doctorApi.listMedicalRecordTemplates('')
  if (isBusinessError(result)) {
    ElMessage.error('病历模板加载失败')
    return
  }
  templates.value = result
}

async function handleSubmit(publish: boolean) {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const request = {
        patient_id: patientId,
        template_id:
          typeof selectedTemplateId.value === 'number' ? selectedTemplateId.value : null,
        prescription_id: null,
        chief_complaint: form.chief_complaint,
        present_illness: form.present_illness,
        past_history: form.past_history,
        diagnosis: form.diagnosis,
        treatment_plan: form.treatment_plan,
        remark: form.remark,
        publish,
      }
      const result = await doctorApi.saveMedicalRecord(request)
      if (isBusinessError(result)) {
        ElMessage.error(result.message)
        return
      }
      ElMessage.success(publish ? '病历已保存并发布' : '病历草稿已保存')
      router.push(`/patient/${patientId}/medical-records`)
    } finally {
      loading.value = false
    }
  })
}

onMounted(async () => {
  loading.value = true
  try {
    await loadTemplates()
    if (editingId !== null) {
      await loadRecordForEdit(editingId)
    } else {
      prefillFromQuery()
    }
  } finally {
    loading.value = false
  }
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

.patient-hint {
  color: #909399;
  font-size: 14px;
}
</style>
