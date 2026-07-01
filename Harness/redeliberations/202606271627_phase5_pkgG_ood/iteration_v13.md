# 再审议判定报告（v13）

## 判定结果

RETRY

## 判定理由

组件B诊断报告经质询确认通过（LOCATED），7个问题均被认定为有效。其中问题1~5严重程度相当于"一般"等级（影响产出完整度与可观测性的非致命缺陷），问题6~7明确标记为"一般"。根据判定标准，审查报告包含一般等级的问题，应重新运行组件A。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：AiOrchestrator.handle() catch 块中薄适配器场景的就诊上下文提取失效，catch 块依赖 instanceof AiRequestBase 判断但薄适配器 DTO 不继承该类，导致部门/就诊/患者/会话ID全部传入 null
- **所在位置**：§4.1 第 1465-1474 行、第 1498-1501 行
- **严重程度**：一般
- **改进建议**：在 catch 块增加 Phase 4 DTO 兼容提取路径，通过 RequestContextHolder/HTTP Header 独立提取就诊上下文

- **问题描述**：ParseFailure 降级路径丢失原始 LLM 响应摘要，doDegrade() 签名不含 outputSummary 参数，降级记录中对应字段为空
- **所在位置**：§4.1 第 1564-1569 行、第 1584-1601 行
- **严重程度**：一般
- **改进建议**：doDegrade() 签名增加可选 String outputSummary 参数，解析失败降级时传入 LLM 原始响应摘要

- **问题描述**：Prompt 模板渲染契约未定义"指定版本已废弃"的回退行为，DEPRECATED 版本的处理方式未冻结
- **所在位置**：§4.4 第 1636-1637 行
- **严重程度**：一般
- **改进建议**：在渲染契约中增加规则：promptVersion 对应版本为 DEPRECATED 时输出 WARN 日志并回退到 ACTIVE 模板

- **问题描述**：薄适配器 doExecuteInternal() 中 ExecutionException 包裹的原始异常类型丢失，降级原因只记录 "ExecutionException" 而非 NPE 等原始类型
- **所在位置**：§3.1 第 751-753 行区域
- **严重程度**：一般
- **改进建议**：在 catch(Exception) 中提取 getCause() 的类型名拼接到降级原因中

- **问题描述**：Experiment PAUSED 状态下 assign() 的返回值未冻结，当前依赖"检索不到"的隐式行为，未来缓存预热等变更易破坏契约
- **所在位置**：§3.4 第 1104 行 vs §4.3 第 1622-1625 行
- **严重程度**：一般
- **改进建议**：在 §4.3 显式增加 PAUSED 状态分支，过滤掉 status=PAUSED 的实验

- **问题描述**：PromptTemplate 状态模型缺少 DEPRECATED→ACTIVE 回退路径，紧急回滚场景下需新建 DRAFT 版本再发布
- **所在位置**：§3.3 第 1081-1086 行
- **严重程度**：一般
- **改进建议**：增加 DEPRECATED→ACTIVE 转换路径或在回滚策略文档中说明工作流

- **问题描述**：不可变 DTO 与防御性拷贝共存时 ObjectMapper 兼容性未说明，按不可变推荐设计但未标注 @JsonCreator 的 DTO 会导致 convertValue() 抛出异常
- **所在位置**：§3.1 第 626 行 vs §4.1 第 1498 行
- **严重程度**：一般
- **改进建议**：在 §3.1 补充 Jackson 兼容反序列化要求，或对不可变 DTO 跳过拷贝步骤
