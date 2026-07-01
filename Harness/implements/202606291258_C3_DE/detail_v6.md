# 详细设计（v6）

## 概述

在 common-module-api 中新增跨模块门面接口（DoctorFacade、DrugFacade、VisitFacade）、DTO（AvailableDoctor、DrugInfo）及事件类（RegistrationEvent），作为 T7（模块骨架）及后续 consultation/prescription/medical-record 模块的前置编译依赖。与已有 UserFacade 门面模式保持一致。

## 文件规划

| 文件路径（相对于 common-module-api/src/main/java/...） | 操作 | 职责 |
|---------|------|------|
| `com/aimedical/modules/commonmodule/doctor/AvailableDoctor.java` | 新建 | 可预约医生 DTO |
| `com/aimedical/modules/commonmodule/doctor/DoctorFacade.java` | 新建 | 跨模块医生排班查询门面 |
| `com/aimedical/modules/commonmodule/drug/DrugInfo.java` | 新建 | 药品信息 DTO，DrugFacade 返回值 |
| `com/aimedical/modules/commonmodule/drug/DrugFacade.java` | 新建 | 跨模块药品信息查询门面 |
| `com/aimedical/modules/commonmodule/visit/VisitFacade.java` | 新建 | 跨模块就诊标识查询门面 |
| `com/aimedical/modules/commonmodule/event/RegistrationEvent.java` | 新建 | 挂号事件契约 |

## 类型定义

### AvailableDoctor

**形态**：record（DTO）
**包路径**：`com.aimedical.modules.commonmodule.doctor`
**职责**：可预约医生信息 DTO，DoctorFacade.findAvailableDoctorsByDepartment() 返回值

```java
package com.aimedical.modules.commonmodule.doctor;

public record AvailableDoctor(
    String doctorId,
    String doctorName,
    String departmentId,
    int availableSlotCount
) {}
```

**构造方式**：编译器生成的 canonical constructor
**类型关系**：独立 DTO，无继承/实现

### DoctorFacade

**形态**：interface
**包路径**：`com.aimedical.modules.commonmodule.doctor`
**职责**：跨模块医生排班查询门面，定义在 common-module-api，由 doctor-module 实现

```java
package com.aimedical.modules.commonmodule.doctor;

import java.util.List;

public interface DoctorFacade {
    List<AvailableDoctor> findAvailableDoctorsByDepartment(String departmentId);
}
```

**公开接口**：
- `List<AvailableDoctor> findAvailableDoctorsByDepartment(String departmentId)` — 查询指定科室当前有排班的医生列表

**构造方式**：无（接口）
**类型关系**：由 doctor-module 实现，application 聚合后 Spring 自动注入（与 UserFacade 模式一致）

### DrugInfo

**形态**：record（DTO）
**包路径**：`com.aimedical.modules.commonmodule.drug`
**职责**：药品信息 DTO，DrugFacade.findByDrugCode() 返回值

```java
package com.aimedical.modules.commonmodule.drug;

public record DrugInfo(
    String drugCode,
    String drugName,
    String specification,
    String dosageForm,
    String manufacturer,
    String packageUnit
) {}
```

**构造方式**：编译器生成的 canonical constructor
**类型关系**：独立 DTO，无继承/实现

### DrugFacade

**形态**：interface
**包路径**：`com.aimedical.modules.commonmodule.drug`
**职责**：跨模块药品信息查询门面，定义在 common-module-api，由 drug-module 实现

```java
package com.aimedical.modules.commonmodule.drug;

public interface DrugFacade {
    DrugInfo findByDrugCode(String drugCode);
}
```

**公开接口**：
- `DrugInfo findByDrugCode(String drugCode)` — 根据药品编码查询药品名称/规格信息

**构造方式**：无（接口）
**类型关系**：由 drug-module 实现，application 聚合后 Spring 自动注入

### VisitFacade

