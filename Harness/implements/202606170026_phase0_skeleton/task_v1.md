# 任务指令（v1）

## 动作
NEW

## 任务描述
创建后端 Maven 父 POM + common 共享基础模块。

### 预期产出文件

**backend/pom.xml** — 父 POM，聚合 9 个子模块，管理所有依赖版本：
- parent: `spring-boot-starter-parent:3.3.0`
- groupId: `com.aimedical`, artifactId: `aimedical-sys`, version: `0.0.1-SNAPSHOT`, packaging: `pom`
- modules: common, modules/common-module/common-module-api, modules/common-module/common-module-impl, modules/ai/ai-api, modules/ai/ai-impl, modules/patient, modules/doctor, modules/admin, application, integration
- dependencyManagement 声明所有内部模块版本及外部依赖（spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-validation, spring-boot-starter-test, springdoc-openapi-starter-webmvc-ui:2.5.0, h2:2.2.224）
- maven-dependency-plugin 配置 ignoredUnusedDeclaredDependencies 豁免 ai-api 和 common-module-api

**backend/common/pom.xml** — common 模块 POM：
- parent: `aimedical-sys`
- artifactId: `common`
- dependencies: spring-boot-starter-web (optional), spring-boot-starter-data-jpa (optional), spring-boot-starter-test (test)
- 至少一个占位单元测试类

以下 Java 类型全部放在 `src/main/java/com/aimedical/common/` 下：

**base/BaseEntity.java** — 抽象 JPA 实体基类：
- `@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`
- 字段：`Long id` (`@Id` + `@GeneratedValue(strategy = IDENTITY)`), `LocalDateTime createdAt` (`@CreatedDate`), `LocalDateTime updatedAt` (`@LastModifiedDate`), `Boolean deleted` (`@Column(nullable = false)` 默认 false)
- `@SQLDelete("UPDATE {h-table} SET deleted = true WHERE id = ?")`, `@SQLRestriction("deleted = false")`
- `@PrePersist` 设置 createdAt, updatedAt；`@PreUpdate` 更新 updatedAt

**base/BaseEnum.java** — 枚举基类接口：
- 方法：`String getCode()`, `String getDesc()`

**result/Result.java** — 统一响应包装：
- 泛型类 `Result<T>`
- 字段：`String code`, `String message`, `T data`
- 静态工厂：`success(T data)`, `fail(String code, String message)`, `fail(ErrorCode errorCode)`

**result/PageQuery.java** — 分页请求：
- 字段：`int page` (`@Min(0)` 默认 0), `int size` (`@Min(1)` `@Max(500)` 默认 20), `List<String> sort`
- getter/setter

**result/PageResponse.java** — 分页响应：
- 泛型类 `PageResponse<T>`
- 字段：`List<T> content`, `long totalElements`, `int totalPages`, `int page`, `int size`
- 静态工厂：`of(List<T> content, long totalElements, int page, int size)`

**exception/ErrorCode.java** — 错误码接口：
- 方法：`String code()`, `String message()`

**exception/BusinessException.java** — 业务异常基类：
- 继承 `RuntimeException`
- 字段：`ErrorCode errorCode`, `Object[] args`
- 构造器：`(ErrorCode)`, `(ErrorCode, Object... args)`, `(ErrorCode, Throwable cause)`

**exception/GlobalErrorCode.java** — 全局错误码枚举：
- 实现 `ErrorCode` 接口
- 常量：`SUCCESS`, `SYSTEM_ERROR`, `PARAM_INVALID`, `NOT_FOUND`

**config/JpaConfig.java** — JPA 审计配置：
- `@Configuration`, `@EnableJpaAuditing`

**config/JacksonConfig.java** — Jackson 配置：
- `@Configuration`
- 配置 `PropertyNamingStrategies.SNAKE_CASE`
- 配置 JavaTimeModule

**config/GlobalExceptionHandler.java** — 全局异常处理：
- `@ControllerAdvice`
- `@ExceptionHandler(BusinessException.class)` → 400, `Result.fail(errorCode)`
- `@ExceptionHandler(MethodArgumentNotValidException.class)` → 400, `Result.fail(PARAM_INVALID)`
- `@ExceptionHandler(Exception.class)` → 500, `Result.fail(SYSTEM_ERROR)`

## 选择理由
common 模块是后端所有模块的共享基础，BaseEntity、Result、ErrorCode 等后续所有模块都依赖。父 POM 为整个后端提供构建骨架和版本统一管理。这是整个 Phase 0 的依赖根节点，必须最先完成。

## 任务上下文
出 OOD §2.1 父 POM 完整骨架示例、§2.3 包命名规范、§3.1-3.2 核心抽象设计。所有 common 模块类型位于 `com.aimedical.common` 包下。

## 已有代码上下文
项目根目录 `AIMedical/` 当前仅含 `.gitignore`，无任何现有代码。所有文件需新建。
