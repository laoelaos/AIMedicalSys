# 设计审查报告（v12 r1）

## 审查结果
REJECTED

## 发现

### **[严重]** dependency 中 `classifier=exec` 与 `@SpringBootTest` 不兼容

**问题**：第 61 行声明 `com.aimedical:application` 的依赖为 `scope=test, classifier=exec`。

**为什么是问题**：`exec` classifier 对应的是 `spring-boot-maven-plugin` 生成的可执行 fat JAR（BOOT-INF 布局）。`@SpringBootTest` 使用标准 Java `URLClassLoader` 扫描类路径注册 Bean，它**无法**识别 fat JAR 内部的 `BOOT-INF/classes/` 路径。因此 `@SpringBootApplication`、`@RestController`、`@Service` 等组件不会被扫描到，Spring 上下文加载必然失败，两个集成测试全部不可用。

**已验证**：`application/pom.xml:91-93` 已配置 `<classifier>exec</classifier>`，这意味着：
- `application-0.0.1-SNAPSHOT.jar`（普通 JAR，类在根路径）→ **主 artifact**
- `application-0.0.1-SNAPSHOT-exec.jar`（fat JAR，BOOT-INF 布局）→ **classified artifact**

因此 integration 模块应依赖普通 JAR（**不带** `classifier`），而不是 fat JAR。

**修正方向**：移除 `classifier=exec`，仅保留 `<scope>test</scope>`：
```xml
<dependency>
    <groupId>com.aimedical</groupId>
    <artifactId>application</artifactId>
    <scope>test</scope>
</dependency>
```

---

### **[一般]** 构建执行流程描述与实现机制矛盾

**问题**：第 107-108 行声称 "Failsafe 插件绑定到 pre-integration-test 阶段 → 启动 Spring Boot 应用（通过 classifier=exec JAR）"。

**为什么是问题**：
1. failsafe 插件配置（第 65-66 行）仅绑定 `integration-test` 和 `verify`，未配置 `pre-integration-test`。
2. `@SpringBootTest(webEnvironment = RANDOM_PORT)` 自身在测试 JVM 进程内启动内嵌容器，无需 failsafe 在 `pre-integration-test` 预先启动外部应用进程。
3. 描述与实际的执行机制不符，会误导实现者添加错误的配置（如 `spring-boot-maven-plugin:start/stop` 或试图在 failsafe 中手动启动 JAR）。

**修正方向**：修正构建流程图，准确描述：
```
mvn verify -pl integration -am
  → Maven reactor 编译所有依赖模块
  → Failsafe 插件绑定到 integration-test 阶段
    → @SpringBootTest 在测试 JVM 内进程启动 Spring 容器 + 内嵌 Tomcat（随机端口）
    → ApplicationContextIT.contextLoads(): 验证上下文加载
    → HealthCheckIT.shouldPingSuccess(): 通过 TestRestTemplate 验证 /api/ping 可达
  → Failsafe 插件绑定到 verify 阶段（检查测试结果）
```

---

### **[一般]** `common` 依赖声明与测试行为不匹配

**问题**：第 63 行声明 `com.aimedical:common`（test scope）用途为 "引用 `Result` 类型验证响应结构"，但第 89-90 行的 `HealthCheckIT` 使用 `restTemplate.getForEntity("/api/ping", **String.class**)`，仅检查响应体字符串包含 `"SUCCESS"`。

**为什么是问题**：
1. `HealthController` 实际返回 `Result.success("pong")` → JSON `{"code":"SUCCESS","data":"pong"}`。断言 `responseBody.contains("SUCCESS")` 可侥幸通过，但这是一个**弱断言**——错误字段或意外添加的字段含 "SUCCESS" 字符串也会误判通过。
2. 若目标是验证 `Result` 响应结构，测试应使用 `Result<String>` 类型反序列化并断言 `code` 和 `data` 字段。
3. 若维持字符串包含检查，`common` 依赖即为冗余声明。

**修正方向**（二选一）：
- 使用 `Result` 类型验证：`restTemplate.getForEntity("/api/ping", Result.class)` 并断言 `result.getCode().equals("SUCCESS")`、`result.getData().equals("pong")`，保留 `common` 依赖。
- 维持简单字符串检查，从依赖声明中移除 `com.aimedical:common`。

---

### **[轻微]** Failsafe 包含模式重复默认值

`**/*IT.java` 已是 maven-failsafe-plugin 的默认包含模式，显式声明虽无害但略冗余。建议可省略以减少配置噪声，不影响正确性。

### **[轻微]** integration POM 缺少完整 XML 示例

设计以 bullet points 描述 failsafe 插件配置，未提供完整的 `integration/pom.xml` 示例。考虑到第 1 条严重问题（`classifier=exec` 错误）可能导致实现偏差，建议修正后在设计文档中补充完整 POM XML 以消除歧义。

## 修改要求

| # | 严重程度 | 问题 | 修正方向 |
|---|---------|------|---------|
| 1 | 严重 | `classifier=exec` 导致 `@SpringBootTest` 扫描不到 BOOT-INF 内组件 | 移除 classifier，仅用 `scope=test` |
| 2 | 一般 | 构建流程描述虚构 failsafe 启动应用的动作 | 修正描述，明确 `@SpringBootTest` 内进程启动机制 |
| 3 | 一般 | `common` 依赖与测试使用的响应类型不一致 | 统一使用 `Result` 类型验证或移除冗余依赖 |
| 4 | 轻微 | 默认包含模式重复 | 可省略 `**/*IT.java` 配置 |
| 5 | 轻微 | POM 缺少完整 XML 示例 | 修正后补充完整 POM |
