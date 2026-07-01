# 详细设计（v1）

## 概述
修复 R29 虚标 PASSED 的测试基础设施问题——`ObjectMapperJavaTimeModuleTest` 在 consultation 模块中测试失败，根因为 `jackson-datatype-jsr310` 依赖未在 `consultation/pom.xml` 中显式声明（仅靠传递依赖），导致 `JavaTimeModule` 注册后未实际生效，`LocalDateTime` 退化为数组形式。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/consultation/pom.xml` | 修改 | 添加 `jackson-datatype-jsr310` 依赖（scope: compile），版本由 Spring Boot parent POM 3.2.5 管理 |
| `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/ObjectMapperJavaTimeModuleTest.java` | 修改 | 所有 3 个测试方法中，在 `registerModule(new JavaTimeModule())` 后追加 `objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)`，确保 `LocalDateTime` 序列化为 ISO-8601 字符串格式 |

## 类型定义

### 无新增类型

仅修改已有文件，不新增任何类、接口、枚举。

## 错误处理
不涉及——本任务仅修复测试基础设施配置，无业务逻辑错误处理变更。测试失败时 Maven Surefire 报告标准错误信息。

## 行为契约

### consultation/pom.xml 变更
- **依赖坐标**：`com.fasterxml.jackson.datatype:jackson-datatype-jsr310`
- **scope**：`compile`（默认）
- **版本**：由 Spring Boot 3.2.5 parent POM 的 `jackson-bom` 管理，不需要显式 `<version>`
- **插入位置**：在 `spring-boot-starter-web` 依赖之后（与 Jackson 系列依赖相邻）
- **生效方式**：在 consultation 模块 classpath 中显式包含 `jackson-datatype-jsr310` JAR，确保 `ObjectMapper.registerModule(new JavaTimeModule())` 实际生效

### ObjectMapperJavaTimeModuleTest.java 变更
- **import 追加**：`import com.fasterxml.jackson.databind.SerializationFeature;`
- **所有 3 个测试方法**在 `objectMapper.registerModule(new JavaTimeModule())` 之后追加：
  ```java
  objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  ```
- **预期行为**：
  - `shouldSerializeLocalDateTimeToIso8601`：`objectMapper.writeValueAsString(LocalDateTime.of(2026, 6, 30, 10, 0, 0))` 返回 `"\"2026-06-30T10:00:00\""`
  - `shouldSerializeRegistrationEventWithLocalDateTimeWithoutException`：序列化 `RegistrationEvent` 时 `eventTime` 字段输出为 ISO-8601 字符串 `"2026-06-30T10:00:00"`，而非数组 `[2026,6,30,10,0]`
  - `shouldDeserializeIso8601ToLocalDateTime`：ISO-8601 字符串反序列化为 `LocalDateTime` 正常通过
- **不修改**：测试逻辑、断言值、测试类结构、包名

## 依赖关系
- **新增依赖**：`com.fasterxml.jackson.datatype:jackson-datatype-jsr310`，scope `compile`
- **依赖管理器**：Spring Boot 3.2.5 parent POM（`spring-boot-starter-parent`）通过 `jackson-bom` 管理版本
- **无传递依赖变更**：其他模块不受影响
- **暴露接口**：无——仅内部测试基础设施修复
