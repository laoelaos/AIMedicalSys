# 设计审查报告（v8 r1）

## 审查结果
REJECTED

## 发现

### **[一般]** TriageRecord 中 `@Index` 注解位置错误

**文件**：detail_v8.md §TriageRecord（第357行附近）

**问题**：设计中将 `@Index` 直接标注在 `patientId` 字段上：
```java
@Column(name = "patient_id")
@Index  // 建索引
private String patientId;
```

**为什么是问题**：`jakarta.persistence.@Index` 是类级别的 `@Table(indexes = {...})` 中的元素，不能直接标注在字段上。项目中已有实体（`DosageStandard.java:16-19`）的正确用法是在 `@Table` 内声明 index。按此设计直接编码将导致编译错误。

**期望修正方向**：将索引声明移至 `@Table(indexes = {@Index(columnList = "patient_id", name = "idx_patient_id")})` 类级别注解内，或使用 `@Column(index = true)`。

---

### **[一般]** TriageConverter.toTriageResponse 参数类型与任务规约不一致

**文件**：detail_v8.md §TriageConverter（第491-492行）

**问题**：设计定义的签名：
```java
public TriageResponse toTriageResponse(com.aimedical.modules.ai.api.dto.triage.TriageResponse aiResponse,
                                         List<RecommendedDoctor> doctors);
```
但 task_v8.md 规约要求第一个参数为 `AiResult<TriageResponse>`。同时设计的行为描述（step 8）内部引用了 `aiResult.isDegraded()`，但方法签名中并未传入 `AiResult`。

**为什么是问题**：
- `AiResult.isDegraded()`（传输/执行层面降级标记）与 `TriageResponse.isDegraded()`（响应体降级标记）语义不同。丢失 `AiResult` 包装可能导致降级状态传递不完整。
- 签名与行为描述自相矛盾，实现者无法同时满足两者。

**期望修正方向**：恢复为 task_v8 规约的签名 `toTriageResponse(AiResult<TriageResponse> aiResult, List<RecommendedDoctor> doctors)`，在方法内部通过 `aiResult.getData()` 获取响应体，通过 `aiResult.isDegraded()` 获取降级标记。

---

### **[一般]** DeadLetterCompensationService 可能覆盖手动选科

**文件**：detail_v8.md §DeadLetterCompensationService（第212-215行）

**问题**：补偿服务直接调用 `triageService.selectDepartment()` 无条件写入 `finalDepartmentId/finalDepartmentName`。行为契约规定了“RegistrationEventListener.finalDepartmentId 写入优先级的覆盖规则：仅当为空时写入（不覆盖手动选科）”，但补偿服务路径没有同样的保护。

**为什么是问题**：如果用户在事件处理失败后、补偿任务执行前通过 `POST /api/triage/select-department` 手动选择了科室，补偿任务会覆盖用户的手动选择，违反行为契约。

**期望修正方向**：
- 方案A：在 `TriageServiceImpl.selectDepartment()` 内部增加 `finalDepartmentId == null` 检查（由 Controller 端点传入显式覆盖标记以区分手动/自动选科）。
- 方案B：补偿服务在调用前从 DB 检查 `finalDepartmentId` 是否已写入。

---

### **[轻微]** FirstTurn 验证组未定义

**文件**：detail_v8.md §DialogueCreateRequest（第67-70行）

**问题**：设计使用了 `groups = FirstTurn.class` 但未定义 `FirstTurn` 接口类型。

**期望**：补充定义 `public interface FirstTurn {}` 或改用无分组校验 + Service 层逻辑判断。

---

### **[轻微]** DeadLetterCompensationService 反序列化描述不完整

**文件**：detail_v8.md §DeadLetterCompensationService（第212行）

**问题**：描述仅提及 "从 eventPayload JSON 反序列化获取 departmentId/departmentName"，但 `selectDepartment(sessionId, departmentId, departmentName)` 还需要 sessionId。

**期望**：补充说明 eventPayload 包含完整 RegistrationEvent 字段（含 sessionId），反序列化后提取所需三个参数。

## 修改要求（仅 REJECTED 时）

1. **@Index 位置修正**（一般）：将字段级 `@Index` 改为类级 `@Table(indexes = {...})` 或 `@Column(index = true)`。
2. **TriageConverter 签名恢复**（一般）：第一个参数恢复为 `AiResult<TriageResponse>`，与 task_v8 规约一致并消除行为描述自相矛盾。
3. **补偿服务覆盖保护**（一般）：为 selectDepartment 路径增加 `finalDepartmentId` 非空保护，确保不覆盖手动选科。
4. **验证组定义**（轻微）：补充 FirstTurn 接口定义。
5. **补偿服务 payload 描述**（轻微：补充 sessionId 的提取说明。
