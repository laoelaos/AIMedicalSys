import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getAccessToken, clearTokens, loginApi, registerApi, logoutApi, getPatientProfile } from '@aimedical/shared'
import type { LoginRequest, RegisterRequest, TokenResponse, PatientProfile, BusinessError } from '@aimedical/shared'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getAccessToken())
  const profile = ref<PatientProfile | null>(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => !!token.value)

  async function login(req: LoginRequest): Promise<string | null> {
    loading.value = true
    try {
      const result = await loginApi(req)
      if ((result as BusinessError).isBusinessError) {
        return (result as BusinessError).message
      }
      token.value = (result as TokenResponse).access_token
      return null
    } finally {
      loading.value = false
    }
  }

  async function register(req: RegisterRequest): Promise<string | null> {
    loading.value = true
    try {
      const result = await registerApi(req)
      if ((result as BusinessError).isBusinessError) {
        return (result as BusinessError).message
      }
      token.value = (result as TokenResponse).access_token
      return null
    } finally {
      loading.value = false
    }
  }

  async function fetchProfile(): Promise<PatientProfile | null> {
    loading.value = true
    try {
      const result = await getPatientProfile()
      if ((result as BusinessError).isBusinessError) {
        return null
      }
      profile.value = result as PatientProfile
      return profile.value
    } finally {
      loading.value = false
    }
  }

  async function setProfile(p: PatientProfile) {
    profile.value = { ...p }
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } finally {
      token.value = null
      profile.value = null
      clearTokens()
    }
  }

  return { token, profile, loading, isLoggedIn, login, register, fetchProfile, setProfile, logout }
})
