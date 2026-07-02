import type { BusinessError, LoginRequest, LoginResponse, UserInfo, MenuItem, TokenResponse, TokenRefreshResponse, RegisterRequest, CurrentUserResponse, TriageRequest, TriageResponse, TriageDepartment, TriageDoctor, ConsultRequest, ConsultResponse, AppointmentSlot, RegistrationRequest, RegistrationRecord, CancelResult, ExamCategory, ExamItem, ReportRecord, MedicalRecordRecord, PrescriptionRecord, PaymentRecord, TriageHistoryRecord } from '../types'
import { apiGet, apiPost, apiPut, apiDelete } from './client'
import { getAccessToken, setTokens, clearTokens, getRefreshToken } from '../utils'

// 重新导出 axios 客户端与底层请求函数（供外部直接使用）
export { apiClient, apiGet, apiPost, apiPut, apiDelete, setAuthToken, clearAuthToken } from './client'

// ==================== Auth API (Patient-centric, fork) ====================

export async function loginApi(req: LoginRequest): Promise<TokenResponse | BusinessError> {
  const result = await apiPost<TokenResponse>('/patient/login', req)
  if (result && !(result as BusinessError).isBusinessError) {
    const token = result as TokenResponse
    if (!token.access_token || !token.refresh_token) {
      console.error('[loginApi] token missing in response: access_token or refresh_token is empty')
      return { code: 'AUTH_TOKEN_MISSING' as const, message: '服务器返回异常', isBusinessError: true as const } as BusinessError
    }
    setTokens(token.access_token, token.refresh_token)
  } else if (result && (result as BusinessError).isBusinessError) {
    console.error('[loginApi] login returned business error:', (result as BusinessError).code, (result as BusinessError).message)
  }
  return result
}

export async function registerApi(req: RegisterRequest): Promise<TokenResponse | BusinessError> {
  const result = await apiPost<TokenResponse>('/patient/register', req)
  if (result && !(result as BusinessError).isBusinessError) {
    const token = result as TokenResponse
    setTokens(token.access_token, token.refresh_token)
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
  tree: (): Promise<MenuItem[] | BusinessError> => {
    return apiGet<MenuItem[]>('/menu/tree')
  },

  all: (): Promise<MenuItem[] | BusinessError> => {
    return apiGet<MenuItem[]>('/menu/all')
  },
}

// 医生端 API
export { doctorApi } from './doctor'

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
}

/**
 * 智能挂号 API
 */
export const appointmentApi = {
  // Legacy: backend AppointmentController removed — use PatientRegistrationController /api/patient/registration.
  // This stub preserves interface compatibility for DoctorAppointmentPage mock fallback.
  getSlots: (_doctorId: number): Promise<AppointmentSlot[] | BusinessError> => {
    return Promise.resolve([
      { slot_id: 1, time_slot: '07-01 08:00-08:30', available: true },
      { slot_id: 2, time_slot: '07-01 09:00-09:30', available: true },
      { slot_id: 3, time_slot: '07-01 10:00-10:30', available: false },
      { slot_id: 4, time_slot: '07-01 14:00-14:30', available: true },
      { slot_id: 5, time_slot: '07-02 08:30-09:00', available: true },
      { slot_id: 6, time_slot: '07-02 10:30-11:00', available: true },
    ])
  },

  book: (_data: { doctor_id: number; slot_id: number }): Promise<{ success: boolean; message: string } | BusinessError> => {
    return Promise.resolve({ success: true, message: '挂号预约成功！请按时前往就诊' })
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
