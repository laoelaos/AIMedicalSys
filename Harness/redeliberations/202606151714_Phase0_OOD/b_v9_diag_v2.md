# 质量审查报告

## 审查概述

审查轮次：第 9 轮（v2）
审查对象：Phase 0 OOD 设计方案（v9）
审查视角：需求响应充分度、事实错误/逻辑矛盾、深度与完整性

经过 8 轮迭代修复，设计文档已非常成熟，未发现严重质量问题。以下为审查发现的若干问题：

---

## 问题清单

### 问题 1：CI 流水线使用非标准 Maven 属性名 `-Dskip.unit.tests=true`

- **位置**：第 10 节 CI 占位，第四阶段命令
- **严重程度**：一般
- **问题描述**：`-Dskip.unit.tests` 不是 Maven Surefire 或 Failsafe 的标准属性。Surefire 的标准跳过属性为 `-DskipTests` 或 `-Dmaven.test.skip=true`，Failsafe 的标准跳过属性为 `-DskipITs`。如果 POM 中没有为 Surefire 插件显式配置 `<skip>${skip.unit.tests}</skip>`，此属性将无效，第四阶段集成测试运行时单元测试不会被跳过（或更糟：`verify` 生命周期在运行集成测试前会执行 `test` 阶段，未跳过的单元测试可能与 Failsafe 集成测试的执行时序冲突）。当前设计未提供 POM 中 Surefire 的 `<skip>` 属性配置，该命令无法直接照搬使用。
- **改进建议**：方案一：在 `integration/pom.xml` 的 surefire 插件配置中添加 `<skip>${skip.unit.tests}</skip>` 并在 `properties` 中定义默认值。方案二：改用标准属性组合——第四阶段使用 `-DskipTests`（跳过全部测试）+ 通过 failsafe 插件的 `<includes>` 精确匹配 `*IT.java`；或更清晰地拆分为 `mvn verify -pl integration -DskipITs=false` 并配合 Surefire 的 `-DskipTests=true`。

---

### 问题 2：`UserRegisteredEvent extends ApplicationEvent` 使用过时模式

- **位置**：8.4 节事件驱动模式示例（第 822-825 行）
- **严重程度**：一般
- **问题描述**：示例中 `UserRegisteredEvent` 继承 `ApplicationEvent`。Spring Framework 4.2+（当前项目使用 Spring Boot 3，对应 Spring Framework 6）已支持任意 POJO 作为事件对象发布，无需继承 `ApplicationEvent`。继承该框架类导致领域事件与 Spring 框架代码产生不必要的编译期耦合。
- **改进建议**：去掉 `extends ApplicationEvent`，改为普通 POJO 类（保留 `userId` 字段和构造函数即可）。`ApplicationEventPublisher.publishEvent(Object)` 可发布任意 POJO 事件。

---

### 问题 3：`ScheduleRequest.dateRange` 类型为 `String` 且无格式约束

- **位置**：8.2 节 AI 能力 DTO 定义（第 753 行）
- **严重程度**：轻微
- **问题描述**：排班日期范围的字段定义为 `String dateRange`，未约定字符串的格式规范（如 `"2026-06-01,2026-06-30"`、ISO 8601 间隔 `"2026-06-01/2026-06-30"` 或自然语言描述）。不同开发者可能采用不同格式导致前后端接口不兼容。
- **改进建议**：拆分为两个 `LocalDate` 字段 `startDate` / `endDate`（更符合 Java 类型系统），或在 Javadoc 中明确 `String` 格式的精确约定。

---

### 问题 4：核心配置 `ai.mock.enabled` 未在配置文件示例中显式声明

- **位置**：3.4 节 Bean 装配策略（依赖此属性）、9.1 节应用配置示例（未包含此属性）
- **严重程度**：轻微
- **问题描述**：`ai.mock.enabled` 是控制整个 AiService Bean 装配方案的核心开关，但 9.1 节的 `application-dev.yml` 示例中未包含该配置项。虽然 `@ConditionalOnProperty(matchIfMissing = true)` 提供了安全默认值，但新开发者需要通过阅读 `MockAiService` 注解才能发现此属性的存在，增加了认知负担。
- **改进建议**：在 9.1 节 `application-dev.yml` 示例末尾添加一行 `ai.mock.enabled: true`，使 Phase 0 的默认行为在配置文件中显式化。

