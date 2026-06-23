# 再审议判定报告（v4）

## 判定结果

RETRY

## 判定理由

组件B诊断报告发现 9 个问题，其中 3 个严重（问题1：@EnableJpaAuditing 缺失；问题2：SecurityConfig 认证策略与 Phase 0 目标冲突；问题9：ErrorCode 类型架构存在设计矛盾）、4 个一般（问题3：FallbackAiService 注入失败；问题4：分页起始值歧义；问题5：Boolean NPE 风险；问题6：前端 DTO 类型同步机制未定义）、2 个轻微（问题7：Integration 模块用途未定义；问题8：API 版本管理策略描述不足）。组件B质询报告结论为 LOCATED，内部循环在 2 轮（最大 12 轮）时提前终止且审查结论被确认。因诊断报告包含严重和一般等级问题，不符合 PASS 条件，须重新运行组件A。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：`@EnableJpaAuditing` 配置缺失导致 `createdAt`/`updatedAt` 自动填充静默失效
- **所在位置**：3.2 节 BaseEntity 字段定义
- **严重程度**：严重
- **改进建议**：在 3.2 节末尾补充 `@EnableJpaAuditing` 的配置位置声明（推荐放在 common 模块的 config 包的 JpaConfig 类中）

- **问题描述**：SecurityConfig 认证策略与 Phase 0「骨架可运行」目标冲突——「其余接口要求认证」与「permitAll 临时放通」矛盾
- **所在位置**：4.5 节 SecurityConfig 设计骨架
- **严重程度**：严重
- **改进建议**：明确 Phase 0 统一使用 `permitAll` 放通所有接口，或提供显式的 `@Profile("!phase0")` 条件化配置

- **问题描述**：FallbackAiService 在 `ai.mock.enabled=false` 时按名称硬编码注入 `mockAiService`，生产环境无此 Bean 导致启动失败
- **所在位置**：3.4 节 Bean 装配策略
- **严重程度**：一般
- **改进建议**：FallbackAiService 内部使用 `@Autowired` + `@Lazy` + `ObjectProvider<AiService>` 延迟获取可用实现

- **问题描述**：PageRequest.page 起始值歧义（从 0 或 1 开始），存在后续模块 off-by-one 风险
- **所在位置**：3.1 节 PageRequest/PageResponse 字段描述
- **严重程度**：一般
- **改进建议**：明确约定为 0-based，添加 Javadoc 注释，前端补充分页参数适配逻辑

- **问题描述**：BaseEntity.deleted 使用包装类型 `Boolean` 存在自动拆箱 NPE 风险
- **所在位置**：3.2 节 BaseEntity 字段定义
- **严重程度**：一般
- **改进建议**：将 `deleted` 字段类型从 `Boolean` 改为 `boolean`（基本类型）

- **问题描述**：前后端 26+ DTO 类型定义的一致性同步机制未定义，多人并行协作存在类型不匹配风险
- **所在位置**：8.2 节 AI 能力方法清单及 2.4 节
- **严重程度**：一般
- **改进建议**：补充 OpenAPI 规范生成配置（springdoc-openapi）并声明前端通过 openapi-generator 自动生成 TypeScript 类型

- **问题描述**：ErrorCode 定义为 `enum` 与「每个模块维护自己的错误码枚举」矛盾，enum 为 final 不可被继承或实现，BusinessException 无法引用多模块错误码
- **所在位置**：3.1 节 ErrorCode + 5.2 节 BusinessException
- **严重程度**：严重
- **改进建议**：将 ErrorCode 从 `enum` 改为 `interface`，各模块提供 enum 实现该接口

- **问题描述**：Integration 模块用途完全未定义，开发者无法判断是否需要创建
- **所在位置**：2.1 节目录布局及第 10 节 CI 占位
- **严重程度**：轻微
- **改进建议**：若需要集成测试骨架则补充职责和交付物要求，否则从目录布局中删除该条目

- **问题描述**：API 版本管理策略描述分散在决策表与正文中，未形成单一声明
- **所在位置**：第 7 节设计决策表及 8.1 节
- **严重程度**：轻微
- **改进建议**：在 8.1 节开头用一句话总结 API 版本管理策略，形成单一声明
