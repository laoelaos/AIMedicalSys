export type ApiSuccess<T> = { code: 'SUCCESS'; data: T }

export type ApiError = {
  code: 'NETWORK_ERROR' | 'UNAUTHORIZED' | 'FORBIDDEN' | 'HTTP_ERROR'
  message: string
}

export type ApiResponse<T> = ApiSuccess<T> | ApiError

export interface ApiResult<T = unknown> {
  code: string
  message?: string
  data?: T
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export type BusinessError = {
  code: string
  message: string
  isBusinessError?: true
}

// ==================== Auth ====================

export interface LoginRequest {
  phone: string
  password: string
}

export interface RegisterRequest {
  phone: string
  password: string
  name: string
  gender: string
  age: number
}

export interface TokenResponse {
  access_token: string
  refresh_token: string
  token_type: string
  expires_in: number
}

export interface CurrentUserResponse {
  user_id: number
  username: string
  nickname: string
  phone: string
  gender: string
  age: number
  user_type: string
  roles: string[]
}

// ==================== Patient Profile ====================

export interface PatientProfile {
  id: number
  user_id: number
  name: string
  phone: string
  gender: string
  age: number
  email: string
  emergency_contact: string
}

export interface PatientProfileUpdateRequest {
  name?: string
  gender?: string
  age?: number
  email?: string
}

// ==================== Health Records ====================

export interface AllergyRecord {
  id: number
  allergen: string
  reaction_type: string
  severity: string
  occurred_at: string
}

export interface AllergyRequest {
  allergen: string
  reaction_type?: string
  severity?: string
  occurred_at?: string
}

export interface ChronicDiseaseRecord {
  id: number
  disease_name: string
  diagnosed_at: string
  current_status: string
}

export interface ChronicDiseaseRequest {
  disease_name: string
  diagnosed_at?: string
  current_status?: string
}

export interface FamilyHistoryRecord {
  id: number
  relationship: string
  disease_name: string
  note: string
}

export interface FamilyHistoryRequest {
  relationship: string
  disease_name: string
  note?: string
}

export interface SurgeryHistoryRecord {
  id: number
  surgery_name: string
  surgery_at: string
  hospital: string
}

export interface SurgeryHistoryRequest {
  surgery_name: string
  surgery_at?: string
  hospital?: string
}

export interface MedicationHistoryRecord {
  id: number
  drug_name: string
  reason: string
  started_at: string
  ended_at: string
}

export interface MedicationHistoryRequest {
  drug_name: string
  reason?: string
  started_at?: string
  ended_at?: string
}

export interface HealthRecordSummary {
  allergies: AllergyRecord[]
  chronic_diseases: ChronicDiseaseRecord[]
  family_histories: FamilyHistoryRecord[]
  surgery_histories: SurgeryHistoryRecord[]
  medication_histories: MedicationHistoryRecord[]
}
