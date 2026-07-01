# 审查范围界定

## 1. 审查目标

对 squash 合并的 `202606291258_C3_DE` 分支到 `feat/task3` 进行 OOD 实现层面的代码审查，验证 Phase 2/3 包C（智能分诊）、包D-AI1（处方审核）、包D-AI2（病历生成）、包E（辅助开方）四个业务包的设计到实现的一致性、契约完整性、关键行为契约达成度，并识别可改进点。

## 2. 审查依据

**主依据**：`Docs/07_ood_phase2_C_3_DE.md`（Phase 2/3 包C/D-AI1/D-AI2/E OOD 架构级设计方案，v15 终稿）

**辅助参考**：
- `Harness/redeliberations/202606281422_phase23_ood/a_v19_copy_from_v18.md`（23 轮定向修订的草案基线）
- `Harness/redeliberations/202606271627_phase5_pkgG_ood/a_v22_copy_from_v21.md`（Phase 5 兼容性约束）
- 需求文档（若模块引用，章节定位见设计文档 §1.1a、§3.1-§3.4 各 DTO 引用处）

## 3. 审查范围

### 3.1 已暂存变更规模

- **文件总数**：376 个
- **新增代码行**：32,332 行（main 137 个 + test 99 个 + 其他 140 个）
- **涉及模块**：
  - `common`（DosageStandard 实体迁移 + pom 测试）
  - `ai-api`（AiResultFactory + 4 类 DTO + DTO 测试）
  - `common-module-api`（DoctorFacade / DrugFacade / VisitFacade / RegistrationEvent / 3 个 Store 接口 / Store 实现）
  - `consultation`（包C 智能分诊全模块）
  - `prescription`（包D-AI1 处方审核 + 包E 辅助开方全模块）
  - `medical-record`（包D-AI2 病历生成全模块）
  - `application`（启动聚合层、Mock 配置）

### 3.2 审查重点维度

依据设计文档章节，覆盖以下重点：
- §1.2 整体架构思路 — 模块划分、依赖方向
- §1.3 核心抽象 — 4 个包的接口/类/DTO 完整性与设计文档字段级对齐
- §2.1 目录结构 — 包路径、类路径、文件命名与设计文档一致
- §2.2 模块依赖 — 不允许跨模块循环依赖、Facade 模式、事件驱动
- §2.3 AiService 接口 — 4 个方法签名、泛型绑定、超时配置、降级判定
- §3.1-§3.4 核心行为契约 — 降级链、并发安全、原子操作、TTL 清理
- §6.1 定时任务 — TTL 清理、DeadLetter 补偿、缓存刷新
- §9 事件驱动缓存刷新 — 5 类 ChangeEvent 与对应消费端
- §10 错误码、§11 审计与可追溯性

## 4. 排除范围

- **本设计不涉及的 Phase 4 范围**：DrugInteractionRule 运行时启用、流式病历输出、SuggestionConfirmation 状态跟踪、DosageStandard 缓存（DosageStandardChangeEvent 已声明但 Phase 2/3 无运行时缓存消费端）——这些仅作预留骨架/标注，审查时标注为"已知 Phase 边界"不视为问题。
- **AI 底座实现细节**：MockAiService 仅作 Mock 行为契约审查（§2.3 行为模式），不审查 Mock 数据字段值的具体业务正确性。
- **pom.xml 模块注册**：仅审查新增模块的模块依赖、依赖方向；pom 模板的正确性已由 ParentPomTest 覆盖。
- **Harness/implements 验证报告**：本轮只审查源代码，不审查 v1-v15 的 verify/test 报告（这些是设计过程产物）。

## 5. 轮次规划

依据设计文档覆盖范围、模块规模、跨切关注点，**规划 3 轮 × 3 个并行 agent = 9 个审查任务**。

| 轮次 | 主题 | Agent 1 | Agent 2 | Agent 3 |
|------|------|---------|---------|---------|
| R1 | 三个业务模块的核心实现 | consultation 模块 | prescription 模块 | medical-record 模块 |
| R2 | 横切关注点 | Store 抽象 + 并发安全 | AI 集成 + 降级策略 | 跨模块事件 + 门面 |
| R3 | 契约完整性与质量 | DTO 字段对齐 + Converter | 错误码 + 异常处理 | 测试覆盖 + 可追溯性 |

每轮 3 个 agent 并行执行，每 agent 独立写一份 `review_v{N}_{agent}.md`（N=轮次，agent=1/2/3）。

## 6. 暂存区锚定

所有变更已通过 `git merge --squash 202606291258_C3_DE` 暂存。审查文件列表以 `git diff --cached --name-only` 输出为准。
