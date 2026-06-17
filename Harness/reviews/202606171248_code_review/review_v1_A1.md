# R1: 后端 Common 模块一致性审查

审查时间：2026-06-17

### 审查范围

| # | 文件 | 设计对照 |
|---|------|---------|
| 1 | `common/.../result/Result.java` | §3.1 Result<T> |
| 2 | `common/.../result/PageQuery.java` | §3.1 PageQuery |
| 3 | `common/.../result/PageResponse.java` | §3.1 PageResponse<T> |
| 4 | `common/.../exception/ErrorCode.java` | §3.1 ErrorCode |
| 5 | `common/.../exception/GlobalErrorCode.java` | §3.1 |
| 6 | `common/.../exception/BusinessException.java` | §3.1 |
| 7 | `common/.../config/GlobalExceptionHandler.java` | §3.1 GlobalExceptionHandler |
| 8 | `common/.../base/BaseEntity.java` | §3.2 BaseEntity |
| 9 | `common/.../base/BaseEnum.java` | 检查是否在设计中定义 |
| 10 | `common/.../config/JpaConfig.java` | §3.2 JpaConfig |
| 11 | `common/.../config/JacksonConfig.java` | 设计对照 |
| 12 | `common/src/test/java/com/aimedical/common/**/*.java` | 测试覆盖扫描 |

### 发现

#### [一般] `BaseEnum` 为设计文档未定义类型

- **位置**：`common/.../base/BaseEnum.java:1-6`
- **描述**：`BaseEnum` interface 存在于 `common.base` 包中，符合 §2.1 目录布局规划（`base/` 含 `BaseEntity, BaseEnum`），但 §3.x 核心抽象规范中**未对该类型做任何定义或说明**。缺少字段/方法命名约定、使用规范或与 `ErrorCode` 接口的关系说明。
- **建议**：在 OOD 设计文档 §3.x 中补充 `BaseEnum` 的规范定义，说明其适用场景（如数据库 enum 映射、DTO 枚举转换），或从目录布局中移除该引用。

#### [轻微] `GlobalExceptionHandler` 缺少序列化异常专用处理器

- **位置**：`common/.../config/GlobalExceptionHandler.java:30-35`
- **描述**：设计文档 §5.3 明确要求 `HttpMessageNotReadableException` / `HttpMessageNotWritableException` 在 `GlobalExceptionHandler` 中注册 `@ExceptionHandler` 方法，避免 Spring 默认错误响应格式污染统一响应契约。当前实现中这两类异常仅由兜底 `Exception` handler 以 500 捕获，无法返回 400 状态码和格式化的 `Result` 响应。
- **建议**：新增以下 `@ExceptionHandler` 方法：
  ```java
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Result<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
      return ResponseEntity.badRequest()
              .body(Result.fail(GlobalErrorCode.PARAM_INVALID));
  }
  ```
  同理处理 `HttpMessageNotWritableException`（返回 500）。

#### [轻微] `BusinessException` 未按 `ErrorCode` 区分 HTTP 状态码

- **位置**：`common/.../config/GlobalExceptionHandler.java:18-22`
- **描述**：设计文档 §5.1 规定 BusinessException 应根据错误类型返回 400（参数校验/业务逻辑）或 404（`NOT_FOUND`）或 409（数据冲突）。当前实现对所有 BusinessException 统一返回 400，未按 `ErrorCode` 类型区分状态码。
- **建议**：在 `GlobalExceptionHandler.handleBusinessException` 中增加状态码判断逻辑，例如对 `NOT_FOUND` 返回 404：
  ```java
  HttpStatus status = e.getErrorCode() == GlobalErrorCode.NOT_FOUND
      ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
  return ResponseEntity.status(status).body(Result.fail(e.getErrorCode()));
  ```
  或由 `ErrorCode` 接口增加 `httpStatus()` 默认方法。

#### [轻微] `JacksonConfig` 存在未使用的 import

- **位置**：`common/.../config/JacksonConfig.java:4`
- **描述**：`import org.springframework.boot.jackson.JsonComponent;` 在类中未被使用。
- **建议**：移除该未使用的 import。

### 测试覆盖评估

