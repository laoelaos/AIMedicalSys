# 计划审查报告（v6 r1）

## 审查结果
REJECTED

## 发现

### **[严重]** R6 任务与实际代码状态严重不符

`findDoctorsForDepartments` 当前代码 (`TriageServiceImpl.java:180-190`) 为 **同步调用**（直接 `doctorFacade.findAvailableDoctorsByDepartment()`），而 task_v6.md:24-27 描述其为 `CompletableFuture` 异步调用 + `future.get()`。若实施者遵循 task_v6 指令，将引入不必要的异步包装，这直接 **违反 OOD §4.1** 明确表述的"DoctorFacade 作为 **强制同步** 跨模块调用路径"设计。此外 OOD 要求超时在基础设施层（feign/restTemplate 超时配置）处理，而非应用层 `future.get(timeout)`。

修正方向：删除 task_v6.md 中所有关于 CompletableFuture/future.get() 的描述，改为纯同步 try/catch 包裹，与 OOD §4.1 保持一致。

### **[严重]** C07 错误归入 R6

诊断报告 `06_phase2C3DE_report.md:127-128` 明确 C07 指向 `TriageServiceImpl.java:87` 的 **AI 服务调用** `future.get()` 无超时——对应当前代码第 95 行 `aiResult = future.get()`，而非 DoctorFacade 调用。R14 才是 C07 的预期修复轮次。将 C07 归入 R6 不仅导致 R6 任务语义错误，还会使 R14 遗漏此问题。

修正方向：从 R6 移除 C07，在 R14 任务描述中确认 C07 覆盖。

### **[严重]** AvailableDoctor 无 score 字段——"按 score 排序取前 5"不可执行

`AvailableDoctor.java` 为 `record` 类型且仅有 doctorId/doctorName/departmentId/availableSlotCount 四个字段，**不含 score 或类似评分字段**。DoctorFacade 接口也无评分返回能力。task_v6.md:35 要求的 `sorted(...)` 按 score 降序取前 5 因缺乏输入数据而无法实施。

修正方向：删除按 score 排序的要求；如需排序需先确定可用排序依据（如 availableSlotCount），在 R6 计划中明确说明。

### **[一般]** 方法签名描述错误

task_v6.md:25 写方法接收 `Map<String, List<RecommendedDepartment>> departments`，实际签名 (`TriageServiceImpl.java:180`) 为 `List<RecommendedDepartment> departments`。

### **[一般]** TriageController.java 归属不一致

plan.md:89 将 TriageController.java 列入 R6 上下文，但 task_v6.md:41 声明"仅修改 TriageServiceImpl.java，R6 不要求修改 TriageController.java"。虽然实际情况确实无需改 Controller，但跟踪信息不一致易造成混淆。

## 修改要求

1. **重写 R6 实施计划**，基于当前同步代码的真实状态，删除所有 CompletableFuture/future.get() 引用，改为同步 try/catch + WARN 日志 + 空列表返回，与 OOD §4.1 对齐。
2. **从 R6 移除 C07**，确认其在 R14 中修复。
3. **明确排序依据**——AvailableDoctor 无 score，需确定是否有其他字段（如 availableSlotCount）可用于排序，或删除排序要求仅做 limit(5)。
4. 修正方法签名描述为 `List<RecommendedDepartment>`。
5. 统一文件跟踪信息（plan.md 与 task_v6.md 之间）。
