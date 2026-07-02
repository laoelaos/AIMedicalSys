<template>
  <div class="page-container">
    <el-card class="form-card">
      <template #header>
        <h2>患者注册</h2>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" @submit.prevent="handleRegister">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6-20位，需包含字母和数字" maxlength="20" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" maxlength="20" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入真实姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="form.gender" placeholder="请选择性别" style="width:100%">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
            <el-option label="未知" value="未知" />
          </el-select>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="0" :max="150" placeholder="请输入年龄" style="width:100%" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="auth.loading" style="width:100%">注册</el-button>
        </el-form-item>
        <el-form-item>
          <el-button text type="primary" @click="$router.push('/login')">已有账号？立即登录</el-button>
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
  confirmPassword: '',
  name: '',
  gender: '',
  age: undefined as number | undefined,
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不合法', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在6-20位之间', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d).+$/, message: '密码必须包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 1, max: 20, message: '姓名长度需在1-20个字符之间', trigger: 'blur' },
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' },
  ],
  age: [
    { required: true, message: '请输入年龄', trigger: 'blur' },
  ],
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    const err = await auth.register({
      phone: form.phone,
      password: form.password,
      name: form.name,
      gender: form.gender,
      age: form.age!,
    })
    if (err) {
      ElMessage.error(err)
    } else {
      ElMessage.success('注册成功')
      // Fetch profile immediately so ProfilePage has data on arrival
      await auth.fetchProfile()
      router.push('/home')
    }
  } catch (e) {
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
  width: 460px;
}
</style>
