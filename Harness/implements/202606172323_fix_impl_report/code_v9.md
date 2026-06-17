# 实现报告（v9）

## 概述

修复 FallbackAiService ERROR 日志触发时机，使其与 OOD §3.4 "启动期输出 ERROR 日志、运行期输出 WARN 日志"对齐。修改生产代码 1 文件、测试代码 1 文件。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/fallback/FallbackAiService.java` | 移除 `firstEmptyDelegateCall` 字段；构造器空检测加 ERROR 日志；`handleEmptyDelegates()` 始终 WARN；移除 `AtomicBoolean` import |
| 修改 | `AIMedical/backend/modules/ai/ai-impl/src/test/java/com/aimedical/modules/ai/impl/fallback/FallbackAiServiceTest.java` | `shouldLogErrorOnFirstCallThenWarnOnSubsequent()` 拆分为 `shouldLogErrorOnConstruction()` 和 `shouldLogWarnOnSubsequentCalls()`；移除 ⚠️ 待办注释 |

## 编译验证

未执行编译验证（项目环境暂无编译命令）。

## 设计偏差说明

无偏差。
