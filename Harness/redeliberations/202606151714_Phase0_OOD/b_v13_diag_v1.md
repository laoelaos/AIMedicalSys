# 质量审查报告：Phase 0 OOD 设计文档（v13）

## 审查范围

- **待审查文件**：`a_v13_copy_from_v12.md`
- **用户需求**：`requirement.md`
- **迭代历史**：`iteration_history.md`（共 12 轮，已修复问题 34+ 项）
- **审查视角**：侧重内部审议未充分覆盖的维度——需求响应充分度、整体深度与完整性、异常/边界条件的实际落地覆盖

---

## 总体评价

该设计文档经过 12 轮迭代后，整体质量较高。需求文档列出的 7 个设计维度（共享工程结构、接口契约框架、数据与权限模型骨架、协作规范、本地开发体验、CI 占位、AI Mock 占位）均已覆盖，核心抽象清晰，多数接口定义足以指导编码实现。以下列出在深度审查中发现的剩余质量问题。

---

## 发现的问题

### 问题 1：FallbackAiService 的 `ObjectProvider<AiService>` 注入存在自引用循环依赖（严重 — 运行时 StackOverflow）

**所在位置**：Section 3.4「Bean 装配策略」— FallbackAiService 的注入方式描述

**问题描述**：文档描述 FallbackAiService 通过 `@Autowired` + `@Lazy` + `ObjectProvider<AiService>` 延迟获取底层 AiService 实现（MockAiService 或真实实现）。但 FallbackAiService 本身标注了 `@Primary` 且 implements AiService。当 `ObjectProvider.getIfAvailable()` 按类型查找时，会返回 @Primary 标注的 AiService Bean——即 FallbackAiService 自身。这将导致：
1. FallbackAiService 内部的 AI 方法（如 `triage()`）调用 `aiServiceProvider.getIfAvailable()`
2. 获取到自身实例
3. 向自身实例委托调用 → 无限递归 → StackOverflowError

该问题在迭代历史第 3 轮（Issue 2）和第 4 轮（Issue 3）中被讨论过，但当前方案（ObjectProvider）并未解决自引用问题。

**严重程度**：严重

**改进建议**：建议以下方案之一：
- 方案 A：FallbackAiService 不标注 `@Primary`，业务模块通过 `@Qualifier("fallbackAiService")` 按名称注入
- 方案 B：FallbackAiService 内部通过 `ApplicationContext.getBeanNamesForType(AiService.class)` 获取所有 AiService Bean 名称，排除自身后取首个可用实例
- 方案 C：引入 `AiServiceRegistry` 中间层，由该 Registry 持有底层实现引用，FallbackAiService 委托 Registry 获取

---

### 问题 2：目录布局与包命名规范之间存在不一致（dto/converter 子包缺失）

**所在位置**：Section 2.1（Monorepo 目录布局）vs Section 2.3（包命名规范）

**问题描述**：Section 2.3 中 patient/doctor/admin 模块包含 `api/`、`dto/`、`service/`、`repository/`、`entity/`、`converter/` 六个子包。但 Section 2.1 的目录树中 `patient/` 仅列出 `api/`、`service/`、`repository/`、`entity/` 四个子目录，缺失 `dto/` 和 `converter/`。基于该目录树创建骨架的开发者会遗漏两个子包。

**严重程度**：一般

**改进建议**：在 Section 2.1 的目录树中为 patient/doctor/admin 模块补全 `dto/` 和 `converter/` 子目录，与 Section 2.3 保持一致。

---

### 问题 3：`ai.mock.enabled=false` 在 Phase 0 下的实际行为与装配条件汇总表描述不一致

**所在位置**：Section 3.4「装配条件汇总表」

