import type { BusinessError, LoginRequest, LoginResponse, UserInfo, MenuItem, TokenResponse, TokenRefreshResponse, RegisterRequest, CurrentUserResponse } from '../types'
import { apiGet, apiPost, apiPut } from './client'
import { setTokens as saveTokens, clearTokens } from '../utils'

// 重新导出 axios 客户端与底层请求函数（供外部直接使用）
export { apiClient, apiGet, apiPost, apiPut, apiDelete, setAuthToken, clearAuthToken } from './client'

// ==================== Auth API (Patient-centric, fork) ====================

export async function loginApi(req: LoginRequest): Promise<TokenResponse | BusinessError> {
  const result = await apiPost<TokenResponse>('/patient/login', req)
  if (result && !(result as BusinessError).isBusinessError) {
    const token = result as TokenResponse
    if (!token.access_token || !token.refresh_token) {
      console.error('[loginApi] token missing in response:', token)
      return { code: 'AUTH_TOKEN_MISSING' as const, message: '服务器返回异常', isBusinessError: true as const } as BusinessError
    }
    saveTokens(token.access_token, token.refresh_token)
  } else if (result && (result as BusinessError).isBusinessError) {
    console.error('[loginApi] login returned business error:', (result as BusinessError).code, (result as BusinessError).message)
  }
  return result
}

export async function registerApi(req: RegisterRequest): Promise<TokenResponse | BusinessError> {
  const result = await apiPost<TokenResponse>('/patient/register', req)
  if (result && !(result as BusinessError).isBusinessError) {
    const token = result as TokenResponse
    saveTokens(token.access_token, token.refresh_token)
  }
  return result
}

export async function logoutApi(): Promise<void | BusinessError> {
  const result = await apiPost<void>('/patient/logout')
  clearTokens()
  return result
}

// ==================== Patient API (fork) ====================

import type {
  PatientProfile, PatientProfileUpdateRequest,
  HealthRecordSummary,
  AllergyRecord, AllergyRequest,
  ChronicDiseaseRecord, ChronicDiseaseRequest,
  FamilyHistoryRecord, FamilyHistoryRequest,
  SurgeryHistoryRecord, SurgeryHistoryRequest,
  MedicationHistoryRecord, MedicationHistoryRequest,
} from '../types'

// Profile
export async function getPatientProfile(): Promise<PatientProfile | BusinessError> {
  return apiGet<PatientProfile>('/patient/profile')
}

export async function updatePatientProfile(req: PatientProfileUpdateRequest): Promise<PatientProfile | BusinessError> {
  return apiPut<PatientProfile>('/patient/profile', req)
}

// Health Record Summary
export async function getHealthRecord(): Promise<HealthRecordSummary | BusinessError> {
  return apiGet<HealthRecordSummary>('/patient/health-record')
}

// Allergy
export async function addAllergy(req: AllergyRequest): Promise<AllergyRecord | BusinessError> {
  return apiPost<AllergyRecord>('/patient/health-record/allergies', req)
}
export async function updateAllergy(id: number, req: AllergyRequest): Promise<AllergyRecord | BusinessError> {
  return apiPut<AllergyRecord>(`/patient/health-record/allergies/${id}`, req)
}
export async function deleteAllergy(id: number): Promise<void | BusinessError> {
  return apiDelete<void>(`/patient/health-record/allergies/${id}`)
}

// Chronic Disease
export async function addChronicDisease(req: ChronicDiseaseRequest): Promise<ChronicDiseaseRecord | BusinessError> {
  return apiPost<ChronicDiseaseRecord>('/patient/health-record/chronic-diseases', req)
}
export async function updateChronicDisease(id: number, req: ChronicDiseaseRequest): Promise<ChronicDiseaseRecord | BusinessError> {
  return apiPut<ChronicDiseaseRecord>(`/patient/health-record/chronic-diseases/${id}`, req)
}
export async function deleteChronicDisease(id: number): Promise<void | BusinessError> {
  return apiDelete<void>(`/patient/health-record/chronic-diseases/${id}`)
}

