# 审查范围界定

## 基本信息

- **源分支**: `202606171600_diagnosis_impl`
- **目标分支**: `main`
- **审查依据**: `Docs/04_ood_phase0.md` — Phase 0 架构级 OOD 设计方案
- **审查分支**: `202606171940_diagnosis_review`

## 审查目标

验证 `202606171600_diagnosis_impl` 分支的代码实现是否与 `Docs/04_ood_phase0.md` 定义的 Phase 0 架构设计一致，确保骨架代码的正确性、设计合理性、可维护性和测试覆盖。

## 审查范围

### 包含的模块（业务源码）

**后端 Java (AIMedical/backend/)**:
1. **common 模块** — BaseEntity, BaseEnum, Result<T>, PageQuery, PageResponse<T>, ErrorCode interface, GlobalErrorCode enum, BusinessException, GlobalExceptionHandler, JacksonConfig, JpaConfig
2. **ai-api 子模块** — AiService 接口(13方法), AiResult<T>, DegradationContext, DegradationStrategy, 13 组 AI DTO (TriageDiagnosis/PrescriptionCheck/MedicalRecordGen等)
3. **ai-impl 子模块** — MockAiService, FallbackAiService, NoOpDegradationStrategy
4. **common-module-api 子模块** — UserType 枚举
5. **common-module-impl 子模块** — User, Role, Post, Function 实体 + UserRepository
6. **patient/doctor/admin 业务模块** — Controller, Service(interface+impl), Entity, Repository, Converter, DTO
7. **application 模块** — Application启动类(@SpringBootApplication扫描配置), HealthController(/api/ping), SecurityConfigPhase0(@Profile("phase0") permitAll)
8. **application 配置** — application.yml(profiles.active=phase0,dev, server.port=8080), application-prod.yml(H2关闭), **application-dev.yml(H2/ai.mock.enabled/springdoc配置)**
9. **integration 模块** — Failsafe集成测试, ApplicationContextIT, HealthCheckIT
10. **所有 POM 文件** — 父POM(依赖管理+模块聚合+ignoredUnusedDeclaredDependencies), 各模块POM, 聚合POM(common-module, ai), application的spring-boot-maven-plugin(classifier=exec)

**后端补充检查项**:
- Springdoc-openapi配置(application-dev.yml: /v3/api-docs, /swagger-ui.html)
- Actuator/Micrometer配置(management.endpoints.web.exposure.include)
- MeterRegistryCustomizer占位配置(common.config)
- Frontend Vite代理配置(/api→localhost:8080)

**前端 TypeScript/Vue (AIMedical/frontend/)**:
1. **apps/patient, apps/doctor, apps/admin** — 三端占位应用(index.html, App.vue, main.ts, vite.config.ts含proxy, tsconfig.json, package.json@aimedical/app-*, env.d.ts)
2. **packages/shared** — ApiClient(Axios实例+拦截器+Result拆包), TypeScript类型定义(snake_case), 工具函数
3. **packages/ui-core** — 共享UI组件库入口(@aimedical/ui-core, 依赖@aimedical/shared)
4. **根配置** — package.json(workspaces: [packages/*, apps/*]), tsconfig.base.json, .gitignore

### 排除的范围

- `Docs/` 下的文档文件（非源码，设计文档本身不作为审查对象）
- `Harness/` 下的审议与实现制品（历史记录，非本轮审查目标）
- `.gitignore`、`LICENSE`、`README.md` 等仓库元数据文件
- 前端 `pnpm-lock.yaml`（自动生成文件，不纳入审核）
- CI 流水线脚本（非代码交付物）
- AuthStore/Pinia（Phase 1+，Phase 0 仅预留类型和目录约定）

## 审查重点维度

1. **与 OOD 设计一致性**: 代码是否遵循 `Docs/04_ood_phase0.md` 定义的模块划分、依赖方向、包命名、核心抽象、接口契约
2. **正确性**: 逻辑、边界条件、异常路径、类型安全、错误处理
3. **设计合理性**: 职责划分、抽象层次、依赖方向、可测试性
4. **代码质量**: 命名、结构、控制流、必要的测试覆盖
5. **POM 配置正确性**: 依赖声明、模块聚合、插件配置与 OOD 设计一致

## 审查轮次规划

按模块分组，每轮 3 个 agent 并行审查:
- **R1**: common 模块（基础骨架 + 异常 + 结果封装）
- **R2**: AI 模块（ai-api + ai-impl）
- **R3**: 权限模块（common-module-api + common-module-impl）
- **R4**: 业务模块（patient + doctor + admin）
- **R5**: application + integration + POM 配置
- **R6**: 前端（三端应用 + shared + ui-core）
