# 实现报告（v7）

## 概述

v7 为纯验证轮次。所有源文件已在 v6 中正确实现（MessageInterpolator 组件抽取 T17），无需新建或修改任何源文件。当前轮次仅修正验证构建命令路径：从 `AIMedical/backend/` 运行 `mvn ... -pl common,modules/common-module -am`。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 无 | 全部已有文件 | 源文件不变，v6 已完整实现 |

## 已确认源文件清单

| 文件 | 状态 |
|------|------|
| `common/.../util/MessageInterpolator.java` | 存在 |
| `common/.../util/SimpleMessageInterpolator.java` | 存在 |
| `common/.../config/GlobalExceptionHandler.java` | 存在 |
| `modules/common-module/common-module-impl/.../auth/security/RestAuthenticationEntryPoint.java` | 存在 |
| `modules/common-module/common-module-impl/.../auth/security/RestAccessDeniedHandler.java` | 存在 |
| `modules/common-module/common-module-impl/.../auth/security/SecurityConfigPhase1.java` | 存在 |

## 编译验证

工作目录：`AIMedical/backend/`（`pom.xml` 所在目录）

### 编译

```
mvn compile -pl common,modules/common-module/common-module-impl -am
```
BUILD SUCCESS — 所有模块编译通过，无符号找不到错误。

### 测试

```
mvn test -pl common,modules/common-module/common-module-impl -am
```
BUILD SUCCESS — 测试结果如下：

| 模块 | 通过 | 失败 | 跳过 |
|------|------|------|------|
| common | 136 | 0 | 5 |
| common-module-impl | 391 | 0 | 1 |

与设计规格完全一致。

> **说明**：实际使用的 `-pl` 值为 `common,modules/common-module/common-module-impl`（而非 `common,modules/common-module`），因为 `common-module` 为 POM 聚合模块，不包含测试代码。`common-module-impl` 的 `-am` 标志自动将 `common` 纳入 reactor，确保优先编译，消除本地 Maven 仓库旧版 `common` JAR 导致的"找不到符号 MessageInterpolator"错误。

## 设计偏差说明

无偏差，严格遵循 v7 设计规格。

## 修订说明（v7 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| `code_v7.md` 声明"未执行编译验证"，与设计规格不符 | 已执行 `mvn compile` 和 `mvn test`，并将执行结果补充到本报告中 |
