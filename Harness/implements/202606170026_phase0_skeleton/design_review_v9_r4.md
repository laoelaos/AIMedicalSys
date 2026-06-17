# 设计审查报告（v9 r4）

## 审查结果
APPROVED

## 发现

- **[轻微]** 设计"关键行为契约"章节声明了运行时端点可达性验证（`GET /api/patient/placeholder → 200 OK`），但实际 application 模块仅为一个裸 POM，不存在 `@SpringBootApplication` 主启动类和 `spring-boot-maven-plugin`。运行时验证依赖的组件不在当前设计范围内。建议在行为契约前补充前提条件说明："前提：application 模块已配置主启动类及 Spring Boot Maven 插件"。

- **[轻微]** 设计概述要求"在 `application/pom.xml` 中为三个新模块添加 compile 范围依赖"，但未提供精确的 XML 配置模板或插入位置。与父 POM `<modules>` 更新的精确定位描述（"在 `<modules>` 中依次添加"）相比，此处的规范级别偏低。实现者需自行推断具体的 `<dependency>` 块写法。

- **[轻微]** 设计决策表描述 `ignoredUnusedDeclaredDependencies` 需新增 patient/doctor/admin 三项豁免，但当前父 POM（`backend/pom.xml:108-111`）仅包含 `ai-api` 和 `common-module-api` 两项，新三项尚未添加。设计虽正确识别了需求，但缺少优先级的标注（此变更应在 application/pom.xml 依赖添加之后执行，否则 `mvn dependency:analyze` 会先报错）。

## 修改要求
（APPROVED — 无强制修改要求）
