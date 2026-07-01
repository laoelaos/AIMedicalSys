# 设计审查报告（v3 r3）

## 审查结果
APPROVED

## 发现

### 审查结论
本详细设计 v3 已完整覆盖 task_v3.md 的全部任务要求，包括：
- **S01**: `SuggestionStore` 新增 `createIfNotExists()` 方法，语义正确（`putIfAbsent`）
- **S03**: `DedupTaskScheduler.schedule()` 采用 `get + createIfNotExists + compute(兜底)` 混合策略，所有跨 key `put` 均在 lambda 外执行，消除原原子性问题
- **S06**: 全程使用 `instanceof` 模式匹配类型安全转型，兜底 `IllegalStateException` 处理极端非预期类型
- **S07**: `ConcurrentHashMapStore` 移除 `DraftContextStore`，新建独立 `DraftContextStoreImpl` 带独立 `ConcurrentHashMap` 实例
- **SuggestionCleanupTask**: `SuggestionStoreEntry` 提升为独立接口，`AiSuggestionResult` 实现该接口，方法名 `getStatus()` → `getStatusName()` 消除签名冲突

### 关键设计质量判断
1. **状态机完整性**: 状态判定矩阵覆盖了全部 6 种状态（absent/PENDING/COMPLETED+unconsumed/COMPLETED+consumed/FAILED/非 AiSuggestionResult），每种状态的路径均正确
2. **并发正确性**: `createIfNotExists` 保证至多一个线程为新 key 创建成功；`compute` 兜底时原子重读消除 TOCTOU 竞态；Step 6 引用相等性判定正确区分替换成功与复用
3. **上一轮审查意见均已修正**:
   - [严重] COMPLETED+consumed/FAILED 行为错误 → 已通过 `get + createIfNotExists + compute(兜底)` 混合策略修复
   - [轻微] `DraftContextStoreImpl` Spring Bean 注册未明确 → 已补充 `@Service` 注解
4. **边界处理**: null 守卫（`instanceof` 含 null 检查）、空列表（`LocalDateTime → Instant` 转换含 null 保护）、`NullPointerException`（`ConcurrentHashMap.putIfAbsent` 原生行为）
