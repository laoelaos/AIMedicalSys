# 任务指令（v1）

## 动作
NEW

## 任务描述
修复 R29 虚标 PASSED 的测试基础设施问题——`ObjectMapperJavaTimeModuleTest` 在 consultation 模块中测试失败，根因为 `jackson-datatype-jsr310` 依赖未在 `consultation/pom.xml` 中显式声明（仅靠传递依赖），导致 `JavaTimeModule` 注册后未实际生效，`LocalDateTime` 退化为数组形式。

### 需要实现：
1. **consultation/pom.xml**：添加 `jackson-datatype-jsr310` 依赖（scope: compile）
2. **ObjectMapperJavaTimeModuleTest.java**：在 `ObjectMapper` 配置中调用 `disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)`，确保 `LocalDateTime` 序列化为 ISO-8601 格式（`"2026-06-30T10:00:00"`）

### 涉及文件：
- `AIMedical/backend/modules/consultation/pom.xml`
- `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/ObjectMapperJavaTimeModuleTest.java`

## 选择理由
R29 验收轮次虚标 PASSED 的核心阻塞项——`mvn clean test` 全量构建中 consultation 模块 2 个测试失败。此问题的修复是后续所有轮次验证的基础（后续每轮需要 `mvn clean test` 验证），必须先解决。

## 任务上下文
- consultation/pom.xml 当前有 spring-boot-starter-web、spring-boot-starter-data-jpa 等依赖，但无 `jackson-datatype-jsr310` 显式声明
- ObjectMapperJavaTimeModuleTest 中测试 `shouldSerializeLocalDateTimeToIso8601` 期望 `"2026-06-30T10:00:00"` 但得到 `[2026,6,30,10,0]`
- JavaTimeModule 的注册是必要的，但需要配合 `disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)` 才能正确输出 ISO-8601

## 已有代码上下文
- `consultation/pom.xml` 继承自 `aimedical-sys` parent POM（version 0.0.1-SNAPSHOT）
- `ObjectMapperJavaTimeModuleTest` 包含 3 个测试方法：`shouldSerializeLocalDateTimeToIso8601`、`shouldSerializeRegistrationEventWithLocalDateTimeWithoutException`、`shouldDeserializeIso8601ToLocalDateTime`
- 测试使用 `new ObjectMapper()` 独立实例化（非 Spring 管理），在测试中 `registerModule(new JavaTimeModule())` 但未配置 `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS`
