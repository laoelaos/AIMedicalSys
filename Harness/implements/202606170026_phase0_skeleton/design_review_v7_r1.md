# 设计审查报告（v7 r1）

## 审查结果
REJECTED

## 发现

### **[严重] pom.xml 依赖区块遗漏 spring-boot-starter**

- **问题**：设计文档"修改后依赖区块"仅列出 ai-api（compile）、common（compile）、spring-boot-starter-test（test scope），**未包含 spring-boot-starter（compile）**。
- **为什么是问题**：实际源码中 MockAiService 使用 `@Service`、`@ConditionalOnProperty`，NoOpDegradationStrategy 使用 `@Component`、`@ConditionalOnMissingBean`，FallbackAiService 使用 `@Service`。这些注解分属 `spring-context` 和 `spring-boot-autoconfigure`，需通过 `spring-boot-starter` 以 compile scope 引入。父 POM 不会自动传递该依赖；common 模块的 `spring-boot-starter-web`/`spring-boot-starter-data-jpa` 均为 `<optional>true</optional>`，不会透传给 ai-impl。按照此设计的依赖区块修改后，ai-impl 将编译失败。
- **期望修正方向**：在依赖区块中添加 `spring-boot-starter`（compile scope），与当前 `ai-impl/pom.xml` 实际文件中的声明保持一致。

### **[轻微] 设计文档错误描述 pom.xml 当前状态**

- **问题**：设计称"当前状态：仅 artifactId，无依赖声明"，但实际 `ai-impl/pom.xml` 已包含 ai-api、common、spring-boot-starter、spring-boot-starter-test 四项依赖声明。
- **为什么是问题**：描述不准确，可能源于设计者基于旧版本编写，但会影响后续实现者对预期变更范围的判断。
- **期望修正方向**：更新为正确的当前状态描述，或直接删除"当前状态"描述，仅保留"修改后依赖区块"作为目标。
