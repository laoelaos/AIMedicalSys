# 详细设计（v1）

## 概述

为 Phase 0 后端创建 Maven 父 POM 及 common 共享基础模块。父 POM 聚合所有 10 个子模块并统一管理依赖版本；common 模块提供 JPA 实体基类、统一响应包装、错误码体系、分页契约、JPA 审计配置、Jackson 配置及全局异常处理等所有模块共享的基础类型。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| backend/pom.xml | 新建 | Maven 多模块聚合父 POM，聚合 10 个子模块，管理所有内部模块版本及外部依赖版本，配置 maven-dependency-plugin 豁免 ai-api 和 common-module-api |
| backend/common/pom.xml | 新建 | common 模块 POM，以 optional 方式依赖 spring-boot-starter-web 和 spring-boot-starter-data-jpa，以 test 方式依赖 spring-boot-starter-test |
| backend/common/src/main/java/com/aimedical/common/base/BaseEntity.java | 新建 | JPA 实体基类（抽象类），提供 id、createdAt、updatedAt、deleted 字段及软删除注解 |
| backend/common/src/main/java/com/aimedical/common/base/BaseEnum.java | 新建 | 枚举基类接口，定义 getCode() / getDesc() 契约 |
| backend/common/src/main/java/com/aimedical/common/result/Result.java | 新建 | 统一响应包装泛型类，静态工厂 success/fail |
| backend/common/src/main/java/com/aimedical/common/result/PageQuery.java | 新建 | 分页请求参数类，含校验注解 |
| backend/common/src/main/java/com/aimedical/common/result/PageResponse.java | 新建 | 分页响应泛型类，含静态工厂 of |
| backend/common/src/main/java/com/aimedical/common/exception/ErrorCode.java | 新建 | 错误码接口，定义 code() / message() |
| backend/common/src/main/java/com/aimedical/common/exception/BusinessException.java | 新建 | 业务异常基类，继承 RuntimeException，持有 ErrorCode |
| backend/common/src/main/java/com/aimedical/common/exception/GlobalErrorCode.java | 新建 | 全局错误码枚举，实现 ErrorCode |
| backend/common/src/main/java/com/aimedical/common/config/JpaConfig.java | 新建 | JPA 审计配置类 |
| backend/common/src/main/java/com/aimedical/common/config/JacksonConfig.java | 新建 | Jackson 配置类（snake_case + JavaTimeModule） |
| backend/common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java | 新建 | 全局异常处理器，将 BusinessException / MethodArgumentNotValidException / Exception 统一转换为 Result |
| backend/common/src/test/java/com/aimedical/common/CommonPlaceholderTest.java | 新建 | common 模块占位单元测试类 |

## 类型定义

### BaseEntity
**形态**：abstract class
**包路径**：com.aimedical.common.base
**职责**：所有 JPA 实体的基类，提供自增主键、审计时间戳、逻辑删除标记
**类型签名**：
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SQLDelete("UPDATE {h-table} SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean deleted = false;
}
```
**公开接口**：
- `Long getId()` / `void setId(Long)` getter/setter
- `LocalDateTime getCreatedAt()` / `void setCreatedAt(LocalDateTime)` getter/setter
- `LocalDateTime getUpdatedAt()` / `void setUpdatedAt(LocalDateTime)` getter/setter
- `Boolean getDeleted()` / `void setDeleted(Boolean)` getter/setter
**构造方式**：默认无参构造器（JPA 规范要求）
**类型关系**：被所有业务实体继承

### BaseEnum
**形态**：interface
**包路径**：com.aimedical.common.base
**职责**：所有枚举类型的基类接口，统一 code/desc 契约
**类型签名**：
```java
public interface BaseEnum {
    String getCode();
    String getDesc();
}
```
**公开接口**：`String getCode()`, `String getDesc()`
**类型关系**：被各业务枚举实现

### Result<T>
**形态**：generic class
**包路径**：com.aimedical.common.result
**职责**：统一 API 响应包装，携带状态码、消息、数据载荷
**类型签名**：
```java
public class Result<T> {
    private String code;
    private String message;
    private T data;
}
```
**公开接口**：
- `static <T> Result<T> success(T data)` — 构造成功响应（code="SUCCESS", data=data）
- `static <T> Result<T> fail(String code, String message)` — 构造失败响应
- `static <T> Result<T> fail(ErrorCode errorCode)` — 通过 ErrorCode 构造失败响应
- `String getCode()` / `void setCode(String)` getter/setter
- `String getMessage()` / `void setMessage(String)` getter/setter
- `T getData()` / `void setData(T)` getter/setter
**构造方式**：默认无参构造器 + 静态工厂方法

### PageQuery
**形态**：class
**包路径**：com.aimedical.common.result
**职责**：分页请求参数，封装 page、size、sort
**类型签名**：
```java
public class PageQuery {
    @Min(0)
    private int page = 0;

