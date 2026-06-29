# 测试审查报告（v15 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `UserConverterTest.java:78-96` — `toUserInfoResponse_shouldMapPositionFromFirstPost` 使用 `HashSet<Post>` 存储 posts，但 `HashSet` 不保证迭代顺序。实现中通过 `posts.stream().findFirst()` 取第一个元素，测试断言固定为 `"OUTPATIENT"`。由于 `HashSet` 迭代顺序非确定（依赖于对象 hashCode 及 JVM 实现），该断言在不同 JVM 环境下可能失败，导致测试不可靠。

## 修改要求（仅 REJECTED 时）

### 问题 1: UserConverterTest 岗位排序测试依赖非确定顺序

**位置**: `UserConverterTest.java:78-96`（`toUserInfoResponse_shouldMapPositionFromFirstPost`）

**问题**: `HashSet<Post>` 不保证迭代顺序，但测试依赖 `findFirst()` 返回特定元素 `"OUTPATIENT"`。这使测试成为潜在的不稳定测试（flaky test）。

**为什么是问题**: 根据 Java `HashSet` 规范，迭代顺序不保证一致；默认 `Object.hashCode()` 在不同 JVM 实例或 Java 版本下可能产生不同哈希值，导致迭代顺序变化，从而使本测试在环境变更时失效。

**期望修正方向**: 将 `HashSet` 替换为 `LinkedHashSet` 以保持插入顺序，确保 `findFirst()` 总是返回第一个插入的 `Post("OUTPATIENT")`。或者构造仅含一个 Post 的集合，避免顺序歧义。
