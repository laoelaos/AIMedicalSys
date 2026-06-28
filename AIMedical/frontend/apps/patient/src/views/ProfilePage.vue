<template>
  <div class="page-container">
    <el-card class="main-card">
      <template #header>
        <div class="header-row">
          <h2>智慧云脑诊疗平台 - 个人中心</h2>
          <el-button type="danger" text @click="handleLogout">退出登录</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="info">
          <el-skeleton :loading="loading" animated>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="姓名">{{ profile?.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ profile?.phone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="性别">{{ profile?.gender || '-' }}</el-descriptions-item>
              <el-descriptions-item label="年龄">{{ profile?.age ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ profile?.email || '-' }}</el-descriptions-item>
              <el-descriptions-item label="紧急联系人">{{ profile?.emergency_contact || '-' }}</el-descriptions-item>
            </el-descriptions>
            <el-button type="primary" style="margin-top:16px" @click="showEditDialog = true">编辑资料</el-button>
          </el-skeleton>
        </el-tab-pane>

        <el-tab-pane label="健康档案" name="health">
          <HealthRecordSection />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Edit Dialog -->
    <el-dialog v-model="showEditDialog" title="编辑个人资料" width="450px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="editForm.name" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" maxlength="11" placeholder="请输入11位手机号" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="editForm.gender" style="width:100%">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
            <el-option label="未知" value="未知" />
          </el-select>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="editForm.age" :min="0" :max="150" style="width:100%" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" maxlength="100" />
        </el-form-item>
        <el-form-item label="紧急联系人" prop="emergency_contact">
          <el-input v-model="editForm.emergency_contact" maxlength="2000" type="textarea" :rows="2" placeholder="紧急联系人及联系方式" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { updatePatientProfile } from '@aimedical/shared'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import type { BusinessError, PatientProfile } from '@aimedical/shared'
import HealthRecordSection from '../components/HealthRecordSection.vue'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(true)
const activeTab = ref('info')
const showEditDialog = ref(false)
const editFormRef = ref<FormInstance>()

const profile = computed(() => auth.profile)

const editForm = reactive({
  name: '',
  phone: '',
  gender: '',
  age: undefined as number | undefined,
  email: '',
  emergency_contact: '',
})

const editRules = {
  name: [{ min: 1, max: 20, message: '姓名长度需在1-20个字符之间', trigger: 'blur' }],
  gender: [{ pattern: /^(男|女|未知)?$/, message: '性别必须为男、女或未知', trigger: 'change' }],
  age: [{ type: 'number', min: 0, max: 150, message: '年龄范围0-150', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不合法', trigger: 'blur' }],
}

onMounted(async () => {
  const p = await auth.fetchProfile()
  if (!p) {
    ElMessage.error('获取个人信息失败')
  } else {

  }
  loading.value = false
})

// Watch the dialog to pre-fill form
watch(showEditDialog, (val) => {
  if (val && profile.value) {
    editFormRef.value?.clearValidate()
    editForm.name = profile.value.name || ''
    editForm.phone = profile.value.phone || ''
    editForm.gender = profile.value.gender || ''
    editForm.age = profile.value.age
    editForm.email = profile.value.email || ''
    editForm.emergency_contact = profile.value.emergency_contact || ''
  }
})

async function handleUpdateProfile() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  const result = await updatePatientProfile({
    name: editForm.name || undefined,
    phone: editForm.phone || undefined,
    gender: editForm.gender || undefined,
    age: editForm.age,
    email: editForm.email || undefined,
    emergency_contact: editForm.emergency_contact || undefined,
  })
  if ((result as BusinessError).isBusinessError) {
    ElMessage.error((result as BusinessError).message)
  } else {
    const updated = result as PatientProfile

    // Use store method instead of direct mutation
    await auth.setProfile(updated)
    ElMessage.success('资料更新成功')
    showEditDialog.value = false
  }
}

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.page-container {
  max-width: 900px;
  margin: 20px auto;
  padding: 0 20px;
}
.main-card {
  min-height: 400px;
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
