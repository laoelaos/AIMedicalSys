# 测试报告（v9）

## 测试文件变更清单

| 操作 | 文件路径 | 测试项 |
|------|---------|--------|
| 修改 | `DefaultTriageRuleEngineTest.java` | 7a: 新增 `shouldLogWarningWhenRuleVersionMismatch` |
| 修改 | `TriageConverterTest.java` | 7b: 新增 `shouldCreateDegradedFallbackResponse`、`shouldSetFallbackHintWhenHintIsTrue`、`shouldSetRuleVersionMismatchOnFallbackResponse`、`shouldHandleNullDepartmentsAndDoctorsInFallback`、`shouldNotSetMatchedRulesOnFallbackResponse` |
| 修改 | `TriageServiceImplTest.java` | 7d: 新增 `shouldLogErrorWhenJsonSerializationFailsInSaveTriageRecord`；7e: 新增 `shouldThrowNullPointerExceptionWhenSelectDepartmentSessionIdIsNull` |
| 修改 | `RegistrationEventListenerTest.java` | 7e: 新增 `shouldSkipWhenSessionIdIsNull` |

## 测试覆盖说明

### 7a (C13) — DefaultTriageRuleEngine warn 日志
- 正向：版本不匹配时输出 WARN 级别日志，包含 version 和 setId 参数

### 7b (T4) — TriageConverter.toFallbackTriageResponse
- 正向：降级响应 degraded=true、confidence=null、字段正确映射
- 边界：departments/doctors 为 null 时转为 emptyList
- 状态：ruleVersionMismatch 透传
- 状态：fallbackHint=true 时设置中文提示语
- 状态：降级路径不设置 matchedRules（null）

### 7c (T42) — 缓存 expireAfterWrite(30s)
- 已有测试 `shouldExpireCacheAfterWriteDuration` 使用 MockTicker 验证

### 7d (C18) — saveTriageRecord 日志级别提升
- 正向：JsonProcessingException 时输出 ERROR 级别日志，包含 serialized JSON 字段

### 7e (T45) — 防御性校验
- RegistrationEventListener: null sessionId 时 log.warn + return，不调用 triageService
- TriageServiceImpl.selectDepartment: null sessionId 抛出 NullPointerException
