# 测试报告（v2）

## 变更文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `AIMedical/backend/modules/consultation/src/test/java/.../TriageConverterTest.java` | 修改 | 新增 C12 截断测试（6 用例），更新 sessionId 为合法 UUID v4 |
| `AIMedical/backend/modules/consultation/src/test/java/.../DialogueSessionManagerTest.java` | 修改 | 新增 C10 UUID v4 校验测试（7 用例），更新 sessionId 为合法 UUID v4 |
| `AIMedical/backend/modules/consultation/src/test/java/.../TriageServiceImplTest.java` | 修改 | 新增 C05 互斥校验（4）、C10 异常转换（1）、C21 session 快照（3）、A08 中文化（2），更新 sessionId 为合法 UUID v4，修复原断言匹配中文化字符串 |
| `AIMedical/backend/modules/ai/ai-api/src/test/java/.../TriageDtoTest.java` | 修改 | 新增 `additionalResponsesText` 字段测试（2 用例） |

## 行为契约覆盖

### C05 — 字段互斥校验
- `shouldThrowWhenChiefComplaintAndAdditionalResponsesBothPresent`：两者同时存在 → BusinessException
- `shouldThrowWhenChiefComplaintAndAdditionalResponsesBothAbsent`：两者均缺失（含空白串） → BusinessException
- `shouldPassWhenOnlyChiefComplaintPresent`：仅主诉 → 正常继续
- `shouldPassWhenOnlyAdditionalResponsesPresent`：仅追问 → 正常继续

### C10 — UUID v4 校验
- `shouldThrowWhenCreateSessionWithNullSessionId`：null → IllegalArgumentException
- `shouldThrowWhenCreateSessionWithInvalidUuid`：非 UUID 格式 → IllegalArgumentException
- `shouldThrowWhenCreateSessionWithNonV4Uuid`：非 v4 UUID → IllegalArgumentException（第13位不为4）
- `shouldThrowWhenRestoreSessionWithNullSessionId`：null → IllegalArgumentException
- `shouldThrowWhenRestoreSessionWithInvalidUuid`：非 UUID 格式 → IllegalArgumentException
- `shouldAcceptUppercaseUuidV4`：大写 UUID → 正常创建
- `shouldThrowBusinessExceptionWhenSessionIdIsInvalidUuid`：服务层捕获 IllegalArgumentException → BusinessException(TRIAGE_SESSION_NOT_FOUND)

### C12 — 截断顺序和格式
- `shouldConcatenateItemsWithQAndAFormat`：拼接格式验证 `Q: {q} A: {a} `
- `shouldNotTruncateWhenExactly3000Chars`：恰好 3000 字符时不截断、不加标记
- `shouldTruncateWhenOver3000CharsAndAppendTruncatedMarker`：超过 3000 字符时截断 + 追加 `[TRUNCATED]`
- `shouldReturnEmptyStringWhenItemsListIsEmpty`：空列表 → 空字符串
- `shouldHandleNullQuestionOrAnswerInItems`：null question/answer → 空字符串处理

### C21 — session 快照优先级
- `shouldCopyRuleVersionFromRequestToSessionWhenSessionNull`：session 中为 null → 从 request 复制
- `shouldNotOverwriteSessionRuleVersionWhenAlreadySet`：session 中已设置 → 不覆盖
- `shouldUseSessionRuleVersionInFallbackMatch`：降级时使用 session 的 ruleVersion/ruleSetId

### A08 — 中文化
- `shouldUseChineseFallbackReason`：降级 reason 为中文
- `shouldUseChineseFallbackHintAfterMaxFailures`：连续失败后 fallbackHint 为中文
- 同时修复原 3 个断言的期望值匹配中文化字符串

## 测试执行

| 测试文件 | 运行结果 | 用例数 |
|---------|---------|--------|
| `TriageConverterTest` | ✅ 全部通过 | 17 |
| `DialogueSessionManagerTest` | ✅ 全部通过 | 18 |
| `TriageServiceImplTest` | ✅ 全部通过 | 49 |
| `TriageDtoTest` | ✅ 全部通过 | 38 |

**总计**：新增 19 个测试用例，原有 85 个测试用例全部保持通过。所有 4 个测试文件编译通过且运行无失败。
