# 详细设计（v7）

## 概述

R5 RETRY: T17（MessageInterpolator 组件抽取）全部代码已在 v6 中正确实现，无需修改任何源文件。当前任务仅为**修正验证构建命令**：从项目根目录 `backend/` 运行 `mvn test -pl common,modules/common-module -am`，确保 `-am` 标志将 `common` 模块纳入 reactor 并优先编译，消除 `common-module-impl` 测试编译时从本地 Maven 仓库解析旧版 `common` JAR 导致的 9 个"找不到符号 MessageInterpolator"错误。

## 文件规划

无。所有源文件已在 v6 中完成，无需新建或修改。

| 文件路径 | 操作 | 说明 |
|---------|------|------|
| 全部已有文件 | 不变 | 代码实现正确，无修改必要 |

## 验证命令

| 步骤 | 命令 | 工作目录 | 预期结果 |
|------|------|---------|---------|
| 编译 | `mvn compile -pl common,modules/common-module -am` | `backend/` (含 `pom.xml`) | BUILD SUCCESS |
| 测试 | `mvn test -pl common,modules/common-module -am` | `backend/` (含 `pom.xml`) | common: 136 pass / 0 fail / 5 skip; common-module-impl: 391 pass / 0 fail / 1 skip |

## 类型定义

完全沿用 v6 设计，无需变更：

- **MessageInterpolator** (`common/src/main/java/.../util/MessageInterpolator.java`) — 接口，`interpolate(String template, Object[] args)`
- **SimpleMessageInterpolator** (`common/src/main/java/.../util/SimpleMessageInterpolator.java`) — `@Component` 实现
- **GlobalExceptionHandler** (`common/src/main/java/.../config/GlobalExceptionHandler.java`) — 构造器注入 `MessageInterpolator`
- **RestAuthenticationEntryPoint** (`common-module-impl/.../auth/security/RestAuthenticationEntryPoint.java`) — 构造器注入 `MessageInterpolator`
- **RestAccessDeniedHandler** (`common-module-impl/.../auth/security/RestAccessDeniedHandler.java`) — 构造器注入 `MessageInterpolator`
- **SecurityConfigPhase1** (`common-module-impl/.../auth/security/SecurityConfigPhase1.java`) — `filterChain()` 参数注入 `MessageInterpolator`
- 以上全部已在 v6 中实现，无需修改

## 错误处理

沿用 v6 设计，无变更。

## 行为契约

沿用 v6 设计，无变更。

## 依赖关系

沿用 v6 设计，无变更。
