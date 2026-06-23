# OOD 设计方案审查报告（v1）

## 审查结果

**REJECTED**

## 逐维度审查

### 1. 类型系统可行性

**[通过]** `Result<T>` 泛型 class 适合 Java 类型系统，编译期类型安全，Jackson 序列化可正确推断控制器方法返回类型

**[通过]** `BaseEntity` 抽象类 + `@MappedSuperclass` 符合 JPA 规范，interface 无法携带字段与注解

**[通过]** `ErrorCode` 枚举类型在 Java 中可穷举错误码，配合 `BusinessException` 持有枚举引用，`GlobalExceptionHandler` 统一转换

**[通过]** `AiService` 接口为 AI 能力抽象层，允许多个实现（Mock、真实 AI、降级装饰器），与 Java 接口机制完全匹配

**[通过]** 权限模型实体（User/Role/Post/Function）间的多对多、一对多关系全部在 JPA `@ManyToMany` / `@OneToMany` 能力范围内

### 2. 标准库与生态覆盖

**[通过]** Spring Boot 3 + Spring Data JPA + Spring Security + Spring MVC 覆盖所有后端骨架需求（统一异常处理、JPA 实体、权限过滤、REST 接口）

**[通过]** Knife4j / SpringDoc 覆盖 API 文档需求，符合项目技术栈文档要求

**[通过]** 前端 Axios + Pinia + Vue Router 覆盖 API 客户端封装、状态管理和路由需求

**[通过]** Maven `<dependencyManagement>` 统一版本管理 + `mvn dependency:analyze` 循环依赖检测均为 Maven 原生或插件能力

### 3. 语言特性可行性

**[通过]** 异常处理策略（`BusinessException` → `@ControllerAdvice` → `Result`）与 Spring MVC 异常处理模型完全匹配

**[通过]** 并发设计（Tomcat 默认线程池 + 同步非阻塞 AI 接口预留）在 Phase 0 规模下可行，`CompletableFuture` 延期到 Phase 2+ 合理

**[通过]** JPA 实体生命周期回调（`@PrePersist` / `@PreUpdate`）完全支持 `BaseEntity` 的自动时间戳填充

**[通过]** Maven 多模块分层构建、前端 Vite workspace 组织均与各自生态的工程组织方式一致

### 4. 设计一致性

**[通过]** 模块职责清晰，`common`（零依赖）→ `common-module`（跨模块业务抽象）→ 业务模块（相互隔离）→ `application`（聚合启动）的方向合理，无循环依赖

**[一般]** `AiService` 统一门面（3.4 节）与 13 项 AI 能力接口清单（8.2 节）之间的关系不清晰。3.4 节定义 `AiService` 单一接口含 `invoke(input) → AiResult` 方法签名，但 8.2 节以独立接口命名列出 `AiTriageService`、`AiPrescriptionCheckService` 等 13 项，且各项输入/输出类型互不相同。Java 无法通过单泛型方法 `invoke(input)` 分发到 13 种不同能力的接口签名。

**[轻微]** 依赖关系图（2.2 节）：`modules/ai` 向 `application` 的箭头方向与正文描述相反。正文正确说明 `application` 依赖所有业务模块（含 ai），但图中箭头从 ai 指向 application，暗示 ai 依赖 application。

**[轻微]** 目录结构（2.1 节）`modules/ai/` 下缺少 `degradation/` 子包，但 2.3 节包命名中列出了该子包。

### 5. 设计质量

**[通过]** 各抽象职责单一（Result 只负责响应包装、BaseEntity 只负责公共字段、AiService 只负责 AI 调用生命周期），无过度工程

**[通过]** 接口契约框架 + Mock 占位允许前端和后端各模块独立开发和验证，便于并行

**[通过]** AiService 接口可 Mock、`@ConditionalOnProperty` 可控制 Mock 开关、模块独立便于隔离测试

**[通过]** 三级权限模型（角色—岗位—功能）解耦合理，角色不直接挂载功能，通过岗位间接关联，变更权限不修改实体代码

## 修改要求（REJECTED）

### 一般问题

- **问题**：`AiService` 统一门面接口与 8.2 节 13 项 AI 能力接口之间的关系未定义。3.4 节描述 `AiService` 为单一门面接口，仅有 `invoke(input) → AiResult` 方法，但 8.2 节以独立接口标识列出 13 项具有不同输入/输出类型的能力。当前描述无法推导出 `AiService` 在 Java 类型系统中的具体形态。

- **原因**：设计方案应明确 13 项能力在类型系统中的表达方式，否则无法直接指导编码实现。以下是三种可行方向但方案未做选择：
  1. `AiService` 含 13 个具体方法 —— 接口膨胀但类型明确
  2. `AiService` 泛型参数化 `AiService<T, R>` —— 需额外工厂/注册表按能力类型分发
  3. 13 个独立子接口 —— 统一门面说法不成立

- **建议方向**：选择以下任一方向并更新设计：
  - **方向 A**：将 `AiService` 定义为拥有 13 个具体方法的接口（如 `triage(TriageRequest): AiResult<TriageResponse>`），每个能力一个方法。Phase 0 的 Mock 实现返回占位数据。这是最直接的方案，类型安全最强，但接口体积较大。
  - **方向 B**：保留 `AiService` 为单方法门面，引入 `AiCapability` 枚举或类型标记作为分发依据，`invoke(AiCapability capability, Object input)`，由实现方按能力类型路由。Mock 实现中按能力分发到对应占位逻辑。这保持了门面简洁性，但需要运行时类型判断。
  - 无论选择哪个方向，需在 3.4 节和 8.2 节之间建立显式的映射关系。

### 轻微问题（不阻塞，建议修复）

- 2.2 节依赖关系图中 `modules/ai → application` 箭头方向应反转为 `application → modules/ai`，以匹配正文"application 依赖所有业务模块"的描述。
- 2.1 节 `modules/ai/` 目录树下补充 `degradation/` 子包，与 2.3 节包命名保持一致。
