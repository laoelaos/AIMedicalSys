# Phase 0 OOD 设计质量审查报告（第 16 轮）

审查人：质量审查 Agent
审查日期：2026-06-15
审查目标：`a_v16_copy_from_v15.md`

---

## 问题 1（严重）：4.3 节 AI 调用契约图中的 `DegradationStrategy.fallback` 引用已不存在的方法

**问题描述**：4.3 节「AI 能力调用契约」的流程图中显示 `FallbackAiService(调用失败) → DegradationStrategy.fallback(request)`，但根据 3.4 节的定义（v15 修订已确认），DegradationStrategy 已取消泛型 fallback 方法，仅保留 `boolean shouldDegrade(DegradationContext context)` 判定逻辑，降级结果由 FallbackAiService 直接构造 `AiResult(success=false, degraded=true, data=null)`。4.3 节的流程图引用了文档中不存在的方法签名，与当前设计存在事实矛盾。

**所在位置**：第 4.3 节，第 677–683 行（流程图）

**严重程度**：严重

**改进建议**：将 4.3 节流程图中的 `DegradationStrategy.fallback(request)` 替换为符合当前设计的正确描述，例如：
```
└── FallbackAiService(调用失败) → DegradationStrategy.shouldDegrade(context)
      ↓ degraded=true
      └── 直接返回 AiResult(success=false, degraded=true, data=null)
```

---

## 问题 2（中等）：springdoc-openapi 配置在生产环境暴露 Swagger UI 的安全风险未处理

**问题描述**：8.3 节的 springdoc-openapi 配置块被放置在 `application.yml`（全局配置，而非 profile 特定配置）中，且 `enabled: true`，这意味着 Swagger UI 和 OpenAPI 文档在生产环境（prod profile）下同样可被访问。文档在 9.1 节已明确处理了 H2 Console 的"生产环境关闭"策略，但对 Swagger UI 的类似安全风险缺少同等处理。

**所在位置**：第 8.3 节，第 1011–1020 行（springdoc 配置块）

**严重程度**：中等

**改进建议**：参照 9.1 节对 H2 Console 的处理方式，补充以下任一整改：
- 将 springdoc 配置移至 `application-dev.yml`，使其仅在 dev profile 下生效；或
- 在 8.3 节末尾补充说明，要求 `application-prod.yml` 中显式设置 `springdoc.api-docs.enabled: false` 和 `springdoc.swagger-ui.enabled: false`

---

## 问题 3（中等）：运行时环境要求未明确说明

**问题描述**：文档指定了 Spring Boot 3.3.0 和 Vite 等技术栈，但未明确声明运行时的最低环境要求：
- Spring Boot 3.3.0 要求 Java 17+，但文档未在任何位置说明所需 JDK 版本
- 前端 workspace 使用了 Vite、Vue 3、TypeScript，但未说明所需 Node.js 版本范围或 npm 版本

新加入项目的开发者无法从文档中获知本地环境配置的最低要求，可能因 JDK 版本不符导致编译失败。

**所在位置**：通篇缺失，应在第 1 节「概述」或第 9 节「本地开发体验」中补充

**严重程度**：中等

**改进建议**：在第 1 节末尾或 9.1 节顶部补充运行环境要求小节，至少包括：
- JDK：17+（Spring Boot 3.3.0 最低要求）
- Node.js：18+ 或 20 LTS（与 Vue 3 + Vite 兼容的主流版本）
- npm：9+ 或指定 pnpm/yarn

---

## 问题 4（低）：PageQuery.page/PageQuery.sort 的边界条件和异常输入未定义

**问题描述**：3.1 节定义了 PageQuery 的 page 为 0-based、size 上限 500（标注 `@Max(500)`），但对以下边界输入未定义处理行为：
- `page` 传入负值（如 `page=-1`）——Spring Data 的 `PageRequest.of(-1, size)` 会抛出 `IllegalArgumentException`
- `sort` 格式无效（如 `"createdAt"` 缺少 direction，或方向拼写错误 `"createdAt,descc"`）——Spring Data 的 `Sort.by()` 解析异常未描述捕获方式

**所在位置**：第 3.1 节「PageQuery / PageResponse<T>」字段描述

**严重程度**：低

**改进建议**：
- 在 PageQuery 的 `page` 字段上标注 `@Min(0)` 注解
- 补充说明 `sort` 格式无效时的异常（`InvalidDataAccessApiUsageException` 或 `BadRequestException`）应由 GlobalExceptionHandler 统一捕获为 400 响应
- 或标注 Phase 0 暂不实现参数校验的 fallback 处理

---

## 问题 5（低）：前端骨架缺少明确的验收标准

**问题描述**：需求要求"骨架可运行——三端前端可一键启动到占位首页"，且 4.1 节定义了后端的验收标准（`GET /api/ping` → `"pong"`），但对前端三端应用缺少同等级别的验收标准定义。开发者无法明确判断前端骨架是否满足 Phase 0 验收要求。

**所在位置**：第 4.1 节「健康检查」及整体缺失

**严重程度**：低

**改进建议**：在 4.1 节补充前端骨架的验收标准，例如：
- 各前端应用（patient/doctor/admin）执行 `npm run dev` 可正常启动 Vite 开发服务器
- 浏览器访问各端端口显示占位页面（包含系统名称及占位提示文本）
- 各端开发服务器可正常代理 `/api/ping` 请求到后端并收到 `"pong"` 响应

---

## 整体评价

该 OOD 文档经过 15 轮审议迭代，在技术可行性、依赖管理、编译正确性、Bean 装配策略、CI 流水线等维度已达到较高成熟度。本文上述问题中，**问题 1（严重）** 最为关键——4.3 节的流程图引用了一个已在 v15 中明确删除的方法，属于 v15 修订范围未完全落实的遗漏，直接修复即可。其余问题集中在安全保密（Swagger 暴露）、环境要求说明和边界条件完整性方面，不影响 Phase 0 骨架的核心启动能力，但建议在最终产出前完成修正。
