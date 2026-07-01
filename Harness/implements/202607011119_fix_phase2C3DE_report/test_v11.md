# 测试报告（v11）

## 验证结果

`mvn test -pl modules/prescription -am`

**Tests run: 326, Failures: 0, Errors: 0, Skipped: 0**

**BUILD SUCCESS**

## 变更覆盖

| 修复 | 被测文件 | 状态 |
|------|---------|:----:|
| F1 | `PrescriptionDraftContextTest.java:116` — `eq()` wrapper for raw value | 通过 |
| F2 | `AuditConverterTest.java:97-99` — `setDose(BigDecimal.valueOf(100))` | 通过 |
| F3 | `AuditConverterTest.java:132-134` — `setDose(BigDecimal.valueOf(100))` | 通过 |
| F4 | `AllergyCheckRule.java:75-84` — `toLowerCase()` in `hasNegationPrefix` | 通过 |
| F5 | `PrescriptionAssistServiceImplTest.java:1013,1018` — `setPrescriptionId` + `times(2)` verify | 通过 |

## 偏差说明

**F5 设计偏差**：详细设计 v11 仅指定在第 1013 行新增 `assistRequest.setPrescriptionId("rx-001")`，但运行后发现 `assist()` 第 176 行与 `clearCriticalAlerts` 第 239 行均以 `"rx-001"` 调用 `updateCriticalAlerts`，导致 Mockito verify 默认 `times(1)` 不匹配（实际 2 次）。修正：将 verify 改为 `times(2)`，准确反映两个调用点。

## 修订记录

| 轮次 | 说明 |
|:----:|------|
| v11 r1 | 初始测试验证，F1-F4 通过，F5 因 verify 调用计数不匹配失败 |
| v11 r2 | 将 F5 verify 改为 `times(2)`，全部 326 项测试通过 |
