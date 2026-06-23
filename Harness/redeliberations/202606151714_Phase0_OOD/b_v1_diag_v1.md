# Phase 0 OOD 设计质量审查报告（第 1 次）

## 审查概述

审查范围：需求响应充分度、事实错误与逻辑矛盾、深度与完整性（可直接指导编码实现维度）
对照依据：Phase 0 OOD 设计需求（requirement.md）
内部审议已覆盖维度：类型系统可行性、标准库与生态覆盖、语言特性可行性、设计一致性、设计质量（参见 a_v1_review_v2.md）——本报告不重复上述维度

---

## 发现问题清单

### 问题 1：[高] AI 方法标识 `analysisReport(检查)` / `analysisReport(检验)` 不合法

- **位置**：8.2 节 AI 能力方法清单表格
- **问题描述**：方法标识列中 `analysisReport(检查)` 和 `analysisReport(检验)` 包含中文括号字符，不能作为 Java 方法名。且两方法名相同，Java 接口中无法通过**方法名**重载来区分（返回类型不同不足以区分重载），直接导致 AiService 接口无法编译。
- **改进建议**：分别命名，如 `analysisReportForInspection` 与 `analysisReportForLabTest`，或 `analyzeInspectionReport` 与 `analyzeLabTestReport`。8.2 节表格中的"对应 AI 能力"列已用中文说明区别，方法标识无需再用中文括号做语义标注。

### 问题 2：[高] 权限模型实体归属模块未定义

- **位置**：3.3 节（权限模型核心抽象）
- **问题描述**：Role、Post、Function、User 四个实体的 Maven 模块归属未说明。2.2 节指出 common-module 提供"权限、字典、配置等跨模块共享的服务接口与实体定义"，但 3.3 节未将四个实体与 common-module 的 `permission` 子包关联。编码人员不清楚实体应定义在哪个模块下，会直接影响模块依赖关系的正确性。特别是 User 实体被视为"统一用户实体"覆盖三类用户，若定义在 patient/doctor/admin 任一模块中都会引入不合理依赖。
- **改进建议**：在 3.3 节明确说明各实体的归属模块与包路径（推荐统一归入 common-module 的 `com.aimedical.modules.commonmodule.permission` 包）。补充 User 实体跨模块共享时的引用机制说明。

### 问题 3：[中] "同步非阻塞"表述存在逻辑矛盾

- **位置**：6 节（并发设计）
- **问题描述**："AI 调用接口设计为同步非阻塞（Spring Async + CompletableFuture 在 Phase 2+ 引入）"——"同步非阻塞"在操作系统/网络模型中是不存在的组合。同步意味着调用方阻塞等待结果，非阻塞意味着调用方不阻塞。Spring Async + CompletableFuture 本质上是**异步非阻塞**。Phase 0 的 MockAiService 直接返回属于**同步阻塞**。
- **改进建议**：区分为两个时态描述——"Phase 0：同步阻塞，MockAiService 直接返回；Phase 2+：引入 Spring Async + CompletableFuture 实现异步非阻塞"。

### 问题 4：[中] BaseEntity 缺少字段级详细定义，无法直接指导编码

