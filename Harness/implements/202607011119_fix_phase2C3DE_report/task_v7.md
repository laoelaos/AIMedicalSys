# 任务指令（v7）

## 动作
RETRY

## 任务描述
修复 R6 验证失败的唯一测试：`MockAiServiceTest.shouldBeAnnotatedWithProfile`（第39-43行）因 A05 修改将 `@Profile("mock")` 替换为 `@ConditionalOnProperty` 后，原断言 `assertNotNull(profile)` 失败（profile 为 null）。

## 失败原因
验证结果：Tests run: 65, Failures: 1 — MockAiServiceTest.shouldBeAnnotatedWithProfile:41 expected: not <null>

根本原因：R6 代码已正确修改 MockAiService.java（`@Profile("mock")` → `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = false)`），但测试文件未同步更新，仍断言 `@Profile` 存在。

**文件**：`AIMedical/backend/modules/ai/ai-impl/src/test/java/com/aimedical/modules/ai/impl/mock/MockAiServiceTest.java`

## 具体修改

### 1. 删除失效 import
```java
// 删除：
import org.springframework.context.annotation.Profile;
```
（仅在本文件有唯一引用，无其他依赖可整行删除）

### 2. 新增 import
```java
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
```

### 3. 替换测试方法
将 `shouldBeAnnotatedWithProfile()` 方法（第38-43行）替换为：
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

## 验证标准
1. 单独运行 `MockAiServiceTest` 全部 19 用例通过（修改后第41行 `assertArrayEquals(new String[]{"mock"}, profile.value())` 不再编译，修改前 18 用例 + 新方法 = 19）
2. `mvn test -pl modules/ai/ai-impl -am` 全部通过（包括 ai-api + ai-impl 合计 201 用例，0 失败）
3. `mvn test` 全量回归通过（确保之前 R1-R5 的修改不受影响，且后续 consultation/prescription/medical-record 模块的 R6 修改编译及测试通过）
