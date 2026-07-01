# 测试报告（v12）

## 测试文件清单

### 新增测试文件（18个）

| 测试文件路径 | 被测类型 |
|-------------|---------|
| `prescription/src/test/java/com/aimedical/modules/prescription/dto/assist/PrescriptionAssistRequestTest.java` | PrescriptionAssistRequest |
| `prescription/src/test/java/com/aimedical/modules/prescription/dto/assist/PrescriptionAssistResponseTest.java` | PrescriptionAssistResponse |
| `prescription/src/test/java/com/aimedical/modules/prescription/dto/assist/DosageCheckRequestTest.java` | DosageCheckRequest |
| `prescription/src/test/java/com/aimedical/modules/prescription/dto/assist/DosageCheckResponseTest.java` | DosageCheckResponse |
| `prescription/src/test/java/com/aimedical/modules/prescription/dto/assist/DosageAlertTest.java` | DosageAlert |
| `prescription/src/test/java/com/aimedical/modules/prescription/dto/assist/DosageAlertLevelTest.java` | DosageAlertLevel |
| `prescription/src/test/java/com/aimedical/modules/prescription/dto/assist/DoseWarningTypeTest.java` | DoseWarningType |
| `prescription/src/test/java/com/aimedical/modules/prescription/dto/assist/DoseWarningTest.java` | DoseWarning |
| `prescription/src/test/java/com/aimedical/modules/prescription/dto/assist/AllergyWarningItemTest.java` | AllergyWarningItem |
| `prescription/src/test/java/com/aimedical/modules/prescription/dto/assist/AllergyWarningSeverityTest.java` | AllergyWarningSeverity |
| `prescription/src/test/java/com/aimedical/modules/prescription/dto/assist/AiSuggestionResultTest.java` | AiSuggestionResult |
| `prescription/src/test/java/com/aimedical/modules/prescription/dto/assist/AiSuggestionStatusTest.java` | AiSuggestionStatus |
| `prescription/src/test/java/com/aimedical/modules/prescription/service/assist/DosageThresholdServiceTest.java` | DosageThresholdService |
| `prescription/src/test/java/com/aimedical/modules/prescription/service/assist/DedupTaskSchedulerTest.java` | DedupTaskScheduler |
| `prescription/src/test/java/com/aimedical/modules/prescription/service/assist/impl/PrescriptionAssistServiceImplTest.java` | PrescriptionAssistServiceImpl |
| `prescription/src/test/java/com/aimedical/modules/prescription/converter/AssistConverterTest.java` | AssistConverter |
| `prescription/src/test/java/com/aimedical/modules/prescription/api/PrescriptionAssistControllerTest.java` | PrescriptionAssistController |

### 修改测试文件（2个）

| 测试文件路径 | 修改内容 |
|-------------|---------|
| `prescription/src/test/java/com/aimedical/modules/prescription/context/PrescriptionDraftContextTest.java` | 新增 updateCriticalAlerts 和 getContextCriticalCount 测试 |
| `prescription/src/test/java/com/aimedical/modules/prescription/PrescriptionErrorCodeTest.java` | 更新 values().length 从 8→11，新增 3 个 RX_ASSIST_* 错误码断言 |

## 测试覆盖

### DTO/枚举层（12个测试类）
- 字段 getter/setter 验证
- 枚举值域完整性验证

### PrescriptionAssistServiceImpl（14个测试用例）
- assist()：prescriptionId 空值自动生成、InterruptedException 降级、ExecutionException 降级、AI 空 drugs 返回 RX_ASSIST_AI_NO_RECOMMENDATION、AI 正常有推荐完整返回、本地剂量告警合并至响应、过敏警告合并
- checkDose()：完整响应返回、prescriptionId 空值生成、CRITICAL 写入上下文
- getSuggestion()：不存在抛 BusinessException、COMPLETED 设 consumed、PENDING 直接返回、FAILED 直接返回

### DosageThresholdService（8个测试用例）
- 标准未找到 → CRITICAL + errorCode
- 单位不匹配 → WARNING
- dosage > singleMax×2 → CRITICAL（短路）
- dosage > singleMax → WARNING
- dosage 在范围内 → 无告警
- 日剂量 + 单次剂量同时超限 → 两条告警
- 六层优先级精确匹配
- 优先级全未命中 → 标准未找到

### DedupTaskScheduler（4个测试用例）
- 无现有 task → 新建
- PENDING → 复用
- COMPLETED+!consumed → 复用
- FAILED → 新建

### AssistConverter（5个测试用例）
- 业务层 → ai-api 映射
- aiApi 失败 → 空响应
- aiApi 成功 → 完整映射
- null warningType/severity → 默认兜底
- 非法 severity → INFO 兜底

### PrescriptionAssistController（3个测试用例）
- POST /assist → 200
- POST /assist/check-dose → 200
- GET /assist/suggestion/{taskId} → 200

### PrescriptionDraftContext 新增（5个测试用例）
- updateCriticalAlerts 非空 → put
- updateCriticalAlerts 空 → remove
- updateCriticalAlerts null → remove
- getContextCriticalCount 无告警 → 0
- getContextCriticalCount 有告警 → 正确计数

### PrescriptionErrorCode 修改
- values().length: 8 → 11
- 新增 RX_ASSIST_AI_NO_RECOMMENDATION / RX_ASSIST_SUGGESTION_NOT_FOUND / RX_ASSIST_DOSE_STANDARD_NOT_FOUND

## 设计偏差说明
- 无偏差

## 依赖的已有类型
- AiResult, AiService, SuggestionStore, DraftContextStore（mock 注入）
- DosageStandard, DosageStandardRepository（mock 注入）
- AllergyCheckRule, LocalRuleResult, AuditRiskLevel（mock 注入）
- PatientInfo, PrescriptionItem, AuditRequest （直接构造）
- ObjectMapper（直接实例化）