**形态**：interface
**包路径**：`com.aimedical.modules.commonmodule.visit`
**职责**：跨模块就诊标识查询门面，定义在 common-module-api，由 visit-module 实现

```java
package com.aimedical.modules.commonmodule.visit;

public interface VisitFacade {
    String findVisitIdByEncounterId(String encounterId);
}
```

**公开接口**：
- `String findVisitIdByEncounterId(String encounterId)` — encounterId → visitId 转换

**构造方式**：无（接口）
**类型关系**：由 visit-module 实现，application 聚合后 Spring 自动注入

### RegistrationEvent

**形态**：class
**包路径**：`com.aimedical.modules.commonmodule.event`
**职责**：挂号事件契约，registration 模块发布 → consultation 模块消费

```java
package com.aimedical.modules.commonmodule.event;

import java.time.LocalDateTime;

public class RegistrationEvent {

    private Long registrationId;
    private String patientId;
    private String sessionId;
    private String departmentId;
    private String departmentName;
    private Long doctorId;
    private LocalDateTime eventTime;

    public RegistrationEvent() {}

    public RegistrationEvent(Long registrationId, String patientId, String sessionId,
                             String departmentId, String departmentName, Long doctorId,
                             LocalDateTime eventTime) {
        this.registrationId = registrationId;
        this.patientId = patientId;
        this.sessionId = sessionId;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.doctorId = doctorId;
        this.eventTime = eventTime;
    }

    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
}
```

**公开接口**：
- 无参构造器 + 全参构造器
- 每个字段的 getter/setter

**构造方式**：无参构造器或全参构造器
**类型关系**：独立事件类，无继承/实现。registration 模块发布，consultation 模块消费，application 模块聚合后通过 Spring ApplicationEvent 跨模块传播。

## 错误处理

- 所有类型仅声明数据结构和行为契约，不包含业务逻辑或异常抛出。
- 运行时错误由实现模块处理（调用超时、降级等），不在 API 层定义自定义异常。
- 方法入参 null 校验由实现模块按需处理。

## 行为契约

### DoctorFacade.findAvailableDoctorsByDepartment
- **前置条件**：departmentId 不为 null
- **后置条件**：返回指定科室当前有排班的医生列表；无排班或 departmentId 无对应科室时返回空列表（非 null）
- **降级策略**：调用超时（2s）或异常时由 TriageServiceImpl 捕获，将 doctors 置为空列表，记录 WARN 日志

### DrugFacade.findByDrugCode
- **前置条件**：drugCode 不为 null
- **后置条件**：返回匹配的药品信息；drugCode 无对应药品时返回 null
- **降级策略**：调用超时（2s）或异常时返回空 DrugInfo + WARN 日志

### VisitFacade.findVisitIdByEncounterId
- **前置条件**：encounterId 不为 null
- **后置条件**：返回对应的 visitId；encounterId 无对应就诊记录时返回 null
- **降级策略**：调用超时或异常时 MedicalRecordService 降级为 encounterId fallback 或返回 MR_GEN_VISIT_NOT_FOUND

### RegistrationEvent
- sessionId 为可选字段（nullable）：前端在分诊结束后保留并传入挂号流程，registration 模块接收后填充至事件；未传递时消费端降级为按 patientId 匹配最近分诊记录
- registration 模块发布事件，consultation 模块的 RegistrationEventListener 消费

## 依赖关系

- **编译期依赖**：仅 JDK 内置类型（java.util.List、java.time.LocalDateTime），无外部依赖新增
- **已有参考类型**：`auth/UserFacade.java`（接口模式）、`auth/UserInfoResponse.java`（DTO record 模式）
- **被依赖**（后续任务）：
  - consultation 模块 `TriageServiceImpl` → 依赖 `DoctorFacade`
  - prescription 模块 → 依赖 `DrugFacade`
  - medical-record 模块 → 依赖 `VisitFacade`
  - consultation 模块 `RegistrationEventListener` → 依赖 `RegistrationEvent`
