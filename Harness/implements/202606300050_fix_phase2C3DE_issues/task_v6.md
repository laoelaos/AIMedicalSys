# 任务指令（v6 r1）

## 动作
NEW

## 任务描述
实现 DoctorFacade 同步调用异常处理+排序取前5（C06/E03+C17）

涉及文件：
- `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java`

## 选择理由
C06(P0)+E03(P0)+C17(P1) 位于同一方法 `findDoctorsForDepartments`。无前置依赖，consultation 模块目前全部 114 测试通过，可独立实施。

## 任务上下文
来自实现报告 `Docs/Diagnosis/impl/06_phase2C3DE_report.md`：
- **C06/E03(P0)**: `findDoctorsForDepartments` 循环中 doctorFacade 调用异常时未捕获→上层中断。应 try/catch 包裹，记录 WARN 日志（含调用耗时、异常类型、departmentId），返回空列表继续下一个科室。
- **C17(P1)**: 多科室并行查询时，部分失败不应全部回退，应部分失败部分成功；对医生列表排序取前 5 名返回。
- **C07(P1) 已从 R6 移除**：C07 指向 TriageServiceImpl 第 95 行 AI 调用 `future.get()` 无超时（DoctorFacade 为同步调用，非异步），已在 R14 覆盖。

## 已有代码上下文
`TriageServiceImpl.findDoctorsForDepartments` 现有结构（第 180-190 行）：
- 接收 `List<RecommendedDepartment> departments`（同步调用，非异步）
- 对每个科室通过 `doctorFacade.findAvailableDoctorsByDepartment()` 同步调用（无 CompletableFuture）
- 当前没有 try/catch 保护，没有排序取前 5 逻辑
- `AvailableDoctor` 为 record 类型：doctorId, doctorName, departmentId, availableSlotCount — 无 score 字段
- `RecommendedDoctor` 有 score 字段但当前始终设为 0f（DoctorFacade 不提供评分数据）
- 排序依据：使用 `availableSlotCount` 降序作为排序键（`AvailableDoctor` 中唯一可用数值字段）

## R6 实施要点

1. **`findDoctorsForDepartments` 方法重构**：
   - 为每个科室的 `doctorFacade.findAvailableDoctorsByDepartment()` 同步调用添加 try/catch 块
   - catch 中记录 WARN 日志：`"DoctorFacade call failed for department {} after {}ms: {} {}", departmentId, elapsedMs, ex.getClass().getSimpleName(), ex.getMessage()`
   - 异常时返回 `Collections.emptyList()` 作为该科室的医生列表
   - 正常路径使用 `stream().sorted(...)` 按 `availableSlotCount` 降序排列，取前 5 个：`collected.stream().sorted((a,b) -> Integer.compare(b.getAvailableSlotCount(), a.getAvailableSlotCount())).limit(5).collect(Collectors.toList())`

2. **不涉及超时配置**：DoctorFacade 为同步调用，超时由基础设施层（feign/restTemplate）配置处理，无需在应用层通过 `@Value` + `future.get(timeout)` 实现。

3. **不涉及外部文件修改**：仅修改 `TriageServiceImpl.java`。

## 修订说明（v6 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| R6 任务与实际代码状态严重不符——findDoctorsForDepartments 为同步调用，task_v6.md 描述为 CompletableFuture 异步 + future.get() | 删除所有 CompletableFuture/future.get() 引用，改为纯同步 try/catch 包裹，与 OOD §4.1 对齐 |
| C07 错误归入 R6——C07 指向 AI 调用 future.get()（第 95 行），非 DoctorFacade 调用 | 从 R6 移除 C07，在 R14 任务描述中确认覆盖 |
| AvailableDoctor 无 score 字段——"按 score 排序取前 5"不可执行 | 删除按 score 排序，改为按 availableSlotCount 降序取前 5（AvailableDoctor 唯一可用数值字段） |
| 方法签名描述错误——接收 List<RecommendedDepartment> 非 Map | 修正方法签名描述为 `List<RecommendedDepartment>` |
| TriageController.java 归属不一致 | 从涉及文件和上下文中移除 TriageController（与实际一致） |
