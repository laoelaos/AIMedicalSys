# R3: 代码质量深度审查 + 潜在问题 + 最佳实践

审查时间：2026-06-17

### 审查范围

- `AIMedical/backend/common/src/main/java/com/aimedical/common/result/Result.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageResponse.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/exception/BusinessException.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/base/BaseEntity.java`
- `AIMedical/backend/modules/common-module/common-module-impl/.../permission/{User,Role,Post,Function}.java`
- `AIMedical/backend/modules/ai/ai-impl/.../mock/MockAiService.java`
- `AIMedical/backend/modules/ai/ai-impl/.../fallback/FallbackAiService.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/config/JacksonConfig.java`
- `AIMedical/backend/application/src/main/resources/application.yml`
- `AIMedical/backend/application/src/main/resources/application-dev.yml`
- `AIMedical/backend/application/src/main/resources/application-prod.yml`
- `AIMedical/backend/application/src/main/java/com/aimedical/config/SecurityConfigPhase0.java`
- `AIMedical/frontend/packages/shared/src/api/index.ts`
- `AIMedical/frontend/packages/shared/src/types/index.ts`
- `AIMedical/frontend/apps/{patient,doctor,admin}/src/App.vue`
- `AIMedical/backend/common/src/test/java/.../result/PageQueryTest.java`
- `AIMedical/backend/common/src/test/java/.../base/BaseEntityTest.java`
- `AIMedical/backend/common/src/test/java/.../config/GlobalExceptionHandlerTest.java`
- `AIMedical/backend/modules/ai/ai-impl/src/test/.../fallback/FallbackAiServiceTest.java`
- `AIMedical/backend/modules/ai/ai-api/src/test/.../dto/triage/TriageDtoTest.java`

### 发现

#### [一般] FallbackAiService ERROR 日志触发时机与 OOD §3.4 不一致

- **位置**：`AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/fallback/FallbackAiService.java:60-67`
- **描述**：OOD §3.4 要求"启动期输出 ERROR 日志、运行期输出 WARN 日志"，但当前实现仅在首次调用时触发 ERROR（`handleEmptyDelegates` 在方法调用时执行），而非在构造器/启动期检查。构造器（第 52 行）仅执行 delegate 过滤，未在无可用委托时立即输出 ERROR 日志。该偏差已由测试注释标注为 K3 已知问题。
- **建议**：在 `FallbackAiService` 构造器中检查 `delegates.isEmpty()`，若为空则立即输出 ERROR 日志，消除首次调用与后续调用的日志级别差异。

#### [一般] PageQuery 缺少 `@Min` / `@Max` 校验注解

- **位置**：`AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:7-9`
- **描述**：OOD §3.1 明确要求 `page` 字段标注 `@Min(0)`、`size` 字段标注 `@Max(500)`（上限 500 防止恶意大分页 OOM），但当前 PageQuery 未添加任何 Bean Validation 注解。虽然 PageQueryTest 测试了边界值 1 和 500 的 setter 赋值，但缺少 `@Valid` 触发时的自动校验屏障。
- **建议**：在 `page` 字段添加 `@Min(0)`，`size` 字段添加 `@Min(1) @Max(500)`。

#### [一般] BaseEntityTest 未验证审计字段自动填充

- **位置**：`AIMedical/backend/common/src/test/java/com/aimedical/common/base/BaseEntityTest.java`
- **描述**：OOD §3.2 明确 `createdAt` 由 `@CreatedDate` + `AuditingEntityListener` 自动填充，`updatedAt` 由 `@LastModifiedDate` 自动填充。但当前测试仅验证了 setter/getter 的 POJO 行为，未在 Spring Data JPA 上下文中验证审计监听器是否按预期自动填充时间戳。虽然该测试为纯单元测试（无 Spring 上下文），但审计字段自动填充是 BaseEntity 的核心契约，应引入 `@DataJpaTest` 验证。
- **建议**：新增 `@DataJpaTest` 集成测试，创建实体并验证 `createdAt` / `updatedAt` 在 `save` 后被自动赋值。

#### [轻微] JacksonConfig 缺少 CLOSE_CLOSEABLE 禁用配置

- **位置**：`AIMedical/backend/common/src/main/java/com/aimedical/common/config/JacksonConfig.java:14-16`
- **描述**：审查要点要求 ObjectMapper 配置包含"禁用手动关闭"（`MapperFeature.ALLOW_EXPLICIT_JSON_ROOT_OR_CLOSE_CLOSEABLE` 或 `SerializationFeature.CLOSE_CLOSEABLE`），但当前 JacksonConfig 仅配置了 `SNAKE_CASE` 命名策略和 `JavaTimeModule`，未显式禁用 Closeable 资源的自动关闭行为。虽然 Spring Boot 默认已禁用 `SerializationFeature.CLOSE_CLOSEABLE`（通过 `SpringBootObjectMapper`），但显式声明可提高代码可读性和防御性。
- **建议**：在 customizer 链中添加 `.featuresToDisable(SerializationFeature.CLOSE_CLOSEABLE)`。

