# 再审议判定报告（v1）

## 判定结果

RETRY

## 判定理由

组件B诊断报告（v2）识别出 10 项质量问题，其中 P1（严重）7 项、P2（一般）3 项。质询报告结论为 LOCATED，表明审查结论已被确认有效且逻辑自洽、证据充分。因诊断报告包含严重及一般等级问题，符合 RETRY 条件，需要重新运行组件A进行修订。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：DialogueSession 不可变声明与可变追加操作的逻辑矛盾
- **所在位置**：§3.1 DialogueSession — "为何使用不可变 class" 说明 + 协作描述
- **严重程度**：严重
- **改进建议**：选择以下任一路径并更新文档：(a) 改为可变 class，DialogueSession 内部状态可变，DialogueSessionManager 负责并发访问控制，删除不可变理由；(b) 保持不可变，显式补充 copy-on-write 机制：DialogueSession.withNewRound(...) 返回新实例，DialogueSessionManager 负责原子替换

- **问题描述**：包E 异步 AI 建议缺少消费路径
- **所在位置**：§6.3 "包E 的异步 AI 建议" + §3.4 PrescriptionAssistController
- **严重程度**：严重
- **改进建议**：补充异步 AI 建议的消费机制，至少包括：(a) 定义 GET /api/prescription/assist/suggestion/{taskId} 查询端点及其响应结构；(b) 或定义事件推送机制（WebSocket/SSE）以及前端订阅方式；(c) 明确 check-dose 响应中是否需要返回 taskId 用于后续查询

- **问题描述**：多轮分诊中对话历史的维护责任与一致性不明确
- **所在位置**：§4.1 "多轮分诊流程" + §3.1 TriageRequest DTO 定义
- **严重程度**：严重
- **改进建议**：明确对话历史的单一真相来源：(a) 推荐方案：服务端 DialogueSession 为单一真相来源，前端只需在首轮请求时携带 chiefComplaint，后续请求仅携带 sessionId，TriageRequest.history 字段移至服务端内部不暴露至 API 契约；(b) 如确需前端维护历史，则需明确 DialogueSession 只维护状态元数据而不存储对话内容，并删除"会话上下文"的职责描述

- **问题描述**：DosageCheckRequest 缺少给药途径参数
- **所在位置**：§3.4 DosageCheckRequest DTO 声明
- **严重程度**：严重
- **改进建议**：在 DosageCheckRequest 中增加 routeOfAdministration 字段（枚举类型，建议值：ORAL、IV、IM、TOPICAL 等），DosageStandard 实体也需相应增加给药途径维度，查询时 drugCode + routeOfAdministration 联合定位唯一阈值记录

- **问题描述**：prescription 模块内 DosageStandard 实体的写权限归属未定义
- **所在位置**：§2.2 "包D-AI1 与包E 的强耦合处理"
- **严重程度**：严重
- **改进建议**：(a) 明确定义 DosageStandard 的数据归属：推荐由管理员端（Phase 5）或独立的数据维护层作为唯一写入者，D-AI1 和 E 仅持有读取权限；(b) 若确有写入需求，定义明确的写入协调机制（如事件驱动的版本递增或最后写入者胜出策略），并补充写入场景的描述

- **问题描述**：分诊规则配置变更的生效机制未定义
- **所在位置**：§3.1 TriageRuleEngine + §7 设计决策 "分诊规则源"
- **严重程度**：严重
- **改进建议**：(a) 明确定义规则缓存策略：若需热加载，在 TriageRuleEngine 实现中引入定时刷新或通过 ApplicationEventPublisher 发布规则变更事件触发缓存失效；(b) 若接受重启生效，则修正 §7 的选型理由描述，删除"非开发人员动态调整"的表述；(c) 补充规则变更的前端/管理员操作路径说明

- **问题描述**：科室模板配置的 CRUD 管理和默认兜底缺失
- **所在位置**：§3.3 DepartmentTemplateConfig / TemplateConfigManager + §2.1 目录结构 DeptTemplateConfigRepository
- **严重程度**：严重
- **改进建议**：(a) 补充默认模板兜底方案：在系统中维护一个 DEFAULT 科室条目，任何未匹配科室标识时回退到该通用模板；(b) 定义 getTemplate(departmentId) 的契约签名（返回 Optional<DepartmentTemplateConfig> 或抛 BusinessException）；(c) 明确模板管理接口；(d) 补充初始模板数据集

- **问题描述**：对话会话内存存储未覆盖服务重启场景
- **所在位置**：§3.1 DialogueSessionManager + §7 设计决策 "多轮对话存储" 条目
- **严重程度**：一般
- **改进建议**：(a) 在 DialogueSessionManager.findOrCreate 中明确 "session 不存在" 时的处理策略（如返回 Optional.empty() 或抛 BusinessException）；(b) 补充统一的错误码 TRIAGE_SESSION_EXPIRED 及前端提示文案；(c) 在 API 文档中说明 session 有效期的限制

- **问题描述**：新模块依赖声明未包含 common-module-api
- **所在位置**：§1.2 "各模块仅依赖 common 和 ai-api" + §2.2 依赖关系图
- **严重程度**：一般
- **改进建议**：在 §1.2 的依赖声明和 §2.2 的依赖关系图中，补充三个新模块对 common-module-api 的依赖（compile scope），与现有 patient/doctor/admin 模块保持一致

- **问题描述**：剂量标准数据初始化方案和编码规范缺失
- **所在位置**：§3.4 DosageThresholdService + DosageStandard entity + DosageStandardRepository
- **严重程度**：一般
- **改进建议**：(a) 补充剂量标准数据的初始化方案，至少包含开发/测试用的种子数据 SQL 脚本路径和基础药品条目；(b) 明确药品编码规范；(c) 在 DosageThresholdService 中补充单位一致性校验逻辑
