# 质量审查诊断报告 — v15 v2

## 审查结论：存在 4 项需修复的问题（2 严重 / 1 一般 / 1 轻微）

---

### 问题 1（严重）：`common-module-impl` 中 `LoginUser` 缺少 `spring-boot-starter-security` 编译依赖

**问题描述**：`LoginUser`（3.3 节，归属 `common-module-impl`）实现 `UserDetails` 接口，该接口来自 `spring-boot-starter-security`。但 `spring-boot-starter-security` 在 `common` 模块中被标注为 `<optional>true</optional>`（2.2 节「Common 模块依赖传播决策」），而 `common-module-impl` 仅依赖 `common-module-api`（→ `common`）。根据 Maven 规则，optional 依赖不会被传递解析，因此 `common-module-impl` 的类路径上不存在 `UserDetails` 接口，导致编译失败。

**所在位置**：3.3 节 `LoginUser` 定义、2.2 节「Common 模块依赖传播决策」security 标注为 optional、2.2 节「依赖管理」需显式声明 security 的模块列表未包含 `common-module-impl`

**严重程度**：严重 — 骨架无法编译通过，属于阻断性缺陷

**改进建议**：将 `common-module-impl` 加入需显式声明 `spring-boot-starter-security` 的模块列表（2.2 节「依赖管理」段落），并在 `modules/common-module/common-module-impl/pom.xml` 中补充该依赖（scope 默认 compile，版本由父 POM 统一管理）。

---

### 问题 2（严重）：`@Profile("phase0")` 无激活机制，Phase 0 骨架无法正常启动

**问题描述**：4.5 节 SecurityConfigPhase0 标注 `@Profile("phase0")`，但全文档未定义 `spring.profiles.active=phase0` 的设置位置：
- `application.yml`（9.1 节）未设置 `spring.profiles.active`
- `application-dev.yml`（9.1 节）使用 `dev` profile，不含 `phase0`
- 9.3 节启动命令 `mvn spring-boot:run -pl application -am` 未携带 `--spring.profiles.active`
- 开发者若使用 `spring.profiles.active=dev` 启动，SecurityConfigPhase0 不会激活；若不设置任何 profile，默认 profile 不含 `phase0`，同样不激活

无 `SecurityFilterChain` Bean 时，Spring Boot 默认安全配置会拦截所有请求，直接破坏「骨架可运行」验收标准（4.1 节：`GET /api/ping` → `200 OK`）。同时，开发者若仅设置 `spring.profiles.active=phase0`，`application-dev.yml` 不会加载（H2 数据库和 AI Mock 配置均在该文件中），正确方式为 `spring.profiles.active=phase0,dev` 同时激活两个 profile，但该约束在文档中完全缺失。

**所在位置**：4.5 节 SecurityConfig 设计、9.3 节启动命令、9.1 节配置管理

**严重程度**：严重 — 开发者无法按文档指示启动骨架并跑通健康检查

**改进建议**：二选一：
- **方案 A（推荐）**：在 `application.yml` 的通用配置中设置 `spring.profiles.active: phase0,dev`，使 Phase 0 默认同时激活两个 profile；在 4.5 节末尾和 9.3 节启动命令旁补充注释说明
- **方案 B**：取消 `@Profile("phase0")`，改为无条件注册 permitAll SecurityConfig

---

### 问题 3（一般）：权限模型实体 JPA 关系映射缺失注解细节，并行开发将产生不兼容实体

**问题描述**：3.3 节定义了 User、Role、Post、Function 四个权限实体及其概念关系（多对多、一对多），但未给出 JPA 注解细节。具体缺失项：
- `User ↔ Role`（多对多）：未指定 `@JoinTable`（表名、列名）、`fetch` 策略、`cascade` 策略
- `User ↔ Post`（多对多）：同上
- `Role ↔ Post`（一对多）：未指定 `mappedBy`、`cascade`、`orphanRemoval`
- `Post ↔ Function`（多对多）：未指定 `@JoinTable`

多名开发者并行实现这四个实体时，默认的 JPA 命名策略产生的关联表名和列名大概率不一致（如 `user_role` vs `user_roles`），且 fetch/cascade 策略的选择差异将导致 LazyInitializationException、意外级联删除等运行时问题。Phase 0 之后的所有阶段均依赖权限模型，此处的设计不精确将逐级传递技术债务。

**所在位置**：3.3 节（User、Role、Post、Function 四个实体的定义段）

**严重程度**：一般 — 不影响编译，但并行开发必然产生不兼容实现，需额外的对齐工作

**改进建议**：在 3.3 节为每个实体补充 JPA 关系注解的伪代码骨架，至少明确：
- 多对多关联的 `@JoinTable` 命名约定（推荐格式：`{entity1}_{entity2}`，如 `user_role`）
- 统一采用 `FetchType.LAZY`（避免 N+1 问题）
- `cascade` 策略（推荐 Entity 端不设 cascade，由 Service 层统一管理）
- Role ↔ Post 的 `mappedBy` 归属（推荐 Post 端为 owning side）

