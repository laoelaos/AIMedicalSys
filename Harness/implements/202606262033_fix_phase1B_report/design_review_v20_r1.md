# 设计审查报告（v20 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** — 测试 3 `buildMenuTree_shouldSupportThreeLevelHierarchy`（三级嵌套）在当前生产代码下无法通过。
  `MenuResponse` 为不可变 record（`withChildren()` 返回新实例而非原地修改）。`MenuServiceImpl.buildMenuTree()`（第 195–230 行）中 `idToMenu.put(parent.id(), parent.withChildren(newChildren))` 将父条目替换为新的 record 实例。对于三级链 1→2→3：
  1. 处理节点 2 时：`idToMenu[1]` 被更新为 `MenuResponse(1, children=[MenuResponse(2, children=null)])`，内部引用的节点 2 是**旧快照**（children=null）
  2. 处理节点 3 时：`idToMenu[2]` 被更新为 `MenuResponse(2, children=[MenuResponse(3)])`，但 `idToMenu[1]` 的 children 仍指向旧节点 2（children=null）
  3. 最终结果：`menus.get(0).children().get(0).children()` 返回 null，断言 `...get(0).name() == "三级菜单"` 抛出 NPE，三级菜单被静默丢失
  问题根因在 `buildMenuTree` 的实现，而非测试设计规格。设计需明确处理此前置依赖——要么在任务范围内修复 `buildMenuTree`，要么将三级嵌套测试降级为二级嵌套，并在依赖关系或附录中记录限制。

## 修改要求

1. **测试 3（三级嵌套）**：在 `detail_v20.md` 的"依赖关系"或新增"已知限制"章节中显式说明 `buildMenuTree` 当前不支持三级嵌套，并选择以下之一：
   - (a) 将测试 3 改为二级嵌套，将三级嵌套写为待办（需先修复 `buildMenuTree`）
   - (b) 保留三级嵌套测试，但明确将修复 `buildMenuTree` 列为此任务的**前置条件**