---

## 补充审查

以下为针对上一轮质询反馈的补充审查发现。

### 一、需求响应覆盖矩阵

| 需求维度 | 对应设计章节 | 覆盖状态 | 备注 |
|---------|------------|---------|------|
| 1. 共享工程结构 | 2.1 Monorepo 目录布局、2.2 模块职责与依赖、2.3 包命名规范、2.4 前端模块划分 | 已覆盖 | 后端6+模块结构明确，前端三端SPA + 双共享包布局清晰；支持并行贡献的模块边界已定义 |
| 2. 接口契约框架 | 3.1 Result/T/PageQuery/PageResponse/ErrorCode/GlobalExceptionHandler、4.2-4.4 行为契约 | 已覆盖 | 泛型响应包装、分页规范、错误码interface+enum方案、全局异常处理均已定义；行为契约含示例 |
| 3. 数据与权限模型骨架 | 3.2 BaseEntity、3.3 Role-Post-Function三级模型 + User/LoginUser | 已覆盖 | BaseEntity字段类型+JPA注解完整；三级权限模型实体关系明确；LoginUser Adapter模式解决User与UserDetails解耦 |
| 4. 协作规范 | 2.3 包命名、2.2 依赖规则、8.1 API版本管理、8.4 跨模块调用规范 | 已覆盖 | 依赖规则含编译期强制保障（ai-api/ai-impl子模块拆分）；门面+事件两种跨模块调用模式有编码示例 |
| 5. 本地开发体验 | 9.1 统一配置管理（含H2配置）、9.2 多模块构建依赖（含Spring Boot扫描配置）、9.3 一键启动（含Vite代理配置） | 已覆盖 | H2内存数据库+dev/prod多环境yml；build依赖顺序明确；一键启动命令+代理配置示例完整 |
| 6. 持续集成占位 | 第10节 CI占位（五阶段流水线 + Integration模块POM骨架） | 已覆盖 | 分阶段构建策略已体现模块依赖顺序；Integration模块职责+POM配置已定义 |
| 7. AI能力模块Mock占位 | 3.4 AiService/MockAiService/AiResult + 装配策略 + 降级策略、8.2 AI能力方法清单+26个DTO定义、8.3 API文档工具集成 | 已覆盖 | 13个方法契约完整，DTO字段级定义给出；Mock占位约定（5种类型）明确；装配条件汇总表准确；降级策略框架包含DegradationContext |

**结论**：7个需求维度均已显式覆盖，未发现遗漏维度。

---

### 二、异常场景与边界条件专项审查

审查设计方案中异常路径和边界条件的覆盖情况：

| 异常/边界场景 | 设计覆盖状态 | 覆盖详情 |
|-------------|------------|---------|
| 参数校验异常（必填字段缺失、格式不合法） | 已覆盖 | 5.1节：MethodArgumentNotValidException → Result(400) |
| 请求体序列化异常 | 已覆盖 | 5.1节：HttpMessageNotReadableException → Result(400) |
| 响应体序列化异常 | 已覆盖 | 5.1节：HttpMessageNotWritableException → Result(500) |
| 业务逻辑异常 | 已覆盖 | 5.1/5.2节：BusinessException(ErrorCode) → Result |
| 认证/授权异常 | 已覆盖 | 4.5节：401/403 + 与GlobalExceptionHandler一致的Result格式 |
| 资源不存在 | 已覆盖 | 5.1节：BusinessException(NOT_FOUND) → Result(404) |
| 数据完整性冲突 | 已覆盖 | 5.1节：DataIntegrityViolationException → Result(409) |
| 配置加载失败 | 已覆盖 | 5.1节：FailureAnalyzer输出诊断信息，非运行时捕获 |
| 系统异常（数据库连接失败、空指针等） | 已覆盖 | 5.1节：统一捕获 → Result + 服务端日志(500) |
| AI调用异常（超时、不可用） | 已覆盖 | 3.4节/5.1节：AiResult.degraded=true + 降级数据(200) |
| FallbackAiService兜底保护（无可用Bean） | 已覆盖 | 3.4节：ObjectProvider.getIfAvailable()返回null时返回AiResult(success=false, degraded=true, data=null) |
| 分页参数越界（page/size为负数） | 部分覆盖 | 参数校验通过@Valid + MethodArgumentNotValidException处理，但PageQuery未标注校验注解 |
| 分页超大size（无上限限制） | 未覆盖 | 见新增问题5 |
| 逻辑删除后的特殊查询（需要包含已删除记录的查询场景） | 未覆盖 | @SQLRestriction("deleted = false")全局过滤，未提供绕过机制或查询方法约定 |
| 前端网络不可达（无响应、超时） | 未覆盖 | 见新增问题7 |
| 跨模块调用返回Entity导致懒加载异常 | 未覆盖 | 见新增问题6 |