---

### 问题 4（轻微）：ASCII 依赖方向图未体现业务模块对 `ai-api` 的依赖关系

**问题描述**：2.2 节正文明确说明 `patient`/`doctor`/`admin` 模块依赖 `common`、`common-module-api` 和 `modules/ai/ai-api`，但 ASCII 依赖方向图中仅显示 `ai-api` 与 `common` 和 `ai-impl` 的连线，未显示指向 `patient`/`doctor`/`admin` 的依赖箭头。阅读 ASCII 图的开发者无法直观获知业务模块需引入 `ai-api` 依赖。

**所在位置**：2.2 节依赖方向图

**严重程度**：轻微 — 正文已明确但图未同步，有误导风险

**改进建议**：在 ASCII 图中补充 `ai-api` 指向 `patient`/`doctor`/`admin` 的箭头，或在图下方以注释形式标注「业务模块同时依赖 common-module-api 与 ai-api」。

---

## 需求响应与完整性评估

### 需求覆盖度

产出覆盖了用户需求中全部 7 大设计范围，未发现遗漏的设计 scope。具体对照如下：

| 需求范围 | 覆盖章节 | 覆盖评估 |
|---------|---------|---------|
| 共享工程结构 | §2.1–2.4 | 完整覆盖 Monorepo 布局、后端多模块、前端三端、并行贡献模块划分 |
| 接口契约框架 | §3.1, §5.1–5.3 | 完整覆盖 Result、PageQuery/PageResponse、ErrorCode、全局异常处理 |
| 数据与权限模型骨架 | §3.2–3.3 | 覆盖 BaseEntity 及三级权限模型实体定义 |
| 协作规范 | §2.2–2.3, §8.1, §8.4 | 覆盖包命名、依赖规则、调用规范、API 版本策略 |
| 本地开发体验 | §9.1–9.3 | 覆盖配置管理、构建依赖、一键启动 |
| CI 占位 | §10 | 覆盖 5 阶段流水线及模块依赖顺序 |
| AI 能力 Mock 占位 | §3.4, §8.2 | 覆盖接口契约、Mock 数据约定、降级策略框架 |

### 深度与完整性

**优势**：核心抽象（AiService 接口方法集合、BaseEntity 字段级定义、降级策略框架、Mock 数据占位约定）达到了可直接指导编码的深度。前端 workspace 配置、Vite 代理、CI 流水线命令均给出可复用的骨架代码。

**关键缺口**：问题 3 指向的权限实体关系映射缺乏 JPA 注解细节，是设计中深度不足的典型示例。当前设计仅停留在"概念关系"层次，距离"可直接指导编码实现"有一步之遥。

**异常场景与边界条件**：§5.1 错误分类表覆盖了参数校验、序列化、业务逻辑、认证授权、资源不存在、数据完整性冲突、配置加载失败、系统异常、AI 调用异常等 11 类场景。§3.4 明确了 AiService 无可用实现时的兜底保护。§3.4 标注了 DegradationContext null 字段的 NPE 风险。异常场景覆盖较为充分。

**接口定义对下游消费者的支撑度**：AiService 接口的 13 个方法均有明确的输入/输出 DTO 类型和核心字段定义；PermissionService 门面接口有方法签名示例；Result<PageResponse<T>> 分页契约有请求/响应示例。接口定义足以支撑前端与后端业务模块的独立开发。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 报告未系统评估"需求响应充分度"和"整体深度和完整性"两个核心维度，所发现的 3 个问题均属于技术细节层面，与内部审议已覆盖的维度高度重叠 | 认可质询。本版本在保留前序 3 个技术性问题的基础上，新增了「需求响应与完整性评估」章节，从需求覆盖度、深度与完整性、异常场景覆盖、接口对下游消费者的支撑度四个维度进行了系统性评估。同时新增问题 3（权限实体 JPA 关系映射缺失），该问题直接影响了"可直接指导编码实现"的深度要求，属于维度 3（深度和完整性）。 |
| 报告未对产出是否充分响应全部 7 大设计范围做出评估 | v2 版本已补充需求覆盖度对照表，逐项验证了全部 7 大设计范围的覆盖情况。评估结论：设计范围全覆盖，未发现 scope 级别的遗漏。 |
| 报告未从实际落地视角评估设计是否可直接指导编码实现、接口定义是否足以支持下游消费者、异常场景和边界条件是否已考虑 | v2 版本「深度与完整性」小节已对上述三个视角逐一评估。主要缺口为权限实体关系映射的 JPA 注解细节（问题 3），已纳入修复清单。异常场景覆盖和接口定义支撑度评价为"充分"。 |
