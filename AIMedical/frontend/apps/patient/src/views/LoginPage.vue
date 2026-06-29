<template>
  <div class="page-container">
    <el-card class="form-card">
      <template #header>
        <h2>患者登录</h2>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" @submit.prevent="handleLogin">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" maxlength="20" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="auth.loading" style="width:100%">登录</el-button>
        </el-form-item>
        <el-form-item>
          <el-button text type="primary" @click="$router.push('/register')">还没有账号？立即注册</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

const auth = useAuthStore()
const router = useRouter()
const formRef = ref<FormInstance>()

const form = reactive({
  phone: '',
  password: '',
})

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不合法', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
  ],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    const err = await auth.login(form)
    if (err) {
      console.error('[LoginPage] login failed:', err)
      if (typeof err === 'object' && err !== null && 'code' in err) {
        const detail = err as { code: string; message: string }
        ElMessage.error(`[${detail.code}] ${detail.message}`)
      } else {
        ElMessage.error(String(err))
      }
      return
    }
    ElMessage.success('登录成功')
    try {
      await auth.fetchProfile()
    } catch (profileErr) {
      console.error('[LoginPage] fetchProfile failed after login:', profileErr)
      ElMessage.warning('获取用户信息失败，请刷新重试')
      router.push('/home')
      return
    }
    router.push('/home')
  } catch (e) {
    console.error('[LoginPage] unexpected error:', e)
    ElMessage.error('网络异常，请稍后重试')
  }
}
</script>

<style scoped>
.page-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f7fa;
}
.form-card {
  width: 420px;
}
</style>
