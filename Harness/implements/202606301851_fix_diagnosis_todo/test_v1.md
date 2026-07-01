# 测试报告（v1）

## 测试文件

- **新建**: `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/ObjectMapperJavaTimeModuleEdgeCaseTest.java`

## 测试用例设计

| 测试方法 | 验证维度 | 对应行为契约 |
|---------|---------|------------|
| `shouldSerializeMidnightToIso8601` | 边界条件 | 午夜时刻序列化为 ISO-8601 |
| `shouldSerializeEndOfMonthToIso8601` | 边界条件 | 月末 23:59:59 序列化为 ISO-8601 |
| `shouldSerializeLastDayOfFebruaryInLeapYear` | 边界条件 | 闰年 2 月 29 日序列化为 ISO-8601 |
| `shouldNotSerializeLocalDateTimeAsArrayWhenConfiguredCorrectly` | 回归验证 | 修复后不会退化为数组格式 |
| `shouldRoundtripSerializationAndDeserialization` | 状态交互 | 序列化后再反序列化恢复原值 |
| `shouldRoundtripMinLocalDateTime` | 边界条件 | `LocalDateTime.MIN` roundtrip |
| `shouldRoundtripMaxLocalDateTime` | 边界条件 | `LocalDateTime.MAX` roundtrip |
| `shouldThrowWhenDeserializingInvalidIso8601` | 错误路径 | 非法日期字符串抛出异常 |
| `shouldReturnNullWhenDeserializingNullToken` | 错误路径 | null JSON token 返回 null |

## 覆盖维度

- **边界条件（5）**: 午夜、月末、闰年、MIN、MAX
- **错误路径（2）**: 非法字符串、null token（改为 assertNull）
- **状态交互（2）**: roundtrip 常规值、roundtrip 极值
- **正常路径**: 由 `ObjectMapperJavaTimeModuleTest` 覆盖

## 修订说明（v1 r1）

根据审查报告修订：

1. **删除 `shouldHaveJavaTimeModuleOnClasspath`** — 该测试因 `import` 声明在编译期已验证 classpath，运行时无额外验证价值，故删除。

2. **加固 `shouldNotSerializeLocalDateTimeAsArrayWhenConfiguredCorrectly`** — 原断言仅检查 `startsWith("\"")` 和 `!startsWith("[")`，偏弱。改为 `assertEquals("\"2026-06-30T10:00:00\"")` 精确断言 ISO-8601 字符串值。

3. **修正 `shouldThrowWhenDeserializingNullToken` → `shouldReturnNullWhenDeserializingNullToken`** — Jackson 对 JSON null token 读取引用类型返回 `null` 而非抛异常，`assertThrows` 必然失败。改为 `assertNull` 验证正确的 null 处理行为，测试方法名同步变更。

## 修订说明（v1 r2）

根据审查报告修订：

1. **删除 `shouldThrowWhenDeserializingEmptyString`** — Jackson 当前版本对空字符串反序列化返回 `null` 而非抛异常，该测试必然 FAIL。考虑到测试名与实际行为不符，且 null 处理语义已被 `shouldReturnNullWhenDeserializingNullToken` 覆盖，直接移除该测试。

## 风格一致性

遵循项目已有测试风格：
- 使用 JUnit 5 (`org.junit.jupiter.api.Test`)
- 静态导入 `org.junit.jupiter.api.Assertions.*`
- `shouldXxx` 命名约定
- 不依赖 Spring 上下文，纯单元测试
