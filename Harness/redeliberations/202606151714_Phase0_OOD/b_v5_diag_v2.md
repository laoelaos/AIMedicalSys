# Phase 0 OOD 设计 v5 质量审查报告

## 审查范围

本报告对产出的第 5 轮设计方案（`a_v5_design_v1.md`）进行质量审查，重点关注内部审议未充分覆盖的维度：需求响应充分度、文档深度和完整性、实际落地可行性。避免重复验证已处理的细项。

---

## 需求覆盖评估

以下对需求文档列出的 7 个设计维度逐一评估覆盖程度：

| 需求维度 | 设计章节 | 覆盖状态 | 关键差距说明 |
|---------|---------|---------|-------------|
| 1. 共享工程结构 | §2 | 完整 | Monorepo 布局、多模块后端/多端前端结构、模块划分均已定义 |
| 2. 接口契约框架 | §3.1, §5 | 完整 | Result\<T\>、PageRequest/PageResponse、ErrorCode 命名空间、全局异常处理均已定义 |
| 3. 数据与权限模型骨架 | §3.2, §3.3 | 完整 | BaseEntity、三级权限模型（Role/Post/Function）、User 实体均已定义 |
| 4. 协作规范（架构层面） | §2.2, §2.3, §8.1 | 部分 | 包命名规范、模块依赖规则、API 版本管理已定义，但跨模块调用编码模式（门面/事件）仅提及名称无具体模式定义（详见 P3） |
| 5. 本地开发体验（架构层面） | §9 | 完整 | 统一配置管理、多模块构建依赖、一键启动说明均已覆盖 |
| 6. 持续集成占位（架构层面） | §10 | 完整 | 分阶段 CI 流水线设计、各阶段模块归属、Integration 模块职责均已定义 |
| 7. AI 能力模块 Mock 占位 | §3.4, §8.2 | 完整 | AiService 接口契约（13 方法）、Mock 数据占位约定、降级策略框架均已定义 |

**评估结论**：7 个需求维度中 6 个为「完整」覆盖，1 个为「部分」覆盖（第 4 维度的跨模块调用编码模式未具体定义）。整体需求响应充分度较高，不存在维度级的设计遗漏。以下问题清单针对各维度中的具体质量问题逐一描述。

---

## 问题清单

### P1. `common` 模块依赖描述与 SecurityConfig 实际需求矛盾

- **所在位置**：第 2.2 节（line 117）"`common`：零依赖（仅依赖 Spring Boot Starter 基础库），所有模块可依赖它" 与 第 4.5 节（line 442-443）"spring-boot-starter-security 为其必需依赖"
- **严重程度**：一般
- **问题描述**：第 2.2 节声明 common 模块为"零依赖，仅依赖 Spring Boot Starter 基础库"，但第 4.5 节要求 common 模块的 `common.config` 包中存放 SecurityConfig，从而必须引入 `spring-boot-starter-security`。`spring-boot-starter-security` 并不属于"基础库"范畴（它不是一个通用基础设施 starter，而是特定的安全框架）。两条描述存在事实矛盾，编码人员无法据此确定 common 模块的 POM 依赖范围。
- **改进建议**：修订第 2.2 节 common 的依赖描述，如实反映包括 `spring-boot-starter-security` 在内的完整依赖集。例如："依赖 Spring Boot 核心 Starters（spring-boot-starter-web、spring-boot-starter-data-jpa、spring-boot-starter-security 等基础骨架所需）"，消除"零依赖"的误导性表述。

---

### P2. 跨业务模块调用机制未定义，不足以指导编码实现

- **所在位置**：第 2.2 节（line 20-21）
- **严重程度**：一般
- **问题描述**：设计仅定义了模块间依赖方向（谁依赖谁），但未定义实际的跨模块调用编码模式。文档在第 2.2 节提及"同层模块之间不允许直接依赖，通过公共门面（facade）或事件解耦"，但"公共门面"和"事件解耦"均无具体的接口形式、注册机制或调用示例。实际医疗场景中（如患者挂号、医生接诊等流程），业务模块间的数据交换不可避免——若不加定义，各模块开发者在遇到跨模块需求时会自行发挥，产生多种不兼容的调用模式。尽管 Phase 0 无业务逻辑不立即暴露此问题，但该决策应在架构层面明确。
- **改进建议**：补充跨模块调用规范章节，明确具体模式（至少给出推荐方案，如「common-module 定义门面接口 + 各模块提供实现注入」或「Spring ApplicationEvent 事件驱动」），并附简短编码示例。建议将其作为第 2.2 节的补充或新增第 2.5 节。

---

### P3. Spring Boot 包扫描策略缺失，骨架可运行缺少关键前提

