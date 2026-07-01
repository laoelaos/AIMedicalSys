# 计划审查报告（v10 r1）

## 审查结果
REJECTED

## 发现

### [一般] 缺少测试修改计划
10 个子项均未提及任何测试文件的修改说明。既往 R1-R9 所有轮次均包含明确的测试文件修改清单（新增测试用例、修改断言、mock 策略调整等）。R10 至少以下子项需要测试适配：
- 8a (T9): catch 行为变更需验证 log.warn + fallback 行为
- 8b (T10): instanceof 校验需验证非法类型处理
- 8c (T14): double→BigDecimal 类型变更将破坏现有测试断言（如 assertEquals(double, double) 改为 assertEquals(BigDecimal, BigDecimal)）
- 8d (T15): 新 ID 格式需测试验证
- 8e (T28): 删除的 BLOCK 分支需确认无测试覆盖
- 8f (T30): null 返回路径需测试覆盖
- 8g (T31): 否定前缀跳过 + 单词边界匹配需新增测试用例
- 8h (T35): unit/weight 映射新增需测试验证
- 8i (P12): @ConditionalOnProperty 注解需测试验证
- 8j (P15): reason 透传需测试验证调用链

无测试修改计划，verify 阶段将因测试失败或测试覆盖不足而无法通过。

### [一般] 8c (T14) double→BigDecimal 类型变更级联影响未分析
DTO 字段类型从 `double` 改为 `BigDecimal` 产生以下影响，计划均未提及：
- getter 返回类型从 `double` 变为 `BigDecimal` → 所有调用方出现编译错误
- JSON 序列化行为变化（double → `1.0`, BigDecimal → `1` 或科学计数法），需确认 Jackson 配置
- equals/hashCode/toString 是否使用 `@Data`/`@EqualsAndHashCode` 自动生成？若手动编写需同步修改
- `AiCheckItem` 对应字段类型是否匹配需确认

### [一般] 多个子项实施方案不完整，与 R5/R6 详细程度差距过大
R5 NEW / R6 NEW 均包含精准的问题定位行号、修改伪码、关键决策说明，R10 NEW 仅罗列问题描述（与诊断报告重复），缺少具体修改方案：

- **8d (T15)**: UUID 方案未明确（全量 36 字符 vs 前 8 字符截断），后者碰撞概率需评估
- **8f (T30)**: `findBestMatch` 可返回 null 后，所有调用方（包括 DosageLimitRule 内部及外部使用者）如何适配 null 未分析；`standards.isEmpty()` 时返回 null，原回退 `standards.get(0)` 的 NullPointerException 被替换为另一路径的 NPE，需全局分析
- **8j (P15)**: "透传具体规则触发的告警原因"未说明方法签名变更、Controller 路由参数变化、以及已有调用方的兼容处理——可能违反 task_v10 "不涉及 API 变更" 前提
