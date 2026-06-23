# 实现报告（v1）

## 概述

尝试实现 POM 依赖治理修复（T3 + T4 + T7），涉及两个 POM 文件共 7 处操作。OP-03（ai-impl 移除 common 依赖）可正常执行；OP-01（父 POM 删除 5 个 Starter 版本号）和 OP-02（删除 h2 的 runtime scope）暴露了设计的"依赖传播"假设缺陷。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 编辑 | `AIMedical/backend/pom.xml` | OP-01：删除 5 个 `<version>3.2.5</version>`（已回退） |
| 编辑 | `AIMedical/backend/pom.xml` | OP-02：删除 h2 的 `<scope>runtime</scope>`（已回退） |
| 编辑 | `AIMedical/backend/modules/ai/ai-impl/pom.xml` | OP-03：删除 `com.aimedical:common` 依赖块（已回退） |

## 编译验证

mvn compile 失败，错误概要：
```
'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing.
```

共涉及 10 个子模块（common, common-module-api, common-module-impl, ai-api, ai-impl, patient, doctor, admin, application, integration），均报告缺失 Spring Boot Starter 的 version。

## 设计偏差说明

### 偏差 1：dependencyManagement 版本继承假设不成立

**设计规格**：OP-01 约束称 "版本继承自 `spring-boot-starter-parent:3.2.5` 的 BOM，无需显式指定"。

**验证结果**：此假设不成立。Maven 的 `dependencyManagement` 合并机制中，子 POM 的 `<dependencyManagement>` 条目会**覆盖**父 POM（spring-boot-starter-parent）的对应条目。当子 POM 声明无版本的 `dependencyManagement` 条目时，覆盖后该条目变为无版本状态，而非"继承"父 BOM 的版本。

**影响范围**：所有在自身 `<dependencies>` 中声明了 Spring Boot Starter 但未指定版本的子模块（common, patient, doctor, admin, application, integration 等共 10 个 POM）。

### 偏差 2：OP-02（h2 scope 删除）逻辑上无运行时风险但需与 OP-01 一同解决

**设计规格**：删除 h2 的 `<scope>runtime</scope>`，scope 约定由各子模块自行声明。

**实际处理**：该操作本身语法正确，无编译/运行风险。但因与 OP-01 在同一文件，一并回退等待设计修正。

### 偏差 3：OP-03（ai-impl 移除 common 依赖）独立验证通过

**设计规格**：删除第 17-20 行（`com.aimedical:common` 整个 dependency 块）。

**实际处理**：该操作独立，无依赖冲突风险。已一并回退以保持原子变更集。

## 修正建议

要使 OP-01 正确工作，有以下两种可行方案：

1. **方案 A（推荐）**：在父 POM 的 `<dependencyManagement>` 中**完全移除**这 5 个 Spring Boot Starter 的条目，不再覆盖 spring-boot-starter-parent BOM 的版本管理。子模块的 `<dependencies>` 不变，版本自动从 grandparent BOM 继承。

2. **方案 B**：保留 `dependencyManagement` 条目但改用 `<type>pom</type><scope>import</scope>` 导入 `spring-boot-dependencies` BOM。但此方案冗余（parent 已自带 BOM）。

请设计方确认方案后重新下达编码指令。
