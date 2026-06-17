# 测试报告（v1 r2）

## 验证策略

1. **Maven 命令执行**：安装 Maven 3.9.9 + JDK 21，在 `AIMedical/backend/` 下执行 `dependency:analyze`、`compile -pl common -am`、`compile -DskipTests`
2. **逐项结构检查**：对照详细设计逐条确认 POM 文件内容
3. **Java XPath 单元测试**：3 个测试文件对 POM 结构进行 XPath 断言

## Maven 命令执行结果

| 命令 | 结果 | 说明 |
|------|------|------|
| `mvn validate -N`（父 POM） | ✅ SUCCESS | 父 POM `aimedical-sys` 自身结构正确 |
| `mvn compile -pl common -am` | ❌ FAILED | 无法读取 10 个子模块 POM |
| `mvn dependency:analyze -pl application -am` | ❌ FAILED | 同上原因 |
| `mvn compile -DskipTests` | ❌ FAILED | 同上原因 |

### 错误分类

**错误类型 A：Spring Boot starter 版本无法解析（波及 10 个模块）**

Maven 报告 `'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing.`（同类错误遍及 data-jpa、security、validation、test）。

**根因**：父 POM `dependencyManagement` 声明了 5 个 Spring Boot starter 条目，但**未指定 `<version>`**。Maven 在 `dependencyManagement` 中匹配到这些条目后停止向 BOM 查找，而条目本身无版本，导致子模块 POM 读取失败。

**涉及 POM**：common、common-module-api、common-module-impl、ai-api、ai-impl、application、patient、doctor、admin、integration

**错误类型 B：业务模块版本无法解析（仅 application 模块）**

Maven 报告 `'dependencies.dependency.version' for com.aimedical:patient:jar is missing.`（以及 doctor、admin）。

**根因**：application/pom.xml 声明了 patient/doctor/admin 依赖，但父 POM `dependencyManagement` 删除了这 3 个条目（修改 2），且 application 未提供 `<version>`。模块不在 BOM 中，故无法解析。

## 结构验证（Java XPath 单元测试）

3 个测试文件使用 JUnit 5 + DOM XPath 对 POM XML 做结构断言，**全部通过**：

| 测试文件 | 覆盖修改 | 测试方法数 | 结果 |
|---------|---------|-----------|------|
| `ParentPomTest.java` | 修改 1/2/4 | 5 | ✅ 全部通过 |
| `CommonPomTest.java` | 修改 3 | 5 | ✅ 全部通过 |
| `ApplicationPomTest.java` | 修改 5 | 3 | ✅ 全部通过 |

结构验证确认：POM 文件内容与 detail_v1.md 的 XML 片段完全一致。

## Git Diff 审查

```
Modified: AIMedical/backend/pom.xml
  - dependencyManagement: +5 starters (web, data-jpa, security, validation, test) -- 无 version
  - dependencyManagement: -3 business modules (patient, doctor, admin)
  - ignoredUnusedDeclaredDependencies: -3 business modules, kept ai-api + common-module-api

Modified: AIMedical/backend/common/pom.xml
  - Removed spring-boot-starter-validation dependency block

Modified: AIMedical/backend/application/pom.xml
  - Added maven-dependency-plugin with 5 ignoredUnusedDeclaredDependency entries
```

## 逐项检查清单

### 修改 1：父 POM `dependencyManagement` 补充 5 个 starter

| 依赖 | groupId | artifactId | 预期 | 实际 | 结果 |
|------|---------|-----------|------|------|------|
| starter-web | org.springframework.boot | spring-boot-starter-web | 存在 | pom.xml:84 存在 | ✅ |
| starter-data-jpa | org.springframework.boot | spring-boot-starter-data-jpa | 存在 | pom.xml:88 存在 | ✅ |
| starter-security | org.springframework.boot | spring-boot-starter-security | 存在 | pom.xml:92 存在 | ✅ |
| starter-validation | org.springframework.boot | spring-boot-starter-validation | 存在 | pom.xml:96 存在 | ✅ |
| starter-test | org.springframework.boot | spring-boot-starter-test | 存在 + scope=test | pom.xml:100 scope=test | ✅ |

