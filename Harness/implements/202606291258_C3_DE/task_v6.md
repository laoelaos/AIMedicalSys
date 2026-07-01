# 任务指令（v6）

## 动作
NEW

## 任务描述
在 `common-module-api` 中新增五个类型：**DoctorFacade**（interface）、**AvailableDoctor**（DTO）、**DrugFacade**（interface）、**VisitFacade**（interface）、**RegistrationEvent**（class），分别位于 `doctor/`、`drug/`、`visit/`、`event/` 子包。

| 文件路径（相对于 common-module-api/src/main/java/...） | 形态 | 职责 |
|------|------|------|
| `.../doctor/AvailableDoctor.java` | DTO class | 可预约医生信息，含 doctorId/doctorName/departmentId/availableSlotCount |
| `.../doctor/DoctorFacade.java` | interface | 跨模块医生排班查询门面，提供 findAvailableDoctorsByDepartment |
| `.../drug/DrugFacade.java` | interface | 跨模块药品信息查询门面，提供 findByDrugCode 方法 |
| `.../visit/VisitFacade.java` | interface | 跨模块就诊标识查询门面，提供 findVisitIdByEncounterId |
| `.../event/RegistrationEvent.java` | class | 挂号事件契约，含 registrationId/patientId/sessionId/departmentId/departmentName/doctorId/eventTime |

包基路径：`com.aimedical.modules.commonmodule`

## 选择理由
门面接口和事件是 T7（模块骨架）和 T8（consultation）/T9/T10（prescription）/T11（medical-record）的前置编译依赖。已完成任务 T1–T4/T6 满足全部前置条件，T5 无其他待办依赖，是当前唯一可推进的任务。

## 任务上下文

### DoctorFacade
- **子包**：`com.aimedical.modules.commonmodule.doctor`
- **方法**：`List<AvailableDoctor> findAvailableDoctorsByDepartment(String departmentId)` — 查询指定科室当前有排班的医生列表
- **AvailableDoctor DTO 字段**：doctorId（String）、doctorName（String）、departmentId（String）、availableSlotCount（int）
- **协作模式**：定义在 common-module-api 中，由 doctor 模块实现，application 聚合后 Spring 自动注入。与 UserFacade 模式一致。
- **降级保护**：配置独立超时阈值（consultation.doctor-facade.timeout=2s），调用超时或异常时 TriageServiceImpl 捕获并将 doctors 置为空列表，不阻断分诊主流程，记录 WARN 日志。

### DrugFacade
- **子包**：`com.aimedical.modules.commonmodule.drug`
- **方法**：`DrugInfo findByDrugCode(String drugCode)` — 查询药品名称/规格信息（供 prescription 模块使用）
- **DrugInfo DTO 字段**：drugCode（String）、drugName（String）、specification（String）、dosageForm（String）、manufacturer（String）、packageUnit（String）
- **协作模式**：与 DoctorFacade 一致的跨模块门面模式，由 drug 模块实现。
- **降级保护**：配置独立超时阈值（prescription.drug-facade.timeout=2s），调用超时或失败时返回空药品信息 + WARN 日志，不阻断辅助开方/处方审核主流程。

### VisitFacade
- **子包**：`com.aimedical.modules.commonmodule.visit`
- **方法**：`String findVisitIdByEncounterId(String encounterId)` — encounterId → visitId 转换
- **协作模式**：由 visit 模块实现，供 medical-record 模块使用。
- **降级保护**：配置独立超时阈值（medical-record.visit-facade.timeout=2s），调用超时或失败时 MedicalRecordService 降级为 encounterId fallback 或返回 MR_GEN_VISIT_NOT_FOUND。

### RegistrationEvent
- **子包**：`com.aimedical.modules.commonmodule.event`
- **字段**：registrationId（Long）、patientId（String）、sessionId（String，nullable）、departmentId（String）、departmentName（String）、doctorId（Long）、eventTime（LocalDateTime）
- **职责**：挂号事件契约，registration 模块发布 → consultation 模块消费。sessionId 可选（前端传递分诊 sessionId 以精确关联 TriageRecord，未传时消费端降级为按 patientId 匹配最近分诊记录）。
- **构造方式**：全参构造器 + 无参构造器；提供 getter/setter

## 已有代码上下文
- common-module-api 已有 `auth/UserFacade.java` 作为门面接口的参考示例
- 项目目录结构：`common-module-api/src/main/java/com/aimedical/modules/commonmodule/` 下已有 `auth/`、`store/`、`api/` 子包，需新建 `doctor/`、`drug/`、`visit/`、`event/` 子包
- 所有新类型仅依赖 JDK 内置类型（java.util.List、java.time.LocalDateTime、java.util.Optional），无需新增外部依赖
- common-module-api 的父 pom 和 spring-boot-starter 依赖已满足编译需求
