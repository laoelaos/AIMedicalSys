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

// ==================== Auth (fork: patient-centric) ====================

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

// ==================== Upstream: Doctor/Admin Auth & Menu ====================

/**
 * 用户信息类型
 *
 * <p>字段命名遵循后端 Jackson SNAKE_CASE 全局约定（JacksonConfig.java），
 * 因此保持 snake_case 以与 /api/auth/me 等响应直接对齐。
 */
export interface UserInfo {
  id: number
  username: string
  real_name: string
  role: string
  position?: string
  permissions?: string[]
}

/**
 * 菜单项类型
 *
 * <p>与后端 MenuResponse record（JacksonConfig 全局 SNAKE_CASE）保持一致，
 * 注意排序字段名为 `sort` 而非 `sortOrder`。
 */
export interface MenuItem {
  id: number
  name: string
  path: string
  component?: string | null
  icon?: string
  permission?: string
  sort?: number
  children?: MenuItem[]
}

/**
 * Doctor/Admin 登录请求类型 (username-based)
 */
export interface DoctorLoginRequest {
  username: string
  password: string
}

/**
 * Doctor/Admin 登录响应类型
 *
 * <p>与后端 LoginResponse record（JacksonConfig 全局 SNAKE_CASE）保持一致：
 * 字段为 snake_case，对应 /api/auth/login 返回的 data。
 */
export interface LoginResponse {
  user_id: number
  username: string
  access_token: string
  refresh_token: string
  token_type: string
  expires_in: number
  password_change_required: boolean
  user: UserInfo
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
  emergency_contact?: string
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