- **所在位置**：缺失（应在第 9.2 节或启动模块说明中）
- **严重程度**：一般
- **问题描述**：多模块 Maven 项目中，application 模块的 `@SpringBootApplication` 需要通过 `scanBasePackages` 显式声明包扫描范围（例如 `@SpringBootApplication(scanBasePackages = "com.aimedical")`），否则 Spring 无法发现 common、modules/* 等子模块中的 Bean（Controller、Service、Configuration 等）。当前设计文档在"骨架可运行"和"一键启动"部分均未涉及此机制。新人开发者直接依文档编码：在 application 模块写 `@SpringBootApplication`（默认只扫描 application 模块），启动时将找不到 common 和 modules 中的任何 Spring Bean。
- **改进建议**：在第 9.2 节（多模块构建依赖）或新增的"启动说明"中，明确 `@SpringBootApplication(scanBasePackages = "com.aimedical")` 的配置方式，并说明需配合 `@EntityScan` 和 `@EnableJpaRepositories` 确保 JPA 实体和 Repository 被扫描。

---

### P4. `BusinessException` 未明确继承层次，事务行为不确定

- **所在位置**：第 5.2 节（line 492）
- **严重程度**：一般
- **问题描述**：文档描述"BusinessException 持有 ErrorCode 和可选的动态参数"，但未明确其继承自 `RuntimeException` 还是 `Exception`。Spring 默认事务管理的回滚策略为：仅对 `RuntimeException` 和 `Error` 回滚，对 checked `Exception` 不回滚。若 BusinessException 被设计为 checked exception 而文档未注明，开发者在使用 `@Transactional` 时将遭遇非预期的数据不一致。
- **改进建议**：明确 BusinessException `extends RuntimeException`，并补充建议的构造方法签名伪代码：
  ```
  public class BusinessException extends RuntimeException {
      private final ErrorCode errorCode;
      private final Object[] args;       // 消息模板动态参数
      public BusinessException(ErrorCode errorCode) { ... }
      public BusinessException(ErrorCode errorCode, Object... args) { ... }
  }
  ```

---

### P5. 自定义 `PageRequest` 与 Spring Data 的 `PageRequest` 类名冲突未处理

- **所在位置**：第 3.1 节（line 197）
- **严重程度**：一般
- **问题描述**：设计文档定义了一个自定义 `PageRequest` 类（位于 `com.aimedical.common.result` 包），与 Spring Data JPA 的 `org.springframework.data.domain.PageRequest` 类名完全相同。在编码时，Controller 层需导入自定义 PageRequest，Repository 层需导入 Spring Data 的 PageRequest，同一文件中同时使用二者时将产生导入冲突，需使用全限定名区分。文档未说明 Controller 层的自定义 PageRequest 如何转换为 Repository 层的 Spring Data Pageable，分页参数在不同层之间的传递路径不清晰。
- **改进建议**：方案一：将自定义类重命名为 `PageQuery` 或 `PageCriteria`，彻底消除命名冲突。方案二：明确声明自定义 PageRequest 仅作为 API 层的请求 DTO，在 Service 层统一转为 Spring Data 的 `Pageable`，并在文档中以伪代码示例说明转换逻辑。推荐方案一。

---

### P6. `FallbackAiService` 零实现回退的兜底路径未定义

- **所在位置**：第 3.4 节（line 354）
- **严重程度**：轻微
- **问题描述**：`FallbackAiService` 通过 `@Autowired` + `@Lazy` + `ObjectProvider<AiService>` 延迟获取底层实现，但未定义当 `ObjectProvider.getIfAvailable()` 或 `getIfUnique()` 返回 `null`（即因配置错误导致没有任何 AiService 实现被注册）时的处理逻辑。在配置错误的极端场景下，业务代码调用 AI 能力时将从 `ObjectProvider.getObject()` 抛出 `NoSuchBeanDefinitionException` 或触发 `NullPointerException`。
- **改进建议**：补充 `FallbackAiService` 的兜底逻辑：当无可用的 AiService 实现时，直接返回一个标记 `degraded=true` 的默认 `AiResult`，而非传播未处理的异常。

---

### P7. 逻辑删除注解 `@Where` 在 Hibernate 6.2+ 中已废弃

- **所在位置**：第 3.2 节 BaseEntity 字段定义（line 234）
- **严重程度**：轻微
- **问题描述**：设计在 BaseEntity 中使用 `@Where` 注解实现软删除的自动过滤。但在 Hibernate 6.2 及以上版本（Spring Boot 3.1+ 使用的 Hibernate 版本）中，`@Where` 已被标记为废弃，官方推荐的替代注解为 `@SQLRestriction`。虽然 `@Where` 在当前版本仍然可以运行，但后续版本可能会移除该注解。设计文档作为 Phase 0 骨架的未来演进参考，应使用当前推荐的 API。
- **改进建议**：将 `@Where(clause = "deleted = false")` 替换为 `@SQLRestriction("deleted = false")`。同时注意 `@SQLDelete` 的 SQL 语句模板应明确定义（如 `@SQLDelete(sql = "UPDATE {entity} SET deleted = true WHERE id = ?")`），以免各模块自行猜测 SQL 模板格式。

---

## 整体评价

经过第 5 轮审议迭代，该设计在全部 7 个需求维度上覆盖完整（6 个维度完整、1 个维度部分），核心骨架定义清晰、无方向性设计错误。上述 7 个问题中，P2（跨模块调用）和 P3（包扫描策略）是影响"设计是否可直接指导编码"的关键缺口；P1 和 P5 为小幅事实修正；P4、P6、P7 为轻微问题。建议修复者在进入编码阶段前优先处理 P2 和 P3，以消除实现阶段的主要不确定性。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 报告声明以"需求响应充分度"为首要审查方向，但未进行系统性覆盖评估，整体评价中"覆盖了所有 7 个设计维度"的结论缺乏逐维度论证 | 接受。已新增「需求覆盖评估」章节，以表格形式对 7 个需求维度逐一评估覆盖状态（完整/部分/缺失），并标注关键差距。通过该表可清晰定位需求覆盖的薄弱环节为第 4 维度的跨模块调用模式定义。同时将整体评价中的覆盖断言从此前的一句结论改为有表格为支撑的明确表述 |
| P2（CI 流水线阶段定义重复行）属于文档校对范畴的格式问题，与架构质量审查任务无关，不应列入问题清单 | 接受。已从问题清单中删除 P2。该重复行仍然存在于设计文档中，但属于文档排版瑕疵，不影响架构质量评估 |
