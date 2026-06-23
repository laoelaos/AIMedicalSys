# 实现报告（v4）

## 概述
在 `GlobalExceptionHandler` 中新增 `handleMessageNotReadable` 和 `handleMessageNotWritable` 两个 `@ExceptionHandler` 方法，处理 `HttpMessageNotReadableException`（400）和 `HttpMessageNotWritableException`（500）；在 `GlobalExceptionHandlerTest` 中新增对应单元测试，覆盖 HTTP 状态码、错误码、消息、data 断言及日志级别验证。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java` | 新增 `handleMessageNotReadable`（log.warn + 400 + PARAM_INVALID）和 `handleMessageNotWritable`（log.error + 500 + SYSTEM_ERROR） |
| 修改 | `AIMedical/backend/common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java` | 新增 `shouldHandleMessageNotReadableWith400` 和 `shouldHandleMessageNotWritableWith500`，使用 `ListAppender` 验证日志级别 |

## 编译验证
BUILD SUCCESS — `common` 模块编译通过，全部 66 个测试通过（0 failures, 0 errors），其中 `GlobalExceptionHandlerTest` 运行 5 个测试（3 个已有 + 2 个新增）。

## 设计偏差说明
- **设计规格**：测试 finally 块中使用 `logger.detachAndStopAppender(appender)`
- **偏差原因**：logback-classic 1.4.14 的 `Logger.detachAndStopAppender(Appender<ILoggingEvent>)` 编译器解析失败（泛型不匹配）
- **实际处理**：替换为 `appender.stop(); logger.detachAppender(appender);` 达到相同清理效果
