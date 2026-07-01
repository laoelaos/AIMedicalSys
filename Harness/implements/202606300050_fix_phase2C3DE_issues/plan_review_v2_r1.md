# 计划审查报告（v2 r1）

## 审查结果
REJECTED

## 发现

### **[一般] R2 接口变更导致 DeadLetterCompensationService.java 编译失败（跨轮次依赖未处理）**

- **问题描述**：R2 将 `TriageService.selectDepartment` 从 4 参改为 3 参（移除 `overwrite`）。但 `DeadLetterCompensationService.java:38` 以 4 参形式调用该方法：
  ```java
  triageService.selectDepartment(sessionId, departmentId, departmentName, false);
  ```
  该文件仅在 R15（C14+E05）中计划修改，导致 R2 实施后至 R15 完成前的整个区间内，`deadLetterCompensationService` 编译失败。

- **为什么是问题**：编译阻塞会使 R2→R15 之间的各轮次无法通过 `mvn compile` 验证。R2 task_v2.md 声称"改为 3参后调用方自动对齐"，但 Java 中接口签名变更不会自动同步调用方——所有 4 参调用点必须手动修改，否则编译不通过。

- **期望的修正方向**：方案 A：在 R2 中同步修改 `DeadLetterCompensationService.java:38` 为 3 参调用（仅移除 `false` 参数，死信逻辑的其它修复保留在 R15）。方案 B：在 plan.md 的 R2 文件清单中显式列出该文件，并在实施顺序约束中说明此依赖。推荐方案 A，因为改动极小且确保编译通过。

### **[轻微] R2 未覆盖测试桩/测试调用点的接口变更**

- **问题描述**：以下测试文件同样使用 4 参接口，R2 变更后编译失败：
  - `TriageControllerTest.java:47` — `StubTriageService.selectDepartment(... boolean overwrite)` 模拟实现需改为 3 参
  - `TriageServiceImplTest.java:286/301` — `service.selectDepartment(..., "内科", true/false)` 调用需改为 3 参
  - `DeadLetterCompensationServiceTest.java:154` — 模拟实现需改为 3 参

- **为什么是问题**：测试编译失败导致无法运行验证 R2 修改的正确性。

- **期望的修正方向**：R2 实施时应同步更新所有测试桩和测试调用点。

### **[轻微] plan.md 与 task_v2.md 在 @RequestBody 使用上不一致**

- **问题描述**：plan.md R2 任务描述第 3 项写"使用 @RequestBody"，但 task_v2.md 第 6 项标记为"可选"并建议保持 `@RequestParam`。

- **为什么是问题**：可能导致实现者不确定采用哪种方式。

- **期望的修正方向**：统一为一种表述。task_v2.md 的"可选项"方案更稳妥——保持 `@RequestParam` 不变，仅移除第 4 个 hardcoded 参数，与 OOD 对齐且最小化 Controller 变更。

## 修改要求（仅 REJECTED 时）

### 问题 1（一般）修正方向
**方案 A（推荐）**：在 R2 任务中显式加入：
- 修改 `DeadLetterCompensationService.java:38`：将 `triageService.selectDepartment(sessionId, departmentId, departmentName, false)` 改为 `triageService.selectDepartment(sessionId, departmentId, departmentName)`
- 行为语义不变：`selectDepartment` 新实现始终覆盖写入，而之前 `false` 仅在 `finalDepartmentId == null` 时才写入。确认此行为变更对死信补偿场景正确（补偿任务需要始终覆盖写入以恢复一致性）。

**方案 B**：在 plan.md 中 R2 文件清单增加 `DeadLetterCompensationService.java`（1 行修改），并在 R15 依赖中注明。
