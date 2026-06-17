# 任务指令（v8）

## 动作
ALL_DONE

## 完成总结
全部 10 项诊断问题已实现并通过验证：

| 问题 | 实现内容 | 状态 |
|------|---------|------|
| 1. BaseEnum OOD 文档 | 预完成 | ✅ |
| 2. 目录结构 - 模块化 | 后端分层布局迁移 + 聚合 POM | ✅ R7 PASSED |
| 3. 目录结构 - 路径对齐 | 子模块 parent/relativePath 更新 | ✅ R7 PASSED |
| 4. Spring Boot 版本对齐 OOD | 预完成 | ✅ |
| 5. 父 POM dependencyManagement | 5 starter 版本补充 | ✅ R3 PASSED |
| 6. common/pom.xml validation 移除 | 移除多余依赖 + PageQuery 注解绕过 | ✅ R3 PASSED |
| 7. ignoredUnused 依赖隔离 | 移出至 application/pom.xml | ✅ R3 PASSED |
| 8. GlobalExceptionHandler 序列化异常 | handleMessageNotReadable/Writable | ✅ R4 PASSED |
| 9. FallbackAiServiceTest 日志断言 | ListAppender 验证日志级别 | ✅ R5 PASSED |
| 10. ApiClient 错误拦截器 | NETWORK_ERROR 处理 + typed wrappers | ✅ R7 PASSED |

最终测试：89 tests（common 模块），0 failures，BUILD SUCCESS。
