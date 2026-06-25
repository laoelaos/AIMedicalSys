<template>
  <div v-loading="loading">
    <!-- Allergy -->
    <el-card class="sub-card">
      <template #header><strong>过敏史</strong></template>
      <div v-if="summary?.allergies?.length">
        <el-tag v-for="a in summary.allergies" :key="a.id" closable style="margin:4px"
          @close="handleDeleteAllergy(a.id)">
          {{ a.allergen }}
          <template v-if="a.severity"> ({{ severityLabel(a.severity) }})</template>
        </el-tag>
      </div>
      <el-empty v-else description="暂无过敏史" :image-size="40" />
      <el-button type="primary" size="small" style="margin-top:8px" @click="openAllergyDialog()">添加</el-button>
    </el-card>

    <!-- Chronic Diseases -->
    <el-card class="sub-card">
      <template #header><strong>慢病史</strong></template>
      <div v-if="summary?.chronic_diseases?.length">
        <el-tag v-for="c in summary.chronic_diseases" :key="c.id" closable style="margin:4px"
          @close="handleDeleteChronic(c.id)">
          {{ c.disease_name }}
          <template v-if="c.current_status">
            ({{ statusLabel(c.current_status) }})
          </template>
        </el-tag>
      </div>
      <el-empty v-else description="暂无慢病史" :image-size="40" />
      <el-button type="primary" size="small" style="margin-top:8px" @click="openChronicDialog()">添加</el-button>
    </el-card>

    <!-- Family History -->
    <el-card class="sub-card">
      <template #header><strong>家族史</strong></template>
      <div v-if="summary?.family_histories?.length">
        <div v-for="f in summary.family_histories" :key="f.id" class="record-row">
          <span>{{ f.relationship }}: {{ f.disease_name }}</span>
          <template v-if="f.note"> ({{ f.note }})</template>
          <el-button type="danger" link size="small" @click="handleDeleteFamily(f.id)">删除</el-button>
        </div>
      </div>
      <el-empty v-else description="暂无家族史" :image-size="40" />
      <el-button type="primary" size="small" style="margin-top:8px" @click="openFamilyDialog()">添加</el-button>
    </el-card>

    <!-- Surgery History -->
    <el-card class="sub-card">
      <template #header><strong>手术史</strong></template>
      <div v-if="summary?.surgery_histories?.length">
        <div v-for="s in summary.surgery_histories" :key="s.id" class="record-row">
          <span>{{ s.surgery_name }} {{ s.surgery_at ? `(${s.surgery_at})` : '' }} {{ s.hospital ? `- ${s.hospital}` : '' }}</span>
          <el-button type="danger" link size="small" @click="handleDeleteSurgery(s.id)">删除</el-button>
        </div>
      </div>
      <el-empty v-else description="暂无手术史" :image-size="40" />
      <el-button type="primary" size="small" style="margin-top:8px" @click="openSurgeryDialog()">添加</el-button>
    </el-card>

    <!-- Medication History -->
    <el-card class="sub-card">
      <template #header><strong>用药史</strong></template>
      <div v-if="summary?.medication_histories?.length">
        <div v-for="m in summary.medication_histories" :key="m.id" class="record-row">
          <span>{{ m.drug_name }} {{ m.started_at ? `${m.started_at}` : '' }}{{ m.ended_at ? `~${m.ended_at}` : '' }}</span>
          <el-button type="danger" link size="small" @click="handleDeleteMedication(m.id)">删除</el-button>
        </div>
      </div>
      <el-empty v-else description="暂无用药史" :image-size="40" />
      <el-button type="primary" size="small" style="margin-top:8px" @click="openMedicationDialog()">添加</el-button>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="450px">
      <el-form ref="healthFormRef" :model="healthForm" :rules="healthFormRules" label-width="100px">
        <template v-if="dialogType === 'allergy'">
          <el-form-item label="过敏原" prop="allergen">
            <el-input v-model="healthForm.allergen" maxlength="100" />
          </el-form-item>
          <el-form-item label="反应类型">
            <el-input v-model="healthForm.reaction_type" maxlength="50" placeholder="如皮疹/呼吸困难/休克" />
          </el-form-item>
          <el-form-item label="严重程度">
            <el-select v-model="healthForm.severity" style="width:100%" clearable placeholder="请选择">
              <el-option label="轻度" value="MILD" />
              <el-option label="中度" value="MODERATE" />
              <el-option label="严重" value="SEVERE" />
            </el-select>
          </el-form-item>
          <el-form-item label="发生时间" prop="occurred_at">
            <el-input v-model="healthForm.occurred_at" placeholder="YYYY-MM-DD" />
          </el-form-item>
        </template>
        <template v-else-if="dialogType === 'chronic'">
          <el-form-item label="慢病名称" prop="disease_name">
            <el-input v-model="healthForm.disease_name" maxlength="100" />
          </el-form-item>
          <el-form-item label="确诊时间" prop="diagnosed_at">
            <el-input v-model="healthForm.diagnosed_at" placeholder="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="当前状态">
            <el-select v-model="healthForm.current_status" style="width:100%" clearable>
              <el-option label="稳定" value="STABLE" />
              <el-option label="不稳定" value="UNSTABLE" />
              <el-option label="已康复" value="RECOVERED" />
            </el-select>
          </el-form-item>
        </template>
        <template v-else-if="dialogType === 'family'">
          <el-form-item label="亲属关系" prop="relationship">
            <el-input v-model="healthForm.relationship" maxlength="50" placeholder="如父亲/母亲/兄弟姐妹" />
          </el-form-item>
          <el-form-item label="疾病名称" prop="disease_name">
            <el-input v-model="healthForm.disease_name" maxlength="100" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="healthForm.note" maxlength="200" />
          </el-form-item>
        </template>
        <template v-else-if="dialogType === 'surgery'">
          <el-form-item label="手术名称" prop="surgery_name">
            <el-input v-model="healthForm.surgery_name" maxlength="100" />
          </el-form-item>
          <el-form-item label="手术时间" prop="surgery_at">
            <el-input v-model="healthForm.surgery_at" placeholder="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="医院">
            <el-input v-model="healthForm.hospital" maxlength="100" />
          </el-form-item>
        </template>
        <template v-else-if="dialogType === 'medication'">
          <el-form-item label="药品名称" prop="drug_name">
            <el-input v-model="healthForm.drug_name" maxlength="100" />
          </el-form-item>
          <el-form-item label="用药原因">
            <el-input v-model="healthForm.reason" maxlength="200" />
          </el-form-item>
          <el-form-item label="开始时间" prop="started_at">
            <el-input v-model="healthForm.started_at" placeholder="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="结束时间" prop="ended_at">
            <el-input v-model="healthForm.ended_at" placeholder="YYYY-MM-DD" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveHealthRecord">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getHealthRecord,
  addAllergy, deleteAllergy,
  addChronicDisease, deleteChronicDisease,
  addFamilyHistory, deleteFamilyHistory,
  addSurgery, deleteSurgery,
  addMedication, deleteMedication,
} from '@aimedical/shared'
import type {
  HealthRecordSummary, BusinessError,
  AllergyRequest, ChronicDiseaseRequest,
  FamilyHistoryRequest, SurgeryHistoryRequest, MedicationHistoryRequest,
} from '@aimedical/shared'

