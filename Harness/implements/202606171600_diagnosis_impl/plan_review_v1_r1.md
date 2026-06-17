# 计划审查报告（v1 r1）

## 审查结果
REJECTED

## 发现

### **[严重] 问题7 实现方案未考虑 Maven 插件配置合并语义，导致 application 模块丢失 ai-api/common-module-api 豁免**

**问题**：计划将 `patient/doctor/admin` 三个 `<ignoredUnusedDeclaredDependency>` 从父 POM 移入 `application/pom.xml` 的 `maven-dependency-plugin` 声明中。但父 POM 的 `maven-dependency-plugin` 定义在 `<build><plugins>` 中（而非 `<pluginManagement>`），当前包含 5 个豁免条目。当 `application/pom.xml` 在自身 `<build><plugins>` 中定义相同插件并设置 `<ignoredUnusedDeclaredDependencies>` 时，Maven 的插件配置合并规则是**子模块的 `<ignoredUnusedDeclaredDependencies>` 元素整体替换**继承自父 POM 的同名元素——而非合并追加。

**后果**：`application/pom.xml` 新增的配置仅包含 `patient/doctor/admin` 三个业务模块豁免，但 `ai-api` 和 `common-module-api` 将不再被豁免。由于 application 模块的代码在 Phase 0 尚未直接引用 ai-api 和 common-module-api 中的类型，`mvn dependency:analyze -pl application` 会将这两个依赖报告为 Unused declared dependencies，导致门禁失败。

**期望的修正方向**：必须明确解决合并语义问题，选择以下任一方案并在 plan/task 中写明：
- **方案A**（推荐）：application/pom.xml 的 `<ignoredUnusedDeclaredDependencies>` 包含全部 5 个条目（ai-api、common-module-api、patient、doctor、admin）
- **方案B**：将父 POM 的 maven-dependency-plugin 移至 `<pluginManagement>`，移除父 POM `<build><plugins>` 中的直接声明，由 application/pom.xml 完全接管其配置（包含全部 5 个条目）
- **方案C**：在父 POM 的 `<ignoredUnusedDeclaredDependencies>` 上标注 `combine.children="append"`，使 application/pom.xml 新增的条目追加而非替换

### **[一般] dependencyManagement 中的 patient/doctor/admin 条目未计划清理**

**问题**：计划仅提及向 `<dependencyManagement>` 补充 5 个 starter 条目，但未处理当前 `<dependencyManagement>` 中现有的 `com.aimedical:patient`、`com.aimedical:doctor`、`com.aimedical:admin` 三个业务模块声明。OOD §2.1 骨架的 dependencyManagement 明确不包含这三个业务模块（仅包含 6 个基础设施内部模块 + 5 个 starter + springdoc + h2）。

**后果**：完成本轮修复后，dependencyManagement 的实际条目与 OOD §2.1 骨架仍存在 3 项冗余偏离，OOD 对齐度不完整。

**期望的修正方向**：在 plan/task 中补充子任务，从 `<dependencyManagement>` 中移除 `com.aimedical:patient`、`com.aimedical:doctor`、`com.aimedical:admin` 三个条目。
