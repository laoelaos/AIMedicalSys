# 详细设计（v20）

## 概述

在 `ai-impl` 模块的 `pom.xml` 中追加 `spring-boot-starter-web` 依赖，以修复因缺少 Spring MVC 注解导致的 15 个编译错误。此变更解决 R19 验证失败的唯一下游根因。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/ai/ai-impl/pom.xml` | 修改 | 追加 `spring-boot-starter-web` 依赖 |

## 类型定义

无新增类型。仅涉及 Maven POM 依赖声明变更。

## 变更内容

**文件**：`AIMedical/backend/modules/ai/ai-impl/pom.xml`

**操作**：在 `<dependencies>` 内，`spring-boot-starter` 之后、`spring-boot-starter-test` 之前，插入：

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
```

**变更后 dependencies 顺序**：
1. `ai-api` (com.aimedical)
2. `spring-boot-starter` (org.springframework.boot)
3. **`spring-boot-starter-web` (org.springframework.boot)** ← 新增
4. `spring-boot-starter-test` (org.springframework.boot, scope=test)

**关键属性**：
- **scope**：默认 compile（不显式声明，与项目中 admin、consultation、patient、doctor、prescription、medical-record、common-module-impl、application、integration 等含 Controller 的模块一致）
- **版本管理**：由 `ai` 父 POM 继承的 Spring Boot BOM 统一管理，无需指定 `<version>`

## 错误处理

不涉及。本变更仅添加编译期依赖，无运行时错误处理逻辑变更。

## 行为契约

- **前置条件**：`ai-impl/pom.xml` 当前存在 `<dependencies>` 块，包含 `spring-boot-starter` 和 `spring-boot-starter-test`
- **后置条件**：`ai-impl` 模块编译时 classpath 包含 `spring-boot-starter-web` 及其传递依赖（spring-webmvc、spring-web、tomcat-embed-core 等），`MockAdminController.java` 中的 Spring MVC 注解（`@RestController`、`@RequestMapping`、`@GetMapping`、`@PostMapping`、`@RequestBody`、`ResponseEntity`）可正常解析编译
- **不变量**：其他依赖的声明和模块结构不变；`spring-boot-starter`（基础自动配置）保留

## 依赖关系

- **新增外部依赖**：`org.springframework.boot:spring-boot-starter-web`（版本由父 POM BOM 管理）
- **无新增模块内部依赖**
- **暴露给后续任务**：此变更为独立修复，无后续任务依赖