const loading = ref(true)
const summary = ref<HealthRecordSummary | null>(null)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogType = ref<'allergy'|'chronic'|'family'|'surgery'|'medication'>('allergy')
const healthFormRef = ref<FormInstance>()

const healthForm = reactive<Record<string, string>>({
  allergen: '', reaction_type: '', severity: '', occurred_at: '',
  disease_name: '', diagnosed_at: '', current_status: '',
  relationship: '', note: '',
  surgery_name: '', surgery_at: '', hospital: '',
  drug_name: '', reason: '', started_at: '', ended_at: '',
})

const healthFormRules: FormRules = {
  allergen: [{ required: true, message: '请输入过敏原名称', trigger: 'blur' }, { max: 100, message: '不能超过100字符', trigger: 'blur' }],
  occurred_at: [{ pattern: /^\d{4}-\d{2}-\d{2}$/, message: '日期格式必须为YYYY-MM-DD', trigger: 'blur' }],
  disease_name: [{ required: true, message: '请输入慢病名称', trigger: 'blur' }, { max: 100, message: '不能超过100字符', trigger: 'blur' }],
  diagnosed_at: [{ pattern: /^\d{4}-\d{2}-\d{2}$/, message: '日期格式必须为YYYY-MM-DD', trigger: 'blur' }],
  relationship: [{ required: true, message: '请输入亲属关系', trigger: 'blur' }, { max: 50, message: '不能超过50字符', trigger: 'blur' }],
  surgery_name: [{ required: true, message: '请输入手术名称', trigger: 'blur' }, { max: 100, message: '不能超过100字符', trigger: 'blur' }],
  surgery_at: [{ pattern: /^\d{4}-\d{2}-\d{2}$/, message: '日期格式必须为YYYY-MM-DD', trigger: 'blur' }],
  drug_name: [{ required: true, message: '请输入药品名称', trigger: 'blur' }, { max: 100, message: '不能超过100字符', trigger: 'blur' }],
  started_at: [{ pattern: /^\d{4}-\d{2}-\d{2}$/, message: '日期格式必须为YYYY-MM-DD', trigger: 'blur' }],
  ended_at: [{ pattern: /^\d{4}-\d{2}-\d{2}$/, message: '日期格式必须为YYYY-MM-DD', trigger: 'blur' }],
}

function clearForm() {
  Object.keys(healthForm).forEach(k => healthForm[k] = '')
}

async function loadSummary() {
  loading.value = true
  try {
    const result = await getHealthRecord()
    if (!(result as BusinessError).isBusinessError) {
      summary.value = result as HealthRecordSummary
    } else {
      ElMessage.error((result as BusinessError).message)
    }
  } finally {
    loading.value = false
  }
}

