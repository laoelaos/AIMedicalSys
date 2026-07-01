# 再审议判定报告（v22）

## 判定结果

RETRY

## 判定理由

组件B诊断报告识别出10个问题，其中2个严重（问题1：DelegatingLlmChatService装配缺陷；问题2：POM依赖缺失）、6个重要/一般（问题3/4/5/6/7/9）、2个一般（问题8/10）。质询报告因agent异常未产出，依据诊断报告独立判定，审查已确认真实问题存在。组件B内部循环实际轮次1（最大12），未达到最大轮次即因LOCATED问题而提前终止。判定标准要求"审查报告包含严重或一般等级的问题"即RETRY，当前报告含严重等级问题，故判定重新运行。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：DelegatingLlmChatService在SpringAiLlmChatService Bean不可用时的装配缺陷，强制注入导致NoSuchBeanDefinitionException
- **所在位置**：§3.2 DelegatingLlmChatService Bean装配伪代码（第1156-1175行）
- **严重程度**：严重
- **改进建议**：将注入改为ObjectProvider或@Autowired(required=false)，运行时判断可用性

- **问题描述**：ai-impl/pom.xml缺失大量Phase 5必需的编译期/运行期依赖声明（JPA、Actuator/Micrometer、Reactor、Jackson、Caffeine、Guava、HTTP客户端、Web）
- **所在位置**：§2.1目录结构、§3.2/§3.3/§3.5/§3.9等章节
- **严重程度**：严重
- **改进建议**：在§8或新增专节列出全部显式Maven依赖清单，标注作用域和可选性

- **问题描述**：DegradationContext扩展字段与现有applyStrategies()零值上下文的兼容性过渡路径未冻结
- **所在位置**：§3.8 DegradationContext扩展段；DegradationContext.java；FallbackAiService.java:187
- **严重程度**：重要
- **改进建议**：明确无参构造器必须永久保留，新增字段通过Builder/setter赋值

- **问题描述**：底座模块JPA配置与多Repository扫描策略未定义
- **所在位置**：§2.1目录结构、§3.9 AiPlatformConfig定义
- **严重程度**：重要
- **改进建议**：补充JPA配置定义，明确@EntityScan和@EnableJpaRepositories的basePackages声明位置

- **问题描述**：AiPlatformConfig以单一@ConfigurationProperties绑定全部配置组，违反职责分离
- **所在位置**：§3.9 AiPlatformConfig定义（第1894-1977行）
- **严重程度**：重要
- **改进建议**：拆分为多个独立配置属性类，通过@EnableConfigurationProperties逐个引入

- **问题描述**：SlidingWindowMetricsStore的WindowedEvent类型和过期清理机制未定义
- **所在位置**：§3.5 SlidingWindowMetricsStore线程安全段（第1747-1748行）
- **严重程度**：重要
- **改进建议**：补充WindowedEvent字段定义、窗口边界判定规则、快照策略触发条件、与YAML配置映射关系

- **问题描述**：Event驱动刷新机制假设单体部署，未记录设计约束也未提供分布式场景适配方案
- **所在位置**：§3.2/§3.3/§3.4事件刷新段、§10协作边界
- **严重程度**：重要
- **改进建议**：在§7设计决策表记录此约束，§10新增分布式部署兜底段

- **问题描述**：CredentialProvider CACHE_ONLY状态下的TTL延长机制与Caffeine expireAfterWrite的兼容性未说明
- **所在位置**：§3.2 Vault降级状态模型（第1417-1446行）
- **严重程度**：一般
- **改进建议**：补充Caffeine实现方案说明，推荐Expiry接口或重新put刷新TTL

- **问题描述**：混合完整管线与薄适配器的超时层级存在歧义，两个超时值关系未定义
- **所在位置**：§3.1 AbstractCapabilityExecutor.execute() vs §4.2 薄适配器超时控制；§9.5 YAML配置
- **严重程度**：重要
- **改进建议**：明确薄适配器场景capabilityTimeout应设置为thinAdapterTimeout+缓冲值，YAML添加注释约束

- **问题描述**：idx_prompt_version独立索引的查询覆盖度不足
- **所在位置**：§3.5 AiCallLogEntity表索引策略（第1706行）
- **严重程度**：一般
- **改进建议**：替换或补充为复合索引(prompt_version, call_time DESC)，新增(capability_id, prompt_version, call_time DESC)
