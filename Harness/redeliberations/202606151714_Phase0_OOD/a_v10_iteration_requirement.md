根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1：CI 流水线使用非标准 Maven 属性名 `-Dskip.unit.tests=true`
- **位置**：第 10 节 CI 占位，第四阶段命令
- **严重程度**：一般
- **问题描述**：`-Dskip.unit.tests` 不是 Maven Surefire 或 Failsafe 的标准属性。若 integration/pom.xml 中未为 Surefire 插件显式配置 `<skip>${skip.unit.tests}</skip>`，此属性将无效。
- **改进建议**：方案一：在 `integration/pom.xml` 的 surefire 插件配置中添加 `<skip>${skip.unit.tests}</skip>` 并在 `properties` 中定义默认值。方案二：改用标准属性组合。

### 问题 2：`UserRegisteredEvent extends ApplicationEvent` 使用过时模式
- **位置**：8.4 节事件驱动模式示例（第 822-825 行）
- **严重程度**：一般
- **问题描述**：Spring Framework 4.2+（当前项目使用 Spring Boot 3，对应 Spring Framework 6）已支持任意 POJO 作为事件对象发布，无需继承 `ApplicationEvent`。
- **改进建议**：去掉 `extends ApplicationEvent`，改为普通 POJO。

### 问题 3：`ScheduleRequest.dateRange` 类型为 `String` 且无格式约束
- **位置**：8.2 节 AI 能力 DTO 定义（第 753 行）
- **严重程度**：轻微
- **问题描述**：排班日期范围字段定义为 `String dateRange`，未约定字符串格式规范，不同开发者可能采用不同格式导致前后端不兼容。
- **改进建议**：拆分为两个 `LocalDate` 字段 `startDate`/`endDate`，或在 Javadoc 中明确 `String` 格式约定。

### 问题 4：核心配置 `ai.mock.enabled` 未在配置文件示例中显式声明
- **位置**：3.4 节 Bean 装配策略（依赖此属性）、9.1 节应用配置示例（未包含此属性）
- **严重程度**：轻微
- **问题描述**：核心开关 `ai.mock.enabled` 未在 application-dev.yml 示例中显式列出，新开发者需通过阅读注解才能发现。
- **改进建议**：在 9.1 节 application-dev.yml 示例末尾添加一行 `ai.mock.enabled: true`。

### 问题 5：分页参数缺少最大 size 约束
- **位置**：3.1 节 PageQuery 字段描述
- **严重程度**：一般
- **问题描述**：PageQuery 的 `size` 字段未定义最大值上限，恶意或错误请求可传入极大值导致 OOM。PageQuery 未标注 `@Max` 等校验注解。
- **改进建议**：在 `size` 字段上补充 `@Max` 约束（建议上限 100-500），添加 Javadoc 说明默认值和上限值。在所有分页 Controller 参数上添加 `@Valid`。

### 问题 6：跨模块门面接口返回 JPA 实体违反自身设计原则
- **位置**：8.4 节 PermissionService 门面示例（第 804-806 行）
- **严重程度**：严重
- **问题描述**：设计文档 8.1 节要求"模块内部类不对外暴露"，8.4 节禁止"直接引用其他模块的 Repository 或 Entity"。但 PermissionService.getUserById() 直接返回 `User` 实体，引入 LazyInitializationException、编译期耦合、序列化循环引用等风险。
- **改进建议**：将返回类型从 `User` 改为 `UserDTO`，在 common-module 的 `dto` 子包中定义 `UserDTO`（仅含 userId、userName、userType 等必要字段），补充注释说明"返回 DTO 而非 Entity"的原则。

### 问题 7：Mock 数据占位约定未处理可选/可空字段
- **位置**：3.4 节 Mock 数据占位约定
- **严重程度**：轻微
- **问题描述**：Mock 约定覆盖集合、字符串、数值、枚举、嵌套 DTO 五种类型，但未说明可选/可空字段的处理方式，所有字段均填充占位值无法测试"字段缺失"场景。
- **改进建议**：补充可选字段约定：对 `@Nullable`、Javadoc 标记 optional 的字段，Mock 应返回 null，并给出明确的判定规则。

### 问题 8：H2 Console 启用但未说明访问限制与生产关闭策略
- **位置**：9.1 节 H2 配置
- **严重程度**：轻微
- **问题描述**：H2 Console 在 dev profile 中启用，但未说明生产环境关闭策略。开发者仅注释配置块但未显式关闭时，生产 jar 包仍可访问控制台。
- **改进建议**：补充说明 H2 Console 仅在 dev profile 启用，prod profile 应设置 `spring.h2.console.enabled: false`；明确 Phase 1+ 切换 MySQL 时将 h2 依赖 scope 从 runtime 调整为 test。

### 问题 9：前端 ApiClient 未定义网络错误处理路径
- **位置**：3.5 节 ApiClient 描述
- **严重程度**：轻微
- **问题描述**：ApiClient 仅定义响应拦截器处理业务层成功/失败，未定义请求/网络错误处理路径（DNS 解析失败、连接超时等），Axios 抛出的 AxiosError 将导致未捕获的 Promise 异常。
- **改进建议**：在 3.5 节补充 Axios 错误拦截器约定：网络错误时返回统一格式提示（如 `{ code: "NETWORK_ERROR", message: "网络不可达" }`）或全局错误弹窗占位逻辑。

## 历史迭代回顾

分析历史迭代反馈记录（第 1-9 轮）与当前审查结果的关系：

### 已解决的问题（出现在历史反馈但当前反馈中不再提及）
以下第 8 轮问题已在上一轮（v9）设计中被修复：
- `spring-boot-starter-web` 未在模块中显式声明 — 第 2.2 节已明确声明及用途
- `spring-boot-starter-test` 未声明 — 父 POM 已统一管理版本，各模块以 test scope 引入
- `spring-boot-starter-validation` 未声明 — 父 POM 已统一管理，含 Controller 的模块以 compile scope 引入
- 前端 Monorepo 内部包依赖配置未定义 — 第 2.4 节已补充 workspaces、导出、引用方式示例
- integration 模块 Maven POM 配置未给出 — 第 10 节已补充 integration/pom.xml 骨架
- ai-impl 的 degradation 路径缺少显式展示 — 第 2.3 节已补充 degradation/ 目录

以上问题不再出现在本轮审查结果中，视为已解决。

### 持续存在的问题（本轮继续出现的未修复问题）
以下 9 个问题在上一轮（v9）审查中被识别，但 v9 设计尚未修复：
1. CI 流水线使用非标准 Maven 属性名 `-Dskip.unit.tests=true`
2. `UserRegisteredEvent extends ApplicationEvent` 使用过时模式
3. `ScheduleRequest.dateRange` 类型为 `String` 且无格式约束
4. 核心配置 `ai.mock.enabled` 未在配置文件示例中显式声明
5. 分页参数缺少最大 size 约束
6. 跨模块门面接口返回 JPA 实体违反自身设计原则（严重）
7. Mock 数据占位约定未处理可选/可空字段
8. H2 Console 启用但未说明访问限制与生产关闭策略
9. 前端 ApiClient 未定义网络错误处理路径

上述问题均需在本轮（v10）设计中修复，重点关注严重级别的问题 6（门面返回 Entity 违反自身原则）。

### 新发现的问题
无。本轮审查未发现新的质量问题，当前 9 个问题均为第 9 轮遗留问题的延续。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\a_v9_design_v1.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\requirement.md
