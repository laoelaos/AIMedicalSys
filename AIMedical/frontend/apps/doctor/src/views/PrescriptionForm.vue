<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <h2>开具处方</h2>
      </template>

      <el-form :model="form" label-position="top" class="prescription-form">
        <el-form-item label="诊断">
          <el-input
            v-model="form.diagnosis"
            type="textarea"
            :rows="2"
            placeholder="处方对应的诊断"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="处方备注（可选）"
          />
        </el-form-item>

        <el-divider content-position="left">处方明细</el-divider>

        <el-table :data="form.items" border stripe class="items-table">
          <el-table-column min-width="140">
            <template #header>
              <span><span class="required-star">*</span> 药品名称</span>
            </template>
            <template #default="{ row }">
              <el-input v-model="row.drug_name" placeholder="药品名称" />
            </template>
          </el-table-column>
          <el-table-column label="规格" width="120">
            <template #default="{ row }">
              <el-input v-model="row.specification" placeholder="规格" />
            </template>
          </el-table-column>
          <el-table-column label="用量" width="100">
            <template #default="{ row }">
              <el-input v-model="row.dosage" placeholder="用量" />
            </template>
          </el-table-column>
          <el-table-column label="用法" width="100">
            <template #default="{ row }">
              <el-input v-model="row.usage_method" placeholder="用法" />
            </template>
          </el-table-column>
          <el-table-column label="频次" width="100">
            <template #default="{ row }">
              <el-input v-model="row.frequency" placeholder="频次" />
            </template>
          </el-table-column>
          <el-table-column label="数量" width="90">
            <template #default="{ row }">
              <el-input-number
                v-model="row.quantity"
                :min="1"
                :precision="2"
                :controls="false"
                style="width: 80px"
              />
            </template>
          </el-table-column>
          <el-table-column label="单位" width="80">
            <template #default="{ row }">
              <el-input v-model="row.unit" placeholder="单位" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ $index }">
              <el-button
                type="danger"
                size="small"
                @click="removeItem($index)"
                :disabled="form.items.length <= 1"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-button
          type="primary"
          plain
          class="add-item-btn"
          @click="addItem"
        >
          + 添加药品
        </el-button>

        <el-divider />

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit(false)">
            保存草稿
          </el-button>
          <el-button type="success" :loading="loading" @click="handleSubmit(true)">
            保存并提交审核
          </el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { doctorApi, isBusinessError } from '@aimedical/shared'
import type { PrescriptionCreateRequest, PrescriptionItemRequest, BusinessError } from '@aimedical/shared'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const patientId = Number(route.params.patientId)

function emptyItem(): PrescriptionItemRequest {
  return {
    drug_name: '',
    specification: '',
    dosage: '',
    usage_method: '',
    frequency: '',
    quantity: 1,
    unit: '',
    remark: '',
  }
}

const form = reactive<PrescriptionCreateRequest>({
  patient_id: patientId,
  diagnosis: '',
  remark: '',
  submit_for_review: false,
  items: [emptyItem()],
})

onMounted(() => {
  if (route.query.diagnosis) form.diagnosis = String(route.query.diagnosis)
})

function addItem() {
  form.items.push(emptyItem())
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

async function handleSubmit(submitForReview: boolean) {
  // 提交前校验：每个明细行的药品名称（drug_name）必填，任一为空则阻止提交
  for (let i = 0; i < form.items.length; i++) {
    if (!form.items[i].drug_name.trim()) {
      ElMessage.error(`第 ${i + 1} 行药品名称不能为空`)
      return
    }
  }

  loading.value = true

  const payload: PrescriptionCreateRequest = {
    ...form,
    submit_for_review: submitForReview,
  }

  try {
    const res = await doctorApi.createPrescription(payload)

    if (isBusinessError(res)) {
      ElMessage.error((res as BusinessError).message)
      return
    }

    ElMessage.success(submitForReview ? '处方已创建并提交审核' : '处方草稿已保存')
    router.push(`/patient/${patientId}`)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.prescription-form {
  max-width: 1000px;
}

.items-table {
  margin-bottom: 12px;
}

.add-item-btn {
  margin-bottom: 16px;
}

.required-star {
  color: #f56c6c;
}
</style>
