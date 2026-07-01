# 质量审查诊断报告（v1）

## 审查范围

- **审查对象**：Phase 5 包 G — AI 进阶底座 架构级 OOD 设计方案（v1.0）
- **审查视角**：实际落地视角，侧重内部审议未充分覆盖的维度（需求响应充分度、整体深度和完整性、事实错误与逻辑矛盾）
- **审查基准**：用户需求 vs 设计产出，可编码实现性，接口定义可消费性，异常/边界覆盖度

---

## 发现的问题

### 问题 1：§2.3 类图中 `doDegrade` 方法签名缺少 `modelId` 参数，与 §4.1 伪代码不一致

- **问题描述**：§2.3 `AbstractCapabilityExecutor` 类图（line 353）中 `doDegrade` 方法仅有 14 个参数（最后一个参数为 `promptVersion`），但 §4.1 line 2962 的伪代码定义和全部 14 处调用点均使用 15 个参数（第 15 个参数 `modelId`）。此不一致是 v9 修订 E（为降级路径增加 `modelId` 传入）的残留——类图未被同步更新。
- **所在位置**：§2.3 类图，line 353（`AbstractCapabilityExecutor` 的 `#doDegrade(...)` 方法条目）
- **严重程度**：重要
- **改进建议**：在 `AbstractCapabilityExecutor` 类图的 `doDegrade` 方法签名末尾追加 `modelId: String` 参数，使类图与 §4.1 伪代码对齐。同时检查类图中其他方法签名是否与正文伪代码完全一致。

---

### 问题 2：§3.1 薄适配器构造器中 `super()` 调用参数数量与 `AbstractCapabilityExecutor` 构造器签名不匹配，无法编译

- **问题描述**：`DiagnosisCapabilityExecutor`（line 964-967）的构造器通过 `super()` 传递 9 个参数，但 `AbstractCapabilityExecutor` 构造器（line 1257-1275）定义了 12 个正式参数。缺少 `capabilityTimeoutConfig`、`thinAdapterTimeout`、`thinAdapterPerCapabilityConfig` 三个参数。直接按此伪代码编码将产生编译错误。缺失的三个参数恰好是薄适配器超时机制的核心依赖——没有它们，`doExecuteInternal()` 中的 `effectiveThinAdapterTimeout` 解析逻辑（line 3014-3015）无法运行。
- **所在位置**：§3.1 line 964-967（`DiagnosisCapabilityExecutor` 构造器及 `super()` 调用）
- **严重程度**：严重
- **改进建议**：在 `super()` 调用中补全缺失的三个参数，与 §4.2 薄适配器 `doExecuteInternal()` 中使用的 `thinAdapterTimeout`/`thinAdapterPerCapabilityConfig` 保持一致。同时同步修正所有 6 个薄适配器子类的构造器示例。

---

### 问题 3：§4.2 薄适配器 catch 块引用未定义/未确认的 `BusinessException` 异常类型

- **问题描述**：§4.2 line 3025 的薄适配器伪代码中 catch `BusinessException e`，但 §3.1 line 925-933 的"Phase 4 模块异常契约"表列出的 6 个 Phase 4 业务模块各有独立异常类型（`DiagnosisException`、`InspectionException`、`LabTestException`、`ImageAnalysisException`、`ExaminationException`、`ExecutionOrderException`），文档未验证这些异常是否存在共同的 `BusinessException` 父类。若不存在共同父类，此 catch 将：
  1. 若 `BusinessException` 不在 classpath 中 → 编译失败
  2. 若 `BusinessException` 存在但 6 个模块异常不继承它 → 异常不会被捕获，落入下方 `catch (Exception e)` 被错误地走基础设施降级路径
- **所在位置**：§4.2 line 3025`catch (BusinessException e)`，§3.1 line 925-933 异常契约表
- **严重程度**：严重
- **改进建议**：方案 A：确认 Phase 4 模块是否存在公共 `BusinessException` 基类，若存在则保持当前伪代码并在 §3.1 表中补充标注。方案 B：将 catch 类型改为 `catch (Exception e)`，在块内通过 `instanceof` 匹配 6 个已知异常类来区分业务异常与基础设施异常，避免依赖未确认的异常类型层次结构。

---

## 整体质量评价

文档整体质量较高，经过 9 轮迭代审查后，主要的结构性缺陷、概念矛盾和管理型问题（如修订说明混入正文、章节编号混乱、DTO 字段状态标注缺失等）均已得到有效修正。§1–§11 正文内容完整且逻辑自洽，覆盖了需求中列出的类图、核心职责、协作关系、关键接口、状态模型等全部 OOD 核心要素，伪代码细节丰富，具备直接指导编码实现的能力。

上述 3 个问题属于本轮清理的残留——类图同步遗漏与构造函数伪代码细节不一致，均不影响设计主旨的正确性，但会直接干扰编码阶段的实现准确性，属于修复者可明确定位和修正的具体问题。

---

## 未发现的其他维度评估

| 评估维度 | 结论 |
|---------|------|
| **需求响应充分度** | 文档完全覆盖用户需求的 4 项要求：引用 Phase0/Phase1ABD 风格一致性（§1.2 列出 5 条具体规则）、完整 OOD 设计（§1–§11）、核心要素齐全 |
| **深度与完整性** | 从架构总览到令牌级行为契约，3754 行的详细度足以指导编码。状态机覆盖 5 个组件（CircuitBreaker/CredentialProvider/EndpointHealth/PromptTemplate/Experiment），异常场景覆盖 11 类（§5.1），并发竞争测试覆盖 5 个场景（§11.5） |
| **异常场景与边界条件** | §5.1 错误分类表覆盖了所有管线步骤的预期异常；防御性拷贝、降级预检前置、thread-safe 契约均有定义 |
| **接口定义可消费性** | 所有核心抽象均有类型形态标注、方法签名、协作对象和"为何使用 X 而非 Y"决策理由 |
