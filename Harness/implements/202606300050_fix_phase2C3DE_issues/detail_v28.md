# 详细设计（v28）

## 概述
修复 `RegistrationEventListenerTest` 中 `ObjectMapper` 缺少 JSR310 模块导致 `LocalDateTime` 序列化异常而使测试 `shouldContainAllSevenFieldsInEventPayloadOnRecover` 失败的问题。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/RegistrationEventListenerTest.java` | 修改 | 在 `setUp()` 中为 `objectMapper` 注册 `JavaTimeModule` |

## 类型定义
无新增类型。仅修改测试类中 `setUp()` 方法的 `ObjectMapper` 配置。

## 修改点

### 文件：RegistrationEventListenerTest.java

**变更 1 — 新增 import**
- 添加 `import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;`

**变更 2 — setUp() 方法 L47**
- 原：`objectMapper = new ObjectMapper();`
- 改：
  ```java
  objectMapper = new ObjectMapper();
  objectMapper.registerModule(new JavaTimeModule());
  ```

## 错误处理
无变更。`JavaTimeModule` 已在 `common` 模块 classpath 可用，注册不会引入新异常。

## 行为契约
- `setUp()` 后 `objectMapper` 能正常序列化 `LocalDateTime` 为 ISO-8601 字符串
- `writeValueAsString(event)` 不再抛出 `JsonProcessingException`，`recover()` 中不再进入回退路径

## 依赖关系
- `com.fasterxml.jackson.datatype.jsr310.JavaTimeModule` — 来自 `common` 模块的传递依赖
