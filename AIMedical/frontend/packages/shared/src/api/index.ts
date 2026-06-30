import axios, { AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import type { ApiResult, BusinessError, LoginRequest, TokenResponse, TokenRefreshResponse, UserInfo, MenuItem, TriageRequest, TriageResponse, TriageDepartment, TriageDoctor, ConsultRequest, ConsultResponse, AppointmentSlot, RegistrationRequest, RegistrationRecord, CancelResult, ExamCategory, ExamItem, ReportRecord, MedicalRecordRecord, PrescriptionRecord, PaymentRecord, TriageHistoryRecord } from '../types'
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
// Note: we intentionally unwrap AxiosResponse → data for ergonomic API wrappers.
// Axios strict interceptor typing disagrees with this pattern, so we assert.
apiClient.interceptors.response.use(
  ((response: AxiosResponse<ApiResult<unknown>>): unknown => {
    const body = response.data as ApiResult<unknown>
    if (body.code !== 'SUCCESS') {
      return { code: body.code, message: body.message ?? '', isBusinessError: true as const } as BusinessError
    }
    return body.data
  }) as Parameters<typeof apiClient.interceptors.response.use>[0],
  async (error: { response?: AxiosResponse; config?: InternalAxiosRequestConfig & { _retry?: boolean }; code?: string; message?: string }) => {
    if (error.response === undefined) {
      return { code: 'NETWORK_ERROR' as const, message: '网络不可达，请检查网络连接', isBusinessError: true as const } as BusinessError
    }

    // Try refresh token on 401 (only once)
    if (error.response.status === 401 && error.config && !error.config._retry) {
      const refreshToken = getRefreshToken()
      if (refreshToken) {
        error.config._retry = true
        try {
          const refreshResponse = await axios.post<ApiResult<{ access_token: string; refresh_token: string }>>(
            '/api/auth/refresh',
            { refresh_token: refreshToken },
            { headers: { Authorization: `Bearer ${refreshToken}` } }
          )
          const refreshBody = refreshResponse.data
          if (refreshBody.code === 'SUCCESS' && refreshBody.data) {
            setTokens(refreshBody.data.access_token, refreshBody.data.refresh_token)
            // Notify all Pinia stores that tokens were refreshed
            window.dispatchEvent(new CustomEvent('aimedical:tokens-refreshed', {
              detail: { access_token: refreshBody.data.access_token }
            }))
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
      return { code: 'UNAUTHORIZED' as const, message: '登录已过期，请重新登录', isBusinessError: true as const } as BusinessError
    }

    const requestUrl = error.config?.url ?? 'unknown'
    const status = error.response.status
    console.warn('[api] HTTP error for', requestUrl, 'status:', status)
    if (status === 401) {
      return { code: 'UNAUTHORIZED' as const, message: '登录已过期，请重新登录', isBusinessError: true as const } as BusinessError
    }
    if (status === 403) {
      return { code: 'FORBIDDEN' as const, message: '无权限访问', isBusinessError: true as const } as BusinessError
    }

    // Backend business error body (GlobalExceptionHandler returns Result JSON)
    const body = error.response.data as ApiResult<unknown> | undefined
    if (body && body.code) {
      return { code: body.code, message: body.message ?? `请求失败（${status}）`, isBusinessError: true as const } as BusinessError
    }
    return { code: 'HTTP_ERROR' as const, message: `请求失败（${status}）`, isBusinessError: true as const } as BusinessError
  },
)

// ==================== API Wrappers (try-catch pattern) ====================

export async function apiGet<T>(url: string, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    return await apiClient.get(url, config) as T
  } catch {
    return { code: 'NETWORK_ERROR', message: '网络不可达，请检查网络连接', isBusinessError: true as const } as BusinessError
  }
}

export async function apiPost<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    return await apiClient.post(url, data, config) as T
  } catch {
    return { code: 'NETWORK_ERROR', message: '网络不可达，请检查网络连接', isBusinessError: true as const } as BusinessError
  }
}

export async function apiPut<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    return await apiClient.put(url, data, config) as T
  } catch {
    return { code: 'NETWORK_ERROR', message: '网络不可达，请检查网络连接', isBusinessError: true as const } as BusinessError
  }
}

