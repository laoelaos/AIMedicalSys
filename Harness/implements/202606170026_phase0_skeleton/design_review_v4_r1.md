# 设计审查报告（v4 r1）

## 审查结果
APPROVED

## 发现
- **[轻微]** 实体中 `Set<Role>`、`Set<Post>` 等集合字段未声明初始化（`new HashSet<>()`）。未经初始化的集合在 transient 实体上调用 `add()` 时会抛出 NPE。虽 Phase 0 为骨架且设计约定 setter 注入，但初始化是 JPA 实体标准实践，建议实现时补齐。
