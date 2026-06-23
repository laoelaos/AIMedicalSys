# 测试报告（v4）

## 概述
在 `GlobalExceptionHandlerTest` 中新增 `shouldHandleMessageNotReadableWith400` 和 `shouldHandleMessageNotWritableWith500` 两个测试方法，覆盖 HTTP 状态码、错误码、消息、data 断言及日志级别验证。

## 测试文件
- `AIMedical/backend/common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java`

## 新增测试方法

### shouldHandleMessageNotReadableWith400
| 步骤 | 验证点 | 期望 |
|------|--------|------|
| 创建 `HttpMessageNotReadableException("Malformed JSON")` | - | - |
| 通过 `ListAppender<ILoggingEvent>` 注入 logger | - | - |
| 调用 `handler.handleMessageNotReadable(ex)` | - | - |
| 验证日志 | `appender.list` 大小=1, `Level.WARN`, 消息="Request body malformed" |
| 验证响应状态码 | 400 |
| 验证 body | `getCode()`=PARAM_INVALID.code(), `getMessage()`=PARAM_INVALID.message(), `getData()`=null |

### shouldHandleMessageNotWritableWith500
| 步骤 | 验证点 | 期望 |
|------|--------|------|
| 创建 `HttpMessageNotWritableException("Serialization failed")` | - | - |
| 通过 `ListAppender<ILoggingEvent>` 注入 logger | - | - |
| 调用 `handler.handleMessageNotWritable(ex)` | - | - |
| 验证日志 | `appender.list` 大小=1, `Level.ERROR`, 消息="Response body serialization failed" |
| 验证响应状态码 | 500 |
| 验证 body | `getCode()`=SYSTEM_ERROR.code(), `getMessage()`=SYSTEM_ERROR.message(), `getData()`=null |

## 与详细设计的偏差
- **清理代码**：设计规格使用 `logger.detachAndStopAppender(appender)`，但 logback-classic 1.4.14 泛型不匹配导致编译失败，实际采用 `appender.stop(); logger.detachAppender(appender);` — 功能等价，行为契约不变。

## 测试覆盖率维度
| 维度 | 覆盖情况 |
|------|---------|
| 正常路径 | `shouldHandleMessageNotReadableWith400`、`shouldHandleMessageNotWritableWith500` |
| 边界条件 | 两个异常直接创建实例，无特殊参数结构 |
| 错误路径 | N/A（被测试方法本身处理异常，无额外错误条件） |
| 状态交互 | 日志 appender 注入-断言-清理完整生命周期 |
| 行为契约 | HTTP 状态码、错误码、日志级别与消息、data 为 null |

## 已通过编译验证
BUILD SUCCESS（引用实现报告，所有 66 个测试通过，其中 GlobalExceptionHandlerTest 运行 5 个测试，0 failures, 0 errors）。
