# 计划审查报告（v10 r3）

## 审查结果
REJECTED

## 发现

### **[严重] T16 (P1, prescription) 完全未覆盖**

T16（PrescriptionDraftContext.hasCriticalAlerts 与 getCriticalAlerts 缺少原子操作，TOCTOU 窗口）是诊断报告确认为 P1 级别的 prescription 模块缺陷。8m（T27）的 per-prescriptionId 锁仅在 `submit()` 内部消除 TOCTOU 窗口，但以下场景仍存在并发风险：

- 线程 A 调用 `submit()` → `hasCriticalAlerts()` 返回 false
- 线程 B 调用 `assist()` → `updateCriticalAlerts()` 写入 CRITICAL alerts
- 线程 A 继续执行 → `getCriticalAlerts()` 获取到刚写入的 alerts，但 `snapshotCriticalAlerts` 已是空的

该 issue 未被列入当前 round 8（prescription 批量）的 8a-8m 子项清单，也未出现在 task_v10.md 中 round 9 的暂缓清单内（该清单列了 T5、T8 等但遗漏 T16）。**必须补充 T16 的修复方案或显式推迟并说明理由。**

### **[一般] 8l (P14) exceptionally 回调与 submit() 的并发冲突未分析**

8l 在 `scheduleSuggestionAsync` 的 `exceptionally` 回调中追加 `clearCriticalAlerts(request.getPrescriptionId())`。exceptionally 在 `aiTaskExecutor` 线程中执行，而 `submit()` 在主线程中称为 `hasCriticalAlerts()` / `getCriticalAlerts()`。虽然 DraftContextStore 基于 ConcurrentHashMap 在数据结构层面安全，但 `exceptionally` 回调清除 alerts 与 `submit()` 读取 alerts 之间的时序窗口可能导致 `submit()` 基于过时状态决策。需在 plan 中说明此并发风险已被接受或给出缓解策略。

### **[一般] Round 9 项清单遗漏 A11 (P2)**

task_v10.md 第 358 行列出的 round 9 暂缓清单（「含 P09、T8、T18、T19、T20、T21、T22、T47、T48、T50、M02、M11、A06、A09、A08、T24、T5 等」）未包含 A11（防御性检查过度，P2）。建议补充。

### **[一般] 8a T9 实施说明缺少 Logger 字段声明**

DosageThresholdService 当前没有 `private static final Logger log` 字段。task_v10.md 8a 变更部分仅给了 `log.warn(...)` 调用示例，未提及同时需添加 Logger 字段和 `import org.slf4j.Logger`/`LoggerFactory`。虽属常识但仍建议在变更明细中显式标注。

### **[轻微] Plan round 编号与路线表序号不一致**

plan.md 将此轮标注为 R10，但路线表序号为第 8 项。此偏差来自 R7/R8/R9 的 A05 retry 轮次插入，虽不影响执行但增加了跟踪混淆。建议在路线表或 plan 标题处加注对应关系。

## 修改要求

1. **[严重] T16**：在 8a-8m 子项清单中补充 T16 修复方案，或在「P09、T8 归属说明」节中显式推迟并写明理由。推荐方案：将 `hasCriticalAlerts()` 与 `getCriticalAlerts()` 合并为单一原子方法（如 `snapshotCriticalAlerts()` 同时返回是否存在 + 列表快照），或明确声明 8m 的 per-prescriptionId 锁已覆盖 T16 并说明剩余风险被接受的理由。
2. **[一般] 8l**：补充 exceptionally 回调与 submit() 路径的并发时序分析，说明是否引入新风险及如何缓解。
3. **[一般] Round 9 清单**：补充 A11 到暂缓清单中。
4. **[一般] 8a**：在 DosageThresholdService 变更明细中补充 Logger 字段和 import 声明。
