# 质量审查报告 — v22（基于 v21 复制）

## 总体评估

经过 22 轮迭代审查，本文档在需求覆盖度、设计深度和细节完备性方面已达到很高水准。以下列出审查发现的剩余质量问题，集中在**图-文一致性**和**多章节伪代码间契约一致性**两个维度。

---

## 发现的问题

### 问题 1（严重）：§2.3 类图 `doDegrade` 签名与 §4.1 伪代码签名不一致

- **问题描述**：§2.3 类图（第 461 行）中 `AbstractCapabilityExecutor` 的 `doDegrade` 方法声明为 7 参数 CallContext 签名：
  `#doDegrade(startTime, degradeReason, request, capabilityId, modelId, sentinelReason, callContext) AiResult~R~~`
  但 §4.1 全文所有 `doDegrade()` 调用点（约 15 处）和方法定义（第 3357 行）**均使用旧的多参数签名**，传入了 14 个独立参数（`departmentId`、`callerRole`、`callerId`、`visitId`、`patientId`、`sessionId`、`inputSummary`、`outputSummary`、`promptVersion` 等）。两处签名不匹配，实现者参考类图将编码出错。

- **所在位置**：§2.3 类图第 461 行 vs §4.1 第 3357 行及所有 `doDegrade()` 调用点（约 15 处）
- **严重程度**：严重
- **改进建议**：v22 修订已明确采纳方案 A（保持旧参数签名，将 CallContext 标注为"Phase 5 第二阶段重构目标"），因此统一路径为：(a) 将 §2.3 类图中 `doDegrade` 签名回退为旧的多参数签名（与 §4.1 伪代码中实际使用的 14 参数一致）；(b) 保持 `CallContext` 类在类图中存在但标注为"Phase 5 第二阶段重构目标"；(c) 同步将类图中 `doExecuteInternal` 签名（当前已使用旧参数）与其他方法签名一并验证一致性

### 问题 2（重要）：§4.2 薄适配器 catch 块缺少两阶段异常检测机制

- **问题描述**：§3.1 的"异常处理规则统一契约"（第 1101–1127 行）和模板方法伪代码（第 1278–1294 行）明确定义了薄适配器的两阶段异常检测机制：第 1 阶段 `instanceof Phase4BusinessException` → 第 2 阶段 `isKnownPhase4BusinessException()` 包路径前缀回退。但 §4.2 薄适配器特化管线伪代码（第 3572–3584 行）的 catch 块**仅包含单阶段检测**：
  ```
  if originalCause instanceof Phase4BusinessException:
      // Phase 4 业务异常处理
  else:
      // 基础设施异常，走降级路径
  ```
  缺少第 2 阶段的 `isKnownPhase4BusinessException()` 回退，导致过渡期内（6 个 Phase 4 模块尚未完成 `Phase4BusinessException` 基类继承改造时）业务异常被误分类为基础设施异常，触发错误降级。

- **所在位置**：§4.2 第 3572–3584 行 vs §3.1 第 1101–1127 行（契约定义）和第 1278–1294 行（模板方法伪代码）
- **严重程度**：重要
- **改进建议**：将 §4.2 catch 块的单阶段 `instanceof` 检测替换为与 §3.1 模板方法一致的两阶段判定：
  ```
  phase4BusinessExceptionDetected = (originalCause instanceof Phase4BusinessException)
  if !phase4BusinessExceptionDetected:
      phase4BusinessExceptionDetected = isKnownPhase4BusinessException(originalCause)
  if phase4BusinessExceptionDetected:
      // 业务异常处理
  else:
      // 基础设施异常处理
  ```

---

## 补充说明

本文档经 22 轮迭代后，在以下方面质量优异：
- 需求响应充分覆盖了 Phase 5 包 G 的全部 OOD 核心要素（类图、核心职责、协作关系、关键接口、状态模型）
- 设计可直接指导编码实现，接口定义（API Surface 状态表、方法签名、伪代码）足以支持下游消费者
- 异常场景、边界条件、非功能性质量均已系统覆盖
- 多实例、分布式部署等演进场景有明确约束记录和兜底方案

以上两个问题解决后，文档可作为稳定的编码实施依据。
