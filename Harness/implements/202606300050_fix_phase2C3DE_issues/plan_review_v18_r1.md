# 计划审查报告（v18 r1）

## 审查结果
APPROVED

## 发现

### 已验证的对齐点
- **OOD §1.1a/§4.6 完全对齐**：WarnResult（riskLevel/alerts/auditRecordId/prescriptionHash）和 WarnAlert（alertCode/alertMessage/severity）字段定义与实际 OOD 文档一致
- **OOD §4.6 状态约束表对齐**：路径"forceSubmit=false + WARN（未变更）"映射为 submitted=false + errorCode=null + warnResult=WarnResult + HTTP 200，与 OOD 表一致
- **OOD §4.6 L678 指令对齐**：明确要求移除 RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT，计划涵盖所有 7 处引用（生产代码 2 处 + enum 定义 1 处 + 测试 4 处）
- **P05 诊断报告覆盖**：SubmitResponse 增 warnResult 字段 + WARN 路径改写 + 错误码移除，与诊断报告 §P05 修复方向完全一致
- **DTO 模式一致**：WarnResult/WarnAlert 遵循 BlockResponse/AuditAlert 的构造+无参构造+getter/setter 模式
- **测试同步完整**：PrescriptionAuditServiceImplTest (L739) / PrescriptionErrorCodeTest (L11, L20-21) / PrescriptionAuditControllerTest (L101) 全部覆盖
- **prescriptionHash 方案合理**：SHA-256(latestRecord.getOriginalPrescription()) 可稳定校验处方版本变更

### **[轻微]** buildStepThreeResponse record=null 边界未显式处理
handleStepThree L203 在 `latestRecord==null` 时调用 `buildStepThreeResponse(request, auditResp, null)`。若新审核结果为 WARN，`record` 为 null 导致 `auditRecordId` 和 `prescriptionHash` 不可直接使用。原代码在该路径下同样运行（仅设 errorCode），R18 建议通过从 `auditResp` 或当前请求补充处理，或确认此边界实际不会到达（首次审核至少一条记录后方可提交）。不影响 R18 主路径正确性。

### **[轻微]** WarnResult.alerts 数据源细节
计划提到 "alerts 从 latestRecord 的 auditIssues 解析"，但 auditIssues 存储的 JSON 中包含 AuditIssue 对象（alertCode/message/severity），需 `objectMapper.readValue()` 反序列化后映射为 WarnAlert。这是合理的实现路径，建议实施时注意 auditIssues 为 null 时的空列表兜底。

## 修改要求
无严重或一般问题。
