# 实现报告（v8）

## 概述

实现 Stage 2 安全异常处理入口：RestAuthenticationEntryPoint（统一 401 响应）、RestAccessDeniedHandler（统一 403 响应）、PasswordChangeRequiredException（异常类型）。新建 3 个生产类、3 个测试类。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/exception/PasswordChangeRequiredException.java` | AccessDeniedException 子类，两个构造器 |
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/RestAuthenticationEntryPoint.java` | 实现 AuthenticationEntryPoint，区分 ACCOUNT_DISABLED |
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/RestAccessDeniedHandler.java` | 实现 AccessDeniedHandler，区分 PasswordChangeRequiredException |
| 新建 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/exception/PasswordChangeRequiredExceptionTest.java` | 3 用例：消息、原因、继承关系 |
| 新建 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/RestAuthenticationEntryPointTest.java` | 3 用例：ACCOUNT_DISABLED、通用未认证、message 为 null |
| 新建 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/RestAccessDeniedHandlerTest.java` | 2 用例：PasswordChangeRequiredException、通用 AccessDenied |

## 编译验证

生产代码编译成功，测试代码编译成功，8 个测试用例全部通过（BUILD SUCCESS）。

## 设计偏差说明

无偏差。测试中 `response.getContentType()` 断言值为 `"application/json;charset=UTF-8"` 而非设计模板中的 `"application/json"`，原因是 `MockHttpServletResponse` 在同时调用 `setContentType("application/json")` 和 `setCharacterEncoding("UTF-8")` 后将 charset 合并到 contentType 返回，此为 Mock 实现行为而非生产代码行为偏差。
