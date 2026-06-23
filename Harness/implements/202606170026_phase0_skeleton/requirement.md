# Phase 0 最小化骨架实现

## 目标

根据 `Docs/04_ood_phase0.md` 的 OOD 设计方案，在 `AIMedical/` 目录下实现 Phase 0 最小化骨架。

## 交付范围

### 后端 (AIMedical/backend/)

**父 POM**: `pom.xml` — Maven 多模块聚合父 POM，管理依赖版本

**common 模块**: 共享基础模块
- `base/BaseEntity.java` — JPA 实体基类 (id, createdAt, updatedAt, deleted)
- `base/BaseEnum.java` — 枚举基类接口
- `result/Result.java` — 统一响应包装
- `result/PageQuery.java` — 分页请求
- `result/PageResponse.java` — 分页响应
- `exception/ErrorCode.java` — 错误码接口
- `exception/BusinessException.java` — 业务异常基类
- `exception/GlobalErrorCode.java` — 全局错误码枚举
- `config/JpaConfig.java` — JPA 审计配置
- `config/JacksonConfig.java` — Jackson 配置 (snake_case)
- `config/GlobalExceptionHandler.java` — 全局异常处理

**modules/common-module/**: 公共业务模块
- `common-module-api/UserType.java` — 用户类型枚举
- `common-module-impl/permission/User.java` — 用户实体
- `common-module-impl/permission/Role.java` — 角色实体
- `common-module-impl/permission/Post.java` — 岗位实体
- `common-module-impl/permission/Function.java` — 功能权限实体
- `common-module-impl/permission/UserRepository.java` — 用户 Repository 骨架
- `common-module-impl/dict/` — 字典占位

**modules/ai/**: AI 能力模块
- `ai-api/AiService.java` — AI 能力接口集合 (13 个方法)
- `ai-api/AiResult.java` — AI 调用结果包装
- `ai-api/dto/` — 13 组 request/response DTO
- `ai-api/degradation/DegradationContext.java` — 降级上下文
- `ai-api/degradation/DegradationStrategy.java` — 降级策略接口
- `ai-impl/MockAiService.java` — Mock 实现
- `ai-impl/FallbackAiService.java` — 降级装饰器
- `ai-impl/NoOpDegradationStrategy.java` — 默认降级策略

**业务模块**: patient, doctor, admin
- 每个模块含占位 Controller, Service 接口+实现, Repository 骨架, Entity 骨架, DTO 占位, Converter 占位

**application 模块**: 启动聚合
- `Application.java` — 启动类
- `HealthController.java` — `/api/ping` 健康检查
- `SecurityConfigPhase0.java` — Phase 0 permitAll 安全配置
- `application.yml`, `application-dev.yml`, `application-prod.yml`

**integration 模块**: 集成测试
- 占位集成测试类
- Failsafe 插件配置

### 前端 (AIMedical/frontend/)

- `package.json` (workspace root) — npm workspaces 配置
- `tsconfig.base.json` — TypeScript 共享配置
- `packages/shared/` — 共享库 (ApiClient, 类型定义, 工具函数)
- `packages/ui-core/` — 共享 UI 组件库占位
- `apps/patient/` — 患者端占位首页
- `apps/doctor/` — 医生端占位首页
- `apps/admin/` — 管理员端占位首页

### 验收标准

1. 后端可在 `AIMedical/backend/` 用 `mvn spring-boot:run -pl application -am` 启动
2. `GET /api/ping` 返回 `Result<String> { code: "SUCCESS", data: "pong" }`
3. 三端前端可在 `AIMedical/frontend/` 用 `npm run dev` (各自目录) 启动到占位首页
4. 每个模块至少一个占位单元测试类

## 项目根目录

`C:\Develop\Software\AIMedicalSys\AIMedical`
