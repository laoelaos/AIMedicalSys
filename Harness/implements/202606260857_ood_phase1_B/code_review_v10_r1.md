# 代码审查报告（v10 r1）

## 审查结果
REJECTED

## 发现
- **[严重]** `PasswordChangeCheckFilterTest.java:69-117` — 3 个白名单路径测试（`shouldSkipForPasswordPath`、`shouldSkipForLogoutPath`、`shouldSkipForRefreshPath`）因 `MockHttpServletRequest` 仅设置 `setRequestURI()` 而未设置 `setServletPath()`，导致 `AntPathRequestMatcher.matches()` 内部调用 `getServletPath()` 返回空字符串 `""`，路径被归一化为 `"/"`，与模式 `/api/auth/password` 等不匹配。`isWhitelisted()` 返回 `false`，过滤器实际抛出 `PasswordChangeRequiredException` 而非预期的 `chain.doFilter` 放行。mvn test 验证 3 个用例均报 ERROR。

## 修改要求
1. **文件**: `PasswordChangeCheckFilterTest.java` 第 71、88、105 行
2. **问题**: 测试中仅调用 `request.setRequestURI(...)`，`MockHttpServletRequest.getServletPath()` 默认为 `""`，`AntPathRequestMatcher` 据此无法正确匹配路径
3. **期望修正方向**: 在 `setRequestURI` 之后补充 `request.setServletPath(...)`，使 servletPath 与 requestURI 一致。例如：
   ```java
   request.setRequestURI("/api/auth/password");
   request.setServletPath("/api/auth/password");
   ```
   或对于 Spring Boot 3.2 环境，验证是否可统一采用 `request.setServletPath(...)` 方式。

## 输出
REJECTED:Harness/implements/202606260857_ood_phase1_B/code_review_v10_r1.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
