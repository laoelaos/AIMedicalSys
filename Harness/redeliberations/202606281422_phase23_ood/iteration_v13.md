# 再审议判定报告（v13）

## 判定结果

RETRY

## 判定理由

组件B诊断报告（v2）共识别出 8 个质量问题，包括 2 个**严重**等级（问题 1：`@DltHandler` 技术栈不匹配、问题 2：DrugCompositionDict 成分编码缺失）和 6 个**一般**等级（问题 3-8）。质询报告确认全部问题 LOCATED，证据充分、逻辑完整、覆盖完备。根据判定标准，存在严重或一般等级问题，判定为 RETRY。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：`@DltHandler` 与 `@TransactionalEventListener` 技术栈不匹配，死信处理方案不可执行，直接指导编码将导致编译错误
- **所在位置**：§2.2 "跨模块事件传递机制"——RegistrationEvent 消费失败补偿策略
- **严重程度**：严重
- **改进建议**：将 `@DltHandler` 替换为 `@Recover`（Spring Retry），在 recover 方法中手动写入 dead_letter_event 表；或引入消息中间件后全面升级事件传递机制

- **问题描述**：DuplicateCheckRule 依赖的 DrugCompositionDict 缺少成分编码定义，字符串匹配会导致临床漏报和假阳性
- **所在位置**：§3.2 DuplicateCheckRule 逻辑描述 + §2.1 DrugCompositionDict 实体定义
- **严重程度**：严重
- **改进建议**：在 ingredients JSON 中增加 ingredientCode 字段（统一编码），补充检测边界说明

- **问题描述**：PrescriptionDraftContext TTL 清理机制缺少扫描实现方案，异常退出场景下 CRITICAL 标记会残留至多 60 分钟
- **所在位置**：§3.4 PrescriptionDraftContext 生命周期管理
- **严重程度**：一般
- **改进建议**：补充 ScheduledExecutorService 定期扫描机制，或采用懒清理策略并补充说明

- **问题描述**：dead_letter_event 表和定时补偿任务的模块归属未定义，影响编码阶段模块划分
- **所在位置**：§2.2 "跨模块事件传递机制"——死信事件表与补偿任务
- **严重程度**：一般
- **改进建议**：补充实体归属（建议 consultation 模块）、补偿任务类路径及 Repository 接口声明

- **问题描述**：SpecialPopulationDosageRule 年龄阈值硬编码，未暴露为配置参数
- **所在位置**：§3.2 SpecialPopulationDosageRule 描述
- **严重程度**：一般
- **改进建议**：提取为 application.yml 配置项，或在设计决策中显式说明硬编码决策及适用场景

- **问题描述**：AiResult 超时降级重载的泛型参数与 ai-api DTO 空壳类的时序依赖在接口定义处未标注
- **所在位置**：§2.3 AiService 接口定义（AiResult 超时降级重载）
- **严重程度**：一般
- **改进建议**：在 §2.3 补充约束标记或显式交叉引用 §10 的时序依赖说明

- **问题描述**：配置变更事件丢失补偿机制在不同实体上覆盖不一致，事件类定义缺失或描述需修正
- **所在位置**：§9.3 规则管理接口描述 + §3.2 数据来源说明
- **严重程度**：一般
- **改进建议**：明确是否需要事件驱动缓存失效，统一修正描述或补全事件类定义

- **问题描述**：DosageAlertLevel/AlertSeverity/AllergyWarningSeverity 三个枚举命名约定不一致，存在编码阶段类型误用风险
- **所在位置**：§1.3 包E/包D-AI1 枚举条目
- **严重程度**：一般
- **改进建议**：统一枚举值命名风格（WARN→WARNING, HIGH→CRITICAL），调整排序方向，评估是否可复用同一枚举
