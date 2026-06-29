# 实现报告（v6）

## 概述

抽取 `GlobalExceptionHandler.formatMessage()` 内联方法为独立 `MessageInterpolator` 组件，统一注入 `GlobalExceptionHandler`、`RestAuthenticationEntryPoint`、`RestAccessDeniedHandler` 三个出口，确保所有错误响应都经过同一消息插值管线。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `common/src/main/java/com/aimedical/common/util/MessageInterpolator.java` | 消息插值接口 |
| 新建 | `common/src/main/java/com/aimedical/common/util/SimpleMessageInterpolator.java` | `@Component` 实现，复制现有 `formatMessage()` 逻辑 |
| 修改 | `common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java` | 注入 `MessageInterpolator`，删除 `formatMessage()` 方法 |
| 修改 | `common-module-impl/src/main/java/.../auth/security/RestAuthenticationEntryPoint.java` | 注入 `MessageInterpolator`，插值后调用 `Result.fail(code, message)` |
| 修改 | `common-module-impl/src/main/java/.../auth/security/RestAccessDeniedHandler.java` | 同上 |
| 修改 | `common-module-impl/src/main/java/.../auth/security/SecurityConfigPhase1.java` | `filterChain()` 增加 `MessageInterpolator` 参数，传参构造两个 handler |
| 修改 | `common/src/test/java/.../config/GlobalExceptionHandlerTest.java` | 构造 `GlobalExceptionHandler` 时注入 `SimpleMessageInterpolator` |
| 修改 | `common-module-impl/src/test/java/.../auth/security/RestAuthenticationEntryPointTest.java` | 构造时传入 mock `MessageInterpolator` |
| 修改 | `common-module-impl/src/test/java/.../auth/security/RestAccessDeniedHandlerTest.java` | 同上 |
| 新建 | `common/src/test/java/.../util/SimpleMessageInterpolatorTest.java` | 独立测试 `SimpleMessageInterpolator` 所有插值场景 |
| 修改 | `common-module-impl/src/test/java/.../auth/security/SecurityConfigPhase1Test.java` | `filterChain()` 调用时传入 mock `MessageInterpolator` |

## 编译验证

`mvn compile -pl common,common-module-impl -am` — 通过

## 测试验证

`mvn test -pl common -am` — 136 tests, 0 failures, 5 skipped
`mvn test -pl common-module-impl -am` — 391 tests, 0 failures, 1 skipped

## 设计偏差说明

无偏差。