| 源文件 | 测试文件 | 覆盖内容 | 评价 |
|--------|---------|---------|------|
| `Result.java` | `ResultTest.java` | success/fail 工厂方法、ErrorCode 重载、getter/setter | 充分 |
| `PageQuery.java` | `PageQueryTest.java` | 默认值、边界值、sort List 设置 | 充分 |
| `PageResponse.java` | `PageResponseTest.java` | 空分页、totalPages 计算、边界条件、setter | 充分 |
| `ErrorCode.java` | 无独立测试 | 通过 GlobalErrorCodeTest 间接覆盖 | 可接受 |
| `GlobalErrorCode.java` | `GlobalErrorCodeTest.java` | 全部 4 个枚举常量的 code/message | 充分 |
| `BusinessException.java` | `BusinessExceptionTest.java` | 3 种构造器、RuntimeException 继承 | 充分 |
| `GlobalExceptionHandler.java` | `GlobalExceptionHandlerTest.java` | 三种 handler 的 HTTP 状态码 + Result 结构 | 充分 |
| `BaseEntity.java` | `BaseEntityTest.java` | 默认值、getter/setter | 充分 |
| `BaseEnum.java` | `BaseEnumTest.java` | 枚举 code/desc 获取 | 充分 |
| `JpaConfig.java` | `JpaConfigTest.java` | 注解存在性验证 | 充分 |
| `JacksonConfig.java` | `JacksonConfigTest.java` | snake_case 命名、JavaTimeModule 注册、序列化 | 充分 |
| 全局占位 | `CommonPlaceholderTest.java` | 空 contextLoads | 占位 |

**测试评价**：所有源文件均有对应的测试文件，覆盖了核心创建逻辑、边界值和 getter/setter。未发现明显测试缺口。`CommonPlaceholderTest` 为合理的 Phase 0 最小占位。

### 一致性对照明细

| 文件 | 设计§ | 一致性 | 说明 |
|------|-------|--------|------|
| `Result.java` | §3.1 | ✅ | 泛型 class，code/data/message 字段，工厂方法齐全 |
| `PageQuery.java` | §3.1 | ✅ | page @Min(0) 0-based, size 默认20 @Max(500), sort List<String> |
| `PageResponse.java` | §3.1 | ✅ | content/totalElements/totalPages/page/size, totalPages 计算正确 |
| `ErrorCode.java` | §3.1 | ✅ | interface, code()/message()方法签名一致 |
| `GlobalErrorCode.java` | §3.1 | ✅ | enum implements ErrorCode, 4 个自有常量 |
| `BusinessException.java` | §3.1 | ✅ | extends RuntimeException, 持有 ErrorCode, 三种构造器齐全 |
| `GlobalExceptionHandler.java` | §3.1 | ⚠️ | @ControllerAdvice + @ExceptionHandler 机制正确；缺少序列化异常处理器，BusinessException 不区分 HTTP 状态码 |
| `BaseEntity.java` | §3.2 | ✅ | abstract @MappedSuperclass, Long IDENTITY id, @CreatedDate/@LastModifiedDate, Boolean deleted, @SQLDelete/@SQLRestriction 完全符合 |
| `BaseEnum.java` | — | ⚠️ | §2.1 目录布局提及但 §3.x 无规范定义 |
| `JpaConfig.java` | §3.2 | ✅ | @EnableJpaAuditing 在 common.config 包 |
| `JacksonConfig.java` | §8.2 | ✅ | SNAKE_CASE 命名策略 + JavaTimeModule |

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 1 |
| 轻微 | 3 |

### 总评

后端 Common 模块核心类型的实现与 OOD 设计文档（`Docs/04_ood_phase0.md`）高度一致。`Result<T>`、`PageQuery`/`PageResponse<T>`、`ErrorCode`/`GlobalErrorCode`、`BusinessException`、`BaseEntity`、`JpaConfig` 均严格按设计实现，未发现结构偏离或逻辑错误。

主要发现：
1. **`BaseEnum`** 存在于目录布局约定中但缺乏设计说明，需在设计文档中补全规范（一般）。
2. **`GlobalExceptionHandler`** 缺少设计文档明确要求的 `HttpMessageNotReadableException`/`HttpMessageNotWritableException` 专用处理器，且未按 ErrorCode 区分 HTTP 状态码（轻微）。
3. **`JacksonConfig`** 存在一个未使用的 import（轻微）。

测试覆盖充分，11 个测试文件覆盖了所有生产代码的核心行为。
