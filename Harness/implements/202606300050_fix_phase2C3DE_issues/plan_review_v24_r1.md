# 计划审查报告（v24 r1）

## 审查结果
REJECTED

## 发现

### **[一般]** VisitIdReconciledTask 执行频率与 OOD 不符
task_v24.md 指定 `@Scheduled(fixedDelay = 60000)`（60 秒执行一次），但 OOD §6.1 明确要求：
> "每 30 分钟由 Spring @Scheduled(cron = "0 */30 * * * ?") 调度"

实现报告 M03 同样要求每 30 分钟。60 秒的频率将导致数据库扫描频率是设计值的 30 倍，产生不必要的负载。

### **[一般]** VisitIdReconciledTask 所在模块错误
task 将其放在 `consultation/.../task/VisitIdReconciledTask.java`，但 OOD §6.1 明确指出：
> "medical-record 模块的 VisitIdReconciledTask（com.aimedical.modules.medicalrecord.task.VisitIdReconciledTask）"

该任务需要访问 MedicalRecord 实体和 VisitFacade，放在 consultation 模块会导致模块依赖不一致或编译失败。

### **[一般]** SuggestionCleanupTask 条目 TTL 值错误
task 写"清理...过期条目（TTL 5min）"，但 OOD §6.1 明确说明：
> "AiSuggestionResult TTL 60 分钟，由统一 Spring @Scheduled 任务每 5 分钟扫描清理 TTL 超过 60 分钟的过期条目"

实施者按 5min 实现会导致条目被过早清理，COMPLETED 状态的结果在用户查询前就被删除。

### **[一般]** DraftContextCleanupTask 条目 TTL 值错误
task 写"超时草稿（TTL 30min）"，但 OOD §6.1 要求：
> "PrescriptionDraftContext TTL 60 分钟"
> "清理 DraftContextStore 清理最后更新时间超过 60 分钟的条目"

实现报告 P03 同样要求 TTL > 60 分钟。

### **[一般]** TemplateConfigChangeEvent 发布/消费关系反转
task 描述为 `DatabaseTemplateConfigManager 变更时发布，监听器调用 Caffeine Cache 的 invalidateAll()`，暗示 DatabaseTemplateConfigManager 是发布方。但 OOD §2.2 领域事件表明确：
- **发布模块**：admin（管理员更新模板配置后发布）
- **消费模块**：medical-record（TemplateConfigManager 监听刷新缓存）

DatabaseTemplateConfigManager 在 medical-record 模块中应作为 **监听方** 消费事件并 invalidate 缓存，而非发布方。如果实施者按 task 描述编码，会导致事件发布在错误的位置，缓存失效逻辑失效。

### **[轻微]** DrugDictChangeEvent 基类名称未在 plan 中显式声明
实现报告建议 `BaseDrugDataChangeEvent`，task 写 `DrugDictChangeEvent`，plan 仅写"共享基类"未明确名称。虽不影响计划可行性，但名称不一致可能引入混淆。

## 修改要求（REJECTED）

1. **VisitIdReconciledTask 频率**：将 `fixedDelay = 60000` 改为 `cron = "0 */30 * * * ?"`（30 分钟），对齐 OOD §6.1
2. **VisitIdReconciledTask 模块**：将模块从 consultation 改为 medical-record，对齐 OOD §6.1
3. **SuggestionCleanupTask TTL**：将"TTL 5min"修正为"扫描间隔 5 分钟，条目 TTL 60 分钟"，对齐 OOD §6.1
4. **DraftContextCleanupTask TTL**：将"TTL 30min"修正为"TTL 60 分钟"，对齐 OOD §6.1
5. **TemplateConfigChangeEvent 发布/消费关系**：修正为"admin 模块 CRUD Service 发布事件 → DatabaseTemplateConfigManager 监听并 invalidateAll()"，对齐 OOD §2.2 领域事件表
6. **（建议修正）Drug 事件类模块位置**：将 `common/event/` 改为 `prescription/event/`，对齐 OOD §2.2
7. **（建议修正）TemplateConfigChangeEvent 模块位置**：将 `admin/event/` 改为 `medical-record/event/`，对齐 OOD §2.2
