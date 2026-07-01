# 任务指令（v18）

## 动作
NEW

## 任务描述
**R18 — SubmitResponse+WarnResult（P05）**

实现 `SubmitResponse.warnResult` 字段、`WarnResult` 和 `WarnAlert` DTO，改写 WARN 路径使用 `warnResult` 替代 `errorCode`，移除 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT`。

涉及文件（4-6 个）：
1. **新建** `com.aimedical.modules.prescription.dto.audit.WarnResult.java`
2. **新建** `com.aimedical.modules.prescription.dto.audit.WarnAlert.java`
3. **修改** `SubmitResponse.java` — 增加 `warnResult` 字段
4. **修改** `PrescriptionAuditServiceImpl.java` — WARN 路径改写
5. **修改** `PrescriptionErrorCode.java` — 移除 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT`
6. **修改** `PrescriptionAuditServiceImplTest.java` — 测试适配
7. **修改** `PrescriptionErrorCodeTest.java` — 测试适配
8. **修改** `PrescriptionAuditControllerTest.java` — 测试适配

## 选择理由
P05(P1) — WARN 路径不应通过 errorCode 承载风险语义。OOD §1.1a/§4.6 明确要求 SubmitResponse 增加 `warnResult`（WarnResult 类型）字段，WARN 风险信息由 warnResult 统一承载，errorCode=null。R18 无前后依赖（R17 测试修复已 PASSED），可独立实施。

## 任务上下文
### OOD §1.1a SubmitResponse 定义：
- submitted（boolean）
- prescriptionOrderId（String，可选）
- blockInfo（BlockResponse，可选）
- errorCode（String，可选）
- **warnResult（WarnResult，可选**——步③ forceSubmit=false + 最新审核为 WARN + 处方未变更路径下填充，errorCode=null）

### OOD §1.1a WarnResult 定义：
- riskLevel（AuditRiskLevel 枚举，必填——值域为 WARN）
- alerts（List\<WarnAlert\>，必填）
- auditRecordId（Long，必填）
- prescriptionHash（String，必填）

### OOD §1.1a WarnAlert 定义：
- alertCode（String，必填）
- alertMessage（String，必填）
- severity（AlertSeverity 枚举，必填——值域为 WARNING）

### OOD §4.6 SubmitResponse 多状态字段约束表：
| 路径 | submitted | blockInfo | errorCode | warnResult | HTTP |
|------|----------|-----------|-----------|------------|------|
| 步③ forceSubmit=false + WARN（未变更） | false | null | null | WarnResult | 200 |
| 步③ forceSubmit=false + WARN（已变更） | false | null | RX_AUDIT_PRESCRIPTION_MODIFIED | null | 409 |

### OOD §4.6 第 678 行：
> WARN 路径的 RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT 错误码从代码中移除——此错误码在 OOD 任何位置均未定义，且与 warnResult 承载的 WARN 结果语义重复。WARN 风险信息统一由 warnResult 承载，errorCode 字段在 WARN 路径下恒为 null。

## 已有代码上下文

### SubmitResponse.java（`modules/prescription/src/main/java/.../dto/audit/SubmitResponse.java`）
当前字段：submitted（boolean）、prescriptionOrderId（String）、blockInfo（BlockResponse）、errorCode（String）。需要增加 `private WarnResult warnResult` + getter/setter。

### BlockResponse.java（参考模式）
```java
public class BlockResponse {
    private List<String> blockReasons;
    private String blockCode;
    private LocalDateTime blockTime;
    // 全参构造 + 无参构造 + getter/setter
}
```
WarnResult 和 WarnAlert 遵循相同模式。

### PrescriptionAuditServiceImpl.java WARN 路径（2 处）
**handleStepThree L220-231**：
```java
if (!request.isForceSubmit() && riskLevel == AuditRiskLevel.WARN) {
    if (!prescriptionsMatch(latestRecord.getOriginalPrescription(), request.getPrescriptionItems())) {
        // 返回 errorCode=RX_AUDIT_PRESCRIPTION_MODIFIED（保持不变）
        ...
    }
    // 当前：submitted=false + errorCode=RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT
    // 改为：submitted=false + warnResult=WarnResult(...) + errorCode=null
}
```

**buildStepThreeResponse L308-320**：
```java
if (riskLevel == AuditRiskLevel.WARN) {
    if (record != null && !prescriptionsMatch(record.getOriginalPrescription(), request.getPrescriptionItems())) {
        // 返回 errorCode=RX_AUDIT_PRESCRIPTION_MODIFIED（保持不变）
        ...
    }
    if (!request.isForceSubmit()) {
        // 当前：submitted=false + errorCode=RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT
        // 改为：submitted=false + warnResult=WarnResult(...) + errorCode=null
    }
}
```

### prescriptionHash 计算方式
从 `latestRecord.getOriginalPrescription()`（JSON 字符串）计算 SHA-256 摘要，转换为十六进制字符串。简单实现：
```java
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hash = digest.digest(originalJson.getBytes(StandardCharsets.UTF_8));
StringBuilder hex = new StringBuilder();
for (byte b : hash) { hex.append(String.format("%02x", b)); }
return hex.toString();
```

### AuditAlert 结构（参考 WarnAlert）
```java
public class AuditAlert {
    private String alertCode;
    private String alertMessage;
    private AlertSeverity severity;
}
```
WarnAlert 使用相同字段结构，与 AuditAlert 为并行 DTO 而非替代关系。

### 测试同步清单
1. **PrescriptionAuditServiceImplTest.java:738-739** — 断言 `result.getWarnResult()` 非空且字段正确，替代 `assertEquals(errorCode, result.getErrorCode())`
2. **PrescriptionErrorCodeTest.java:11** — `assertEquals(11, ...)` → `assertEquals(10, ...)`；移除 L20-21（RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT 断言）
3. **PrescriptionAuditControllerTest.java:101** — `response.setErrorCode("RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT")` 改为 `response.setErrorCode("RX_AUDIT_PRESCRIPTION_MODIFIED")`（或调整为其他已有错误码），保持 400 断言逻辑
