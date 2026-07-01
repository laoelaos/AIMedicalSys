# 再审议判定报告（v18）

## 判定结果

RETRY

## 判定理由

组件B诊断报告识别出6个问题，质询报告结论为LOCATED（全部问题通过证据充分性、逻辑完整性、覆盖完备性、报告必要性四维审查）。问题清单中包含1个严重等级问题（问题1：Phase4ServiceMetaProvider并发安全设计缺陷）和4个一般等级问题（问题2：类图缺少doDegrade方法；问题3：压缩调用缺少模型路由设计；问题4：estimateTokens()方法未定义；问题5：薄适配器异常分类字符串匹配脆弱性）。组件B内部循环实际轮次（1轮）小于最大轮次（12轮）且审查被确认为LOCATED，但审查报告确实包含严重及一般等级问题，满足RETRY条件。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：Phase4ServiceMetaProvider并发安全设计缺陷，多个请求线程同时调用同一单例Bean时，getRetryCount()等方法返回的元数据可能跨请求污染
- **所在位置**：§3.1 Phase4ServiceMetaProvider接口定义（lines 1050-1076）、§4.2 薄适配器成功路径元数据提取（lines 3449-3459）
- **严重程度**：严重
- **改进建议**：将元数据返回方式从服务实例级接口改为请求/响应级绑定（响应DTO内嵌元数据字段），或改为请求级上下文对象通过方法参数传递

- **问题描述**：类图缺少doDegrade方法，v18修订声明声称已补充但实际未修改，类图中AbstractCapabilityExecutor仅包含8个方法，无doDegrade声明
- **所在位置**：§2.3 类图AbstractCapabilityExecutor（lines 448-465）与§4.1 doDegrade()伪代码（lines 3276-3303）
- **严重程度**：一般
- **改进建议**：在§2.3类图AbstractCapabilityExecutor类节点中新增doDegrade方法声明（与§4.1伪代码签名一致），同步修正v18修订说明

- **问题描述**：DiscussionConclusionCapabilityExecutor前置LLM压缩调用缺少模型路由设计，压缩调用发生在实验分流/模板渲染/模型路由之前，此时无法获知目标模型端点和clientType
- **所在位置**：§4.1 DiscussionConclusionCapabilityExecutor.doExecuteInternal()特化伪代码（lines 3307-3388），特别是line 3339
- **严重程度**：一般
- **改进建议**：为压缩调用引入固定轻量模型配置（硬编码endpoint+低成本摘要模型），或在extractVariables()阶段之前允许独立ModelRouter调用并缓存结果

- **问题描述**：estimateTokens()方法未定义，设计文档未给出使用的Tokenizer、中文字符Token换算比例、角色标记开销等具体信息，实现者无法直接编码
- **所在位置**：§4.1 DiscussionConclusionCapabilityExecutor.doExecuteInternal()伪代码（line 3326）
- **严重程度**：一般
- **改进建议**：在§3.11.7或§4.1伪代码补充estimateTokens()的具体实现策略——明确Tokenizer方案（推荐tiktoken）、给出中文医疗文本保守换算比例、说明>3000阈值的决策依据

- **问题描述**：薄适配器异常分类通过字符串数组匹配6个异常类名，Phase4模块重构时重命名异常类或新增Phase模块时，薄适配器将静默错误归类
- **所在位置**：§4.2 薄适配器特化管线伪代码（lines 3424-3445）
- **严重程度**：一般
- **改进建议**：建立Phase4业务异常的公共基类约定，薄适配器通过instanceof匹配；或通过Class.forName()加isInstance()实现可配置的匹配
