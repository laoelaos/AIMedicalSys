<template>
  <LoginBase
    ref="loginBaseRef"
    subtitle="管理员端登录"
    bg-gradient="linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)"
    btn-gradient="linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)"
    focus-color="#1e3c72"
    @login="handleLogin"
  />
</template>

<script setup lang="ts">
/**
 * 管理员端登录页
 *
 * <p>薄包装组件：复用 shared 包的 LoginBase（T3 抽取重复组件），
 * 仅提供管理员端的副标题、渐变色配置和登录逻辑。
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { LoginBase } from '@aimedical/shared/components'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loginBaseRef = ref<InstanceType<typeof LoginBase> | null>(null)

async function handleLogin(payload: { username: string; password: string }) {
  loginBaseRef.value?.setLoading(true)

  const result = await authStore.login({
    username: payload.username,
    password: payload.password,
  })

  loginBaseRef.value?.setLoading(false)

  if (result.success) {
    router.push('/')
  } else {
    // 透传后端返回的错误信息
    loginBaseRef.value?.setError(result.errorMessage || '登录失败，请检查用户名和密码')
  }
}
</script>
