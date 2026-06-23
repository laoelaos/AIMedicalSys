# 审查范围界定

## 审查依据
- **设计文档**: `Docs/04_ood_phase0.md` — Phase 0 最小化骨架架构级 OOD 设计方案
- **审查分支**: `202606170026_phase0_skeleton`
- **审查目标分支**: `main`
- **审查基线**: squash merge 暂存区全部变更（271 文件，12753 行新增）

## 审查目标
验证当前分支代码实现与 OOD 设计文档的一致性，确保 Phase 0 骨架代码正确实现了设计意图。

## 审查重点

### 1. 后端 Common 模块（一致性审查）
- `Result<T>` 统一响应包装 — 与 §3.1 设计一致
- `PageQuery` / `PageResponse<T>` 分页规范 — 与 §3.1 设计一致
- `ErrorCode` interface + `GlobalErrorCode` enum — 与 §3.1 设计一致
- `BusinessException` 业务异常基类 — 与 §3.1 设计一致
- `GlobalExceptionHandler` 全局异常处理 — 与 §3.1 设计一致
- `BaseEntity` 实体基类 — 与 §3.2 设计一致（软删除、审计字段）
- `JpaConfig` / `JacksonConfig` 配置 — 与 §3.2 设计一致
- `BaseEnum` 枚举基类 — 检查是否在设计中定义

### 2. 后端 POM 与依赖管理（一致性审查）
- 父 POM `pom.xml` 模块声明与 §2.1 设计一致
- 依赖管理（dependencyManagement）与 §2.2 设计一致
- common 模块 optional 依赖策略与 §2.2 一致
- 聚合 POM（common-module、ai）与 §2.1 设计一致
- `maven-dependency-plugin` 豁免配置与 §2.2 一致
- 各模块 POM 依赖方向与 §2.2 模块依赖图一致

### 3. 业务模块（Patient/Doctor/Admin）（一致性审查）
- 目录结构和包命名与 §2.1/2.3 设计一致
- Controller 返回类型使用 Result<T>（§3.1）
- 占位 Controller 不含 AiService 注入（§2.2 约定）
- 实体继承 BaseEntity（§3.2）
- POM 依赖声明与 §2.2 一致

### 4. common-module-api/common-module-impl（一致性审查）
- `UserType` 枚举归属 common-module-api（§3.3）
- 权限实体（User, Role, Post, Function）归属 common-module-impl（§3.3）
- JPA 关系映射（ManyToMany、OneToMany、FetchType.LAZY）与 §3.3 一致
- 不设 cascade、join table 命名约定（§3.3）
- 数据范围扩展点仅文档记录（§3.3）

### 5. ai-api/ai-impl（一致性审查）
- `AiService` 接口定义与 §3.4 一致
- `AiResult<T>` 类结构与 §3.4 设计一致
- 13 个 AI 能力 DTO 定义与 §3.4 一致
- `MockAiService` 实现与 §3.4 Bean 装配策略一致
- `FallbackAiService` 装饰器实现与 §3.4 一致
- 降级策略框架（DegradationStrategy/Context/NoOpDegradationStrategy）与 §3.4 一致

### 6. Application 模块（一致性审查）
- `HealthController` 提供 `/api/ping` 端点（§4.1）
- `SecurityConfigPhase0` permitAll 占位（§2.2）
- application.yml 配置（§2.1）
- 启动类主入口

### 7. 测试覆盖与集成（质量审查）
- 各模块单元测试
- 集成测试（ApplicationContextIT, HealthCheckIT）
- 测试与设计的覆盖一致性

### 8. 前端 Monorepo（一致性审查）
- npm workspaces 配置与 §2.4 一致
- packages/shared 导出与 §2.4 一致
- packages/ui-core 导出与 §2.4 一致
- 三端应用结构与 §2.4 一致
- tsconfig.base.json 共享配置

## 排除范围
- 业务功能逻辑（Phase 0 仅为骨架占位）
- 真实 AI 接入（Phase 2+ 才引入）
- 认证/授权逻辑（Phase 1+）
- Docs/ 目录下的其他文档
- Harness/ 目录下的历史审议制品
- 性能优化

## 问题严重性分级
- **严重**: 与设计文档明确冲突，或导致骨架无法运行
- **一般**: 与设计文档部分偏离，但不影响骨架运行
- **轻微**: 代码风格、命名规范、注释等非功能性偏离