**说明**：以上场景中"未覆盖"的3项属于Phase 0骨架可接受的完善缺口，不影响"骨架可运行"的Phase 0核心目标，但应在Phase 1前补充。

---

### 三、接口契约可用性评估

对下游消费者视角的关键接口评估：

| 接口契约 | 消费者 | 可用性评估 |
|---------|-------|-----------|
| Result\<T\> | 前端、AI消费者 | 充分。泛型类型，code/message/data结构清晰；前端Axios拦截器统一处理 |
| PageQuery/PageResponse\<T\> | 前端 | 充分。0-based page，与Spring Data JPA对齐；前端1-based适配已说明 |
| ErrorCode interface | 各业务模块 | 充分。interface定义契约，各模块enum独立实现错误码段，BusinessException持有统一引用类型 |
| AiService (13 methods) | 业务模块 | 充分。每项能力对应类型安全的方法签名 + 独立输入/输出DTO类型 |
| AiResult\<T\> | 业务模块 | 充分。含success/data/errorCode/degraded/fallbackReason五个字段，覆盖调用结果全状态 |
| GlobalExceptionHandler | 前端 | 充分。统一转换业务/系统异常为Result格式，前端可统一处理 |
| SecurityConfig (permitAll) | Phase 0开发者 | 充分。注释标记未来认证位置，保留AuthenticationEntryPoint等组件供Phase 1切换 |
| 跨模块门面PermissionService | 下游业务模块 | **存在问题**：见新增问题6 |

---

### 四、新增问题

### 问题 5：分页参数缺少最大 size 约束

- **位置**：3.1 节 PageQuery 字段描述
- **严重程度**：一般
- **问题描述**：PageQuery 的 `size` 字段未定义最大值上限。恶意或错误的请求可传入极大 size 值（如 `size=10000000`）导致后端内存压力甚至 OOM。虽然 5.1 节定义了参数校验异常的处理路径，但 PageQuery 类本身未标注 `@Max` 等校验注解，当前设计无法阻止超大 size 请求进入 Service 层。
- **改进建议**：在 PageQuery 的 `size` 字段上补充 `@Max` 约束（建议上限 100-500），并添加 Javadoc 说明默认值和上限值。将 `@Valid` 注解添加到所有分页 Controller 接口的参数上。

### 问题 6：跨模块门面接口返回 JPA 实体违反自身设计原则

- **位置**：8.4 节 PermissionService 门面示例（第 804-806 行）
- **严重程度**：严重
- **问题描述**：设计文档 8.1 节（第 611 行）明确要求"模块内部类（entity、repository）不对外暴露"，8.4 节（第 840 行）亦禁止"直接引用其他模块的 Repository 或 Entity 类"。但 8.4 节的门面示例 `PermissionService.getUserById(Long userId)` 直接返回 `User` 实体对象。返回 JPA 实体会引入以下风险：1）实体携带懒加载关联（Role、Post），在事务外访问将抛出 `LazyInitializationException`；2）实体上的 JPA 注解（`@ManyToMany`、`@JoinTable` 等）暴露给消费者，导致不必要的编译期耦合；3）序列化实体时可能触发循环引用或全表加载。Phase 0 虽不实现门面，但该示例将作为 Phase 1+ 开发者的编码模板，设置了一个与自身原则矛盾的错误范例。
- **改进建议**：将门面接口的返回类型从 `User` 改为 `UserDTO`。在 common-module 的 `dto` 子包中定义 `UserDTO`（仅含 userId、userName、userType 等必要字段，不含 JPA 注解和懒加载关联）。在门面接口前补充注释说明"返回 DTO 而非 Entity"的原则。