**问题描述**：装配条件汇总表第二行描述 `ai.mock.enabled=false` 时「激活的 AiService 实现」为「真实 AiService 实现」。但 Phase 0 不存在真实 AiService 实现（标注 Phase 2+），此时 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "false")` 的条件无匹配 Bean。最终仅 FallbackAiService 可用，且其内部的 ObjectProvider 将返回 null → 走兜底保护逻辑（返回 `AiResult(success=false, degraded=true, data=null)`）。该表未反映这一降级路径，可能误导 QA 或运维人员认为 Phase 0 下 `ai.mock.enabled=false` 是有效的正式环境配置。

**严重程度**：一般

**改进建议**：在装配条件汇总表中增加一行说明 Phase 0 下的行为：`ai.mock.enabled=false` 时 Phase 0 无底层 AiService Bean 可用，FallbackAiService 返回降级结果。或明确注明 Phase 0 仅支持 `ai.mock.enabled=true` 配置。

---

### 问题 4：`PermissionService.getUserPermissions()` 返回值语义不明确

**所在位置**：Section 8.4「PermissionService 门面接口定义」

**问题描述**：
```java
Set<String> getUserPermissions(Long userId);
```
`Set<String>` 中的 String 具体含义未定义——是功能权限编码（如 `"patient:create"`）、权限 ID、还是其他标识？不同开发者可能给出不同实现，导致前端消费时格式不一致。

**严重程度**：轻微

**改进建议**：在 Javadoc 或行内注释中明确 `String` 的语义（例如：功能权限标识符，格式为 `"{module}:{action}"`，如 `"patient:create"`、`"doctor:schedule:view"`）；或定义 `PermissionCode` 值类型替代裸 String。

---

### 问题 5：`mvn spring-boot:run` 一键启动命令缺少依赖模块编译支持

**所在位置**：Section 9.3「一键启动」

**问题描述**：文档给出的启动命令为：
```
mvn spring-boot:run -pl application
```
首次构建（clean checkout）时，application 模块的模块间依赖（common、common-module-api、ai-api 等）尚未安装到本地 Maven 仓库，该命令将因依赖解析失败而报错。开发者需要先执行 `mvn install -DskipTests` 编译所有模块，或使用 `-am`（also-make）参数。

**严重程度**：轻微

**改进建议**：将命令补充为：
```
mvn spring-boot:run -pl application -am
```
或在命令前添加前置说明：首次启动前需执行 `mvn install -DskipTests` 编译所有模块。

---

### 问题 6：错误分类表中「配置加载失败」的 HTTP 状态码与实际场景矛盾

**所在位置**：Section 5.1「错误分类表」—「配置加载失败」行

**问题描述**：该行「HTTP 状态码」列为 `500（启动时）`，但「处理方式」描述为「应用启动失败，由 Spring Boot 的失败分析器（FailureAnalyzer）输出诊断信息」。应用启动失败发生在 HTTP 容器就绪之前，不存在任何 HTTP 响应。因此标 500 会产生误导——读者可能认为这是一个可在运行时捕获并返回 500 响应的异常。

**严重程度**：轻微

**改进建议**：将 HTTP 状态码列改为 `N/A（启动失败，无 HTTP 响应）`，或在状态码后增加括号说明「启动阶段，无 HTTP 响应，此处仅为分类标识」。

---

### 问题 7：前端 `packages/shared` 与 `packages/ui-core` 的导出入口内容未定义

**所在位置**：Section 2.4「前端模块划分」

**问题描述**：文档提供了 `shared/package.json` 和 `ui-core/package.json` 的 `main` 和 `types` 字段指向 `src/index.ts`，但未定义 `index.ts` 应导出哪些内容。对于 `shared`，至少应导出 `ApiClient` 类、各 DTO 类型定义、工具函数；对于 `ui-core`，应导出所有共享组件。缺失该定义可能导致多个开发者对入口导出内容的理解不一致。

**严重程度**：轻微

**改进建议**：在 `package.json` 配置之后补充 `index.ts` 的导出内容说明，例如：
- `shared/src/index.ts`：导出 `ApiClient`、`AuthStore`、`Result<T>` 及后端 DTO 对应的 TypeScript 类型
- `ui-core/src/index.ts`：导出所有共享组件（Layout、Sidebar、Table、Form 等）

---

## 总结

该文档经过 12 轮迭代后已基本成熟，7 大需求维度均有覆盖，核心抽象（Result<T>、BaseEntity、AiService、三级权限模型）定义清晰，多数异常场景已考虑。

需优先处理的问题为 **问题 1（FallbackAiService 自引用循环依赖）**，该问题会导致应用启动或运行时 StackOverflowError，直接影响 Phase 0 骨架的可用性。其余问题均属完整性或细节层面的补充。

### 问题按严重程度汇总

| 严重程度 | 数量 | 问题编号 |
|---------|------|---------|
| 严重 | 1 | 问题 1 |
| 一般 | 2 | 问题 2, 3 |
| 轻微 | 4 | 问题 4, 5, 6, 7 |
