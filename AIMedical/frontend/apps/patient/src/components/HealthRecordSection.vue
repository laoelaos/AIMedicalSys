<template>
  <div v-loading="loading">
    <!-- Allergy -->
    <el-card class="sub-card">
      <template #header><strong>过敏史</strong></template>
      <div v-if="summary?.allergies?.length">
        <el-tag v-for="a in summary.allergies" :key="a.id" closable style="margin:4px"
          @close="handleDeleteAllergy(a.id)">
          {{ a.allergen }}
          <template v-if="a.severity"> ({{ a.severity === 'SEVERE' ? '严重' : a.severity === 'MODERATE' ? '中度' : '轻度' }})</template>
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
      <el-empty v-else description="暂无用History" :image-size="40" />
      <el-button type="primary" size="small" style="margin-top:8px" @click="openMedicationDialog()">添加</el-button>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="450px">
      <el-form ref="healthFormRef" :model="healthForm" label-width="100px">
        <template v-if="dialogType === 'allergy'">
          <el-form-item label="过敏原" required><el-input v-model="healthForm.allergen" maxlength="100" /></el-form-item>
          <el-form-item label="反应类型"><el-input v-model="healthForm.reaction_type" maxlength="50" placeholder="如皮疹/呼吸困难/休克" /></el-form-item>
          <el-form-item label="严重程度">
            <el-select v-model="healthForm.severity" style="width:100%" clearable placeholder="请选择">
              <el-option label="轻度" value="MILD" />
              <el-option label="中度" value="MODERATE" />
              <el-option label="严重" value="SEVERE" />
            </el-select>
          </el-form-item>
          <el-form-item label="发生时间"><el-input v-model="healthForm.occurred_at" placeholder="YYYY-MM-DD" /></el-form-item>
        </template>
        <template v-else-if="dialogType === 'chronic'">
          <el-form-item label="慢病名称" required><el-input v-model="healthForm.disease_name" maxlength="100" /></el-form-item>
          <el-form-item label="确诊时间"><el-input v-model="healthForm.diagnosed_at" placeholder="YYYY-MM-DD" /></el-form-item>
          <el-form-item label="当前状态">
            <el-select v-model="healthForm.current_status" style="width:100%" clearable>
              <el-option label="稳定" value="STABLE" />
              <el-option label="不稳定" value="UNSTABLE" />
              <el-option label="已康复" value="RECOVERED" />
            </el-select>
          </el-form-item>
        </template>
        <template v-else-if="dialogType === 'family'">
          <el-form-item label="亲属关系" required><el-input v-model="healthForm.relationship" maxlength="50" placeholder="如父亲/母亲/兄弟姐妹" /></el-form-item>
          <el-form-item label="疾病名称" required><el-input v-model="healthForm.disease_name" maxlength="100" /></el-form-item>
          <el-form-item label="备注"><el-input v-model="healthForm.note" maxlength="200" /></el-form-item>
        </template>
        <template v-else-if="dialogType === 'surgery'">
          <el-form-item label="手术名称" required><el-input v-model="healthForm.surgery_name" maxlength="100" /></el-form-item>
          <el-form-item label="手术时间"><el-input v-model="healthForm.surgery_at" placeholder="YYYY-MM-DD" /></el-form-item>
          <el-form-item label="医院"><el-input v-model="healthForm.hospital" maxlength="100" /></el-form-item>
        </template>
        <template v-else-if="dialogType === 'medication'">
          <el-form-item label="药品名称" required><el-input v-model="healthForm.drug_name" maxlength="100" /></el-form-item>
          <el-form-item label="用药原因"><el-input v-model="healthForm.reason" maxlength="200" /></el-form-item>
          <el-form-item label="开始时间"><el-input v-model="healthForm.started_at" placeholder="YYYY-MM-DD" /></el-form-item>
          <el-form-item label="结束时间"><el-input v-model="healthForm.ended_at" placeholder="YYYY-MM-DD" /></el-form-item>
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
import { ElMessage } from 'element-plus'
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

const healthForm = reactive<Record<string, string>>({
  allergen: '', reaction_type: '', severity: '', occurred_at: '',
  disease_name: '', diagnosed_at: '', current_status: '',
  relationship: '', note: '',
  surgery_name: '', surgery_at: '', hospital: '',
  drug_name: '', reason: '', started_at: '', ended_at: '',
})

function clearForm() {
  Object.keys(healthForm).forEach(k => healthForm[k] = '')
}

async function loadSummary() {
  loading.value = true
  const result = await getHealthRecord()
  loading.value = false
  if (!(result as BusinessError).isBusinessError) {
    summary.value = result as HealthRecordSummary
  }
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

async function handleSaveHealthRecord() {
  let result: unknown
  switch (dialogType.value) {
    case 'allergy':
      result = await addAllergy(healthForm as unknown as AllergyRequest); break
    case 'chronic':
      result = await addChronicDisease(healthForm as unknown as ChronicDiseaseRequest); break
    case 'family':
      result = await addFamilyHistory(healthForm as unknown as FamilyHistoryRequest); break
    case 'surgery':
      result = await addSurgery(healthForm as unknown as SurgeryHistoryRequest); break
    case 'medication':
      result = await addMedication(healthForm as unknown as MedicationHistoryRequest); break
  }
  if ((result as BusinessError).isBusinessError) {
    ElMessage.error((result as BusinessError).message)
  } else {
    ElMessage.success('添加成功')
    dialogVisible.value = false
    loadSummary()
  }
}

async function handleDeleteAllergy(id: number) {
  await deleteAllergy(id)
  loadSummary()
}
async function handleDeleteChronic(id: number) {
  await deleteChronicDisease(id)
  loadSummary()
}
async function handleDeleteFamily(id: number) {
  await deleteFamilyHistory(id)
  loadSummary()
}
async function handleDeleteSurgery(id: number) {
  await deleteSurgery(id)
  loadSummary()
}
async function handleDeleteMedication(id: number) {
  await deleteMedication(id)
  loadSummary()
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