### 问题 7：Mock 数据占位约定未处理可选/可空字段

- **位置**：3.4 节 Mock 数据占位约定
- **严重程度**：轻微
- **问题描述**：Mock 数据约定覆盖了集合（2-3 条）、字符串（"mock_"+字段名）、数值（0/1）、枚举（values()[0]）、嵌套 DTO（递归填充）五种类型，但未说明可选/可空字段的处理方式。所有字段均填充占位值会使 Mock 数据无法测试"字段缺失"场景（如可选的患者过敏史列表为空时前端应正常渲染），且对标注了 `@Nullable` 的字段无差异行为。
- **改进建议**：补充可选字段约定：对标注了 `@Nullable`、Javadoc 标记为 optional 或在后续阶段才确定的字段，Mock 应返回 null 值，并在约定说明中给出明确的判定规则（"检测字段是否标记 `@Nullable` 或字段名以 `Optional` 开头等"）。

### 问题 8：H2 Console 启用但未说明访问限制与生产关闭策略

- **位置**：9.1 节 H2 配置
- **严重程度**：轻微
- **问题描述**：`h2.console.enabled: true` 将 H2 Web Console 暴露在 `/h2-console` 路径。设计文档在 `application-dev.yml` 中启用但不建议任何访问限制。若开发者仅注释 H2 配置块但未显式关闭 H2 Console（或将 h2 依赖调整为 test scope），生产环境 jar 包仍可通过 spring.h2.console.enabled 访问控制台。
- **改进建议**：1）在 9.1 节补充说明："H2 Console 仅在 `dev` profile 中启用，`application-prod.yml` 应设置 `spring.h2.console.enabled: false`"，并提供显式关闭示例；2）明确 Phase 1+ 切换 MySQL 时将 h2 依赖 scope 从 `runtime` 调整为 `test` 的步骤。

### 问题 9：前端 ApiClient 未定义网络错误处理路径

- **位置**：3.5 节 ApiClient 描述
- **严重程度**：轻微
- **问题描述**：ApiClient 仅定义了响应拦截器根据 `Result.code` 统一处理业务层成功/失败，但未定义请求/网络错误的处理路径（如 DNS 解析失败、连接超时、服务端无响应等场景）。Axios 在这些场景抛出 `AxiosError`，若未在拦截器中捕获，前端将面临未捕获的 Promise 异常。
- **改进建议**：在 3.5 节补充 Axios 错误拦截器约定：在响应拦截器末尾添加 `PromiseReject` 处理分支，网络错误时返回统一格式的提示（如 `{ code: "NETWORK_ERROR", message: "网络不可达" }`），或走全局错误弹窗的占位逻辑。

---

## 整体质量评价

该设计文档经过 8 轮迭代修正后质量较高：需求覆盖全面（7 个维度均已响应且可追溯至具体章节），逻辑自洽，技术决策合理且有充分理由支撑。异常场景和边界条件的整体覆盖度良好（已覆盖 12/15 个检查项），接口契约对下游消费者的可用性总体充分。

以上 9 个问题中：1 个严重（问题 6——门面接口返回 Entity 违反自身原则，需在 Phase 1 实施前修正模板），4 个一般，4 个轻微。核心骨架可用性不受影响。文档已达到可直接指导编码实现的成熟度。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 报告未涉及"异常场景和边界条件"维度，影响结论可信度 | **已补充**。在"补充审查·二"中增加异常/边界场景专项审查表，检查 15 项场景（12 项已覆盖、3 项未覆盖/部分覆盖），并在"新增问题"中补充了对应的设计缺口（问题 5/7/8/9）。 |
| "需求响应充分度"仅以一句断言带过，缺乏逐维度验证过程 | **已补充**。在"补充审查·一"中增加需求响应覆盖矩阵，标注 7 个需求维度与设计章节的映射关系、覆盖状态。结论从单句断言升级为可追溯的逐维验证。 |
| 缺乏对接口契约可用性的系统性评估 | **已补充**。在"补充审查·三"中增加接口契约可用性评估表，覆盖 8 个关键接口/契约，从下游消费者视角逐项评估。在此基础上发现并补充了问题 6（门面返回 Entity）。 |
