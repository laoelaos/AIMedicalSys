# 任务指令（v12）

## 动作
NEW

## 任务描述
创建 `backend/integration/` 集成测试模块，含 Failsafe 插件配置和占位集成测试类；更新父 POM 聚合列表和 dependencyManagement。

### 1. 父 POM 变更（backend/pom.xml）

**modules 列表追加**：
```xml
<module>integration</module>
```

**dependencyManagement 追加**：
```xml
<dependency>
    <groupId>com.aimedical</groupId>
    <artifactId>integration</artifactId>
    <version>${project.version}</version>
</dependency>
```

**maven-dependency-plugin 追加 ignoredUnusedDeclaredDependency**（`backend/pom.xml:104-116`，父 POM 已有的插件配置）：
```xml
<ignoredUnusedDeclaredDependency>com.aimedical:integration</ignoredUnusedDeclaredDependency>
```

### 2. integration/pom.xml

**路径**: `backend/integration/pom.xml`
**parent**: `com.aimedical:aimedical-sys:0.0.1-SNAPSHOT`
**artifactId**: `integration`
**packaging**: `jar`

**依赖**：
- `com.aimedical:application` (test, classifier=exec) — 使用 executable JAR 启动 Spring Boot 上下文
- `org.springframework.boot:spring-boot-starter-test` (test)
- `com.aimedical:common` (test) — 引用 Result 类型验证响应

**插件**：maven-failsafe-plugin
- 绑定到 `integration-test` 和 `verify` 生命周期阶段
- 包含模式：`**/*IT.java`（集成测试命名规范）
- 跳过属性：`skipITs`
- 确保 `spring-boot-maven-plugin` 的 classifier=exec 生成的 JAR 在 test classpath 中

### 3. 占位集成测试 — HealthCheckIT.java

**路径**: `backend/integration/src/test/java/com/aimedical/integration/HealthCheckIT.java`
**包**: `com.aimedical.integration`
**注解**: `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`（通过 application executable JAR 启动）
**行为**：
- `@Autowired TestRestTemplate restTemplate`
- `@Test void shouldPingSuccess()` → `restTemplate.getForEntity("/api/ping", String.class)` 验证 HTTP 200 且响应体包含 `"SUCCESS"`

### 4. 占位集成测试 — ApplicationContextIT.java

**路径**: `backend/integration/src/test/java/com/aimedical/integration/ApplicationContextIT.java`
**包**: `com.aimedical.integration`
**注解**: `@SpringBootTest`
**行为**: 空测试方法 `@Test void contextLoads()`，验证 Spring 应用上下文可成功加载

## 选择理由
integration 是后端验收标准中的必需模块（requirement.md §1），当前完全缺失；其前置依赖（application 模块启动基础设施 + classifier=exec）已在 R10 完成实现；integration 是 backend 中唯一缺失的模块，完成后后端骨架满足验收标准。前端 monorepo 将在后续轮次独立实现。

## 任务上下文
- requirement.md §1:「integration 模块: 占位集成测试类, Failsafe 插件配置」
- detail_v10.md §决策: Spring Boot Maven Plugin classifier=exec 的动机——"integration 模块以 test scope 依赖 application 后因 BOOT-INF/lib 布局无法解析 transitive 依赖；exec classifier 同时生成普通 JAR（供 Maven 依赖解析）和可执行 JAR（供 java -jar 启动）"
- 集成测试命名约定: 使用 `*IT.java` 后缀（而非 `*Test.java`），确保 surefire（单元测试）和 failsafe（集成测试）互不干扰

## 已有代码上下文
- `backend/pom.xml`（父 POM）当前聚合 9 个模块，无 integration
- `backend/application/pom.xml` 已配置 `spring-boot-maven-plugin` 的 classifier=exec
- `backend/application/src/main/java/com/aimedical/HealthController.java` 提供 `GET /api/ping → Result.success("pong")`
- 后端全模块 172 测试已全部通过（R10 验证状态: BUILD SUCCESS）

## 修订说明（v12 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| §1 `ignoredUnusedDeclaredDependency` 应添加到 `maven-dependency-plugin`（父 POM 行 103-116），而非 `spring-boot-maven-plugin` | 指令从「spring-boot-maven-plugin 追加 ignoredUnusedDeclaredDependency」改为「maven-dependency-plugin 追加 ignoredUnusedDeclaredDependency」，并注明行号和上下文 |
