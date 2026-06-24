import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAccessToken, clearTokens, loginApi, registerApi, logoutApi, getPatientProfile } from '@aimedical/shared'
import type { LoginRequest, RegisterRequest, PatientProfile, BusinessError } from '@aimedical/shared'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getAccessToken())
  const profile = ref<PatientProfile | null>(null)
  const loading = ref(false)

  const isLoggedIn = () => !!token.value

  async function login(req: LoginRequest): Promise<string | null> {
    loading.value = true
    const result = await loginApi(req)
    loading.value = false
    if ((result as BusinessError).isBusinessError) {
      return (result as BusinessError).message
    }
    token.value = getAccessToken()
    return null
  }

  async function register(req: RegisterRequest): Promise<string | null> {
    loading.value = true
    const result = await registerApi(req)
    loading.value = false
    if ((result as BusinessError).isBusinessError) {
      return (result as BusinessError).message
    }
    token.value = getAccessToken()
    return null
  }

  async function fetchProfile(): Promise<PatientProfile | null> {
    loading.value = true
    const result = await getPatientProfile()
    loading.value = false
    if ((result as BusinessError).isBusinessError) {
      return null
    }
    profile.value = result as PatientProfile
    return profile.value
  }

  async function logout(): Promise<void> {
    await logoutApi()
    token.value = null
    profile.value = null
    clearTokens()
  }

  return { token, profile, loading, isLoggedIn, login, register, fetchProfile, logout }
})
