# R1: common 模块骨架实现与 OOD 设计一致性审查

审查时间：2026-06-17

### 审查范围

- `AIMedical/backend/common/src/main/java/com/aimedical/common/base/BaseEntity.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/base/BaseEnum.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/result/Result.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageResponse.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/exception/ErrorCode.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/exception/GlobalErrorCode.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/exception/BusinessException.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/config/JacksonConfig.java`
- `AIMedical/backend/common/src/main/java/com/aimedical/common/config/JpaConfig.java`
- `AIMedical/backend/common/pom.xml`
- Test files under `src/test/java/com/aimedical/common/`

审查依据：Docs/04_ood_phase0.md §2.2、§2.3、§3.1、§3.2

### 发现

#### [一般] PageQuery 缺少 @Min(0) / @Max(500) 校验注解

- **位置**：`src/main/java/com/aimedical/common/result/PageQuery.java:7-9`
- **描述**：OOD §3.1 明确要求 `page` 标注 `@Min(0)`、`size` 标注 `@Max(500)`（上限防止恶意大分页 OOM），当前代码的 `page=0` 和 `size=20` 仅有默认值而无校验注解。同时 `@Valid` 需在 Controller 参数侧使用以触发校验。
- **建议**：在 `page` 字段添加 `@Min(0)`，在 `size` 字段添加 `@Max(500)` 注解（需在 pom.xml 添加 spring-boot-starter-validation 依赖或确认父 POM 已管理）。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 1 |
| 轻微 | 0 |

### 总评

common 模块骨架实现整体与 OOD phase0 设计高度一致。BaseEntity（@MappedSuperclass + 审计 + 软删除）、BaseEnum、Result<T>（泛型 + success/fail 工厂）、PageResponse、ErrorCode 接口、GlobalErrorCode enum、BusinessException（多构造器覆盖 ErrorCode/args/cause）、GlobalExceptionHandler（5种异常处理 + 日志）、JacksonConfig（SNAKE_CASE + JavaTimeModule）、JpaConfig（@EnableJpaAuditing）均正确实现 OOD 设计契约。common/pom.xml 依赖声明（spring-boot-starter-web/data-jpa 均 optional）符合 §2.2 依赖传播策略。测试覆盖完整（BaseEntity、BaseEnum、Result、PageQuery、PageResponse、BusinessException、GlobalErrorCode、GlobalExceptionHandler、JacksonConfig、JpaConfig、POM 结构等均有对应测试类，内容充实）。唯一不足是 PageQuery 缺少字段级校验注解，建议在后续迭代补充。
