# 详细设计（v4）

## 概述

为 `PageQuery.java` 的 `page`/`size`/`sort` 字段添加 `jakarta.validation` 校验注解，并在 `common/pom.xml` 中补充 `spring-boot-starter-validation` 依赖（optional），对齐 OOD §3.1 校验要求，防止恶意大分页 OOM 风险。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java` | 修改 | 添加 @Min/@Max/@Size 校验注解及对应 import |
| `AIMedical/backend/common/pom.xml` | 修改 | 添加 spring-boot-starter-validation optional 依赖 |

## 类型定义

### PageQuery（修改）
**形态**：class
**包路径**：`com.aimedical.common.result`
**职责**：分页查询参数校验

**字段变更**：
| 字段 | 当前 | 变更后 |
|------|------|--------|
| `page` | `private int page = 0;` | `@Min(0) private int page = 0;` |
| `size` | `private int size = 20;` | `@Max(500) private int size = 20;` |
| `sort` | `private List<String> sort;` | `@Size(max = 10) private List<String> sort;` |

**新增 import**：
```java
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
```

**方法签名**：保持不变（getter/setter 无变更）

## 错误处理

校验失败时由 Spring 的 `MethodArgumentNotValidException` 或 `ConstraintViolationException` 统一处理，当前骨架无全局异常处理器，但本变更不引入新的错误处理逻辑。未来 Controller 需确保分页参数标注 `@Valid`。

## 行为契约

1. `page` 值域：`[0, Integer.MAX_VALUE)`，注解约束 `@Min(0)`
2. `size` 值域：`[1, 500]`，注解约束 `@Max(500)`（OOD 未要求 `@Min(1)`，保持 int 默认值 0 可由上层校验）
3. `sort` 元素最大数量约束：`@Size(max = 10)`（防止排序字段数量过大）
4. 所有变更仅为编译期注解添加，不影响运行时二进制兼容性

## 依赖关系

- 新增依赖：`spring-boot-starter-validation`（optional）
- 版本管理：由 `spring-boot-starter-parent:3.2.5` BOM 统一管理，无需显式 version
- 可选标记：`<optional>true</optional>` 避免透传到 consumer 模块
- 插入位置：`spring-boot-starter-data-jpa` 之后、`spring-boot-starter-test` 之前

## 验证方式

1. `mvn compile -pl common -q` 确认编译通过
2. 无新增测试要求（纯注解添加，JDK 编译期验证即可覆盖）
