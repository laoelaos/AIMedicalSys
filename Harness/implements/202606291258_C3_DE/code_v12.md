# 实现报告（v12）

## 概述

实现了处方辅助开方（prescription assist）子域的全部 18 个新文件，并修改 2 个已有文件。覆盖 REST 端点、业务层 DTO、Service 层、剂量阈值校验服务、去重调度器、Converter，以及 PrescriptionDraftContext 和 PrescriptionErrorCode 的修改。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `dto/assist/DosageAlertLevel.java` | 剂量告警级别枚举：INFO / WARNING / CRITICAL |
| 新建 | `dto/assist/DoseWarningType.java` | 剂量告警类型枚举：OVER_SINGLE_DOSE / OVER_DAILY_DOSE / OVER_DURATION |
| 新建 | `dto/assist/DosageAlert.java` | 剂量告警值对象（API 响应层） |
| 新建 | `dto/assist/DoseWarning.java` | 处方草案剂量告警 DTO |
| 新建 | `dto/assist/AllergyWarningSeverity.java` | 过敏告警严重程度枚举：INFO / WARNING / HIGH |
| 新建 | `dto/assist/AllergyWarningItem.java` | 过敏冲突告警 DTO |
| 新建 | `dto/assist/AiSuggestionStatus.java` | AI 建议状态枚举：PENDING / COMPLETED / FAILED |
| 新建 | `dto/assist/AiSuggestionResult.java` | 异步 AI 建议结果值对象 |
| 新建 | `dto/assist/PrescriptionAssistRequest.java` | 辅助开方主端点请求 DTO |
| 新建 | `dto/assist/PrescriptionAssistResponse.java` | 辅助开方主端点响应 DTO |
| 新建 | `dto/assist/DosageCheckRequest.java` | 剂量即时校验请求 DTO |
| 新建 | `dto/assist/DosageCheckResponse.java` | 剂量即时校验响应 DTO |
| 新建 | `service/assist/PrescriptionAssistService.java` | 辅助开方业务接口 |
| 新建 | `service/assist/DosageThresholdService.java` | 剂量阈值校验服务（六层匹配优先级） |
| 新建 | `service/assist/DedupTaskScheduler.java` | 异步 AI 去重调度器 |
| 新建 | `service/assist/impl/PrescriptionAssistServiceImpl.java` | 辅助开方业务实现 |
| 新建 | `converter/AssistConverter.java` | 业务层 DTO ↔ ai-api DTO 双向映射 |
| 新建 | `api/PrescriptionAssistController.java` | 3 个 REST 端点 |
| 修改 | `context/PrescriptionDraftContext.java` | 增加 updateCriticalAlerts 和 getContextCriticalCount |
| 修改 | `PrescriptionErrorCode.java` | 增加 RX_ASSIST_AI_NO_RECOMMENDATION / RX_ASSIST_SUGGESTION_NOT_FOUND / RX_ASSIST_DOSE_STANDARD_NOT_FOUND |

所有文件前缀路径：`AIMedical/backend/modules/prescription/src/main/java/com/aimedical/modules/prescription/`

## 编译验证

`mvn compile -pl modules/prescription -am -q` — 编译通过，无错误。

## 设计偏差说明

| 设计规格 | 偏差原因 | 实际处理 |
|---------|---------|---------|
| PrescriptionAssistServiceImpl 构造依赖 DrugFacade（可选） | DrugFacade 仅提供 findByDrugCode 返回 DrugInfo，在 assist 流程中未直接使用 | 未注入 DrugFacade，不影响剂量校验和过敏检查流程 |
| DedupTaskScheduler 调用 SuggestionStore.createIfNotExists | SuggestionStore 接口仅有 `compute` 方法无 `createIfNotExists` | 在 DedupTaskScheduler.schedule() 内部通过 `suggestionStore.compute(dedupKey, remappingFunction)` 实现等价逻辑 |
| getSuggestion 使用 SuggestionStore.findByTaskId | SuggestionStore 无该方法 | 使用 `suggestionStore.get(taskId)` 查询 |
| 过敏检查实现：构造 AuditRequest 调用 AllergyCheckRule.check() 获得 LocalRuleResult 后映射为 List\<AllergyWarningItem\> | check() 返回单条结果（第一条匹配），无法逐药品产出过敏告警 | 构造 AuditRequest 调用 check()，从 LocalRuleResult.message 解析 drugId 和 allergen，生成单个 AllergyWarningItem |
| assist 端点签名 `Result<PrescriptionAssistResponse>` | 设计文档中返回 200 + Result.success | Controller 直接返回 `Result.success(response)`，由 Spring 自动包装为 200 |
| check-dose 端点 `Result<DosageCheckResponse>` | 同上 | 同上 |

## 关键实现说明

- **剂量超限判定**：DosageThresholdService 按六层优先级匹配 DosageStandard，短路优先级：dosage > singleMax×2 → CRITICAL；dosage > singleMax → WARNING；频率非空且 dailyMax 非空时独立产出 OVER_DAILY_DOSE 告警
- **去重调度**：DedupTaskScheduler 使用 `prescriptionId + ":suggestion-dedup"` 作为 compute key，原子性检查 PENDING / COMPLETED+!consumed 复用逻辑
- **CRITICAL 告警同步**：assist 和 checkDose 流程均将 CRITICAL 级别告警转换为 context.DosageAlert 后调用 PrescriptionDraftContext.updateCriticalAlerts 全量覆盖写入

## 修订说明（v12 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| AI 失败/异常路径（InterruptedException / ExecutionException / success=false/null data）缺失 errorCode | 在 buildEmptyResponse() 中增加 `response.setErrorCode(PrescriptionErrorCode.RX_ASSIST_AI_NO_RECOMMENDATION.getCode())`，使所有调用该方法的路径均自动携带 errorCode；无推荐路径已有独立 setErrorCode 保持不变 |

## 修订说明（v12 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] assist() 构建 DosageCheckRequest 未设置 unit 导致剂量校验始终命中单位不匹配分支 | 1) PrescriptionItem 新增 `unit` 字段 + getter/setter；2) parseDraftItems() 从 draft JSON 提取 `unit`；3) 构建 DosageCheckRequest 时调用 `setUnit(item.getUnit())` |
| [一般] doseAlerts 死代码：被填充但未被用于构建响应 | 将 localDoseWarnings 转换为 DoseWarning 后合并至 `response.getDoseWarnings()`，使本地剂量校验结果参与响应输出 |
