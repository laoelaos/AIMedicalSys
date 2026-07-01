# 任务指令（v15）

## 动作
NEW

## 任务描述
**P02/E06 — DrugFacade 注入**：在 `PrescriptionAuditServiceImpl` 和 `PrescriptionAssistServiceImpl` 中构造器注入 `DrugFacade` 接口，在遍历处方条目时调用 `DrugFacade.findByDrugCode()` 获取药品信息，异常/超时时捕获并返回空药品信息（不阻断主流程）。

### 具体变更

**1. PrescriptionAuditServiceImpl.java**
- 构造器增加 `DrugFacade drugFacade` 参数
- 在 `audit()` 或 `persistAuditRecord()` 流程中，遍历 `request.getPrescriptionItems()` 对每个 `item.getDrugId()` 调用 `drugFacade.findByDrugCode()`
- 调用包裹 try-catch，超时/异常时记录 WARN 日志（含调用耗时、异常类型、drugCode），继续处理下一项
- `@Value("${prescription.drug-facade.timeout:2}")` 注入超时阈值（用于 future.get 或预留扩展）

**2. PrescriptionAssistServiceImpl.java**
- 构造器增加 `DrugFacade drugFacade` 参数
- 在 `parseDraftItems()` 或 `assist()` 流程中，遍历处方条目时调用 `drugFacade.findByDrugCode()`
- 调用包裹 try-catch，异常时记录 WARN 日志（含调用耗时、异常类型、drugCode），继续处理下一项
- `@Value("${prescription.drug-facade.timeout:2}")` 注入超时阈值

**3. 测试文件同步**
- `PrescriptionAuditServiceImplTest.java`：构造器传参增加 `drugFacade` mock
- `PrescriptionAssistServiceImplTest.java`：构造器传参增加 `drugFacade` mock

### 日志格式
```
log.warn("DrugFacade.findByDrugCode({}) failed after {}ms: {}", drugCode, elapsed, e.getClass().getSimpleName());
```

### 不修改
- `DrugFacade` 接口本身
- `DrugInfo` record
- 两个 Service 的业务逻辑主流程（DrugFacade 调用为旁路信息查询，失败时不影响主流程）
- 不新增 DTO 字段（药品信息暂不入 response，仅为后续功能预留数据通路）

## 选择理由
P02/E06(P0) DrugFacade 在两个 Service 中均未注入。OOD §2.2 明确要求 DrugFacade 的注入和使用。此轮无其他前后依赖，可独立实施。R14 已清除全量构建阻塞，R15 为剩余轮次中编号最前的 P0 任务。

## 任务上下文
### DrugFacade 接口
```java
// common-module-api 模块
public interface DrugFacade {
    DrugInfo findByDrugCode(String drugCode);
}
```

### DrugInfo record
```java
public record DrugInfo(
    String drugCode,
    String drugName,
    String specification,
    String dosageForm,
    String manufacturer,
    String packageUnit
) {}
```

### PrescriptionAuditServiceImpl 当前构造器（7 参）
```java
public PrescriptionAuditServiceImpl(AiService aiService, LocalRuleEngine localRuleEngine,
                                     AuditRecordRepository auditRecordRepository,
                                     AuditConverter auditConverter,
                                     PrescriptionDraftContext prescriptionDraftContext,
                                     CurrentUser currentUser,
                                     ObjectMapper objectMapper)
```

### PrescriptionAssistServiceImpl 当前构造器（8 参）
```java
public PrescriptionAssistServiceImpl(AiService aiService, AssistConverter assistConverter,
                                      AllergyCheckRule allergyCheckRule,
                                      DosageThresholdService dosageThresholdService,
                                      PrescriptionDraftContext prescriptionDraftContext,
                                      DedupTaskScheduler dedupTaskScheduler,
                                      SuggestionStore suggestionStore,
                                      ObjectMapper objectMapper)
```

## 验证方法
- `mvn test -pl modules/prescription` 确保 prescription 模块 155 测试全部通过
- `mvn test` 全量构建通过（16 模块 BUILD SUCCESS，~1500+ 测试全部 0 失败）
