import axios, { AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import type { ApiResult, BusinessError } from '../types'
import { getAccessToken, setTokens, clearTokens, getRefreshToken } from '../utils'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

// Request interceptor: attach JWT token
apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor: unwrap Result<T>, handle errors
apiClient.interceptors.response.use(
  (response: AxiosResponse<ApiResult<unknown>>) => {
    const body = response.data as ApiResult<unknown>
    if (body.code !== 'SUCCESS') {
      return { code: body.code, message: body.message ?? '', isBusinessError: true as const } as BusinessError
    }
    return body.data
  },
  async (error: { response?: AxiosResponse; config?: InternalAxiosRequestConfig & { _retry?: boolean } }) => {
    if (error.response === undefined) {
      return Promise.resolve({ code: 'NETWORK_ERROR' as const, message: '网络不可达，请检查网络连接', isBusinessError: true as const })
    }

    // Try refresh token on 401 (only once)
    if (error.response.status === 401 && error.config && !error.config._retry) {
      const refreshToken = getRefreshToken()
      if (refreshToken) {
        error.config._retry = true
        try {
          const refreshResponse = await axios.post<ApiResult<{ access_token: string; refresh_token: string }>>(
            '/api/patient/refresh',
            {},
            { headers: { Authorization: `Bearer ${refreshToken}` } }
          )
          const refreshBody = refreshResponse.data
          if (refreshBody.code === 'SUCCESS' && refreshBody.data) {
            setTokens(refreshBody.data.access_token, refreshBody.data.refresh_token)
            if (error.config.headers) {
              error.config.headers.Authorization = `Bearer ${refreshBody.data.access_token}`
            }
            return apiClient(error.config)
          }
        } catch {
          // Refresh failed, clear tokens and redirect to login
        }
      }
      clearTokens()
      return Promise.resolve({ code: 'UNAUTHORIZED' as const, message: '登录已过期，请重新登录', isBusinessError: true as const })
    }
    if (error.response.status === 403) {
      return Promise.resolve({ code: 'FORBIDDEN' as const, message: '无权限访问', isBusinessError: true as const })
    }
    return Promise.resolve({ code: 'HTTP_ERROR' as const, message: `请求失败（${error.response.status}）`, isBusinessError: true as const })
  },
)

export async function apiGet<T>(url: string, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  return apiClient.get(url, config) as Promise<T | BusinessError>
}

export async function apiPost<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  return apiClient.post(url, data, config) as Promise<T | BusinessError>
}

export async function apiPut<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  return apiClient.put(url, data, config) as Promise<T | BusinessError>
}

export async function apiDelete<T>(url: string, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  return apiClient.delete(url, config) as Promise<T | BusinessError>
}

export { apiClient }

// ==================== Auth API ====================

import type { LoginRequest, RegisterRequest, TokenResponse, CurrentUserResponse } from '../types'
import { setTokens as saveTokens } from '../utils'

export async function loginApi(req: LoginRequest): Promise<TokenResponse | BusinessError> {
  const result = await apiPost<TokenResponse>('/patient/login', req)
  if (result && !(result as BusinessError).isBusinessError) {
    const token = result as TokenResponse
    saveTokens(token.access_token, token.refresh_token)
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

export async function refreshTokenApi(): Promise<TokenResponse | BusinessError> {
  return apiPost<TokenResponse>('/patient/refresh')
}

export async function logoutApi(): Promise<void | BusinessError> {
  const result = await apiPost<void>('/patient/logout')
  clearTokens()
  return result
}

// ==================== Patient API ====================

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
