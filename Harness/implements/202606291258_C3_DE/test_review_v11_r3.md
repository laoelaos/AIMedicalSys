# 测试审查报告（v11 r3）

## 审查结果
REJECTED

## 发现

- **[严重]** `PrescriptionAuditServiceImplTest.java:198-217` (`submitShouldBlockOnAuditResultStep2`) — `getCriticalAlerts` 被 stubbed 但从未被调用（实现在第 2 步找到 BLOCK 记录后提前返回）。Mockito `@ExtendWith(MockitoExtension.class)` 默认 `STRICT_STUBS` 会将这个未使用的 stub 报告为 `UnnecessaryStubbingException`，测试会失败。

- **[严重]** `PrescriptionAuditServiceImplTest.java:445-465` (`submitShouldDetectNewCriticalAlertsBetweenStep2AndStep3`) — `hasCriticalAlerts` 未 mock（返回默认 `false`），因此第 1 步不调用 `getCriticalAlerts`。chained stub `.thenReturn(new ArrayList<>()).thenReturn(List.of(...))` 期望 2 次调用，但实际仅 1 次（在第 2→3 步之间的重新查询），返回第一个值（空列表），`hasNewAlerts(empty, empty)` 返回 `false`，提交流程继续到第 3 步而不阻断。测试期望阻断，实际不阻断，断言失败。

- **[一般]** `PrescriptionAuditServiceImplTest.java:159` (`auditShouldPersistRecordWithCorrectSequence`) — `verify(auditRecordRepository, times(2)).save(any(AuditRecord.class))` 应为 `times(1)`。实现中 `save` 仅被调用一次（保存新记录），`saveAll` 用于处理已有记录。

- **[一般]** `PrescriptionAuditServiceImplTest.java:183-196` (`submitShouldBlockOnCriticalDoseStep1`) — `hasCriticalAlerts` 未 mock（返回 `false`），阻断实际发生在第 2→3 步重新查询而非第 1 步。测试侥幸通过（相同 blockCode），但未验证第 1 步阻断行为。

- **[一般]** `PrescriptionAuditServiceImplTest.java:468-494` (`submitShouldReAuditWhenNoLatestRecordFoundThenReturnBlock`) — `getCriticalAlerts` 和 `hasCriticalAlerts` 均未 mock，依赖默认返回值（`false`/`null`）。测试脆弱，任何告警检查实现变更都可能导致失败。

- **[轻微]** `PrescriptionErrorCodeTest.java:20` — `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT` 的 `.getMessage()` 未断言，与其他 7 个错误码不一致。

## 修改要求（仅 REJECTED 时）

### [严重] submitShouldBlockOnAuditResultStep2

**位置**: `PrescriptionAuditServiceImplTest.java:198-217`
**问题**: `getCriticalAlerts` stub 从未被消费，`STRICT_STUBS` 模式下测试失败。
**修正方向**: 移除不必要的 `getCriticalAlerts` stub，或使用 `Mockito.lenient()` 包裹。

### [严重] submitShouldDetectNewCriticalAlertsBetweenStep2AndStep3

**位置**: `PrescriptionAuditServiceImplTest.java:445-465`
**问题**: `hasCriticalAlerts` 未 mock，导致 chained stub 偏移，实际返回错误的值。
**修正方向**: 增加 `when(prescriptionDraftContext.hasCriticalAlerts("rx-001")).thenReturn(false)`，并将 `getCriticalAlerts` stub 改为单一返回值（空列表），分别 mock 第 1 次（空）和第 2 次（有告警），或改用 `thenReturn` 配合 `thenAnswer`。

### [一般] auditShouldPersistRecordWithCorrectSequence — save 计数

**位置**: `PrescriptionAuditServiceImplTest.java:159`
**问题**: `times(2)` 应为 `times(1)`。
**修正方向**: 将 `times(2)` 改为 `times(1)`。

### [一般] submitShouldBlockOnCriticalDoseStep1 — 测试意图与实现不符

**位置**: `PrescriptionAuditServiceImplTest.java:183-196`
**问题**: 未 mock `hasCriticalAlerts`，不能验证第 1 步阻断。
**修正方向**: 增加 `when(prescriptionDraftContext.hasCriticalAlerts("rx-001")).thenReturn(true)` 以确保第 1 步真实验证。

### [一般] submitShouldReAuditWhenNoLatestRecordFoundThenReturnBlock — 缺少必要 mock

**位置**: `PrescriptionAuditServiceImplTest.java:468-494`
**问题**: `getCriticalAlerts` 和 `hasCriticalAlerts` 未 mock。
**修正方向**: 增加 `when(prescriptionDraftContext.hasCriticalAlerts("rx-001")).thenReturn(false)` 和 `when(prescriptionDraftContext.getCriticalAlerts("rx-001")).thenReturn(new ArrayList<>())`。
