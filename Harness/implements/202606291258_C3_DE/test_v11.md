# 测试报告（v11）

## 测试文件清单

| 测试文件 | 被测单元 | 覆盖维度 |
|---------|---------|---------|
| `prescription/.../service/audit/impl/PrescriptionAuditServiceImplTest.java` | PrescriptionAuditServiceImpl | audit() 正常/降级/异常/持久化；submit() 三步阻断/forceSubmit/乐观锁/无结果回查/二次CRITICAL验证；revoke() 成功/不存在/已撤销/非WARN；hasNewAlerts() 快照-增量边界（反射辅助） |
| `prescription/.../api/PrescriptionAuditControllerTest.java` | PrescriptionAuditController | audit() 200/422；submit() 200/422/400；revoke() 200；422响应体blockCode+blockReasons断言 |
| `prescription/.../PrescriptionErrorCodeTest.java` | PrescriptionErrorCode | 枚举数量（8项）；code/message取值验证 |

## 覆盖维度

- **正常路径**：audit AI成功、submit PASS直接提交、revoke WARN+latest
- **边界条件**：hasNewAlerts 快照空/current空/一致/有增量
- **错误路径**：audit AI失败/异常降级、submit阻断步①/步②/二次验证、revoke 不存在/已撤销/非WARN
- **状态交互**：auditSequence递增、提交乐观锁冲突、forceSubmit五字段比对

## 修订说明（v11 r3）

| 审查意见 | 修正措施 |
|---------|---------|
| [严重] submitShouldBlockOnAuditResultStep2 — getCriticalAlerts stub 未被消费导致 UnnecessaryStubbingException | 移除不必要的 `getCriticalAlerts` stub，增加显式 `hasCriticalAlerts` mock |
| [严重] submitShouldDetectNewCriticalAlertsBetweenStep2AndStep3 — hasCriticalAlerts 未 mock 导致链式 stub 偏移 | 增加 `hasCriticalAlerts` mock，将 `getCriticalAlerts` 链式 stub 改为单值返回 |
| [一般] auditShouldPersistRecordWithCorrectSequence — save 计数错误 | `times(2)` → `times(1)` |
| [一般] submitShouldBlockOnCriticalDoseStep1 — 未 mock hasCriticalAlerts，验证步③而非步① | 增加 `hasCriticalAlerts(true)` mock，确保真实验证步①阻断 |
| [一般] submitShouldReAuditWhenNoLatestRecordFoundThenReturnBlock — 缺少必要 mock | 增加显式 `hasCriticalAlerts(false)` 和 `getCriticalAlerts(empty)` mock |
| [轻微] PrescriptionErrorCodeTest — RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT 缺少 message 断言 | 增加 `.getMessage()` 断言 |
