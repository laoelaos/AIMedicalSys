# 详细设计（v8）

## 概述

修复 v7 验证失败的唯一问题：`shouldBeAnnotatedWithConditionalOnProperty:42` 中 `assertEquals(String, String[])` 类型不匹配。将 `assertEquals` 改为 `assertArrayEquals`。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `ai/ai-impl/src/test/java/.../mock/MockAiServiceTest.java` | 修改 | 第42行断言类型修正 |

## 类型定义

无新增类型。仅修改测试方法中一行断言。

## 修改明细

### MockAiServiceTest.java（第42行）

**当前（v7 错误）：**
```java
assertEquals("ai.mock.enabled", annotation.name());
```

**改为：**
```java
assertArrayEquals(new String[]{"ai.mock.enabled"}, annotation.name());
```

**理由：** `@ConditionalOnProperty.name()` 返回 `String[]`，`assertEquals(Object, Object)` 无法正确比较数组，实际运行时因类型擦除编译通过但断言语义错误。错误输出 `expected: <ai.mock.enabled> but was: <[ai.mock.enabled]>` 印证了值类型不匹配。

## 错误处理

无运行时错误处理变化。

## 行为契约

- `shouldBeAnnotatedWithConditionalOnProperty` 其余三个断言（`assertNotNull`、`assertEquals("true", annotation.havingValue())`、`assertFalse(annotation.matchIfMissing())`）保持不变
- 测试文件的其余 18 个测试方法不受影响

## 依赖关系

无变更。

## 修订说明（v8 R1）

| 审查意见 | 修改措施 |
|---------|---------|
| R7 验证失败：`MockAiServiceTest.shouldBeAnnotatedWithConditionalOnProperty:42 — expected: <ai.mock.enabled> but was: <[ai.mock.enabled]>`，`@ConditionalOnProperty.name()` 返回 `String[]`，`assertEquals` 无法比较数组 | 将 `assertEquals` 改为 `assertArrayEquals(new String[]{"ai.mock.enabled"}, annotation.name())` |
