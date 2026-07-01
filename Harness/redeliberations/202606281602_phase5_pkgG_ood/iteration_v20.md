# 再审议判定报告（v20）

## 判定结果

RETRY

## 判定理由

组件B诊断报告经质询审查确认为 LOCATED，共识别 6 个问题，其中 2 个严重（查询 1：SlidingWindowMetricsStore 数据完整性；查询 2：Phase4BusinessException 过渡期兼容性），2 个重要（查询 3：LocalRuleFallback 空指针风险；查询 6：16 参数工厂方法编码风险），2 个一般（查询 4：Spring AI 条件类名准确性；查询 5：超时降级原因二义性）。严重等级问题构成 RETRY 条件。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：SlidingWindowMetricsStore 标注 @RefreshScope 将清空全部滑动窗口数据，刷新后熔断器保护失效 60 秒
- **所在位置**：§3.9「运行时配置热加载机制」表（line 2548）
- **严重程度**：严重
- **改进建议**：移除 @RefreshScope，改用 AtomicLong + 定时刷新方式（方案 A）；或声明为静态配置重启生效（方案 B）；或拆分配置层与存储层（方案 C）

- **问题描述**：Phase4BusinessException catch 块缺少过渡期回退，未迁移的 Phase 4 模块业务异常被误分类为基础设施异常并触发错误降级
- **所在位置**：§3.1 line 1097-1101、§4.2 line 3462-3479
- **严重程度**：严重
- **改进建议**：在 instanceof 主分支前增加过渡期字符串匹配回退机制

- **问题描述**：LocalRuleFallback.fallback() 返回值未加 null 保护，result 为 null 时 NPE 导致 doDegrade() 异常退出
- **所在位置**：§4.1 doDegrade() line 3287-3288
- **严重程度**：重要（等价于 一般）
- **改进建议**：增加 null 守卫，明确 LocalRuleFallback 接口 @return 非 null 约定

- **问题描述**：16 参数工厂方法在实施阶段的高概率编码错误风险，参数顺序跨方法不一致
- **所在位置**：§3.5 line 2081-2098、§3.1 line 1409-1424、§4.1 line 3274
- **严重程度**：重要（等价于 一般）
- **改进建议**：Phase 5 实施期立即引入 CallContext 值对象，降维参数数量

- **问题描述**：@ConditionalOnClass 引用的 Spring AI ChatModel 包路径可能不准确，条件判断永远不匹配
- **所在位置**：§3.2 line 1486、§3.2 AiPlatformConfig line 1510
- **严重程度**：一般
- **改进建议**：同时兼容 chat.model.ChatModel 和 chat.ChatModel 两种包路径

- **问题描述**：前置压缩失败与主流程超时叠加时降级原因无法区分根因
- **所在位置**：§4.1 DiscussionConclusionCapabilityExecutor 特化伪代码 line 3317-3422
- **严重程度**：一般
- **改进建议**：在 exceptionally() 回调中细化超时降级原因分类