    @Min(1) @Max(500)
    private int size = 20;

    private List<String> sort;
}
```
**公开接口**：所有字段的 getter/setter
**构造方式**：默认无参构造器

### PageResponse<T>
**形态**：generic class
**包路径**：com.aimedical.common.result
**职责**：统一分页响应格式
**类型签名**：
```java
public class PageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
```
**公开接口**：
- `static <T> PageResponse<T> of(List<T> content, long totalElements, int page, int size)` — 工厂方法，自动计算 totalPages
- 所有字段的 getter/setter
**构造方式**：默认无参构造器 + 静态工厂方法

### ErrorCode
**形态**：interface
**包路径**：com.aimedical.common.exception
**职责**：错误码命名空间契约
**类型签名**：
```java
public interface ErrorCode {
    String code();
    String message();
}
```
**公开接口**：`String code()`, `String message()`

### BusinessException
**形态**：class
**包路径**：com.aimedical.common.exception
**职责**：业务异常基类，继承 RuntimeException，持有 ErrorCode
**类型签名**：
```java
public class BusinessException extends RuntimeException {
    private ErrorCode errorCode;
    private Object[] args;
}
```
**公开接口**：
- `BusinessException(ErrorCode errorCode)` — 仅指定错误码
- `BusinessException(ErrorCode errorCode, Object... args)` — 指定错误码及动态参数
- `BusinessException(ErrorCode errorCode, Throwable cause)` — 指定错误码及原始异常
- `ErrorCode getErrorCode()` getter
- `Object[] getArgs()` getter
**类型关系**：继承 RuntimeException

### GlobalErrorCode
**形态**：enum（implements ErrorCode）
**包路径**：com.aimedical.common.exception
**职责**：全局通用错误码枚举
**类型签名**：
```java
public enum GlobalErrorCode implements ErrorCode {
    SUCCESS("SUCCESS", "成功"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常"),
    PARAM_INVALID("PARAM_INVALID", "参数校验失败"),
    NOT_FOUND("NOT_FOUND", "资源不存在");

    private final String code;
    private final String message;

    @Override
    public String code() { return code; }

    @Override
    public String message() { return message; }
}
```
**公开接口**：实现 ErrorCode 的 code() / message() 方法
**构造方式**：枚举常量，构造器参数为 (String code, String message)

### JpaConfig
**形态**：class
**包路径**：com.aimedical.common.config
**职责**：启用 JPA 审计功能
**类型签名**：
```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```
**构造方式**：Spring 自动扫描注册为 Bean

### JacksonConfig
**形态**：class
**包路径**：com.aimedical.common.config
**职责**：配置 Jackson 全局属性（snake_case 命名策略 + JavaTimeModule 支持）
**类型签名**：
```java
@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> builder
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .modules(new JavaTimeModule());
    }
}
```
**构造方式**：Spring 自动扫描注册为 Bean

### GlobalExceptionHandler
**形态**：class
**包路径**：com.aimedical.common.config
**职责**：全局异常处理，将所有 Controller 异常统一转换为 Result 响应
**类型签名**：
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e);
}
```
**行为契约**：
- BusinessException → HTTP 400 + `Result.fail(errorCode)`
- MethodArgumentNotValidException → HTTP 400 + `Result.fail(PARAM_INVALID)`
- Exception → HTTP 500 + `Result.fail(SYSTEM_ERROR)`
- 所有异常栈信息不暴露给前端
- 系统异常需记日志

