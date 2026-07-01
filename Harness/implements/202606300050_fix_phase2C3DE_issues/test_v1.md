# 测试报告（v1）

## 测试文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `ai-api/.../dto/triage/TriageDtoTest.java` | 新增 `correctedChiefComplaint` 字段 DTO 级别测试 |
| 修改 | `consultation/.../TriageConverterTest.java` | 新增 cc 透传、回写及 null-safety 测试 |
| 修改 | `consultation/.../DialogueSessionManagerTest.java` | 新增 `TriageRecordRepository` stub、修正构造器、新增 DB 恢复路径测试 |
| 修改 | `consultation/.../TriageServiceImplTest.java` | 修正构造器、新增 stub 方法、新增 cc 数据流测试 |

## 测试用例清单

### TriageDtoTest

| 测试方法 | 覆盖契约 |
|---------|---------|
| `shouldSetAndGetCorrectedChiefComplaintInRequest` | TriageRequest.correctedChiefComplaint 正常 getter/setter |
| `shouldBuildFullTriageResponseWithAllNewFields`（扩展） | cc 在完整构建链路中正确传递 |

### TriageConverterTest

| 测试方法 | 覆盖契约 |
|---------|---------|
| `shouldPassCorrectedChiefComplaintFromSessionToAiRequest` | A04: session cc → TriageRequest cc 透传（正向） |
| `shouldNotSetCorrectedChiefComplaintWhenSessionIsNull` | A04: session 为 null 时不抛异常 |
| `shouldNotSetCorrectedChiefComplaintWhenSessionCcIsNull` | A04: session cc 为 null 时不设置请求 cc |
| `shouldNotWriteBackCorrectedChiefComplaintWhenSessionIsNull` | C03: session 为 null 时不抛异常 |
| `shouldNotWriteBackCorrectedChiefComplaintWhenAiDataIsNull` | C03: aiData 为 null 时 session cc 不变 |

### DialogueSessionManagerTest

| 测试方法 | 覆盖契约 |
|---------|---------|
| `shouldRestoreSessionFromTriageRecordWhenNotInStore` | C02: session 不在内存 + TriageRecord 存在 → 重建 session 并恢复 cc |
| `shouldRestoreSessionWithoutCcWhenTriageRecordHasNullCc` | C02: 记录中 cc 为 null → 重建 session 时 cc 为 null |

### TriageServiceImplTest

| 测试方法 | 覆盖契约 |
|---------|---------|
| `shouldSetCorrectedChiefComplaintFromRequestToSession` | request cc → session → TriageRecord 写入 |
| `shouldWriteBackCorrectedChiefComplaintFromAiResultToSessionAndRecord` | AI 返回 cc → session 回写 → TriageRecord 持久化 |
| `shouldOverrideCorrectedChiefComplaintFromAiResultOverRequest` | AI cc 覆盖请求 cc（幂等覆盖） |
| `shouldNotSetCorrectedChiefComplaintOnRecordWhenSessionCcIsNull` | session cc 为 null 时记录 cc 为 null |

## 覆盖维度汇总

| 维度 | 覆盖情况 |
|------|---------|
| 正常路径 | session cc → aiRequest（A04）；AI cc → session 回写（C03）；session → TriageRecord（C19）；TriageRecord → session 恢复（C02） |
| 边界条件 | cc 为 null 时透传/回写/持久化均不执行 |
| 错误路径 | session 为 null、aiData 为 null 时不抛 NPE |
| 状态交互 | request cc → session → AI → AI cc 回写 → TriageRecord 完整闭环 |