**位置验证**：5 个 starter 位于 h2 条目之后、`</dependencies>` 闭合标签之前 ✅

### 修改 2：父 POM `dependencyManagement` 移除 3 个业务模块

| artifactId | 预期 | 实际 | 结果 |
|-----------|------|------|------|
| patient | 不存在 | 搜索无匹配 | ✅ |
| doctor | 不存在 | 搜索无匹配 | ✅ |
| admin | 不存在 | 搜索无匹配 | ✅ |

**内部模块保留验证**：common、common-module-api、common-module-impl、ai-api、ai-impl、application 共 6 个内部模块均存在 ✅

### 修改 3：common/pom.xml 移除 validation starter

| 检查项 | 预期 | 实际 | 结果 |
|--------|------|------|------|
| validation starter | 不存在 | 无此依赖 | ✅ |
| web starter | 存在 + optional=true | optional=true | ✅ |
| data-jpa starter | 存在 + optional=true | optional=true | ✅ |
| test starter | 存在 + scope=test | scope=test | ✅ |
| 依赖总数 | 3 个 | 3 个 | ✅ |

### 修改 4：父 POM `ignoredUnusedDeclaredDependencies` 移除业务模块

| 条目 | 预期 | 实际 | 结果 |
|------|------|------|------|
| ai-api | 保留 | pom.xml:115 存在 | ✅ |
| common-module-api | 保留 | pom.xml:116 存在 | ✅ |
| patient | 已删除 | 搜索无匹配 | ✅ |
| doctor | 已删除 | 搜索无匹配 | ✅ |
| admin | 已删除 | 搜索无匹配 | ✅ |

### 修改 5：application/pom.xml 新增 maven-dependency-plugin

| 检查项 | 预期 | 实际 | 结果 |
|--------|------|------|------|
| maven-dependency-plugin 存在 | 是 | application/pom.xml:95 存在 | ✅ |
| 未指定 version | 是 | 无 `<version>` 元素 | ✅ |
| ai-api | 存在 | application/pom.xml:100 | ✅ |
| common-module-api | 存在 | application/pom.xml:101 | ✅ |
| patient | 存在 | application/pom.xml:102 | ✅ |
| doctor | 存在 | application/pom.xml:103 | ✅ |
| admin | 存在 | application/pom.xml:104 | ✅ |

## 结论

**FAILED** — 结构验证全部通过，但 Maven 构建验证失败。

### 失败原因摘要

| # | 问题 | 严重性 | 所属修改 | 类型 |
|---|------|--------|---------|------|
| A | `dependencyManagement` 中 5 个 Spring Boot starter 无 `<version>`，覆盖父 BOM 导致版本不可解析 | 严重 | 修改 1 | 设计缺陷 |
| B | application/pom.xml 依赖 patient/doctor/admin，但 `dependencyManagement` 已删除三者且未提供 `<version>` | 严重 | 修改 2 | 设计缺陷 |

### 建议修正方向

1. **问题 A**：从父 POM `dependencyManagement` 中删除 5 个 Spring Boot starter 条目。这些版本已由 `spring-boot-starter-parent` BOM 统一管理，子模块无需 dependencyManagement 介入即可直接声明使用。
2. **问题 B**：将 `com.aimedical:patient`、`com.aimedical:doctor`、`com.aimedical:admin` 保留在父 POM `dependencyManagement` 中（添加 `<version>${project.version}</version>`），或为它们创建独立的内部 BOM 模块。移除设计规约中的删除要求需与架构师确认。

### 单元测试状态

3 个 Java XPath 单元测试在结构层面验证通过，但无法捕获上述 Maven 构建语义级别的错误。建议补充 Maven 构建集成测试（如 `maven-invoker-plugin` 或 CI pipeline 中的 build 检查）。
