# 计划审查报告（v24 r2）

## 审查结果
REJECTED

## 发现

### **[严重] — DialogueSessionManager 变更指令与代码现状严重不符**

任务第 7 项要求："DialogueSessionManager：`TTL_DURATION` 从 `1` 改为 `5`（分钟）"。

存在问题：
1. **字段不存在**：代码中无 `TTL_DURATION` 字段。实际字段为 `SESSION_TTL_MINUTES = 30`（自 v1 commit 90f8319 起始终为 30）。
2. **C11 实际意图**：诊断报告 C11 指出 `@Scheduled(fixedRate = 60000)` 清理周期为 1 分钟，OOD §3.1 要求每 5 分钟扫描清理。问题在**调度间隔**而非 TTL 时长。
3. **OOD 设计的 TTL 已实现**：OOD 要求"会话 TTL 30 分钟"（§6.1），当前 `SESSION_TTL_MINUTES = 30` 已正确满足。

**期望修正**：将指令修改为"将 `evictExpiredSessions()` 的 `@Scheduled(fixedRate = 60000)` 改为 `fixedRate = 300000`（或 `cron = "0 */5 * * * ?"`），使清理扫描周期与 C11/OOD 要求的每 5 分钟一致"。同时移除"TTL_DURATION 从 1 改为 5"的错误描述。

### **[一般] — DraftContextCleanupTask 缺少条目时间戳机制**

计划要求按 TTL 60 分钟清理 DraftContextStore 过期条目。但 DraftContextStore 的条目值类型为 `List<DosageAlert>` 等不带时间戳的纯业务对象。清理任务无法判断条目的写入/最后更新时间，TTL 清理实际不可行。

**期望修正**：补充说明实现方式——例如使用包装对象携带 `lastWriteTime`，或使用独立时间戳 Map `ConcurrentHashMap<String, Instant>` 追踪，或在 `ConcurrentHashMapStore` 层面提供辅助方法。

### **[轻微] — ScheduledTaskConfig 与现有 SchedulingRetryConfig 重复 @EnableScheduling**

`consultation/config/SchedulingRetryConfig.java` 已标注 `@EnableScheduling`。新建 `ScheduledTaskConfig` 会重复启用；Spring 允许重复但建议任务中注明此举并非必需，避免后续维护混淆。

## 修改要求（仅 REJECTED 时）

1. **[严重]** 修正任务第 7 项 DialogueSessionManager 变更描述——从"TTL_DURATION 1→5"改为"evictExpiredSessions @Scheduled fixedRate 60000→300000（每 5 分钟）"
2. **[一般]** 补充 DraftContextCleanupTask 条目时间戳追踪的实现方式
3. **[轻微]** 可选：注明 ScheduledTaskConfig 与现有 SchedulingRetryConfig 的 @EnableScheduling 关系
