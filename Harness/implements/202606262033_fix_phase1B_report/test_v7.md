# 测试报告（v7）

## 测试文件

`AIMedical/backend/common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java`

## 变更说明

为 `handleBusinessException` 的日志行为新增 3 个测试用例，覆盖不同模板类型下的日志输出验证。

## 新增测试用例

| 测试方法 | 覆盖维度 | 验证内容 |
|---------|---------|---------|
| `shouldLogOriginalTemplateWhenNoArgs` | 正常路径-无参数 | 简单模板（"业务异常"）无插值参数时，日志输出原始模板和错误码 |
| `shouldLogOriginalTemplateWithNumberedPlaceholders` | 正常路径-编号占位符 | `MessageFormat` 编号占位符模板（`{0}`, `{1}`）日志输出原始模板而非插值结果 |
| `shouldLogOriginalTemplateForRateLimited` | 正常路径-无参数含占位符 | 无 args 的 `RATE_LIMITED` 模板日志输出原始消息 |
| `shouldInterpolateAccountLockedMessage_logsOriginalTemplate` | **已有** | 命名占位符模板（`{锁定时间}`）日志输出原始模板 |

## 覆盖维度分析

| 维度 | 用例数 | 说明 |
|------|-------|------|
| 正常路径 | 3（含已有1个） | 无参数、编号占位符、命名占位符 |
| 边界条件 | 1 | 无参数模板（`shouldLogOriginalTemplateWhenNoArgs`） |
| 错误路径 | — | `log.warn` 不抛出异常 |
| 状态交互 | — | 日志不影响响应体 |

## 行为契约覆盖率

**契约**: `handleBusinessException` 每次调用均以 WARN 级别记录 `errorCode.getCode()` 和 `e.getMessage()`（原始消息模板）

- ✅ `TEST_ERROR`（简单模板 "业务异常"）
- ✅ `GlobalErrorCode.ACCOUNT_LOCKED`（命名占位符模板 `{锁定时间}`）
- ✅ `NUMBERED_TEMPLATE`（编号占位符模板 `{0}`/`{1}`）
- ✅ `GlobalErrorCode.RATE_LIMITED`（无 args 含占位符模板）

## 测试总数

原有 10 个 + 新增 3 个 = **13 个测试用例**，全部独立、无顺序依赖。