// Family History
export async function addFamilyHistory(req: FamilyHistoryRequest): Promise<FamilyHistoryRecord | BusinessError> {
  return apiPost<FamilyHistoryRecord>('/patient/health-record/family-history', req)
}
export async function updateFamilyHistory(id: number, req: FamilyHistoryRequest): Promise<FamilyHistoryRecord | BusinessError> {
  return apiPut<FamilyHistoryRecord>(`/patient/health-record/family-history/${id}`, req)
}
export async function deleteFamilyHistory(id: number): Promise<void | BusinessError> {
  return apiDelete<void>(`/patient/health-record/family-history/${id}`)
}

// Surgery
export async function addSurgery(req: SurgeryHistoryRequest): Promise<SurgeryHistoryRecord | BusinessError> {
  return apiPost<SurgeryHistoryRecord>('/patient/health-record/surgeries', req)
}
export async function updateSurgery(id: number, req: SurgeryHistoryRequest): Promise<SurgeryHistoryRecord | BusinessError> {
  return apiPut<SurgeryHistoryRecord>(`/patient/health-record/surgeries/${id}`, req)
}
export async function deleteSurgery(id: number): Promise<void | BusinessError> {
  return apiDelete<void>(`/patient/health-record/surgeries/${id}`)
}

// Medication
export async function addMedication(req: MedicationHistoryRequest): Promise<MedicationHistoryRecord | BusinessError> {
  return apiPost<MedicationHistoryRecord>('/patient/health-record/medications', req)
}
export async function updateMedication(id: number, req: MedicationHistoryRequest): Promise<MedicationHistoryRecord | BusinessError> {
  return apiPut<MedicationHistoryRecord>(`/patient/health-record/medications/${id}`, req)
}
export async function deleteMedication(id: number): Promise<void | BusinessError> {
  return apiDelete<void>(`/patient/health-record/medications/${id}`)
}

// ==================== Upstream: Doctor/Admin Auth & Menu API ====================

import type { DoctorLoginRequest } from '../types'

/**
 * 认证相关API (Doctor/Admin)
 */
export const authApi = {
  /**
   * 用户登录 (Doctor/Admin)
   * Backend returns TokenResponse (accessToken/refreshToken/expiresIn).
   */
  login: async (request: DoctorLoginRequest): Promise<TokenResponse | BusinessError> => {
    const result = await apiPost<TokenResponse>('/auth/login', request)
    if (result && !(result as BusinessError).isBusinessError) {
      const resp = result as TokenResponse
      setTokens(resp.access_token, resp.refresh_token)
    }
    return result
  },

  /**
   * 用户登出
   */
  logout: async (): Promise<void | BusinessError> => {
    const result = await apiPost<void>('/auth/logout')
    clearTokens()
    return result
  },

  /**
   * 刷新令牌 — sends refresh_token in body.
   * Backend returns TokenRefreshResponse (accessToken/refreshToken/expiresIn).
   */
  refresh: async (refreshToken: string): Promise<TokenRefreshResponse | BusinessError> => {
    const result = await apiPost<TokenRefreshResponse>('/auth/refresh', { refresh_token: refreshToken })
    if (result && !(result as BusinessError).isBusinessError) {
      const resp = result as TokenRefreshResponse
      setTokens(resp.access_token, resp.refresh_token)
    }
    return result
  },

  /**
   * 获取当前用户信息
   */
  me: (): Promise<UserInfo | BusinessError> => {
    return apiGet<UserInfo>('/auth/me')
  },

  /**
   * 编辑当前用户个人资料
   * Backend maps to PUT /api/auth/profile
   */
  updateMe: (data: { nickname?: string; phone?: string; email?: string }): Promise<UserInfo | BusinessError> => {
    return apiPut<UserInfo>('/auth/profile', data)
  },
}

/**
 * 菜单相关API
 */
export const menuApi = {
  /**
   * 获取当前用户菜单树
   */
  tree: (): Promise<MenuItem[] | BusinessError> => {
    return apiGet<MenuItem[]>('/menu/tree')
  },

  /**
   * 获取所有菜单（管理员）
   */
  all: (): Promise<MenuItem[] | BusinessError> => {
    return apiGet<MenuItem[]>('/menu/all')
  },
}

// 医生端 API
export { doctorApi } from './doctor'
