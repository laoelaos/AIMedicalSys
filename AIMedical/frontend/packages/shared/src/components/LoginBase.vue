<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">智慧云脑诊疗平台</h1>
      <h2 class="login-subtitle">{{ subtitle }}</h2>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-item">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model="form.username"
            type="text"
            placeholder="请输入用户名"
            required
          />
        </div>
        <div class="form-item">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>
        <div v-if="errorMsg" class="error-message">{{ errorMsg }}</div>
        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 通用登录页组件（T3 抽取到 shared 包）
 *
 * <p>两端登录页结构完全相同（157 行重复），仅副标题和渐变色不同。
 * 通过 props 传入 subtitle、bgGradient、btnGradient、focusColor，
 * 由各 app 薄包装组件提供。登录逻辑（authStore.login）通过 emit 委托给父组件。
 *
 * <p>安全提示（T6）：密码以明文经 HTTP body 传输，HTTPS 是上线前置条件。
 * 部署文档须标注：生产环境必须启用 HTTPS（TLS 1.2+），否则存在凭据被中间人截获的风险。
 */
import { ref } from 'vue'

const props = defineProps<{
  /** 副标题（如 "管理员端登录"） */
  subtitle: string
  /** 背景渐变 CSS 值 */
  bgGradient: string
  /** 按钮渐变 CSS 值 */
  btnGradient: string
  /** 输入框 focus 边框色 */
  focusColor: string
}>()

const emit = defineEmits<{
  (e: 'login', payload: { username: string; password: string }): void
}>()

const form = ref({
  username: '',
  password: '',
})

const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  loading.value = true
  errorMsg.value = ''

  // 安全提示（T6）：密码以明文经 HTTP body 传输，HTTPS 是上线前置条件。
  // 部署文档须标注：生产环境必须启用 HTTPS（TLS 1.2+），否则存在凭据被中间人截获的风险。
  // 本地开发可通过 vite dev server 的 https 选项或反向代理启用临时证书。
  emit('login', {
    username: form.value.username,
    password: form.value.password,
  })
}

// 供父组件调用的方法：设置错误信息和 loading 状态
function setError(message: string) {
  errorMsg.value = message
  loading.value = false
}

function setLoading(value: boolean) {
  loading.value = value
}

defineExpose({ setError, setLoading })
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: v-bind('props.bgGradient');
}

.login-card {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  text-align: center;
  color: #333;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 16px;
  text-align: center;
  color: #666;
  margin-bottom: 32px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  color: #333;
}

.form-item input {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-item input:focus {
  outline: none;
  border-color: v-bind('props.focusColor');
}

.error-message {
  color: #f56c6c;
  font-size: 14px;
  text-align: center;
}

.login-btn {
  padding: 12px;
  background: v-bind('props.btnGradient');
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.login-btn:hover {
  opacity: 0.9;
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
