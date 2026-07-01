# 任务指令（v1）

## 动作
NEW

## 任务描述
**修复 P0-C06: DoctorFacade 跨模块调用无超时控制**

目标文件：
- `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java`
- `AIMedical/backend/modules/consultation/src/main/resources/application.yml`（若需要确认/调整配置）

具体要求：
1. 在 findDoctorsForDepartments 方法中，对 `doctorFacade.findAvailableDoctorsByDepartment(departmentId)` 调用注入超时控制
2. 读取 `consultation.doctor-facade.timeout` 配置（默认 2s）
3. 当超时发生时，捕获异常并记录 WARN 级别日志（含调用耗时、异常类型和科室 ID），将 TriageResponse.doctors 置为空列表（不阻断分诊主流程）
4. 保持现有 try/catch 结构，将超时保护嵌入调用逻辑

## 选择理由
该问题是 P0 级别的最高优先级，直接关系系统稳定性——"doctorFacade 调用 → 无超时 → 下游服务阻塞 → 线程耗尽"。修复影响范围小（仅 TriageServiceImpl 一个方法），快速见效。

## 任务上下文
- 问题报告描述：TriageServiceImpl.java:213-236 的 findDoctorsForDepartments 方法无超时控制。try/catch（第226行）和 WARN 日志（第228-229行）已存在，但 application.yml:29 配置的 `consultation.doctor-facade.timeout: 2` 未被 TriageServiceImpl 注入。
- OOD 要求：DoctorFacade 跨模块调用降级保护——配置独立超时阈值（默认 2s），超时时捕获异常并将 doctors 置为空列表，记录 WARN 日志。
- 当前代码已注入 `@Value` 参数（第 68 行 `aiTimeout`），需补充注入 `doctorFacadeTimeout` 或复用已有注入模式。

## 已有代码上下文
- TriageServiceImpl.java:50-79 构造函数注入 AiService、TriageRuleEngine、DoctorFacade 等
- TriageServiceImpl.java:213-236 findDoctorsForDepartments 方法：遍历 departments，对每个 department 调用 doctorFacade.findAvailableDoctorsByDepartment()，异常时 catch 记录日志，最终按 availableSlotCount 排序取前 5
- application.yml 中 `consultation.doctor-facade.timeout: 2` 已存在配置

---

## 修订说明（v1 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] R1 实施方式不明确 | 已在 plan.md R1 中明确为 CompletableFuture.supplyAsync + .get(timeout) 方案，task 要求 4「保持现有 try/catch 结构」与此方案一致 |
| [一般] R1 缺少 task 已有的详细要求 | 已在 plan.md 补充 WARN 日志（耗时/异常类型/科室ID）、空列表兜底、复用现有结构的确认；当前 task 内容保持不变，已在具体要求中涵盖 |

## 修订说明（v1 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] P1 问题 C13 未被覆盖 | 已将 C13 补入 plan.md 任务 16（consultation P1 批量）子项清单，当前任务（C06）不受影响 |
