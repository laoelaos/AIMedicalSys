# 再审议判定报告（v16）

## 判定结果

RETRY

## 判定理由

诊断报告包含 1 个**严重**级别问题（问题 1：4.3 节 AI 调用契约图中的 `DegradationStrategy.fallback` 引用已不存在的方法）和 2 个**中等**级别问题（问题 2：Swagger UI 生产环境安全暴露；问题 3：运行时环境要求缺失）。质询报告确认该诊断 LOCATED，所有问题均证据充分、逻辑有效。未满足 PASS 条件中的任意一条（存在严重和一般等级的问题、非全轻微、且在非最大轮次下已被 LOCATED）。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：4.3 节 AI 调用契约图中的 `DegradationStrategy.fallback` 引用已不存在的方法
- **所在位置**：第 4.3 节，第 677–683 行（流程图）
- **严重程度**：严重
- **改进建议**：将 4.3 节流程图中的 `DegradationStrategy.fallback(request)` 替换为 `DegradationStrategy.shouldDegrade(context)` + 直接返回 `AiResult(success=false, degraded=true, data=null)`

- **问题描述**：springdoc-openapi 配置在生产环境暴露 Swagger UI 的安全风险未处理
- **所在位置**：第 8.3 节，第 1011–1020 行（springdoc 配置块）
- **严重程度**：中等
- **改进建议**：将 springdoc 配置移至 `application-dev.yml` 或要求在 `application-prod.yml` 中显式禁用

- **问题描述**：运行时环境要求未明确说明
- **所在位置**：通篇缺失，应在第 1 节「概述」或第 9 节「本地开发体验」中补充
- **严重程度**：中等
- **改进建议**：补充 JDK 17+、Node.js 18+/20 LTS、npm 9+ 等最低版本要求

- **问题描述**：PageQuery.page/PageQuery.sort 的边界条件和异常输入未定义
- **所在位置**：第 3.1 节「PageQuery / PageResponse<T>」字段描述
- **严重程度**：低
- **改进建议**：添加 `@Min(0)` 注解并说明 sort 格式无效时的异常处理方式

- **问题描述**：前端骨架缺少明确的验收标准
- **所在位置**：第 4.1 节「健康检查」及整体缺失
- **严重程度**：低
- **改进建议**：在 4.1 节补充前端三端应用的验收标准