function severityLabel(s: string) {
  const map: Record<string, string> = { MILD: '轻度', MODERATE: '中度', SEVERE: '严重' }
  return map[s] || s
}

function statusLabel(s: string) {
  const map: Record<string,string> = { STABLE:'稳定', UNSTABLE:'不稳定', RECOVERED:'已康复' }
  return map[s] || s
}

// Open dialogs
function openAllergyDialog() { dialogType.value = 'allergy'; dialogTitle.value = '添加过敏史'; clearForm(); dialogVisible.value = true }
function openChronicDialog() { dialogType.value = 'chronic'; dialogTitle.value = '添加慢病史'; clearForm(); dialogVisible.value = true }
function openFamilyDialog() { dialogType.value = 'family'; dialogTitle.value = '添加家族史'; clearForm(); dialogVisible.value = true }
function openSurgeryDialog() { dialogType.value = 'surgery'; dialogTitle.value = '添加手术史'; clearForm(); dialogVisible.value = true }
function openMedicationDialog() { dialogType.value = 'medication'; dialogTitle.value = '添加用药史'; clearForm(); dialogVisible.value = true }

function buildHealthFormRequest(): AllergyRequest | ChronicDiseaseRequest | FamilyHistoryRequest | SurgeryHistoryRequest | MedicationHistoryRequest {
  const f = healthForm
  switch (dialogType.value) {
    case 'allergy':
      return { allergen: f.allergen, reactionType: f.reaction_type || undefined, severity: f.severity || undefined, occurredAt: f.occurred_at || undefined } as AllergyRequest
    case 'chronic':
      return { diseaseName: f.disease_name, diagnosedAt: f.diagnosed_at || undefined, currentStatus: f.current_status || undefined } as ChronicDiseaseRequest
    case 'family':
      return { relationship: f.relationship, diseaseName: f.disease_name, note: f.note || undefined } as FamilyHistoryRequest
    case 'surgery':
      return { surgeryName: f.surgery_name, surgeryAt: f.surgery_at || undefined, hospital: f.hospital || undefined } as SurgeryHistoryRequest
    case 'medication':
      return { drugName: f.drug_name, reason: f.reason || undefined, startedAt: f.started_at || undefined, endedAt: f.ended_at || undefined } as MedicationHistoryRequest
  }
}

async function handleSaveHealthRecord() {
  const valid = await healthFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const req = buildHealthFormRequest()
  let result: unknown
  switch (dialogType.value) {
    case 'allergy':
      result = await addAllergy(req as AllergyRequest); break
    case 'chronic':
      result = await addChronicDisease(req as ChronicDiseaseRequest); break
    case 'family':
      result = await addFamilyHistory(req as FamilyHistoryRequest); break
    case 'surgery':
      result = await addSurgery(req as SurgeryHistoryRequest); break
    case 'medication':
      result = await addMedication(req as MedicationHistoryRequest); break
  }
  if ((result as BusinessError).isBusinessError) {
    ElMessage.error((result as BusinessError).message)
  } else {
    ElMessage.success('添加成功')
    dialogVisible.value = false
    await loadSummary()
  }
}

async function handleDeleteAllergy(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该过敏记录？', '提示', { type: 'warning' })
    const result = await deleteAllergy(id)
    if ((result as BusinessError)?.isBusinessError) {
      ElMessage.error((result as BusinessError).message)
    } else {
      ElMessage.success('删除成功')
      await loadSummary()
    }
  } catch {
    // User cancelled
  }
}

async function handleDeleteChronic(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该慢病记录？', '提示', { type: 'warning' })
    const result = await deleteChronicDisease(id)
    if ((result as BusinessError)?.isBusinessError) {
      ElMessage.error((result as BusinessError).message)
    } else {
      ElMessage.success('删除成功')
      await loadSummary()
    }
  } catch {
    // User cancelled
  }
}

async function handleDeleteFamily(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该家族史记录？', '提示', { type: 'warning' })
    const result = await deleteFamilyHistory(id)
    if ((result as BusinessError)?.isBusinessError) {
      ElMessage.error((result as BusinessError).message)
    } else {
      ElMessage.success('删除成功')
      await loadSummary()
    }
  } catch {
    // User cancelled
  }
}

async function handleDeleteSurgery(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该手术记录？', '提示', { type: 'warning' })
    const result = await deleteSurgery(id)
    if ((result as BusinessError)?.isBusinessError) {
      ElMessage.error((result as BusinessError).message)
    } else {
      ElMessage.success('删除成功')
      await loadSummary()
    }
  } catch {
    // User cancelled
  }
}

async function handleDeleteMedication(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该用药记录？', '提示', { type: 'warning' })
    const result = await deleteMedication(id)
    if ((result as BusinessError)?.isBusinessError) {
      ElMessage.error((result as BusinessError).message)
    } else {
      ElMessage.success('删除成功')
      await loadSummary()
    }
  } catch {
    // User cancelled
  }
}

onMounted(loadSummary)
</script>

<style scoped>
.sub-card {
  margin-bottom: 12px;
}
.record-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  border-bottom: 1px solid #f0f0f0;
}
</style>