#### [轻微] BusinessException 异常处理器缺少日志记录

- **位置**：`AIMedical/backend/common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java:20-24`
- **描述**：`handleBusinessException` 方法直接返回 `Result.fail` 而未输出任何日志（包括 DEBUG/TRACE 级别）。虽然 OOD §5.3 仅要求系统异常记录完整堆栈，但 `BusinessException` 是系统重要事件，缺少日志将导致线上问题排查困难（无法追溯触发异常的业务上下文和参数）。
- **建议**：在 `handleBusinessException` 中添加 `log.warn("Business exception: code={}, message={}", e.getErrorCode().code(), e.getMessage())`。

### 已确认通过项（未发现问题）

- **Result.java**：`success()` / `fail()` static 泛型方法类型参数传递正确（`<T> Result<T>`）。
- **PageResponse.java**：无参构造器存在，getter/setter 完整，Jackson 序列化正常。
- **BusinessException.java**：三种构造器形式完全覆盖（ErrorCode, ErrorCode+args, ErrorCode+cause），继承 RuntimeException 正确。
- **BaseEntity.java**：`@GeneratedValue(strategy = IDENTITY)` 与 §3.2 一致；`@SQLDelete` 使用 Hibernate 6 `{h-table}` 语法；`@SQLRestriction("deleted = false")` 正确替代 Hibernate 5 的 `@Where`；`@Column(nullable = false)` 在 `deleted` 字段存在。
- **User/Role/Post/Function 实体关系**：`@ManyToMany`/`@ManyToOne`/`@OneToMany` 均正确配置 `name`/`mappedBy`/`fetch = FetchType.LAZY`；`@JoinTable`/`@JoinColumn` 命名与 §3.3 约定一致；两端均未设 cascade。
- **MockAiService**：实现全部 13 个方法，签名与 AiService 接口完全一致；triage 方法按 mock 数据规则填充（集合字段 List.of(dept)，字符串字段 `"mock_" + 字段名`）。
- **GlobalExceptionHandler**：`@ExceptionHandler` 覆盖所有 OOD §5.1 列出的异常类型（BusinessException, MethodArgumentNotValidException, HttpMessageNotReadableException, HttpMessageNotWritableException, Exception 兜底）；日志记录完整（序列化异常 ERROR、系统异常 ERROR、请求体格式异常 WARN）。
- **application.yml profiles.active**：值为 `phase0,dev`，与 §9.1 完全一致。
- **SecurityConfigPhase0 @Profile**：正确标注 `@Profile("phase0")` 与 application.yml 匹配。
- **MockAiService @ConditionalOnProperty**：配置 `havingValue = "true", matchIfMissing = true` 与 OOD §3.4 一致。
- **Axios 拦截器**：NETWORK_ERROR 响应包含 `code` 和 `message` 字段；错误拦截器使用 `Promise.resolve` 返回统一格式。
- **TypeScript 类型定义**：所有类型使用 `type` 或 `interface`，无 class 定义。
- **三端 App.vue**：正确使用 `<template>` + `<script setup lang="ts">` + `<style scoped>` 语法。
- **PageQueryTest**：测试了 size 最小边界（1）和最大边界（500）。
- **GlobalExceptionHandlerTest**：覆盖 BusinessException、MethodArgumentNotValidException、HttpMessageNotReadableException、HttpMessageNotWritableException、Exception 五种类型。
- **FallbackAiServiceTest**：测试了空委托列表时的降级返回值（无委托空降级场景）。
- **TriageDtoTest**：测试了 TriageRequest/TriageResponse/RecommendedDepartment 的 DTO 字段填充。
- **无通配符 import**：检查的 Java 文件中均使用显式单类 import。
- **无 public 字段**：所有类使用 private + getter/setter（DTO 类同理）。
- **文件末尾空行**：检查的所有文件末尾均有空行。
- **日志记录使用 SLF4J**：GlobalExceptionHandler 和 FallbackAiService 均使用 `LoggerFactory.getLogger`。其余 POJO 类无需日志记录。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 3 |
| 轻微 | 2 |

### 总评

后端核心骨架代码整体质量良好，实体关系映射、异常处理体系、前端类型定义和测试覆盖均符合 OOD 设计约定。存在 3 个一般性问题（FallbackAiService 日志触发时机不符合 §3.4 规范、PageQuery 缺少 `@Min`/`@Max` 校验注解、BaseEntity 审计字段自动填充缺少集成测试验证）和 2 个轻微改进点（Jackson 显式禁用 Closeable、BusinessException 增加日志追溯），修复优先级较低但在进入 Phase 1 前建议补齐。
