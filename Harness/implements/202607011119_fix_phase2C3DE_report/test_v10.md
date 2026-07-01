# 测试报告（v10）

## 概述

针对 8a-8n 缺陷修复的单元测试更新。涉及 10 个测试文件：修改 9 个已有测试文件，新增 28 个测试用例。

## 测试文件变更清单

### 1. `PrescriptionItemTest.java` — 8c
| 变更 | 说明 |
|------|------|
| `setDose(100.0)` → `setDose(BigDecimal.valueOf(100.0))` | dose 类型从 double 变为 BigDecimal |
| `assertEquals(100.0, getDose(), 0.001)` → `assertEquals(BigDecimal.valueOf(100.0), getDose())` | 断言适配 BigDecimal |

### 2. `PrescriptionDraftContextTest.java` — 8b/8n
| 变更 | 说明 |
|------|------|
| 删除 `shouldReturnFalseWhenNoAlertsInStore` | `hasCriticalAlerts` 方法已删除 |
| 删除 `shouldReturnTrueWhenAlertsExist` | `hasCriticalAlerts` 方法已删除 |
| 新增 `shouldReturnEmptyListWhenStoredValueIsNotList` | 8b: 非 List 值时返回 emptyList |
| 新增 `snapshotShouldReturnNoAlertsWhenStoreHasNull` | 8n: snapshotCriticalAlerts 空值 |
| 新增 `snapshotShouldReturnHasAlertsTrueWhenAlertsExist` | 8n: snapshotCriticalAlerts 有告警 |
| 新增 `snapshotShouldReturnHasAlertsFalseForNonListValue` | 8n: 非 List 值快照 |
| 新增 `snapshotShouldReturnHasAlertsFalseForEmptyList` | 8n: 空列表快照 |

### 3. `AuditConverterTest.java` — 8c/8h
| 变更 | 说明 |
|------|------|
| `item.setDose(100)` → `item.setDose(BigDecimal.valueOf(100))` | 8c: dose 类型适配 |
| `assertEquals(100, checkItem.getDose())` → `assertEquals(100.0, checkItem.getDose(), 0.001)` | ai-api dose 仍为 double |
| 新增 `shouldMapUnitFieldToAiCheckItem` | 8h: unit 字段映射 |
| 新增 `shouldMapWeightFieldToAiPatientInfo` | 8h: weight 字段映射 |
| 新增 `shouldMapUnitAsNullWhenNotSet` | 8h: unit 可 null |
| 新增 `shouldMapWeightAsNullWhenNotSet` | 8h: weight 可 null |

### 4. `PrescriptionAuditServiceImplTest.java` — 8c/8d/8e/8j/8m/8n
| 变更 | 说明 |
|------|------|
| `sampleItem.setDose(100)` → `setDose(BigDecimal.valueOf(100.0))` | 8c: dose 类型适配（保留 scale=1 确保 JSON 比对一致） |
| 全部 submit 测试：`hasCriticalAlerts`/`getCriticalAlerts` mock → `snapshotCriticalAlerts` | 8n: TOCTOU 消除后单次快照调用 |
| 删除 `invokeHasNewAlerts` 辅助方法 | 8e: hasNewAlerts 方法已删除 |
| 删除 4 个 `hasNewAlerts*` 测试 | 8e: 对应被删除的私有方法 |
| 重命名 `submitShouldDetectNewCriticalAlertsBetweenStep2AndStep3` → `submitShouldBlockWhenCriticalAlertsExist` | 8n: TOCTOU 场景消除 |
| 新增 `submitShouldIncludeAlertMessagesAsReasonsWhenCriticalDoseBlock` | 8j: reasons 从 snapshot.alerts 透传 |
| 新增 `submitShouldUseFallbackReasonWhenAlertMessagesEmpty` | 8j: 空 reasons 回退固定字符串 |
| 新增 `submitShouldSerializeSamePrescriptionIdRequests` | 8m: per-prescriptionId 锁 |

### 5. `PrescriptionAuditControllerTest.java` — 8j
| 变更 | 说明 |
|------|------|
| 新增 `auditShouldPassReasonsFromAlertsWhenBlocked` | 8j: BLOCK 路径 reasons 从 response.getAlerts() 提取 |
| 新增 `auditShouldUseFallbackReasonWhenAlertsEmptyOnBlock` | 8j: alerts 空时回退到固定字符串 |

### 6. `DosageLimitRuleTest.java` — 8c/8f
| 变更 | 说明 |
|------|------|
| 全部 `item.setDose(X)` → `item.setDose(BigDecimal.valueOf(X))` | 8c: dose 类型适配 |
| 新增 `shouldLogWarnWhenFindBestMatchReturnsNull` | 8f: null 回退行为的完整性验证 |

### 7. `SpecialPopulationDosageRuleTest.java` — 8c
| 变更 | 说明 |
|------|------|
| 全部 `item.setDose(X)` → `item.setDose(BigDecimal.valueOf(X))` | 8c: 级联剂量类型适配 |

### 8. `AllergyCheckRuleTest.java` — 8g
| 变更 | 说明 |
|------|------|
| 新增 `shouldNotMatchPartialWord` | 8g: 单词边界 — "cillin" 不匹配 "Penicillin" |
| 新增 `shouldSkipWhenNegationPrefixFound` | 8g: "No allergy to Penicillin" → PASS |
| 新增 `shouldBlockWhenAllergenFoundWithoutNegation` | 8g: "Has Penicillin allergy" → BLOCK |
| 新增 `shouldSkipWhenMultipleNegationPrefixes` | 8g: "Patient denies Penicillin" → PASS |
| 新增 `shouldHandleCaseInsensitiveAllergenMatch` | 8g: 大小写不敏感匹配 |

### 9. `DosageThresholdServiceTest.java` — 8a
| 变更 | 说明 |
|------|------|
| 新增 `shouldLogWarnAndSkipDailyDoseCheckWhenFrequencyIsNonNumeric` | 8a: 非数字频率时 log.warn + 跳过日剂量检查 |

### 10. `PrescriptionAssistServiceImplTest.java` — 8l
| 变更 | 说明 |
|------|------|
| 新增 `asyncSuggestionShouldClearCriticalAlertsOnExceptionally` | 8l: exceptionally 回调追加 clearCriticalAlerts |

## 设计偏差说明

无偏差。所有测试更新严格按详细设计 v10 规格实现。

## 未修改的已有测试文件

| 文件 | 原因 |
|------|------|
| `DrugInteractionRuleTest.java` | 8i @ConditionalOnProperty 为编译期注解，无需测试 |
| `DosageThresholdServiceTest.java` 已有测试 | 8k 为纯重构，行为不变，已有测试覆盖 |
| `PrescriptionAssistServiceImplTest.java` 已有测试 | 8c 级联（setDosage .doubleValue + parseDraftItems BigDecimal）已由源文件体现，测试无需变更 |
