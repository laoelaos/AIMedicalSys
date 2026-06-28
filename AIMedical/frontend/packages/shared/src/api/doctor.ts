import type { BusinessError } from '../types'
import { apiGet, apiPost } from './client'

import type {
  AiDiagnosisRequest,
  AiDiagnosisResponse,
  AiExaminationRequest,
  AiExaminationResponse,
  AiMedicalRecordGenRequest,
  AiMedicalRecordGenResponse,
  AiPrescriptionAssistRequest,
  AiPrescriptionAssistResponse,
  AiPrescriptionAuditRequest,
  AiPrescriptionAuditResponse,
  AiResultResponse,
  ConsultationQueueResponse,
  MedicalRecordCreateRequest,
  MedicalRecordResponse,
  MedicalRecordTemplateResponse,
  PrescriptionAuditRequest,
  PrescriptionCreateRequest,
  PrescriptionResponse,
} from '../types'

/**
 * 医生端 API
 *
 * <p>对应后端 /api/doctor/* 系列接口。所有方法返回 Promise<T | BusinessError>，
 * 调用方需用 isBusinessError() 判断结果。
 *
 * <p>注意：AI 相关接口返回的是 AiResultResponse<T>（已被响应拦截器解包外层 Result），
 * 其内部的 success/degraded 字段标识 AI 是否可用，前端据此展示降级 UI。
 */
