# 实现报告（v2）

## 概述

基于 v2 详细设计，对 Phase 0 后端骨架进行修正编译问题。核心变更：删除父 POM `dependencyManagement` 中的「External starters」区块；修复 common 模块中因 Spring Boot 3.2 / Hibernate 6.4 API 变化导致的 3 类编译错误。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | backend/pom.xml | 删除 `dependencyManagement` 中 5 个外置 Starter 声明（spring-boot-starter-web/data-jpa/security/validation/test），其版本已由 `spring-boot-starter-parent:3.2.5` 传递管理 |
| 修改 | backend/common/pom.xml | 新增 `spring-boot-starter-validation` 依赖（optional），为 PageQuery 的 `jakarta.validation` 注解提供编译支持 |
| 修改 | backend/common/src/main/java/com/aimedical/common/base/BaseEntity.java | `@SQLDelete` 改为显式 `sql` 属性（Hibernate 6.4 API） |
| 修改 | backend/common/src/main/java/com/aimedical/common/config/JacksonConfig.java | `Jackson2ObjectMapperBuilderCustomizer` 导入修正为 `org.springframework.boot.autoconfigure.jackson` 包 |

## 编译验证

```
$ mvn compile -pl common -am -q
# 无输出，编译成功
$ mvn test -pl common -am -q
# 输出中仅有 GlobalExceptionHandlerTest 预期的异常日志
# 测试报告：36 tests run, 0 failures, 0 errors, 0 skipped
# BaseEntityTest(4), BaseEnumTest(2), CommonPlaceholderTest(1),
# GlobalExceptionHandlerTest(3), BusinessExceptionTest(4),
# GlobalErrorCodeTest(5), PageQueryTest(6), PageResponseTest(6), ResultTest(5)
```

## 设计偏差说明

| 设计规格 | 偏差原因 | 实际处理 |
|---------|---------|---------|
| common 模块依赖仅声明 spring-boot-starter-web、data-jpa、test | PageQuery 使用 `@Min/@Max` 需要 `jakarta.validation` API，但 Spring Boot 3.x 的 `spring-boot-starter-web` 不再传递 `spring-boot-starter-validation` | 新增 `spring-boot-starter-validation` 依赖（optional），从 `spring-boot-starter-parent` 继承版本号 |
| JacksonConfig 导入 `org.springframework.http.converter.json.Jackson2ObjectMapperBuilderCustomizer` | Spring Boot 3.x 中该类型位于 `org.springframework.boot.autoconfigure.jackson` 包 | 修正为正确包路径 |
| BaseEntity `@SQLDelete("...")` 简写形式 | Hibernate 6.4 中 `@SQLDelete` 的 `value()` 属性已移除，需使用 `sql` 属性 | 改为 `@SQLDelete(sql = "...")` |
