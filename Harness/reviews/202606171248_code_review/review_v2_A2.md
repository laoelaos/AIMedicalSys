# R2: 测试覆盖分析 — 全模块单元测试 + 集成测试

审查时间：2026-06-17

### 审查范围

共 41 个测试文件，覆盖 Common、ai-api、ai-impl、common-module-api、common-module-impl、patient、doctor、admin、application、integration 十个模块。

### 发现

#### [一般] GlobalExceptionHandler 缺少 HttpMessageNotReadableException / HttpMessageNotWritableException 专用处理器

- **位置**：`AIMedical/backend/common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java:30-35`
- **描述**：设计文档 §5.3 明确要求注册 `@ExceptionHandler` 方法处理 `HttpMessageNotReadableException`（请求体序列化错误 → 400）和 `HttpMessageNotWritableException`（响应体序列化错误 → 500），但当前实现仅通过通用的 `@ExceptionHandler(Exception.class)` 兜底捕获，实际会返回 500 `SYSTEM_ERROR` 而非正确的 400/500 区分。当前行为与设计约定部分偏离。
- **建议**：在 GlobalExceptionHandler 中增加两个专用 `@ExceptionHandler` 方法，分别返回 400 和 500 状态码，并对应补充单元测试。

#### [一般] FallbackAiServiceTest 未验证空委托列表的日志输出

- **位置**：`AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/fallback/FallbackAiServiceTest.java:35-42`
- **描述**：`shouldReturnFallbackResultWhenNoDelegateAvailable()` 正确验证了空委托列表场景下返回 `degraded=true, success=false, fallbackReason="No available AiService delegate"`，但设计文档 §3.4 还要求启动期输出 `ERROR` 日志、运行期输出 `WARN` 日志。该行为未在测试中验证。
- **建议**：使用日志附加器（如 Logback's `ListAppender` 或配置测试 LoggerAppender）验证日志输出内容。

#### [轻微] PageQueryTest 未验证校验注解的编译期存在性

- **位置**：`AIMedical/backend/common/src/test/java/com/aimedical/common/result/PageQueryTest.java:13-17`
- **描述**：设计文档 §3.1 要求 `page` 标注 `@Min(0)`、`size` 标注 `@Max(500)` 且上限为 500。测试覆盖了 setSize(1) 和 setSize(500) 的边界赋值正确性，但未通过反射验证这两个注解在 `PageQuery` 字段上实际存在。
- **建议**：添加注解存在性测试（如 `assertNotNull(PageQuery.class.getDeclaredField("page").getAnnotation(Min.class))`），防止后续重构误删校验注解。

#### [轻微] BaseEntityTest 未验证 JPA 核心注解的存在性

- **位置**：`AIMedical/backend/common/src/test/java/com/aimedical/common/base/BaseEntityTest.java:15-21`
- **描述**：测试覆盖了字段默认值和 setter/getter，但未验证 `@MappedSuperclass`、`@Id`、`@GeneratedValue`、`@CreatedDate`、`@LastModifiedDate`、`@SQLDelete`、`@SQLRestriction` 等 JPA 注解是否存在于 `BaseEntity` 上（设计 §3.2）。这些注解的缺失会导致 JPA 元模型加载失败。
- **建议**：添加反射注解存在性验证，确保 JPA 映射骨架完整。

#### [轻微] SecurityConfigPhase0 无单元测试

- **位置**：`AIMedical/backend/application/src/main/java/com/aimedical/config/SecurityConfigPhase0.java`
- **描述**：该占位配置类（`@Profile("phase0") permitAll` 放通）无对应的单元测试。由于集成测试 `ApplicationContextIT`（`@SpringBootTest`，默认使用 `phase0,dev` profile）已验证上下文可加载，且 `HealthCheckIT` 隐式验证了 `/api/ping` 的可达性（前提是 permitAll 生效），因此该缺失不影响骨架验收，但未来 Phase 1 切换认证策略后需补齐。
- **建议**：Phase 1 补充 security 配置的专项测试。

### 验收项检查

| 验收项目 | 状态 | 说明 |
|---------|------|------|
| HealthCheckIT 测试 `GET /api/ping`，期望 `Result.success("pong")` | ✅ 通过 | `AIMedical/backend/integration/src/test/java/com/aimedical/integration/HealthCheckIT.java:20-24` 验证 200 OK、SUCCESS 码、"pong" 数据 |
| ApplicationContextIT 验证 Spring 上下文加载 | ✅ 通过 | `AIMedical/backend/integration/src/test/java/com/aimedical/integration/ApplicationContextIT.java:10` `@SpringBootTest` + `contextLoads()` |
| FallbackAiServiceTest 空委托列表场景 | ✅ 通过 | `AIMedical/backend/ai-impl/src/test/java/com/aimedical/modules/ai/impl/fallback/FallbackAiServiceTest.java:35-42` 验证 degraded=true、success=false、fallbackReason 符合设计 §3.4 |

### 覆盖完整性总结

- **Common 模块**（11 个生产类 ↔ 11 个测试类）：全覆盖。`Result`、`PageQuery`、`PageResponse`、`BusinessException`、`GlobalErrorCode`、`GlobalExceptionHandler`、`BaseEntity`、`BaseEnum`、`JpaConfig`、`JacksonConfig` 均有对应测试。
- **ai-api 模块**（31 个生产类 ↔ 3 个测试类）：`AiResult` 和 `AiService` 接口契约全覆盖；`DegradationStrategy` / `DegradationContext` 接口与上下文覆盖；13 组 DTO 中仅 `TriageDto` 组（含 `@phase0-mock-field` 注解字段）有字段级测试，其余 12 组空壳 DTO 无独立测试——符合设计 §3.4/8.2 的两层冻结策略。
- **ai-impl 模块**（3 个生产类 ↔ 3 个测试类）：全覆盖。`MockAiService`（13 方法）、`FallbackAiService`（5 场景）、`NoOpDegradationStrategy` 均有测试。
- **common-module-api 模块**（1 个生产类 ↔ 1 个测试类）：全覆盖。
- **common-module-impl 模块**（5 个生产类 ↔ 5 个测试类）：全覆盖。`User`、`Role`、`Post`、`Function` 实体 + `UserRepository`。
- **Patient/Doctor/Admin 模块**（各 7 个生产类 ↔ 各 4 个测试类）：Controller/ServiceImpl/Entity 有测试；`PatientService`/`DoctorService`/`AdminService` 接口、`*Repository`、`*Dto`、`*Converter` 等占位类无独立测试——对于 Phase 0 骨架可接受。
- **Application 模块**（3 个生产类 ↔ 2 个测试类）：`HealthController` 有 3 个断言；`Application` 启动类由集成测试间接覆盖；`SecurityConfigPhase0` 无独立测试，由集成测试隐式覆盖。
- **Integration 模块**（2 个测试类）：上下文加载 + 健康检查端点，覆盖 §4.1 契约。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 2 |
| 轻微 | 3 |

### 总评

测试覆盖整体良好。41 个测试文件覆盖了所有 11 个后端模块的核心契约与骨架占位。Common 模块的核心抽象（Result、PageQuery、PageResponse、BusinessException、GlobalErrorCode、GlobalExceptionHandler）均有较细致的测试，边界场景覆盖合理。ai-api/ai-impl 的 AI 能力模块测试完备，FallbackAiService 的空委托列表降级场景正确实现并验证。集成测试准确覆盖了 §4.1 的健康检查契约。发现问题均为一般/轻微级别，不影响 Phase 0 骨架验收，建议在后续阶段针对日志验证、注解存在性检查和序列化异常处理器差异项补充覆盖。
