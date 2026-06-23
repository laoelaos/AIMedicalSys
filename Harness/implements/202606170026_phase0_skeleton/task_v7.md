# 任务指令（v7）

## 动作
NEW

## 任务描述
实现 AI 能力实现子模块（ai-impl）的全部源文件，包含 MockAiService 占位实现、NoOpDegradationStrategy 默认降级策略、FallbackAiService 降级装饰器及单元测试。

### 需要创建/修改的文件

**POM 依赖更新**：
- `backend/ai-impl/pom.xml` — 添加 ai-api（compile scope）、common（compile scope）、spring-boot-starter-test（test scope）依赖

**类型文件**：
| 文件路径 | 类型形态 | 职责 |
|---------|---------|------|
| `.../impl/mock/MockAiService.java` | class | 实现 AiService 13 个方法，返回固定结构 Mock 占位数据，用 CompletableFuture.completedFuture() 包装 |
| `.../impl/degradation/NoOpDegradationStrategy.java` | class | 实现 DegradationStrategy，shouldDegrade() 始终返回 false |
| `.../impl/fallback/FallbackAiService.java` | class | AiService 装饰器，注入 List\<AiService\> 和 List\<DegradationStrategy\>，遍历策略判定降级，兜底保护 |

**测试文件**：
| 文件路径 | 测试内容 |
|---------|---------|
| `.../impl/mock/MockAiServiceTest.java` | MockAiService 13 方法返回非 null 的 CompletableFuture、success=true、degraded=false、data 不为 null |
| `.../impl/degradation/NoOpDegradationStrategyTest.java` | NoOpDegradationStrategy 始终返回 false |
| `.../impl/fallback/FallbackAiServiceTest.java` | FallbackAiService 正常委托、降级触发、兜底保护（无可用委托时返回 degraded AiResult） |

### MockAiService 实现规范

**包路径**：`com.aimedical.modules.ai.impl.mock`

**装配条件**：
- 标注 `@Service` 和 `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)`
- 当 `ai.mock.enabled=true`（默认或未配置）时，MockAiService 被注册为 Spring Bean

**方法行为**：
- 所有 13 个方法返回 `CompletableFuture.completedFuture(AiResult.success(mockData))`
- `AiResult.success(data)` 中：`success=true`, `degraded=false`, `errorCode=null`, `fallbackReason=null`
- Mock 数据填充规则：
  - `TriageResponse`：`recommendedDepartments` 含 1 条 `RecommendedDepartment`（`departmentName="mock_departmentName"`），`reason="mock_reason"`
  - 其余 12 组 Response DTO：new 空实例直接传入 `AiResult.success(new XxxResponse())`

### NoOpDegradationStrategy 实现规范

**包路径**：`com.aimedical.modules.ai.impl.degradation`

**装配条件**：
- 标注 `@Component` 和 `@ConditionalOnMissingBean(DegradationStrategy.class)`
- `shouldDegrade(DegradationContext context)` 始终返回 `false`

### FallbackAiService 实现规范

**包路径**：`com.aimedical.modules.ai.impl.fallback`

**装配条件**：
- 标注 `@Service`
- 始终注册为 Bean

**构造器签名**：
```java
public FallbackAiService(List<AiService> aiServiceList,
                         List<DegradationStrategy> strategies)
```

**行为逻辑**：
1. 构造器排除自身：`aiServiceList.stream().filter(s -> !(s instanceof FallbackAiService)).collect(toList())`
2. 保存排除后的委托列表（可为空）和策略列表
3. **委托分发**：每个 AiService 方法中，若委托列表为空 → 直接返回 `AiResult.degraded("No available AiService delegate")`，启动期输出 ERROR 日志、运行期输出 WARN 日志
4. 若委托列表非空 → 取第一个委托对象调用同名方法：
   - 先检查返回的 `AiResult`：若 `success` 为 `true` 或 `degraded` 为 `true`，直接返回原结果
   - 若 `success` 为 `false` 且 `degraded` 为 `false` → 遍历 `strategies`，任一 `shouldDegrade(context)` 返回 `true` 则返回 `AiResult.degraded("Degraded by strategy")`
   - 若未触发降级 → 返回原 `AiResult`

### 包路径结构
```
com.aimedical.modules.ai.impl
├── mock/
│   └── MockAiService.java
├── degradation/
│   └── NoOpDegradationStrategy.java
└── fallback/
    └── FallbackAiService.java
```

## 选择理由
- ai-impl 是 ai-api 的直接下游实现层，提供 13 个 AI 能力接口的 Mock 占位实现
- 所有业务模块的 Controller/Service 需要 AiService Bean 才能注入，application 模块需要 ai-impl 才能启动
- NoOpDegradationStrategy 和 FallbackAiService 构成降级策略框架的 Phase 0 基线

## 任务上下文
- AiService 接口定义 13 个方法，均在 `com.aimedical.modules.ai.api.AiService`
- AiResult 提供 `success(data)`, `failure(errorCode)`, `degraded(reason)` 三个静态工厂
- DTO 全部在 `com.aimedical.modules.ai.api.dto.*` 包下，均有无参构造器
- DegradationContext 在 `com.aimedical.modules.ai.api.degradation`，仅无参构造
- DegradationStrategy 接口定义 `boolean shouldDegrade(DegradationContext context)`
- ai-impl/pom.xml 当前为空壳（仅 artifactId，无依赖声明）
- 已有 ai-api 模块（38 测试通过）、common 模块（基类）、common-module-api（UserType 枚举）
