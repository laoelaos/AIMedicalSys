# 设计审查报告（v18 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** 任务要求**新增**测试方法 `login_shouldThrowLoginFailed_whenUserDeleted()`，但设计描述为**修正/补全**已有的 `login_shouldThrowUserDeleted()` 方法（L260-282）。方法名不同、意图不同（新增 vs 修改），设计未覆盖任务要求。
- **[严重]** 任务要求新增的方法名是 `login_shouldThrowLoginFailed_whenUserDeleted`，设计输出的最终方法签名却是 `login_shouldThrowUserDeleted`，方法命名不一致。

## 修改要求

1. 澄清任务范围：究竟是已有方法需修正，还是需新增独立测试方法。若已有 `login_shouldThrowUserDeleted` 方法存在，则任务描述需更新为修正；若需新增，则设计应产出新方法而非修改既有方法。
2. 确保方法命名与任务指令一致。
