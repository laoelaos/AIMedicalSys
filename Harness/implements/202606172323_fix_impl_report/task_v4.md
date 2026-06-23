# 任务指令（v4）

## 动作
NEW

## 任务描述
**T1 — PageQuery 校验注解补全**

为 `PageQuery.java` 的 `page` 和 `size` 字段添加 `jakarta.validation` 校验注解，并在 `common/pom.xml` 中补充 `spring-boot-starter-validation` 依赖（optional）。

**预期文件路径：**
- `AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java` — 添加 `@Min(0)` / `@Max(500)` 注解
- `AIMedical/backend/common/pom.xml` — 添加 `spring-boot-starter-validation` 依赖

## 选择理由
T1 优先级最高（安全 → 恶意大分页 OOM 风险）。PageQuery 位于 common 基础模块，先修复底层校验再处理上层任务（T10/T11/T6），符合"底层依赖优先"策略。改动量小、风险可控，不涉及运行时行为变更。

## 任务上下文
- OOD §3.1 明确要求 `page` 标注 `@Min(0)`、`size` 标注 `@Max(500)`
- 当前 `PageQuery.java:7-9` 仅有默认值，无校验注解
- `common/pom.xml` 当前仅声明 `spring-boot-starter-web`（optional）、`spring-boot-starter-data-jpa`（optional）、`spring-boot-starter-test`（test），不包含 `jakarta.validation-api`
- `spring-boot-starter-validation` 已由 `spring-boot-starter-parent:3.2.5` BOM 统一管理版本，无需显式指定 version
- 校验注解生效需要 Controller 方法参数同时标注 `@Valid`（或 `@Validated`）。当前骨架无分页 Controller（仅占位 ping），但修复时需确保未来所有分页接口的 Controller 参数标注 `@Valid`

## 已有代码上下文
- `PageQuery.java` 位于 `com.aimedical.common.result` 包，当前字段：
  - `private int page = 0;`（第7行）
  - `private int size = 20;`（第9行）
  - `private List<String> sort;`（第11行）
- `common/pom.xml` 第27-31行有 `spring-boot-starter-test`（test scope），需在它之前添加 `spring-boot-starter-validation`（optional）

## 操作说明

### OP-01：PageQuery 添加校验注解

**文件**：`AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java`

**变更**：
1. 在 `page` 字段添加 `@Min(0)` 注解
2. 在 `size` 字段添加 `@Max(500)` 注解
3. 在 `sort` 字段添加 `@Size` 注解（可选，OOD 无此要求但作为分页查询的排序参数有实际校验意义）

```java
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Min(0)
private int page = 0;

@Max(500)
private int size = 20;

@Size(max = 10)
private List<String> sort;
```

### OP-02：common/pom.xml 添加 validation 依赖

**文件**：`AIMedical/backend/common/pom.xml`

**变更**：在 `spring-boot-starter-data-jpa` 块之后、`spring-boot-starter-test` 块之前，插入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
    <optional>true</optional>
</dependency>
```

**理由**：`<optional>true</optional>` 避免将 validation 依赖透传到所有 consumer 模块（仅 common 内部使用 PageQuery 校验注解的场景需要）。

## 验证方式
1. `mvn compile -pl common -q` 确认编译通过，无 missing version 错误
2. `mvn test -pl common -Dtest=PageQueryTest`（如已有测试）确认测试通过
3. 无新增测试要求（纯注解添加，JDK 编译期验证即可覆盖）
