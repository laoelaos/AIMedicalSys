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
 * 登录请求类型
 */
export interface LoginRequest {
  username: string
  password: string
}

/**
 * 登录响应类型
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

// ===========================================================================
// 医生端业务类型（Phase 3）
//
// <p>所有字段遵循后端 JacksonConfig 全局 SNAKE_CASE 约定，与 doctor 模块的
// record DTO 一一对应。时间字段为 ISO-8601 字符串（后端 LocalDateTime 序列化）。
// ===========================================================================

/**
 * AI 接口统一响应包装。
 *
 * <p>三类状态：
 * - success=true, degraded=false：AI 正常，data 有效
 * - success=false, degraded=true：AI 不可用已降级，fallback_reason 说明原因，data 携带兜底建议
 * - success=false, degraded=false：AI 调用失败，error_code 提供错误码，data 为 null
 *
 * <p>对应后端 AiResultResponse<T>。
 */
export interface AiResultResponse<T = unknown> {
  success: boolean
  degraded: boolean
  fallback_reason: string | null
  error_code: string | null
  data: T | null
}

/** 业务错误码判断（非 BusinessError 的运行时类型守卫） */
export function isBusinessError(value: unknown): value is BusinessError {
  return (
    typeof value === 'object' &&
    value !== null &&
    (value as BusinessError).isBusinessError === true
  )
}

// ---- 处方中心 ----

/** 处方明细（响应）。对应后端 PrescriptionItemDto。 */
export interface PrescriptionItem {
  id: number | null
  drug_name: string
  specification: string
  dosage: string
  usage_method: string
  frequency: string
  quantity: number
  unit: string
  remark: string
}

/** 处方明细（创建请求）。对应后端 PrescriptionItemRequest。 */
export interface PrescriptionItemRequest {
  drug_name: string
  specification: string
  dosage: string
  usage_method: string
  frequency: string
  quantity: number
  unit: string
  remark: string
}

/** 处方状态机枚举值（字符串，与后端 PrescriptionStatus.code 一致）。 */
export type PrescriptionStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED'

/** 创建处方请求。对应后端 PrescriptionCreateRequest。 */
export interface PrescriptionCreateRequest {
  patient_id: number
  diagnosis: string
  remark: string
  submit_for_review: boolean
  items: PrescriptionItemRequest[]
}

/** 处方审核请求。对应后端 PrescriptionAuditRequest。 */
export interface PrescriptionAuditRequest {
  approve: boolean
  audit_remark: string
}

/** 处方响应。对应后端 PrescriptionResponse。 */
export interface PrescriptionResponse {
  id: number
  patient_id: number
  patient_name: string
  doctor_id: number
  department: string
  status: PrescriptionStatus
  diagnosis: string
  ai_checked: boolean
  ai_risk_level: string | null
  audit_remark: string | null
  audited_by: number | null
  audited_at: string | null
  remark: string | null
  created_at: string
  updated_at: string
  items: PrescriptionItem[]
}

// ---- 病历中心 ----

/** 病历状态枚举值。 */
export type MedicalRecordStatus = 'DRAFT' | 'OFFICIAL'

/** 创建/更新病历请求。对应后端 MedicalRecordCreateRequest。 */
export interface MedicalRecordCreateRequest {
  patient_id: number
  template_id: number | null
  prescription_id: number | null
  chief_complaint: string
  present_illness: string
  past_history: string
  diagnosis: string
  treatment_plan: string
  remark: string
  publish: boolean
}

/** 病历响应。对应后端 MedicalRecordResponse。 */
export interface MedicalRecordResponse {
  id: number
  patient_id: number
  doctor_id: number
  department: string
  version_no: number
  status: MedicalRecordStatus
  chief_complaint: string
  present_illness: string
  past_history: string
  diagnosis: string
  treatment_plan: string
  prescription_id: number | null
  template_id: number | null
  ai_generated: boolean
  remark: string | null
  created_at: string
  updated_at: string
}

/** 病历模板响应。对应后端 MedicalRecordTemplateResponse。 */
export interface MedicalRecordTemplateResponse {
  id: number
  department: string
  name: string
  chief_complaint_tpl: string
  present_illness_tpl: string
  past_history_tpl: string
  diagnosis_tpl: string
  treatment_plan_tpl: string
  enabled: boolean
  remark: string | null
}

// ---- 挂号/叫号队列 ----

/** 叫号状态枚举值。 */
export type ConsultationStatus =
  | 'WAITING'
  | 'CALLED'
  | 'IN_CONSULTATION'
  | 'FINISHED'
  | 'SKIPPED'

/** 叫号队列响应。对应后端 ConsultationQueueResponse。 */
export interface ConsultationQueueResponse {
  id: number
  patient_id: number
  patient_name: string
  doctor_id: number
  department: string
  queue_no: string
  status: ConsultationStatus
  registered_at: string
  called_at: string | null
  finished_at: string | null
  remark: string | null
}

// ---- AI 子类型 ----

/** AI 辅助诊断请求。对应后端 AiDiagnosisRequest。 */
export interface AiDiagnosisRequest {
  patient_id: number | null
  chief_complaint: string
  present_illness: string
  past_history: string
}

/** AI 辅助诊断响应。对应后端 AiDiagnosisResponse。 */
export interface AiDiagnosisResponse {
  possible_diagnoses: string[]
  summary: string
}

/** AI 开立检查请求。对应后端 AiExaminationRequest。 */
export interface AiExaminationRequest {
  patient_id: number | null
  diagnosis: string
  chief_complaint: string
}

/** AI 开立检查推荐项。对应后端 AiExaminationResponse.ExaminationItem。 */
export interface ExaminationItem {
  name: string
  category: string
  reason: string
}

/** AI 开立检查响应。对应后端 AiExaminationResponse。 */
export interface AiExaminationResponse {
  items: ExaminationItem[]
}

/** AI 辅助开方请求。对应后端 AiPrescriptionAssistRequest。 */
export interface AiPrescriptionAssistRequest {
  patient_id: number | null
  diagnosis: string
  chief_complaint: string
}

/** AI 推荐药品。对应后端 AiPrescriptionAssistResponse.RecommendedDrug。 */
export interface RecommendedDrug {
  drug_name: string
  specification: string
  dosage: string
  frequency: string
  reason: string
}

/** AI 辅助开方响应。对应后端 AiPrescriptionAssistResponse。 */
export interface AiPrescriptionAssistResponse {
  drugs: RecommendedDrug[]
  summary: string
}

/** AI 处方审核请求。对应后端 AiPrescriptionAuditRequest。 */
export interface AiPrescriptionAuditRequest {
  prescription_id: number
  diagnosis: string
  drug_names: string[]
}

/** AI 处方审核响应。对应后端 AiPrescriptionAuditResponse。 */
export interface AiPrescriptionAuditResponse {
  risk_level: string
  warnings: string[]
  passed: boolean
}

/** AI 病历生成请求。对应后端 AiMedicalRecordGenRequest。 */
export interface AiMedicalRecordGenRequest {
  patient_id: number
  template_id: number | null
  chief_complaint: string
  present_illness: string
  past_history: string
  diagnosis: string
}

/** AI 病历生成响应。对应后端 AiMedicalRecordGenResponse。 */
export interface AiMedicalRecordGenResponse {
  chief_complaint: string
  present_illness: string
  past_history: string
  diagnosis: string
  treatment_plan: string
}
