# 设计审查报告（v11 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** 测试用例 3 `shouldReturnSingleSlidingWindowCounterInstance`（第 236 行）设计为无 Spring 上下文的纯单元测试（第 239 行明确说明"无 Spring 上下文"），但 `assertSame` 验证 `@Bean` 单例语义必须依赖 Spring CGLIB 代理。在无 Spring 上下文时，直接 `new SecurityConfigPhase1()` 导致 `@Bean` 注解不被处理，两次调用 `config.slidingWindowCounter()` 返回不同的实例，`assertSame` 必然失败。此测试按当前设计无法通过。

## 修改要求

- **[严重]** 移除或重设计测试用例 3。方向：(a) 删除此用例（`@Bean` 单例语义由 Spring 框架保障，无需单元测试验证）；(b) 改用 `assertNotNull` 验证两实例均非 null 即可，放弃 `assertSame` 断言；或 (c) 使用 Spring 上下文测试方式。推荐方案 (a) 或 (b)，与"无 Spring 上下文"的测试策略一致。
