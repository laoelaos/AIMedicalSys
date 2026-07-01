# 再审议判定报告（v12）

## 判定结果

RETRY

## 判定理由

组件B诊断报告识别出8个问题，内部循环1轮即达成LOCATED（质询确认全部问题有效，无驳回事由）。对照判定标准映射严重程度：

- **严重**：Issue 1（Maven依赖作用域二义性可致运行期NoClassDefFoundError）、Issue 2（CompletableFuture.cancel(true)事实错误——无实际中止效果）、Issue 3（doDegrade()缺少promptVersion参数致降级场景实验分组信息永久丢失）
- **一般**：Issue 4（同步抛异常破坏CompletableFuture统一契约）、Issue 5（ExperimentAssignment构造方式未定义）、Issue 6（ModelRoute密钥获取接口未定义）
- **轻微**：Issue 7（retryCount硬编码0）、Issue 8（实验数据生命周期未涉及）

诊断报告包含严重和一般等级问题，符合RETRY条件。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：薄适配器Maven依赖作用域未做确定性决策，开发者无法判断应使用test还是compile作用域
- **所在位置**：§2.2 依赖规则段
- **严重程度**：严重
- **改进建议**：冻结选择——推荐provided或compile并给出理由；若选compile需在§10记录耦合治理规则；若倾向松耦合，在ai-impl内定义SPI接口消除对Phase 4的直接Maven依赖

- **问题描述**：薄适配器超时路径下CompletableFuture.cancel(true)无法真正中止Phase 4服务执行，属于事实错误
- **所在位置**：§3.1 薄适配器伪代码第738行；§9.5 YAML默认超时30s
- **严重程度**：严重
- **改进建议**：删除cancel(true)调用，改用WARN日志注明"服务将继续执行至完成"；若需真正中止，需Phase 4接口支持可中断模式并在§7记录取舍

- **问题描述**：doDegrade()方法签名缺少promptVersion参数，降级记录中永远丢失实验分组信息
- **所在位置**：§4.1 doDegrade()方法定义第1508-1527行；三处调用点
- **严重程度**：严重
- **改进建议**：doDegrade()签名增加Integer promptVersion参数，调用点传入assignment.getTargetPromptVersion()；同步更新§2.3类图

- **问题描述**：AiOrchestrator.handle()中未注册能力的异常在try块外直接throw，破坏CompletableFuture异步契约
- **所在位置**：§4.1 AiOrchestrator.handle()第1386行
- **严重程度**：一般
- **改进建议**：将null检查移入try块，走CompletableFuture.completedFuture(AiResult.failure(...))路径，或使用completedExceptionally()包装

- **问题描述**：ExperimentAssignment仅以字段表形式定义，未定义构造器/Builder/工厂方法
- **所在位置**：§3.4 ExperimentAssignment段落
- **严重程度**：一般
- **改进建议**：显式定义全参数构造器+无参默认工厂方法（所有字段null/default），与无实验命中返回值语义对齐

- **问题描述**：ModelRoute密钥获取接口未定义，LlmClient实现者无法推断正确实现
- **所在位置**：§3.2 ModelRoute字段扩展表第1008-1011行
- **严重程度**：一般
- **改进建议**：新增CredentialProvider接口定义，明确密钥缓存策略（首次成功缓存5分钟），定义Vault不可达回退行为
