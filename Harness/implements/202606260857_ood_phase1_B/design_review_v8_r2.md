# 设计审查报告（v8 r2）

## 审查结果
APPROVED

## 发现
- **[轻微]** 文件/类名与 task_v8.md 规范不一致：task 指定 `AuthenticationEntryPoint.java` / `AccessDeniedHandler.java`，设计改为 `RestAuthenticationEntryPoint.java` / `RestAccessDeniedHandler.java`。此改动避免了自定义类与 `org.springframework.security.web.AuthenticationEntryPoint` / `org.springframework.security.web.access.AccessDeniedHandler` 接口简单名冲突，是合理的改进，不影响功能正确性。

其余设计均满足 task_v8.md 要求：
- RestAuthenticationEntryPoint 正确实现 AuthenticationEntryPoint 接口，commence() 区分 ACCOUNT_DISABLED 与通用未认证场景，写入统一 JSON 401 响应
- RestAccessDeniedHandler 正确实现 AccessDeniedHandler 接口，handle() 使用 instanceof 区分 PasswordChangeRequiredException 与通用 AccessDeniedException，写入统一 JSON 403 响应
- PasswordChangeRequiredException 正确继承 AccessDeniedException，双构造器
- ObjectMapper 构造模式与已有 GlobalRateLimitFilter 一致
- 无 @Component 注解，由 SecurityConfigPhase1 显式注册
- 三测试类覆盖所有指定场景，验证 status、content-type、body code 字段
- 序列化失败降级 fallback 字符串正确
- 异常场景（message 为 null、IllegalStateException）已在契约中说明
