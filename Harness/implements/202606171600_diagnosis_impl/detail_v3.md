# 详细设计（v3）

## 概述
移除 AIMedical/backend/common 模块 PageQuery.java 中依赖 `spring-boot-starter-validation` 的 `jakarta.validation.constraints` 注解（`@Min`、`@Max`），消除因 common/pom.xml 不包含 validation starter 导致的编译阻塞。该验证职责已由依赖 validation starter 的业务模块（patient、doctor、admin）的 Controller 层承担。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java` | 修改 | 移除 `@Min(0)`、`@Min(1)`、`@Max(500)` 注解及对应 import 语句 |

## 类型定义

### PageQuery
**形态**：class
**包路径**：`com.aimedical.common.result`
**当前结构**：
```java
package com.aimedical.common.result;

import jakarta.validation.constraints.Max;   // ← 删除
import jakarta.validation.constraints.Min;   // ← 删除

import java.util.List;

public class PageQuery {

    @Min(0)                                  // ← 删除
    private int page = 0;

    @Min(1)                                  // ← 删除
    @Max(500)                                // ← 删除
    private int size = 20;

    private List<String> sort;

    // getter/setter...
}
```

**修改后结构**：
```java
package com.aimedical.common.result;

import java.util.List;

public class PageQuery {

    private int page = 0;

    private int size = 20;

    private List<String> sort;

    // getter/setter (保持不变)
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public List<String> getSort() { return sort; }
    public void setSort(List<String> sort) { this.sort = sort; }
}
```

**构造方式**：默认无参构造器 + setter
**类型关系**：无继承/实现关系

## 错误处理
本次变更不涉及运行时错误处理逻辑。编译期错误消除后，common 模块可正常通过 `mvn compile`。

## 行为契约
- **前置条件**：`PageQuery.java` 中 `@Min`、`@Max` 注解对应的验证逻辑由业务模块 Controller 层通过 `@Valid` + `spring-boot-starter-validation` 完成
- **后置条件**：`common` 模块不再包含 `jakarta.validation` 依赖，编译无错误
- **无运行时行为变化**：字段 `page`、`size` 默认值保持不变（page=0, size=20）

## 依赖关系
- 依赖的已有类型：`java.util.List`（保持不变）
- 移除的依赖：`jakarta.validation.constraints.Min`、`jakarta.validation.constraints.Max`
- 项目已有代码约束：OOD §2.2 规定 common 模块不包含 validation starter；业务模块 patient、doctor、admin 的 POM 均已声明 `spring-boot-starter-validation` 为 compile 依赖
