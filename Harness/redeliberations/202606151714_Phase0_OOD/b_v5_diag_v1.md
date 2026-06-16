# Phase 0 OOD 设计 v5 质量审查报告

## 审查范围

本报告对产出的第 5 轮设计方案（`a_v5_design_v1.md`）进行质量审查，重点关注内部审议未充分覆盖的维度：需求响应充分度、文档深度和完整性、实际落地可行性。避免重复验证已处理的细项。

---

## 问题清单

### P1. `common` 模块依赖描述与 SecurityConfig 实际需求矛盾

- **所在位置**：第 2.2 节（line 117）"`common`：零依赖（仅依赖 Spring Boot Starter 基础库），所有模块可依赖它" 与 第 4.5 节（line 442-443）"spring-boot-starter-security 为其必需依赖"
- **严重程度**：一般
- **问题描述**：第 2.2 节声明 common 模块为"零依赖，仅依赖 Spring Boot Starter 基础库"，但第 4.5 节要求 common 模块的 `common.config` 包中存放 SecurityConfig，从而必须引入 `spring-boot-starter-security`。`spring-boot-starter-security` 并不属于"基础库"范畴（它不是一个通用基础设施 starter，而是特定的安全框架）。两条描述存在事实矛盾，编码人员无法据此确定 common 模块的 POM 依赖范围。
- **改进建议**：修订第 2.2 节 common 的依赖描述，如实反映包括 `spring-boot-starter-security` 在内的完整依赖集。例如："依赖 Spring Boot 核心 Starters（spring-boot-starter-web、spring-boot-starter-data-jpa、spring-boot-starter-security 等基础骨架所需）"，消除"零依赖"的误导性表述。

---

### P2. CI 流水线阶段定义存在重复行

- **所在位置**：第 10 节（line 760-761）
- **严重程度**：轻微
- **问题描述**："第三阶段（聚合层）: mvn install -DskipTests -pl application" 在 CI 流水线列表中出现两次，为完全相同的重复行。这会导致 CI 执行时重复构建同一个模块，或至少给读者造成阶段编号/顺序的混淆。
- **改进建议**：删除重复行。

---

### P3. 跨业务模块调用机制未定义，不足以指导编码实现

- **所在位置**：第 2.2 节（line 20-21）
- **严重程度**：一般
- **问题描述**：设计仅定义了模块间依赖方向（谁依赖谁），但未定义实际的跨模块调用编码模式。文档在第 2.2 节提及"同层模块之间不允许直接依赖，通过公共门面（facade）或事件解耦"，但 "公共门面"和"事件解耦"均无具体的接口形式、注册机制或调用示例。实际医疗场景中（如患者挂号、医生接诊等流程），业务模块间的数据交换不可避免——若不加定义，各模块开发者在遇到跨模块需求时会自行发挥，产生多种不兼容的调用模式。尽管 Phase 0 无业务逻辑不立即暴露此问题，但该决策应在架构层面明确。
- **改进建议**：补充跨模块调用规范章节，明确具体模式（至少给出推荐方案，如「common-module 定义门面接口 + 各模块提供实现注入」或「Spring ApplicationEvent 事件驱动」），并附简短编码示例。建议将其作为第 2.2 节的补充或新增第 2.5 节。

---

### P4. Spring Boot 包扫描策略缺失，骨架可运行缺少关键前提

- **所在位置**：缺失（应在第 9.2 节或启动模块说明中）
- **严重程度**：一般
- **问题描述**：多模块 Maven 项目中，application 模块的 `@SpringBootApplication` 需要通过 `scanBasePackages` 显式声明包扫描范围（例如 `@SpringBootApplication(scanBasePackages = "com.aimedical")`），否则 Spring 无法发现 common、modules/* 等子模块中的 Bean（Controller、Service、Configuration 等）。当前设计文档在"骨架可运行"和"一键启动"部分均未涉及此机制。新人开发者直接依文档编码：在 application 模块写 `@SpringBootApplication`（默认只扫描 application 模块），启动时将找不到 common 和 modules 中的任何 Spring Bean。
- **改进建议**：在第 9.2 节（多模块构建依赖）或新增的"启动说明"中，明确 `@SpringBootApplication(scanBasePackages = "com.aimedical")` 的配置方式，并说明需配合 `@EntityScan` 和 `@EnableJpaRepositories` 确保 JPA 实体和 Repository 被扫描。

---

### P5. `BusinessException` 未明确继承层次，事务行为不确定

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

### P6. 自定义 `PageRequest` 与 Spring Data 的 `PageRequest` 类名冲突未处理

- **所在位置**：第 3.1 节（line 197）
- **严重程度**：一般
- **问题描述**：设计文档定义了一个自定义 `PageRequest` 类（位于 `com.aimedical.common.result` 包），与 Spring Data JPA 的 `org.springframework.data.domain.PageRequest` 类名完全相同。在编码时，Controller 层需导入自定义 PageRequest，Repository 层需导入 Spring Data 的 PageRequest，同一文件中同时使用二者时将产生导入冲突，需使用全限定名区分。文档未说明 Controller 层的自定义 PageRequest 如何转换为 Repository 层的 Spring Data Pageable，分页参数在不同层之间的传递路径不清晰。
- **改进建议**：方案一：将自定义类重命名为 `PageQuery` 或 `PageCriteria`，彻底消除命名冲突。方案二：明确声明自定义 PageRequest 仅作为 API 层的请求 DTO，在 Service 层统一转为 Spring Data 的 `Pageable`，并在文档中以伪代码示例说明转换逻辑。推荐方案一。

---

### P7. `FallbackAiService` 零实现回退的兜底路径未定义

- **所在位置**：第 3.4 节（line 354）
- **严重程度**：轻微
- **问题描述**：`FallbackAiService` 通过 `@Autowired` + `@Lazy` + `ObjectProvider<AiService>` 延迟获取底层实现，但未定义当 `ObjectProvider.getIfAvailable()` 或 `getIfUnique()` 返回 `null`（即因配置错误导致没有任何 AiService 实现被注册）时的处理逻辑。在配置错误的极端场景下，业务代码调用 AI 能力时将从 `ObjectProvider.getObject()` 抛出 `NoSuchBeanDefinitionException` 或触发 `NullPointerException`。
- **改进建议**：补充 `FallbackAiService` 的兜底逻辑：当无可用的 AiService 实现时，直接返回一个标记 `degraded=true` 的默认 `AiResult`，而非传播未处理的异常。

---

## 整体评价

该设计经过 5 轮审议迭代已趋于成熟，核心骨架（模块划分、依赖规则、接口契约框架、权限模型、AI 抽象层）定义清晰，覆盖了需求中所有 7 个设计维度。未发现导致方向性错误的严重问题。

上述 7 个问题中，P3、P4、P6 涉及"从设计到编码"的桥梁细节，直接影响产出是否真正可指导编码实现；P1 和 P5 属于小幅事实修正；P2 和 P7 为轻微问题。建议修复者在进入编码阶段前优先处理 P3~P6，以消除实现阶段的不确定性。
