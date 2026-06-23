# 诊断质询报告（v1）

## 质询结果

LOCATED

## 逐维度审查

### 1. 证据充分性

[通过] 1.1 应用名不一致：经核实 OOD L52 `aimedical-sys/`、L129 `<name>AIMedicalSys Backend</name>`、L1006-1008 `spring.application.name: aimedical-application`，三处名称确实不一致，证据充分。

[通过] 1.2 "依赖"表述缺乏限定词：OOD L306 前半句"依赖 spring-boot-starter-web 及 spring-boot-starter-data-jpa"单独看缺少 optional 限定，虽 L306 后半句及 L320-322 已阐明 optional 策略，但首句确可能误导快速浏览的读者。证据充分。

[通过] 2.1 跨文档编号引用：OOD L928 "3.4.x" 引用的是需求文档编号体系（03_roadmap.md L24 及 01_requirement.md §3.4），OOD 自身节编号为 1~10、8.2 节为"AI 能力方法清单"，不存在 "3.4.x" 小节。证据充分。

[通过] 2.2 `@phase0-mock-field` 语义混淆：OOD L628-632 描述 Mock 数据填充规则（填充集合/字符串/数值等），L902-906 描述两层冻结策略，L909-925 示例将 `TriageRequest.chiefComplaint`（输入 DTO 字段）标记为 `@phase0-mock-field`。输入 DTO 由调用方填充，MockAiService 不会对其进行"Mock 数据填充"，两处语义存在混淆。证据充分。

[问题-轻微] 2.3 springdoc-openapi 版本兼容性：诊断将该问题归为"事实错误"，但实际指出的仅是"兼容性未经验证"这一潜在风险，而非 OOD 中存在可验证的事实性错误。该观察有效但分类稍欠精确。不影响结论方向。

[通过] 3.1 GlobalExceptionHandler 下沉：OOD L66 将 GlobalExceptionHandler 置于 common/config，L306/L320 使用 `optional=true` 缓解传递依赖。诊断指出的耦合问题是有效的架构关注点，common 编译时仍绑定 spring-boot-starter-web。证据充分。

[通过] 3.2 AI 降级框架过度设计：经与 Roadmap Phase 0.2 对照，AI Mock 标注为"推荐补齐"且"可跨阶段持续完善"。OOD L634-666 包含完整的 FallbackAiService、DegradationStrategy 体系、条件装配矩阵等，超出 roadmap 对 Phase 0 的要求。诊断指出的 roadmap 张力成立。证据充分。

[通过] 3.3 `BaseEntity.deleted` 用 `boolean`：OOD L528 定义 `deleted` 为 `boolean` 基本类型。诊断自承为"设计争议而非硬错误"，标注合理。

[通过] 3.4 ai-api 依赖已声明但 Phase 0 无消费方注入策略未明确：OOD L309 声明业务模块依赖 ai-api，但未明确 Phase 0 是否要求业务 Controller 注入 `AiService`。观察有效。

[通过] 4.1 协作规范缺失：Roadmap Phase 0.2 "骨架必备"第一项明确要求"协作规范：分支约定、Commit 格式、PR 模板、Code Review 必查项"。OOD 全文无任何协作规范相关描述。证据充分，严重度标注合理。

[通过] 4.2 新人入门引导文档缺失：Roadmap 验收标准要求"新人按入门引导文档可在 1 小时内完成本地环境搭建"。OOD §9 包含启动命令但无独立引导文档。证据充分。

[通过] 4.3 推荐补齐项缺失：Roadmap Phase 0.2 "推荐补齐"列出 7 项，OOD 缺失 5 项且未声明为何排除。证据充分。

### 2. 逻辑完整性

[通过] 从问题现象到根因的因果链完整，每个诊断项均标注了具体行号并建立了清晰的因果关系。未发现逻辑跳跃。

[问题-轻微] 3.1 将 GlobalExceptionHandler 下沉定性为"逻辑错误"略为过度。OOD L852 明确将此列为有意识的设计决策（理由：Spring 原生机制、无需额外 AOP 配置），且 L320 已使用 `optional=true` 缓解。该问题更准确的定性是"设计权衡争议"而非"逻辑错误"，但不影响诊断结论方向。

[问题-轻微] 3.2 将 AI 降级框架定性为"逻辑错误"亦略显过度。OOD 设计目标 L13 明确"可演进"为骨架原则之一，降级框架作为架构预留有其合理性。诊断指出的 roadmap 张力成立，但定性为"逻辑错误"偏强。不影响诊断结论方向。

### 3. 覆盖完备性

[通过] 诊断覆盖了任务描述中要求的全部四类问题（定义矛盾、事实错误、逻辑错误、偏离路线图），无遗漏。

[通过] 针对路线图偏离的分析细致区分了"骨架必备阻塞项""验收标准项""推荐补齐项"三个层级，覆盖完整。

[通过] 诊断结论整体回答了"问题是什么"和"为什么发生"。

## 质询要点

无严重/一般问题。
