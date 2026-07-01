# 测试审查报告（v1 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** `ObjectMapperJavaTimeModuleEdgeCaseTest.java:92-95` — `shouldThrowWhenDeserializingNullToken` 将失败：`objectMapper.readValue("null", LocalDateTime.class)` 在 Jackson 默认行为下**不会抛异常**（JSON null token 对引用类型返回 `null`）。`assertThrows` 会因未捕捉到异常而失败，导致该测试红色（failed）。

- **[一般]** `ObjectMapperJavaTimeModuleEdgeCaseTest.java:22-24` — `shouldHaveJavaTimeModuleOnClasspath` 测试冗余：`JavaTimeModule` 已在文件顶部 `import`（第 5 行），编译本身就要求 classpath 中必须存在该 JAR，运行时 `Class.forName` 不可能失败。该测试不提供有意义的验证。

- **[轻微]** `ObjectMapperJavaTimeModuleEdgeCaseTest.java:45-50` — `shouldNotSerializeLocalDateTimeAsArrayWhenConfiguredCorrectly` 断言偏弱：仅检查 `startsWith("\"")` 和 `!startsWith("[")`，若输出格式变为其他非数组形式（如 `"2026-06-30T10:00:00.000"` 等变体）仍会被错误放过。应直接断言精确的 ISO-8601 字符串值。

## 修改要求（仅 REJECTED 时）

1. `ObjectMapperJavaTimeModuleEdgeCaseTest.java:92-95`
   - **问题**：`shouldThrowWhenDeserializingNullToken` 方法调用 `mapper.readValue("null", LocalDateTime.class)`，Jackson 对 JSON null token 读取引用类型时返回 `null` 不抛异常，导致 `assertThrows` 永远失败。
   - **修正方向**：方案一：将测试改为验证 `readValue("null", LocalDateTime.class)` 返回 `null`（`assertNull`），因为这本身就是正确的 null 处理行为；方案二：若设计意图是验证异常路径，可改为输入非法 null 字符串（如 `"nulla"`）或启用 `FAIL_ON_NULL_FOR_PRIMITIVES` 之类特性，但 `LocalDateTime` 作为引用类型不适于此场景。推荐方案一：改为 `assertNull` 测试。

2. `ObjectMapperJavaTimeModuleEdgeCaseTest.java:22-24`
   - **问题**：`shouldHaveJavaTimeModuleOnClasspath` 测试因 import 导致编译期已验证 classpath，运行时测试无额外价值。
   - **修正方向**：删除该测试方法，或用更有意义的验证替代（例如通过反射确认 `JavaTimeModule` 可被实例化并正常注册到 `ObjectMapper`）。但删除是最简洁的做法。
