# R3: Business Modules (Patient/Doctor/Admin) + common-module — consistency with OOD design

审查时间：2026-06-17

### 审查范围

**Part A: Patient Module**
- `AIMedical/backend/patient/src/main/java/com/aimedical/modules/patient/api/PatientController.java`
- `AIMedical/backend/patient/src/main/java/com/aimedical/modules/patient/dto/PatientDto.java`
- `AIMedical/backend/patient/src/main/java/com/aimedical/modules/patient/entity/PatientEntity.java`
- `AIMedical/backend/patient/src/main/java/com/aimedical/modules/patient/repository/PatientRepository.java`
- `AIMedical/backend/patient/src/main/java/com/aimedical/modules/patient/service/PatientService.java`
- `AIMedical/backend/patient/src/main/java/com/aimedical/modules/patient/service/impl/PatientServiceImpl.java`
- `AIMedical/backend/patient/src/main/java/com/aimedical/modules/patient/converter/PatientConverter.java`

**Part B: Doctor Module**
- `AIMedical/backend/doctor/src/main/java/com/aimedical/modules/doctor/api/DoctorController.java`
- `AIMedical/backend/doctor/src/main/java/com/aimedical/modules/doctor/entity/DoctorEntity.java`
- `AIMedical/backend/doctor/src/main/java/com/aimedical/modules/doctor/service/impl/DoctorServiceImpl.java`

**Part C: Admin Module**
- `AIMedical/backend/admin/src/main/java/com/aimedical/modules/admin/api/AdminController.java`
- `AIMedical/backend/admin/src/main/java/com/aimedical/modules/admin/entity/AdminEntity.java`
- `AIMedical/backend/admin/src/main/java/com/aimedical/modules/admin/service/impl/AdminServiceImpl.java`

**Part D: common-module-api**
- `AIMedical/backend/common-module-api/src/main/java/com/aimedical/modules/commonmodule/api/UserType.java`

**Part E: common-module-impl (Permission Entities)**
- `AIMedical/backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java`
- `AIMedical/backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Role.java`
- `AIMedical/backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Post.java`
- `AIMedical/backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Function.java`
- `AIMedical/backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/UserRepository.java`

**Part F: Application Module**
- `AIMedical/backend/application/src/main/java/com/aimedical/Application.java`
- `AIMedical/backend/application/src/main/java/com/aimedical/HealthController.java`
- `AIMedical/backend/application/src/main/java/com/aimedical/config/SecurityConfigPhase0.java`
- `AIMedical/backend/application/src/main/resources/application.yml`
- `AIMedical/backend/application/src/main/resources/application-prod.yml`

### 发现

#### [轻微] Business entities (PatientEntity, DoctorEntity, AdminEntity) missing `@Table` annotation

- **位置**：
  - `patient/.../entity/PatientEntity.java:6`
  - `doctor/.../entity/DoctorEntity.java:6`
  - `admin/.../entity/AdminEntity.java:6`
- **描述**：三个业务模块的实体类标注了 `@Entity`，但未标注 `@Table`。Permission 实体（User、Role、Post、Function）均显式标注 `@Table(name = "sys_xxx")`（见 §3.3）。缺少 `@Table` 时 JPA 会使用实体类名自动生成表名（如 `patient_entity`），与 permission 实体的 `sys_` 前缀命名风格不一致。Phase 0 当前无业务字段，不影响功能。
- **建议**：为三个业务实体补充 `@Table` 注解，与 permission 实体保持一致的命名风格，例如 `@Table(name = "sys_patient")`。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 0 |
| 轻微 | 1 |

### 总评

24 个审查文件整体与 OOD Phase 0 设计方案高度一致。目录结构与包命名完全遵循 §2.1/2.3 规范（三个业务模块均包含 api/dto/service/repository/entity/converter 六个子包）。Controller 统一使用 `Result<T>` 返回类型、RequestMapping 前缀 `/api/{module}`，未注入 `AiService`（符合 §2.2 Phase 0 约定）。Permission 实体的 JPA 关系映射（`@ManyToMany`/`@OneToMany`/`@ManyToOne`、`FetchType.LAZY`、无 cascade、JoinTable 命名）严格遵循 §3.3 映射约定。HealthController 位于 application 模块 `com.aimedical` 根包下、提供 `GET /api/ping → Result.success("pong")`，符合 §4.1。SecurityConfigPhase0 配置 `permitAll` 占位策略与 §4.5 一致。唯一轻微发现是三个业务实体缺少 `@Table` 注解，属于 Phase 0 占位期的细微风格偏离。
