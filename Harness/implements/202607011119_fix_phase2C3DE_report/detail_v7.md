# 详细设计（v7）

## 概述

修复 R6 验证失败的唯一测试：`MockAiServiceTest.shouldBeAnnotatedWithProfile` 仍断言 `@Profile` 存在，但 `MockAiService.java` 已改为 `@ConditionalOnProperty`。本设计仅涉及该测试文件的修改，其余 v6 已通过的部分保持不变。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `ai/ai-impl/src/test/java/.../mock/MockAiServiceTest.java` | 修改 | 替换过时 Profile 测试为 ConditionalOnProperty 测试 |

## 类型定义

无新增类型。仅修改测试类 `MockAiServiceTest` 的 import 和一个测试方法。

## 修改明细

### MockAiServiceTest.java

**包路径**：`com.aimedical.modules.ai.impl.mock`

#### 1. 删除失效 import（第4行）
```java
// 删除整行：
import org.springframework.context.annotation.Profile;
```
（该 import 仅被 `shouldBeAnnotatedWithProfile` 方法引用，方法整体替换后无其他依赖）

#### 2. 新增 import
```java
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
```

#### 3. 替换测试方法（第38-43行）
将 `shouldBeAnnotatedWithProfile()` 整体替换为：

```java
@Test
void shouldBeAnnotatedWithConditionalOnProperty() {
    ConditionalOnProperty annotation = MockAiService.class.getAnnotation(ConditionalOnProperty.class);
    assertNotNull(annotation);
    assertEquals("ai.mock.enabled", annotation.name());
    assertEquals("true", annotation.havingValue());
    assertFalse(annotation.matchIfMissing());
}
```

**方法计数变化**：18 原有 + 1 新 = 19（验证标准要求）

## 错误处理

无运行时错误处理变化。测试方法仅执行反射读取注解 + 断言，不会抛出受检异常。

## 行为契约

- `shouldBeAnnotatedWithConditionalOnProperty` 验证 `@ConditionalOnProperty` 三个属性值与 `MockAiService` 类上声明一致
- 测试类构造方式：`new MockAiService("STATIC")`（第31行），不受本修改影响
- 其余 18 个测试方法不受影响

## 依赖关系

- 移除对 `org.springframework.context.annotation.Profile` 的编译期依赖（测试类）
- 新增对 `org.springframework.boot.autoconfigure.condition.ConditionalOnProperty` 的编译期依赖（测试类）

## 修订说明（v7 R1）

| 审查意见 | 修改措施 |
|---------|---------|
| V6 验证失败：`MockAiServiceTest.shouldBeAnnotatedWithProfile:41 expected: not <null>` — `@Profile` 已被 `@ConditionalOnProperty` 替代，测试未同步更新 | 新增本设计，将 `shouldBeAnnotatedWithProfile` 替换为 `shouldBeAnnotatedWithConditionalOnProperty`，删除 `Profile` import，新增 `ConditionalOnProperty` import |
