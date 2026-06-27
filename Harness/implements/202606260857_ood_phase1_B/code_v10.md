# 实现报告（v10）

## 概述
实现了 `PasswordChangeCheckFilter`（OncePerRequestFilter）及其对应的7个单元测试。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/PasswordChangeCheckFilter.java` | 密码变更检查过滤器 |
| 新建 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/PasswordChangeCheckFilterTest.java` | 7个单元测试，无Spring上下文 |

## 编译验证
mvn compile test-compile 成功通过。

## 设计偏差说明
无偏差。

## 修订说明（v10 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| 3个白名单路径测试中 MockHttpServletRequest 仅设置 setRequestURI() 未设置 setServletPath()，导致 AntPathRequestMatcher.matches() 内部 getServletPath() 返回空串，路径匹配失败 | 在 shouldSkipForPasswordPath、shouldSkipForLogoutPath、shouldSkipForRefreshPath 三个测试方法中，于 setRequestURI() 之后补充 request.setServletPath(同路径)，使 servletPath 与 requestURI 一致 |
