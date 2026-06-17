# 测试报告（v5）

## 概述
验证 `FallbackAiServiceTest.shouldLogErrorOnFirstCallThenWarnOnSubsequent` 已按详细设计 v5 正确实现。

## 验证结果
- **测试方法位置**: `FallbackAiServiceTest.java:117-142`
- **设计匹配度**: 完全匹配
- **设计偏差**: 无

## 验证清单
| 设计项 | 实现状态 | 说明 |
|--------|---------|------|
| 测试方法签名 `shouldLogErrorOnFirstCallThenWarnOnSubsequent` | ✓ | 正确命名 |
| 通过 `LoggerFactory.getLogger(FallbackAiService.class)` 获取 Logger | ✓ | 正确类型转换为 `ch.qos.logback.classic.Logger` |
| `ListAppender<ILoggingEvent>` 创建、start、addAppender | ✓ | 正确注入 |
| try-finally 清理模式：`appender.stop(); logger.detachAppender(appender)` | ✓ | 符合 R4 已知清理模式 |
| 首次调用：assert size==1, Level.ERROR, message | ✓ | 断言正确 |
| K3 已知偏差标注 | ✓ | `// ⚠️ 此断言依赖...` 注释存在 |
| 后续调用：clear, 再次调用, assert size==1, Level.WARN, message | ✓ | 断言正确 |
| 通过 public 方法 `triage()` 触发，不直接调用 private 方法 | ✓ | 符合行为契约 |
| 无生产代码修改 | ✓ | 纯测试变更 |

## 生产代码验证
`FallbackAiService.java:60-67` 的 `handleEmptyDelegates()` 实现：
- `firstEmptyDelegateCall.getAndSet(false)` 原子切换状态
- 首次调用 `log.error(...)` → 后续调用 `log.warn(...)`
- 返回 `AiResult.degraded("No available AiService delegate")`

与测试断言完全一致。
