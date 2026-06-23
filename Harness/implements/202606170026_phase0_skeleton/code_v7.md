# 实现报告（v7 r2）

## 概述

实现 ai-impl 模块全部源文件，包含 MockAiService（Mock 占位实现）、NoOpDegradationStrategy（默认降级策略）、FallbackAiService（降级装饰器）及对应单元测试。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | backend/ai-impl/pom.xml | 添加 ai-api（compile）、common（compile）、spring-boot-starter（compile）、spring-boot-starter-test（test）依赖 |
| 新建 | .../impl/mock/MockAiService.java | 实现 AiService 13 个方法，返回固定 Mock 占位数据，triage 含 mock_departmentName/mock_reason |
| 新建 | .../impl/degradation/NoOpDegradationStrategy.java | 实现 DegradationStrategy，shouldDegrade 始终返回 false |
| 新建 | .../impl/fallback/FallbackAiService.java | AiService 装饰器，注入委托和策略列表，遍历策略判定降级，兜底保护 |
| 新建 | .../impl/mock/MockAiServiceTest.java | 验证 13 方法返回非 null CompletableFuture、success=true、degraded=false、data!=null |
| 新建 | .../impl/degradation/NoOpDegradationStrategyTest.java | 验证 shouldDegrade 始终返回 false |
| 新建 | .../impl/fallback/FallbackAiServiceTest.java | 验证正常委托、降级触发、兜底保护、自身排除 |

## 编译验证

未执行编译验证（环境无 Maven 命令）。

## 设计偏差说明

| 设计规格 | 偏差原因 | 实际处理 |
|---------|---------|---------|
| ai-impl/pom.xml 仅含 ai-api/common/spring-boot-starter-test | @ConditionalOnProperty/@ConditionalOnMissingBean 来自 spring-boot-autoconfigure，需要 spring-boot-starter 作为依赖 | 额外添加 spring-boot-starter（compile scope） |
| FallbackAiService 不应含辅助方法 | 13 个方法降级策略判定逻辑完全一致，提取 applyStrategies 私有方法消除重复 | 保留私有辅助方法 applyStrategies |

## 修订说明（v7 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| FallbackAiService 13 个方法使用 .join() 阻塞等待委托 CompletableFuture，破坏异步契约 | 改为 .thenApply(this::applyStrategies) future 组合；applyStrategies 返回类型从 CompletableFuture<AiResult<T>> 改为 AiResult<T>，消除冗余包装 |
| MockAiServiceTest 方法名后缀 ShouldReturnSuccessResult 与设计 ShouldReturnMockData 不一致 | 13 个方法名统一重命名为 xxxShouldReturnMockData |
| FallbackAiServiceTest 方法名 shouldReturnDegradedWhenNoDelegatesAvailable 与设计不一致 | 重命名为 shouldReturnFallbackResultWhenNoDelegateAvailable |
| FallbackAiService.firstEmptyDelegateCall 非线程安全 | 将 boolean 改为 AtomicBoolean，使用 getAndSet(false) 保证原子性和可见性 |