export async function apiDelete<T>(url: string, config?: AxiosRequestConfig): Promise<T | BusinessError> {
  try {
    return await apiClient.delete(url, config) as T
  } catch {
    return { code: 'NETWORK_ERROR', message: '网络不可达，请检查网络连接', isBusinessError: true as const } as BusinessError
  }
}

export { apiClient }

// ==================== Auth API (Patient-centric, fork) ====================

import type { RegisterRequest, CurrentUserResponse } from '../types'
import { setTokens as saveTokens } from '../utils'

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

// ==================== Upstream: Token Helpers ====================

/**
 * 设置认证令牌
 */
export function setAuthToken(token: string): void {
  apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`
}

/**
 * 清除认证令牌
 */
export function clearAuthToken(): void {
  delete apiClient.defaults.headers.common['Authorization']
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
  tree: (): Promise<MenuItem[] | BusinessError> => {
    return apiGet<MenuItem[]>('/menu/tree')
  },

  all: (): Promise<MenuItem[] | BusinessError> => {
    return apiGet<MenuItem[]>('/menu/all')
  },
}

/**
 * AI 智能导诊 API
 */
export const triageApi = {
  triage: (data: TriageRequest): Promise<TriageResponse | BusinessError> => {
    return apiPost<TriageResponse>('/patient/triage', data)
  },

  getDepartments: (): Promise<TriageDepartment[] | BusinessError> => {
    return apiGet<TriageDepartment[]>('/patient/triage/departments')
  },
}

/**
 * AI 病情咨询 API
 */
export const consultApi = {
  ask: (data: ConsultRequest): Promise<ConsultResponse | BusinessError> => {
    return apiPost<ConsultResponse>('/patient/consult', data)
  },

  mockToggleFault: (): Promise<{ fault: boolean } | BusinessError> => {
    return apiPost<{ fault: boolean }>('/patient/consult/mock-fault')
  },
}

/**
 * 智能挂号 API
 */
export const appointmentApi = {
  getSlots: (doctorId: number): Promise<AppointmentSlot[] | BusinessError> => {
    return apiGet<AppointmentSlot[]>(`/patient/appointment/${doctorId}/slots`)
  },

  book: (data: { doctor_id: number; slot_id: number }): Promise<{ success: boolean; message: string } | BusinessError> => {
    return apiPost<{ success: boolean; message: string }>('/patient/appointment', data)
  },
}

/**
 * 线上挂号 API
 */
export const registrationApi = {
  getDepartments: (): Promise<TriageDepartment[] | BusinessError> => {
    return apiGet<TriageDepartment[]>('/patient/registration/departments')
  },

  getDoctors: (deptId: number): Promise<TriageDoctor[] | BusinessError> => {
    return apiGet<TriageDoctor[]>(`/patient/registration/departments/${deptId}/doctors`)
  },

  getExamCategories: (): Promise<ExamCategory[] | BusinessError> => {
    return apiGet<ExamCategory[]>('/patient/registration/exam-categories')
  },

  getExamItems: (categoryId: number): Promise<ExamItem[] | BusinessError> => {
    return apiGet<ExamItem[]>(`/patient/registration/exam-categories/${categoryId}/items`)
  },

  getTimeSlots: (doctorId?: number, examItemId?: number): Promise<AppointmentSlot[] | BusinessError> => {
    const params = doctorId ? `/doctor/${doctorId}` : `/exam/${examItemId}`
    return apiGet<AppointmentSlot[]>(`/patient/registration/slots${params}`)
  },

  create: (data: RegistrationRequest): Promise<RegistrationRecord | BusinessError> => {
    return apiPost<RegistrationRecord>('/patient/registration', data)
  },

  list: (): Promise<RegistrationRecord[] | BusinessError> => {
    return apiGet<RegistrationRecord[]>('/patient/registration')
  },

  cancel: (regId: number): Promise<CancelResult | BusinessError> => {
    return apiPost<CancelResult>(`/patient/registration/${regId}/cancel`, {})
  },

  getMockDoctors: (): { doctor_id: number; doctor_name: string; available_slot_count: number; score: number }[] => {
    return [
      { doctor_id: 201, doctor_name: '王主任', available_slot_count: 5, score: 95 },
      { doctor_id: 202, doctor_name: '张副主任', available_slot_count: 3, score: 82 },
      { doctor_id: 203, doctor_name: '李主治医师', available_slot_count: 8, score: 70 },
    ]
  },

  getMockSlots: (): AppointmentSlot[] => {
    return [
      { slot_id: 1, time_slot: '07-01 08:00-08:30', available: true },
      { slot_id: 2, time_slot: '07-01 09:00-09:30', available: true },
      { slot_id: 3, time_slot: '07-01 10:00-10:30', available: false },
      { slot_id: 4, time_slot: '07-01 14:00-14:30', available: true },
      { slot_id: 5, time_slot: '07-02 08:30-09:00', available: true },
      { slot_id: 6, time_slot: '07-02 10:30-11:00', available: true },
    ]
  },

  getMockRegistrations: (): RegistrationRecord[] => {
    return [
      {
        id: 1001,
        registration_type: 'OUTPATIENT',
        doctor_name: '王主任',
        department_name: '神经内科',
        time_slot: '07-01 08:00-08:30',
        status: 'CONFIRMED',
        created_at: '2026-06-29 10:30',
        can_cancel: true,
      },
      {
        id: 1002,
        registration_type: 'EXAMINATION',
        exam_item_name: '头颅 CT',
        time_slot: '07-02 10:30-11:00',
        status: 'PENDING',
        created_at: '2026-06-29 14:00',
        can_cancel: true,
      },
      {
        id: 1003,
        registration_type: 'OUTPATIENT',
        doctor_name: '李主治医师',
        department_name: '普通内科',
        time_slot: '07-01 15:00-15:30',
        status: 'DISPENSED',
        created_at: '2026-06-28 09:00',
        can_cancel: false,
      },
    ]
  },
}

/**
 * 报告/病历/处方/缴费查询 API
 */
export const recordsApi = {
  getReports: (): Promise<ReportRecord[] | BusinessError> => {
    return apiGet<ReportRecord[]>('/patient/records/reports')
  },

  getMedicalRecords: (): Promise<MedicalRecordRecord[] | BusinessError> => {
    return apiGet<MedicalRecordRecord[]>('/patient/records/medical')
  },

  getPrescriptions: (): Promise<PrescriptionRecord[] | BusinessError> => {
    return apiGet<PrescriptionRecord[]>('/patient/records/prescriptions')
  },

  getPayments: (params?: { start_date?: string; end_date?: string; category?: string }): Promise<PaymentRecord[] | BusinessError> => {
    const query = new URLSearchParams()
    if (params?.start_date) query.set('start_date', params.start_date)
    if (params?.end_date) query.set('end_date', params.end_date)
    if (params?.category) query.set('category', params.category)
    const qs = query.toString()
    return apiGet<PaymentRecord[]>(`/patient/records/payments${qs ? '?' + qs : ''}`)
  },

  getReportDetail: (id: number): Promise<ReportRecord | BusinessError> => {
    return apiGet<ReportRecord>(`/patient/records/reports/${id}`)
  },

  getPrescriptionDetail: (id: number): Promise<PrescriptionRecord | BusinessError> => {
    return apiGet<PrescriptionRecord>(`/patient/records/prescriptions/${id}`)
  },
}

/**
 * 分诊记录查询 API
 */
export const triageRecordsApi = {
  list: (params?: { startTime?: string; endTime?: string; degraded?: boolean }): Promise<TriageHistoryRecord[] | BusinessError> => {
    const query = new URLSearchParams()
    if (params?.startTime) query.set('startTime', params.startTime)
    if (params?.endTime) query.set('endTime', params.endTime)
    if (params?.degraded) query.set('degraded', 'true')
    const qs = query.toString()
    return apiGet<TriageHistoryRecord[]>(`/patient/triage-records${qs ? '?' + qs : ''}`)
  },
}