### CommonPlaceholderTest
**形态**：class
**包路径**（测试）：com.aimedical.common
**职责**：common 模块占位单元测试类，验证 Spring 上下文或简单断言通过
**类型签名**：
```java
class CommonPlaceholderTest {
    void contextLoads();
}
```

## 错误处理

- BusinessException 继承 RuntimeException，Spring 事务管理默认回滚
- 全局异常处理器将三类异常统一转换为 Result<Void> 返回，不暴露异常栈给前端
- 系统异常（Exception 兜底）需记录完整堆栈到服务端日志
- ErrorCode 接口 + 各模块 enum 实现的模式：common 模块提供 GlobalErrorCode 枚举作为全局通用错误码，各业务模块后续可自行实现 ErrorCode 接口扩展业务错误码

## 行为契约

1. **构建顺序**：父 POM 必须先于 common 模块构建；common 模块作为所有其他模块的共享基础
2. **依赖传播**：spring-boot-starter-web 和 spring-boot-starter-data-jpa 在 common 中标记为 optional，避免传递给 common-module-api 和 ai-api 等纯契约模块
3. **依赖分析门禁**：父 POM 的 maven-dependency-plugin 配置 `<ignoredUnusedDeclaredDependencies>` 豁免 ai-api 和 common-module-api，避免 Phase 0 业务模块声明但暂未引用的 api 模块依赖被误报
4. **Jackson 命名**：全局 snake_case 确保所有 DTO 序列化时字段名统一为下划线格式

## 依赖关系

### 父 POM（backend/pom.xml）
- **依赖的内部模块**（dependencyManagement）：common, common-module-api, common-module-impl, ai-api, ai-impl, application（version = ${project.version}）
- **依赖的外部 Starter**（dependencyManagement）：spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-validation, spring-boot-starter-test
- **依赖的外部库**（dependencyManagement）：springdoc-openapi-starter-webmvc-ui:2.5.0, h2:2.2.224
- **插件**：maven-dependency-plugin（配置 ignoredUnusedDeclaredDependencies: ai-api, common-module-api）

### Common 模块（backend/common/pom.xml）
- **父 POM**：com.aimedical:aimedical-sys:0.0.1-SNAPSHOT
- **artifact**: common
- **依赖**：
  - spring-boot-starter-web（compile, optional）— 为 @ControllerAdvice、Result 序列化提供编译支持
  - spring-boot-starter-data-jpa（compile, optional）— 为 BaseEntity JPA 注解、JpaConfig 审计配置提供编译支持
  - spring-boot-starter-test（test）— 单元测试

### Java 类型内部依赖
- `BusinessException` → 依赖 `ErrorCode`
- `GlobalErrorCode` → 实现 `ErrorCode`
- `GlobalExceptionHandler` → 依赖 `Result`、`BusinessException`、`GlobalErrorCode`
- `BaseEntity` → 无内部依赖（仅依赖 spring-boot-starter-data-jpa 的 JPA 注解）
- `Result<T>` → 依赖 `ErrorCode`
- `PageQuery`、`PageResponse<T>` → 无内部依赖

## 修订说明（v1 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| BaseEntity 同时使用了 `@PrePersist`/`@PreUpdate` 和 `@CreatedDate`/`@LastModifiedDate` + `AuditingEntityListener` 两套机制，重复且矛盾，需二择一 | 删除 `@PrePersist`/`@PreUpdate` 方法声明，保留 `@CreatedDate`/`@LastModifiedDate` + `AuditingEntityListener` 注解式审计，符合 Spring 惯用做法 |
