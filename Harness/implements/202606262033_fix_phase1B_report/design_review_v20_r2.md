# 设计审查报告（v20 r2）

## 审查结果
REJECTED

## 发现

- **[严重]** — 测试 3 与任务规格不符。`task_v20.md` 要求实现 `buildMenuTree_shouldSupportThreeLevelHierarchy`（三级嵌套），但设计将其替换为 `buildMenuTree_shouldNestMultipleChildrenUnderParent`（二级多子）。"已知限制"章节虽记录了 `MenuResponse` 不可变 record 导致三级嵌套不可行的技术原因，但设计并未履行任务要求。正确的做法是：(a) 保留三级嵌套测试（即使预期失败，用于显式记录已知缺陷），或 (b) 先更新任务规格为 v21 再相应修改设计。当前设计擅自变更测试范围，不符合任务指令。

## 修改要求

1. **测试 3 履行任务要求**：将 `buildMenuTree_shouldNestMultipleChildrenUnderParent` 恢复为三级嵌套测试 `buildMenuTree_shouldSupportThreeLevelHierarchy`，或先更新 `task_v20.md` 为 v21 以反映测试替换决策。
