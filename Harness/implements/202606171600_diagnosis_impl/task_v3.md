# 任务指令（v3）

## 动作
NEW

## 任务描述
修改 `AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java`，移除 `@Min(0)`、`@Min(1)`、`@Max(500)` 三个 `jakarta.validation.constraints` 注解及相关 import。绕过 问题6 修复（移除 common/pom.xml 中 spring-boot-starter-validation）导致的编译阻塞。

## 选择理由
绕过方案。POM 基础设施修复任务（问题5/6/7 + 问题2补充）已连续失败 2 次（R1 FAILED → R2 FAILED），但 POM 结构本身已正确（`mvn validate` 11/11 模块全部通过）。唯一阻塞点是 PageQuery.java 编译错误——移除 validation 依赖后，`@Min`/`@Max` 注解不可解析。将这些 Controller 层职责的验证注解从 common 模块的 DTO 中移除，是符合 OOD §2.2 规范的正确方案。

## 任务上下文
- **OOD §2.2 "Common 模块依赖传播决策"**：common 模块仅保留自身骨架真正需要的 Starter——`spring-boot-starter-web (optional)` 和 `spring-boot-starter-data-jpa (optional)`。validation starter 不属于 common 的能力范围。
- **OOD §3.1 PageQuery**: `@Min(0)`、`@Max(500)` 等校验注解标注在 PageQuery 字段上，`@Valid` 对 `@ModelAttribute`（GET 查询参数绑定）和 `@RequestBody`（POST JSON 绑定）均生效，前提是 `spring-boot-starter-validation` 在类路径上（父 POM 的 `<dependencyManagement>` 中已统一声明，含 Controller 的业务模块以 `compile` scope 引入即可）。
- **实际依赖现状**：patient、doctor、admin 三个业务模块的 POM 均已独立声明 `spring-boot-starter-validation` 为 compile 依赖。这些模块的 Controller 使用 `@Valid @RequestBody` 或 `@Valid @ModelAttribute PageQuery` 时可直接触发 Hibernate Validator 完成字段校验。

## 已有代码上下文
当前 `PageQuery.java`（`common/.../result/PageQuery.java:1-42`）：
```java
package com.aimedical.common.result;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class PageQuery {

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(500)
    private int size = 20;

    // getters/setters...
}
```

操作：
1. 删除第 3-4 行 `import jakarta.validation.constraints.Max;` 和 `import jakarta.validation.constraints.Min;`
2. 删除第 10 行 `@Min(0)`
3. 删除第 13 行 `@Min(1)`
4. 删除第 14 行 `@Max(500)`
5. 保留全部字段、构造器、getter/setter 不变
