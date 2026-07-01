# 代码审查报告（v10 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `DosageThresholdService.java` — 私有方法 `matchesBothRanges`（L145-149）已定义但未被任何调用者使用。详细设计（8k）明确要求抽取该方法并用于 Loop1/Loop2 以合并共同条件，但实际代码中 5 个 loop 全部内联了独立条件，`matchesBothRanges` 成为死代码。应删除该未使用方法或将 Loop1/Loop2 的公共条件替换为 `matchesBothRanges` 调用。

## 修改要求（仅 REJECTED 时）

**问题 1 — `DosageThresholdService.java` L145-149：未使用的 `matchesBothRanges` 方法**

- **位置**：`prescription/.../service/assist/DosageThresholdService.java` L145-149
- **问题**：`matchesBothRanges` 私有方法被定义但未被任何调用方引用。设计规格（详细设计 8k）要求将该方法用于 Loop1（L111）与 Loop2（L119）以提取共同条件 `age != null && weight != null && isInRange(age, ...) && isInRange(weight, ...)`。
- **原因**：该方法实际上可简化 Loop2 为 `findFirstCandidate(candidates, ds -> matchesBothRanges(ds, age, weight))`，且 Loop1 可在此基础上附加 `&& ds.getAgeRangeStart() != null && ...`。当前代码未使用方法，造成死代码。
- **期望修正方向**：二选一：
  - 方案 A：删除 `matchesBothRanges` 方法（若设计意图已被内联实现替代），并在注释或下一步沟通中确认方法确实不需要。
  - 方案 B：将 Loop2（L119-123）改为 `findFirstCandidate(candidates, ds -> matchesBothRanges(ds, age, weight))`，将 Loop1（L111-116）改为 `findFirstCandidate(candidates, ds -> matchesBothRanges(ds, age, weight) && ds.getAgeRangeStart() != null && ds.getAgeRangeEnd() != null && ds.getWeightRangeStart() != null && ds.getWeightRangeEnd() != null)`。