export const doctorApi = {
  // ---- 挂号/叫号队列 ----

  /** 查询当前医生的活跃队列（候诊+已叫号+接诊中）。GET /api/doctor/queue */
  listMyQueue: (): Promise<ConsultationQueueResponse[] | BusinessError> => {
    return apiGet<ConsultationQueueResponse[]>('/doctor/queue')
  },

  /** 查询当前医生的候诊队列（仅 WAITING）。GET /api/doctor/queue/waiting */
  listWaiting: (): Promise<ConsultationQueueResponse[] | BusinessError> => {
    return apiGet<ConsultationQueueResponse[]>('/doctor/queue/waiting')
  },

  /** 叫下一位患者（WAITING -> CALLED）。POST /api/doctor/queue/call-next */
  callNext: (): Promise<ConsultationQueueResponse | BusinessError> => {
    return apiPost<ConsultationQueueResponse>('/doctor/queue/call-next')
  },

  /** 开始接诊（CALLED -> IN_CONSULTATION）。POST /api/doctor/queue/{id}/start */
  startConsultation: (id: number): Promise<ConsultationQueueResponse | BusinessError> => {
    return apiPost<ConsultationQueueResponse>(`/doctor/queue/${id}/start`)
  },

  /** 完成接诊（IN_CONSULTATION -> FINISHED）。POST /api/doctor/queue/{id}/finish */
  finishConsultation: (id: number): Promise<ConsultationQueueResponse | BusinessError> => {
    return apiPost<ConsultationQueueResponse>(`/doctor/queue/${id}/finish`)
  },

  /** 过号（WAITING/CALLED -> SKIPPED）。POST /api/doctor/queue/{id}/skip */
  skipQueue: (id: number): Promise<ConsultationQueueResponse | BusinessError> => {
    return apiPost<ConsultationQueueResponse>(`/doctor/queue/${id}/skip`)
  },

  // ---- 处方中心 ----

  /** 创建处方（草稿或直接提交审核）。POST /api/doctor/prescriptions */
  createPrescription: (
    request: PrescriptionCreateRequest,
  ): Promise<PrescriptionResponse | BusinessError> => {
    return apiPost<PrescriptionResponse>('/doctor/prescriptions', request)
  },

  /** 获取处方详情。GET /api/doctor/prescriptions/{id} */
  getPrescription: (id: number): Promise<PrescriptionResponse | BusinessError> => {
    return apiGet<PrescriptionResponse>(`/doctor/prescriptions/${id}`)
  },

  /** 按患者查询处方列表。GET /api/doctor/prescriptions?patientId= */
  listPrescriptionsByPatient: (
    patientId: number,
  ): Promise<PrescriptionResponse[] | BusinessError> => {
    return apiGet<PrescriptionResponse[]>('/doctor/prescriptions', {
      params: { patientId },
    })
  },

  /** 提交处方审核（DRAFT/REJECTED -> PENDING_REVIEW）。POST /api/doctor/prescriptions/{id}/submit */
  submitPrescription: (id: number): Promise<PrescriptionResponse | BusinessError> => {
    return apiPost<PrescriptionResponse>(`/doctor/prescriptions/${id}/submit`)
  },

  /** 审核处方（PENDING_REVIEW -> APPROVED/REJECTED）。POST /api/doctor/prescriptions/{id}/audit */
  auditPrescription: (
    id: number,
    request: PrescriptionAuditRequest,
  ): Promise<PrescriptionResponse | BusinessError> => {
    return apiPost<PrescriptionResponse>(`/doctor/prescriptions/${id}/audit`, request)
  },

  // ---- 病历中心 ----

  /** 创建或更新草稿病历（publish=true 时同时发布为正式版本）。POST /api/doctor/medical-records */
  saveMedicalRecord: (
    request: MedicalRecordCreateRequest,
  ): Promise<MedicalRecordResponse | BusinessError> => {
    return apiPost<MedicalRecordResponse>('/doctor/medical-records', request)
  },

  /** 获取病历详情。GET /api/doctor/medical-records/{id} */
  getMedicalRecord: (id: number): Promise<MedicalRecordResponse | BusinessError> => {
    return apiGet<MedicalRecordResponse>(`/doctor/medical-records/${id}`)
  },

  /** 按患者查询病历列表（按版本号倒序）。GET /api/doctor/medical-records?patientId= */
  listMedicalRecordsByPatient: (
    patientId: number,
  ): Promise<MedicalRecordResponse[] | BusinessError> => {
    return apiGet<MedicalRecordResponse[]>('/doctor/medical-records', {
      params: { patientId },
    })
  },

  /** 将草稿病历发布为正式版本。POST /api/doctor/medical-records/{id}/publish */
  publishMedicalRecord: (id: number): Promise<MedicalRecordResponse | BusinessError> => {
    return apiPost<MedicalRecordResponse>(`/doctor/medical-records/${id}/publish`)
  },

  /** 按科室查询启用的病历模板列表。GET /api/doctor/medical-records/templates?department= */
  listMedicalRecordTemplates: (
    department: string,
  ): Promise<MedicalRecordTemplateResponse[] | BusinessError> => {
    return apiGet<MedicalRecordTemplateResponse[]>('/doctor/medical-records/templates', {
      params: { department },
    })
  },

  // ---- AI 入口（全部返回 AiResultResponse<T>，degraded=true 时需展示降级 UI） ----

  /** 占位诊断入口。POST /api/doctor/ai/diagnosis */
  aiDiagnosis: (
    request: AiDiagnosisRequest,
  ): Promise<AiResultResponse<AiDiagnosisResponse> | BusinessError> => {
    return apiPost<AiResultResponse<AiDiagnosisResponse>>('/doctor/ai/diagnosis', request)
  },

  /** 开立检查推荐入口。POST /api/doctor/ai/examination */
  aiExamination: (
    request: AiExaminationRequest,
  ): Promise<AiResultResponse<AiExaminationResponse> | BusinessError> => {
    return apiPost<AiResultResponse<AiExaminationResponse>>('/doctor/ai/examination', request)
  },

  /** 辅助开方入口。POST /api/doctor/ai/prescription-assist */
  aiPrescriptionAssist: (
    request: AiPrescriptionAssistRequest,
  ): Promise<AiResultResponse<AiPrescriptionAssistResponse> | BusinessError> => {
    return apiPost<AiResultResponse<AiPrescriptionAssistResponse>>(
      '/doctor/ai/prescription-assist',
      request,
    )
  },

  /** 处方审核入口。POST /api/doctor/ai/prescription-audit */
  aiPrescriptionAudit: (
    request: AiPrescriptionAuditRequest,
  ): Promise<AiResultResponse<AiPrescriptionAuditResponse> | BusinessError> => {
    return apiPost<AiResultResponse<AiPrescriptionAuditResponse>>(
      '/doctor/ai/prescription-audit',
      request,
    )
  },

  /** 病历生成入口。POST /api/doctor/ai/medical-record-gen */
  aiMedicalRecordGen: (
    request: AiMedicalRecordGenRequest,
  ): Promise<AiResultResponse<AiMedicalRecordGenResponse> | BusinessError> => {
    return apiPost<AiResultResponse<AiMedicalRecordGenResponse>>(
      '/doctor/ai/medical-record-gen',
      request,
    )
  },
}
