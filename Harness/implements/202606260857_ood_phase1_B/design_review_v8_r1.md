# 设计审查报告（v8 r1）

## 审查结果
REJECTED

## 发现

### **[严重]** AuthenticationEntryPoint / AccessDeniedHandler 类名与导入接口名冲突

**位置**：`detail_v8.md` 类型定义章节，AuthenticationEntryPoint 和 AccessDeniedHandler 的骨架代码

**问题**：
两个类的骨架代码均写为：
```java
package com.aimedical.modules.commonmodule.auth.security;

import org.springframework.security.web.AuthenticationEntryPoint;  // ← 编译错误

public class AuthenticationEntryPoint implements AuthenticationEntryPoint { ... }
```

和：
```java
package com.aimedical.modules.commonmodule.auth.security;

import org.springframework.security.web.access.AccessDeniedHandler;  // ← 编译错误

public class AccessDeniedHandler implements AccessDeniedHandler { ... }
```

在 Java 中，当一个编译单元（`.java` 文件）声明了某个类型的简单名称后（此处 `AuthenticationEntryPoint` 作为类名），就不能再 `import` 同名类型（`org.springframework.security.web.AuthenticationEntryPoint`）。编译器报错：`"org.springframework.security.web.AuthenticationEntryPoint is already defined in this compilation unit"`。

**实证验证**：使用独立的双文件测试（接口在 package `a`，类在 package `b` import + implements）确认无法通过编译。

**期望修正方向**：
方案一（推荐）：重命名类，避免与 Spring Security 接口冲突，例如 `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler`，或遵循项目已有的 `GlobalRateLimitFilter` 命名风格（具有业务含义的名称）。
方案二：删除 `import` 语句，在 `implements` 子句中直接使用全限定名 `org.springframework.security.web.AuthenticationEntryPoint`。但此方式可读性差且不符合 Java 常规实践。

### **[轻微]** 骨架代码缺少 `JsonProcessingException` 导入

**位置**：`detail_v8.md:48-57`（AuthenticationEntryPoint 骨架）、`detail_v8.md:90-97`（AccessDeniedHandler 骨架）

**问题**：
行为契约中明确 `catch (JsonProcessingException e)`，但两处骨架代码的 `import` 列表中均未包含 `com.fasterxml.jackson.core.JsonProcessingException`。虽然这属于实现阶段的明显补充项，但仍应修正以避免遗漏。

**期望修正方向**：在 import 列表中增加 `import com.fasterxml.jackson.core.JsonProcessingException;`。

### **[轻微]** `catch` 块中 `response.getWriter()` 的异常处理

**位置**：`detail_v8.md:164` 和 `detail_v8.md:194`

**问题**：
`response.getWriter()` 在 JSON 序列化失败的 `catch` 块中被调用，但 `getWriter()` 可能抛出 `IllegalStateException`（响应已提交或 `getOutputStream()` 已被调用）。当前算法未对此做任何处理。虽然在 Filter 正常调用流程中此场景极难触发，但理想设计应注明该风险可接受或增加防御。

**期望修正方向**：在行为契约中增加说明，承认 `getWriter()` 失败的风险可接受（由 Servlet 容器处理），或指出此场景的不可达性（在自定义 Handler 中，响应尚未提交）。
