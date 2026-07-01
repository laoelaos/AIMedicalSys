# 实现报告（v10）

## 概述
修复 prescription 模块 14 项缺陷（8a-8n），涉及 12 个源文件和 2 个 ai-api DTO 文件。所有变更按详细设计规格实现，编译通过。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `prescription/.../dto/audit/PrescriptionItem.java` | 8c: dose double → BigDecimal，getter/setter 类型变更 |
| 修改 | `prescription/.../service/assist/DosageThresholdService.java` | 8a: 空 catch 补充 log.warn + 新增 Logger；8k: 抽取 matchesBothRanges/findFirstCandidate 辅助方法，5 个循环改为 findFirstCandidate 调用 |
| 修改 | `prescription/.../context/PrescriptionDraftContext.java` | 8b: 删除 @SuppressWarnings + instanceof List → List<?>；8n: 新增 snapshotCriticalAlerts + SnapshotResult 内部类，删除 hasCriticalAlerts |
| 修改 | `prescription/.../rule/DosageLimitRule.java` | 8c: BigDecimal.valueOf(item.getDose()) → item.getDose()；8f: findBestMatch null 回退追加 log.warn + Logger |
| 修改 | `prescription/.../converter/AuditConverter.java` | 8c: aiItem.setDose → .doubleValue()；8h: 追加 aiItem.setUnit() + aiPatient.setWeight() |
| 修改 | `prescription/.../rule/AllergyCheckRule.java` | 8g: contains → 单词边界正则 + 否定前缀跳过，新增 hasNegationPrefix 方法 |
| 修改 | `prescription/.../rule/DrugInteractionRule.java` | 8i: 新增 @ConditionalOnProperty 注解 |
| 修改 | `prescription/.../service/impl/PrescriptionAuditServiceImpl.java` | 8d: UUID ID 生成替换 5 处；8e: 删除 hasNewAlerts 死代码块及方法；8j: reasons 从 snapshot.alerts 透传；8m: per-prescriptionId ReentrantLock + doSubmit 提取；8n: snapshotCriticalAlerts 替代两次读取；移除 SubmitContext |
| 修改 | `prescription/.../api/PrescriptionAuditController.java` | 8j: BLOCK 路径 reasons 从 response.getAlerts() 提取 |
| 修改 | `prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java` | 8c: setDosage(.doubleValue()) + setDose(BigDecimal.valueOf())；8l: exceptionally 回调追加 clearCriticalAlerts |
| 修改 | `ai/ai-api/.../dto/prescription/PrescriptionCheckItem.java` | 8h: 新增 unit 字段 + getter/setter |
| 修改 | `ai/ai-api/.../dto/prescription/PatientInfo.java` | 8h: 新增 weight 字段 + getter/setter |
| 修改 | `prescription/.../rule/SpecialPopulationDosageRule.java` | 8c 级联: BigDecimal.valueOf(item.getDose()) → item.getDose() |

## 编译验证
`mvn compile -pl modules/prescription,modules/ai/ai-api -am -q` — 编译通过，无错误。
r1 修订后再次编译验证：通过，无错误。

## 设计偏差说明
无偏差。所有变更严格按详细设计 v10 规格实现。

## 修订说明（v10 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| `DosageThresholdService.java` — `matchesBothRanges` 方法已定义但未被调用，沦为死代码 | 将 Loop1/Loop2 的公共条件替换为 `matchesBothRanges(ds, age, weight)` 调用：Loop1 附加非空范围检查，Loop2 直接委托。详见方案 B。
