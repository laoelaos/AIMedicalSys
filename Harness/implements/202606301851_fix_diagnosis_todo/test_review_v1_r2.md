# 测试审查报告（v1 r2）

## 审查结果
REJECTED

## 发现

- **[严重]** `ObjectMapperJavaTimeModuleEdgeCaseTest.java:78-82` — `shouldThrowWhenDeserializingEmptyString` 测试预期 `mapper.readValue("\"\"", LocalDateTime.class)` 抛出 `Exception`，但实际 Jackson 的 `LocalDateTimeDeserializer` 对空字符串返回 `null` 而非抛异常。该测试运行时必然 FAIL，使测试套件无法通过。

## 修改要求

- **文件**: `ObjectMapperJavaTimeModuleEdgeCaseTest.java` 第 78-82 行（`shouldThrowWhenDeserializingEmptyString` 方法）
- **问题**: `assertThrows(Exception.class, () -> mapper.readValue("\"\"", LocalDateTime.class))` 在 Jackson 当前版本下不抛出异常（空字符串被处理为 null）。
- **原因**: 该测试预期与 Jackson 实际行为不符，导致测试失败。与 `shouldReturnNullWhenDeserializingNullToken` 类似，Jackson 的 JSR310 反序列化器静默处理空输入。
- **修正方向**: 有两个合理选项：
  1. 改为 `assertNull(mapper.readValue("\"\"", LocalDateTime.class))` 匹配 Jackson 实际行为（与 null token 处理逻辑一致）；
  2. 如认为空字符串不应被静默容忍，则移除该测试（实际行为与初衷不符，测试无法验证设计意图）。