- **位置**：3.2 节（数据实体基类）
- **问题描述**：仅以文字描述了 BaseEntity 包含 id、createdAt、updatedAt、逻辑删除标记四个字段，未给出：①各字段的 Java 类型（Long/UUID/String、LocalDateTime 等）；②ID 生成策略（@GeneratedValue 的 strategy）；③逻辑删除的实现方式（@SQLDelete + @Where 还是 @QueryHint 或其他）；④乐观锁版本字段是否预留（@Version）。编码人员无法据此写出正确的 BaseEntity 代码。
- **改进建议**：补充 BaseEntity 的伪代码或具体字段定义，至少包含字段类型、JPA 注解、ID 策略。示例：

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
    @Column(nullable = false)
    private Boolean deleted = false;
}
```

### 问题 5：[中] MockAiService 注入机制与 Bean 装配策略不完整

- **位置**：3.4 节（AI 能力模块抽象）
- **问题描述**：设计决策（7 节）选用 `@ConditionalOnProperty` 控制 Mock 开关，但 3.4 节正文未说明：①业务模块如何获取 AiService 实例（@Autowired 注入 interface 时，若 Mock 和 Fallback 两个实现并存，谁被注入）；②`@ConditionalOnProperty` 的 property 名称与默认值；③FallbackAiService 如何包装真实/Mock AiService（是 `@Primary`、`@Qualifier` 还是手动构造）；④`MockAiService` 的 `@Service` 扫描范围是否受 Spring Boot 组件扫描影响。
- **改进建议**：在 3.4 节补充 AiService 的 Bean 装配策略说明，明确三者的装配条件：

| Bean | 条件 | 作用 |
|------|------|------|
| `MockAiService` | `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)` | Phase 0 默认启用 |
| 真实实现 | `@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "false")` | Phase 2+ 启用 |
| `FallbackAiService` | 手动构造包装 | 装饰器模式，由业务侧选择使用 |

### 问题 6：[中] Spring Security 配置骨架未定义

- **位置**：4.5 节（权限校验契约）
- **问题描述**：描述了认证/授权失败的响应流程，但缺少可编码实现的具体配置设计：①SecurityFilterChain Bean 的定义方式（Spring Boot 3 的 Lambda DSL）；②PasswordEncoder 的选择与配置（BCryptPasswordEncoder 或其他）；③CORS 配置的集成位置（SecurityConfig 还是独立 CorsConfig）；④`/api/ping` 的 permitAll 与其他接口的鉴权规则配置方式；⑤Spring Security 异常处理如何与 GlobalExceptionHandler 协同（AuthenticationEntryPoint、AccessDeniedHandler 的配置）。
- **改进建议**：在 4.5 节补充 SecurityConfig 配置类的设计骨架，或明确 Phase 0 暂时放通所有接口的 SecurityFilterChain 配置示例。

### 问题 7：[中] User 实体与 Spring Security UserDetails 的适配关系未定义

- **位置**：3.3 节（User）、4.5 节（权限校验契约）
- **问题描述**：User 实体作为"统一用户实体"，但未说明其是否实现 Spring Security 的 `UserDetails` 接口，或通过 Adapter 模式适配。User 实体的字段（如 password、enabled、accountNonExpired 等）与 UserDetails 接口的契约关系未建立，导致 SecurityConfig 中的 AuthenticationProvider 或 UserDetailsService 无法落地。
- **改进建议**：明确 User 实体与 UserDetails 的关系——推荐采用 Adapter 模式（LoginUser 包装 User + 实现 UserDetails）以保持实体纯净，并在 3.3 节补充说明。

### 问题 8：[中] "仅依赖 ai 的 api 子包"缺乏编译期强制保障

- **位置**：2.2 节（模块依赖规则）
- **问题描述**：规则要求业务模块"仅依赖 ai 的 api 子包"，但 Maven 模块依赖是粗粒度的——业务模块在 pom.xml 中声明依赖 ai 模块后，ai 模块下所有 public 类型（api、mock、dto、degradation 子包）均对业务模块可见。纯靠代码审查约定无法在编译期防止业务模块误引入 ai.mock 或 ai.degradation 的内部类，违反了最小知识原则。
- **改进建议**：①方案 A（推荐）：在 ai 模块内部分离为两个 Maven 模块——`ai-api`（仅含 api 子包与 DTO）和 `ai-impl`（含 mock 与 degradation），业务模块仅依赖 `ai-api`；②方案 B：在设计文档中明确标注此风险，并在 CI 门禁中补充 ArchUnit 或 `mvn dependency:analyze` 的检查规则。

### 问题 9：[低] AI 能力 Mock 占位数据结构未定义

- **位置**：3.4 节（MockAiService）、8.2 节（AI 能力方法清单）
- **问题描述**：MockAiService 被描述为"返回固定结构占位数据"，但未给出任何 Mock 数据的结构约定或示例。编码人员不清楚"占位数据"是什么格式（如 triage 返回空列表还是固定 3 条推荐科室、字符串字段填充空字符串还是带 `mock_` 前缀的占位文本）。多个 Mock 实现若结构不统一，后续替换为真实实现时前端接口联调会出现断崖式差异。
- **改进建议**：补充 Mock 数据约定的简单规则，例如：①集合字段固定返回 2-3 条占位数据；②字符串字段填充 `"mock_" + 字段名` 格式；③数值字段填充 0 或 1；④枚举字段填充第一个枚举值。

### 问题 10：[低] 异常分类中部分边界场景未覆盖

- **位置**：5.1 节（错误分类表）
- **问题描述**：错误分类覆盖了 7 类场景，但未考虑以下边界：①Jackson 序列化/反序列化异常（HttpMessageNotReadableException/HttpMessageNotWritableException）未列入，发生频率不低；②配置加载失败（如 application.yml 解析错误）的框架层错误处理方式未说明；③AI 能力模块中 `DataIntegrityViolationException`（数据库约束冲突）未单独分类，统一归入"系统异常"导致前端无法区分业务约束冲突与真正的系统故障。
- **改进建议**：在 5.1 节补充上述边界场景的分类定义，或明确其处理范围（如"配置加载失败在骨架初始化时即应快速失败，不进入异常处理流程"）。

---

## 综合评估

**需求响应充分度**：设计覆盖了需求中全部 7 个维度的所有子项，无遗漏。响应充分度整体评分为"满足"。

**事实错误与逻辑矛盾**：存在 1 处事实错误（#3 "同步非阻塞"）和 1 处不合规设计（#1 analysisReport 方法名不合法），建议在下一轮修订中修复。

**深度与完整性**：整体深度处于"概念设计"与"详细设计"之间，部分抽象（Result<T>、模块划分、ErrorCode 设计思路）已足够指导编码，但实体定义层（BaseEntity、权限实体）和基础设施层（SecurityConfig、AiService 装配）的细节不足。若要真正"可直接指导编码实现"（需求明确要求），建议优先补齐 #4、#5、#6、#7 四项的字段级/代码级细节。

**可修复性**：上述 10 个问题中，#3、#9、#10 可快速修订（文字级调整）；#1、#4、#5、#6、#7、#8 需结构性补充；#2 需明确设计决策。无不可修复的架构性问题。
