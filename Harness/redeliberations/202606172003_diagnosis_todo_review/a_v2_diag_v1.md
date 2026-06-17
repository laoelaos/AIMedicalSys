# 诊断报告：审查报告 `todo.md` 逐条分析

## 总体结论

| 编号 | 分类 | 优先级 | 修复影响范围 | 真实存在 | 误报 | OOD文档问题 | 其他 |
|------|------|--------|-------------|---------|------|-----------|------|
| T1 | PageQuery缺少校验注解 | **高** | 所有分页接口的校验层 | ✓ | | | |
| T2 | common-module-impl缺少config包目录 | 中 | 工程结构，无运行时影响 | ✓ | | | |
| T3 | 父POM Starter冗余版本号 | **高** | 父POM依赖管理的可维护性 | ✓ | | | |
| T4 | 父POM h2误设scope | **高** | h2依赖在构建链路中的scope行为 | ✓ | | | |
| T5 | common缺少MeterRegistryCustomizer | 低 | 可观测性骨架，无关验收 | | | ✓ | |
| T6 | Axios未实现Result.code拆包 | **高** | 前端API调用层的契约行为 | ✓ | | | |
| T7 | ai-impl冗余common依赖 | 中 | ai-impl的依赖维护 | ✓ | | | |
| T8 | common-module-impl冗余common依赖 | | | | ✓ | | |
| T9 | common缺少util包 | 中 | 工程结构，与T2同质 | ✓ | | | |
| T10 | FallbackAiService日志时机不一致 | 中 | FallbackAiService的日志行为 | ✓ | | | |
| T11 | BaseEntityTest未验证审计自动填充 | 中 | 单元测试覆盖率 | ✓ | | | |

### 分类修正说明

- **T5 → OOD文档问题**：OOD §10.1 在正文中以设计规范形式描述 MeterRegistryCustomizer，但同段声明"该骨架为推荐补齐项，不影响 Phase 0 骨架验收"；路线图 0.2 将其列为"推荐补齐"类。OOD 正文与阶段性声明间的矛盾导致审查工具有理由将其标记为缺失，但代码实际正确遵循了"可选"语义。根因在 OOD 文档的呈现方式，而非代码缺陷。
- **T8 → 误报**：common-module-impl 的 permission 实体（User.java:19、Role.java:15、Post.java:17、Function.java:14）均 `extends BaseEntity`，BaseEntity 来自 common 模块。common 是 common-module-impl 的真实直接编译依赖，并非通过 common-module-api 获得的传递性依赖。审查报告错误地将此标记为冗余。
- **T10 → 同时标注真实存在与OOD文档问题**：代码确实未实现 OOD §3.4 的"构造器阶段 ERROR"要求。但 OOD 未考虑 Spring 构造器依赖注入的工程约束，详见 T10 分析。

---

## T1 — PageQuery 缺少 `@Min(0)` / `@Max(500)` 校验注解

**分类**：真实存在 | **优先级**：高

**现象**：`PageQuery.java:7-9` 的 `page` 和 `size` 字段仅有默认值，缺少 `jakarta.validation.constraints.Min` 和 `jakarta.validation.constraints.Max` 注解。

**根因**：OOD §3.1 明确要求 `page` 标注 `@Min(0)`、`size` 标注 `@Max(500)`，当前代码未添加。

**修复者行动指引**：
1. 在 `PageQuery` 的 `page` 字段添加 `@Min(0)`，`size` 字段添加 `@Max(500)`
2. **前置条件**：校验注解生效需要 Controller 方法参数同时标注 `@Valid`（或 `@Validated`）。当前骨架无分页 Controller（仅占位 ping），但修复时需确保未来所有分页接口的 Controller 参数标注 `@Valid`。OOD §3.1 已明确"所有分页 Controller 参数需标注 `@Valid`"，执行者需在新增分页 Controller 时同步添加该注解。
3. 需确认 `spring-boot-starter-validation` 已作为 compile scope 依赖引入到含 Controller 的模块（patient/doctor/admin）的 pom.xml 中。

**证据**：
- `PageQuery.java:7`：`private int page = 0;` — 无 `@Min(0)`
- `PageQuery.java:9`：`private int size = 20;` — 无 `@Max(500)`

---

## T2 — common-module-impl 缺少 `config/` 包目录

**分类**：真实存在 | **优先级**：中

**现象**：OOD §2.3 包命名规范要求 common-module-impl 包含 `permission/`、`config/`、`dict/` 三个子包。实际代码中 `dict/` 和 `permission/` 已存在，但 `config/` 缺失。

**审查描述偏差**：todo.md T2 描述"当前仅实现了 permission 包"不精确——`dict/` 实际已存在。真实问题是缺少 `config/` 而非缺少 `config/`和`dict/`。建议修正 todo.md T2 描述为"当前缺少 config/ 包"。

**根因**：代码实现未覆盖 OOD 指定的全部子包时序（`config/` 在 OOD 包命名规范中存在但未实现）。

**修复者行动指引**：在 `common-module-impl/src/main/java/com/aimedical/modules/commonmodule/` 下创建 `config/` 目录，放入占位说明文件（如 `package-info.java`）作为 Phase 0 占位，待 Phase 1 起补充业务级配置类。

**证据**：
- 目录 `common-module-impl/src/main/java/com/aimedical/modules/commonmodule/` 下仅有 `dict/` 和 `permission/`，缺少 `config/`

---

## T3 — 父 POM dependencyManagement 中 Starter 依赖冗余显式版本号

**分类**：真实存在 | **优先级**：高

**现象**：`pom.xml:84-109` 中 `spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-security`、`spring-boot-starter-validation`、`spring-boot-starter-test` 均显式标注了 `<version>3.2.5</version>`。

**根因**：父 POM 继承自 `spring-boot-starter-parent:3.2.5`，该 parent 的 BOM 已统一管理所有 Spring Boot Starter 的版本。dependencyManagement 中重复标注版本号违反"版本由 BOM 统一管理"的原则，且增加升级时遗漏同步的风险。

**修复关联**：T3 与 T4 同处 `pom.xml` 的 `<dependencyManagement>` 节，建议合并为一个 PR 提交，避免对同一文件产生多份修改冲突。

**证据**：
- `pom.xml:84-109`：五个 Starter 均显式声明 `<version>3.2.5</version>`

---

## T4 — 父 POM dependencyManagement 中 h2 依赖误设 scope

**分类**：真实存在 | **优先级**：高

**现象**：`pom.xml:82` 中 h2 依赖条目在 `<dependencyManagement>` 内设置了 `<scope>runtime</scope>`。

**根因**：OOD §2.2 及 §9.1 明确约定 h2 及其 scope 仅由 application 模块自行声明，父 POM 的 dependencyManagement 只负责管理版本号。当前写法强制所有继承该 dependencyManagement 的模块继承 runtime scope，与"仅 application 使用 h2"的设计矛盾。

**修复关联**：T4 与 T3 同处 `pom.xml` 的 `<dependencyManagement>` 节，建议合并处理。

**证据**：
- `pom.xml:78-83`：h2 条目含 `<scope>runtime</scope>`
- OOD 父 POM 骨架示例（§2.1）：h2 条目无 scope

---

## T5 — common 模块缺少 MeterRegistryCustomizer 占位配置

**分类**：**OOD文档问题** | **优先级**：低

**现象**：OOD §10.1 要求在 `com.aimedical.common.config` 包中声明 `MeterRegistryCustomizer` 配置类占位，当前不存在。

**根因分歧说明**：OOD §10.1 第 1 段声明"该骨架为推荐补齐项，不影响 Phase 0 骨架验收"，路线图 §0.2 也将其列为"推荐补齐"类。OOD 在正文中以设计规范形式描述该配置类的具体位置和内容，又同时声明其可选性，两者自相矛盾。代码不包含该配置类是正确定遵循"可选"语义的结果，而非缺陷。根因在 OOD 文档的呈现方式——在设计规范正文中包含可选实现项会导致审查工具产生误报。

**证据**：
- `common/src/main/java/com/aimedical/common/config/` 目录下现有 `GlobalExceptionHandler.java`、`JacksonConfig.java`、`JpaConfig.java`，无 MeterRegistryCustomizer 相关文件
- OOD §10.1："该骨架为推荐补齐项，不影响 Phase 0 骨架验收"

---

## T6 — Axios 响应拦截器未实现 OOD §4.2 规定的 Result.code 拆包逻辑

**分类**：真实存在 | **优先级**：高

**现象**：`index.ts:10-26` 的 Axios 响应拦截器直接返回 `response.data`（完整 `Result<T>` 包装体），未按 OOD §4.2 对 `Result.code` 做拆包。

**根因**：OOD §4.2 明确规定前端响应拦截器行为：
- `response.data.code === "SUCCESS"` → 返回 `response.data.data`
- `response.data.code !== "SUCCESS"` → 走错误处理

当前拦截器仅执行 `return response.data`，导致各 API 调用方获得的是完整包装体而非解包后的业务数据。

**现有错误拦截器路径分析**：`index.ts:14-25` 的 error 回调已处理四类错误：`NETWORK_ERROR`（网络不可达）、`UNAUTHORIZED`（401）、`FORBIDDEN`（403）、`HTTP_ERROR`（其他状态码），均返回无 `data` 字段的 `{ code, message }` 对象。若 success 拦截器实现拆包（`response.data.code === "SUCCESS"` 时返回 `response.data.data`），需注意：
- 拆包后返回值去掉了外层 `Result` 包装，`apiGet`/`apiPost` 等包装函数的返回类型应从 `Promise<ApiResponse<T>>` 调整为 `Promise<T>`
- 现有 error 回调返回 `{ code, message }` 无 `data` 字段，不会触发拆包逻辑，无需联动修改

**证据**：
- `index.ts:11-12`：`(response) => { return response.data as ApiResponse<unknown> }` — 未检查 `code` 字段

---

## T7 — ai-impl POM 声明了冗余的 common 直接依赖

**分类**：真实存在 | **优先级**：中

**现象**：`ai-impl/pom.xml:17-20` 同时声明了 `ai-api` 和 `common`。ai-impl 的 Java 代码无任何对 common 类型（`com.aimedical.common.*`）的直接引用，全部依赖来自 ai-api 的接口和 DTO。

**根因**：ai-impl 的所有 import 均来自 `com.aimedical.modules.ai.api.*`、`java.*`、`org.*`，无一导入 common。`common` 作为传递性依赖可通过 ai-api 获得，无需重复声明。当前写法会被 `mvn dependency:analyze` 标记为 Unused declared dependency。

**工程权衡说明**：某些 Maven 实践主张显式声明传递性依赖以增强可读性，但 Maven 官方推荐避免重复声明。本案中因 ai-impl 代码无 common 直接引用，显式声明既不提升可读性（开发者在 ai-impl 源码中看不到对 common 的使用），又增加了 `dependency:analyze` 的误报干扰，应当移除。

**证据**：
- `ai-impl/pom.xml:17-20`：同时声明 `ai-api` 和 `common`
- `ai-api/pom.xml:13-16`：`common` 在 `ai-api` 中以 compile scope 声明
- ai-impl 全量 import 检查：无 `com.aimedical.common` 导入

---

## T8 — common-module-impl POM 声明了冗余的 common 直接依赖

**分类**：**误报** | **优先级**：—

**现象**：todo.md 报告 common-module-impl 同时声明了 `common-module-api` 和 `common`，称 common 可经由 common-module-api 传递获得。

**误报判定**：common-module-impl 中的 permission 实体（`User.java:19`、`Role.java:15`、`Post.java:17`、`Function.java:14`）均 `extends BaseEntity`，而 BaseEntity 定义在 common 模块的 `com.aimedical.common.base` 包中。因此 common 是 common-module-impl 的真实直接编译依赖，`mvn dependency:analyze` 会将其标记为 Used declared dependency，与审查报告的"冗余"判断相反。该条目应从 todo.md 中移除。

**证据**：
- `User.java:19`：`public class User extends BaseEntity`
- `Role.java:15`：`public class Role extends BaseEntity`
- `Post.java:17`：`public class Post extends BaseEntity`
- `Function.java:14`：`public class Function extends BaseEntity`

---

## T9 — common 模块缺少 util 包目录

**分类**：真实存在 | **优先级**：中

**现象**：OOD §2.3 包命名规范要求 common 模块包含 `base`、`result`、`exception`、`util`、`config` 五个子包，当前 `util` 包缺失。

**根因**：代码实现未覆盖 OOD 指定的全部子包。

**修复关联**：T9（缺少 util 包）与 T2（缺少 config 包）同属"OOD 包命名规范未完全对齐"类，均为 Phase 0 工程结构占位缺失。建议合并处理，在对应目录创建 `package-info.java` 占位。

**证据**：
- 目录 `common/src/main/java/com/aimedical/common/` 下仅有 `base/`、`config/`、`exception/`、`result/`，无 `util/`

---

## T10 — FallbackAiService ERROR 日志触发时机与 OOD §3.4 不一致

**分类**：真实存在（代码与 OOD 不符）| **优先级**：中

**现象**：OOD §3.4 规定"启动期输出 ERROR 日志、运行期输出 WARN 日志"，但当前 `FallbackAiService.java:60-67` 的 `handleEmptyDelegates()` 在首次调用时触发 ERROR，而非在构造器/启动期检查。后续调用触发 WARN。

**根因**：代码使用 `AtomicBoolean firstEmptyDelegateCall` 的 once-only 模式实现"首 ERROR → 后续 WARN"切换，但将 ERROR 触发时机从"构造器阶段"延迟到了"首次调用阶段"。

**Spring DI 环境可行性分析**：在 Spring 中，构造器在 Bean 创建阶段（启动期）执行，此时注入的 `List<AiService>` 已可用。在构造器中直接检查 `delegates.isEmpty()` 并输出 ERROR 日志是技术可行的，OOD 的构造器阶段要求并非不切实际。

**当前惰性检测的工程合理性评估**：当前 AtomicBoolean once-only 模式是一种典型的"惰性首次检测"实现，可有效防止重复 ERROR 日志刷屏。但其缺陷是：
- 将启动期的配置错误信号延迟到首次调用时才暴露，运维人员无法在应用启动后立即感知"AI 模块异常配置"状态
- Phase 0 中 FallbackAiService 的兜底保护行为（返回 `AiResult.degraded()`）不依赖该日志时机，功能正确性不受影响

**修复方案对比**（非修复步骤）：
- **方案 A（对齐 OOD）**：在构造器末尾立即调用 `handleEmptyDelegates()` 输出 ERROR（delegates 为空时），方法体改为直接用 `log.isErrorEnabled()` 控制单次输出（移除 AtomicBoolean）
- **方案 B（维持惰性）**：保留当前 AtomicBoolean 模式，但有悖于 OOD 明确要求。若选此方案需修改 OOD 以使文档与实际行为一致

**证据**：
- `FallbackAiService.java:52-58`：构造器仅完成 delegates 过滤和 strategies 赋值，不做空检测
- `FallbackAiService.java:60-67`：`handleEmptyDelegates()` 在第一次调用时输出 ERROR

---

## T11 — BaseEntityTest 未验证审计字段自动填充

**分类**：真实存在 | **优先级**：中

**现象**：OOD §3.2 明确 `createdAt` 由 `@CreatedDate` + `AuditingEntityListener` 自动填充，`updatedAt` 由 `@LastModifiedDate` 自动填充。`BaseEntityTest.java:46-48` 仅通过 `new TestEntity()` 验证了 POJO 级 setter/getter 默认值行为，未在 Spring Data JPA 上下文（`@SpringBootTest` + `@EntityListeners`）中验证审计监听器是否按预期自动填充时间戳。

**修复者行动指引**：需添加 `@SpringBootTest`（或 `@DataJpaTest`）测试类或测试方法，在 JPA 上下文中持久化实体并验证 `createdAt` 和 `updatedAt` 在 `@PrePersist` / `@PreUpdate` 阶段被正确赋值。当前纯 JUnit 5 测试不可用于验证审计行为。

**证据**：
- `BaseEntityTest.java`：纯 JUnit 5 测试类，无 `@SpringBootTest` 或 `@DataJpaTest` 注解
- `shouldCreateWithDefaultValues()` 验证 `assertNull(entity.getCreatedAt())` — 此为 POJO 默认 null，与审计填充后的行为相反
- `shouldSetAndGetTimestamps()` 手动 set 时间戳，绕过了审计机制

---

## 优先级排序说明

基于"影响范围 × 修复风险"评估：

| 优先级 | 条目 | 理由 |
|--------|------|------|
| **高** | T1 | 校验缺失可能引入安全风险（恶意大分页 OOM），修复需确认 @Valid 链路 |
| **高** | T6 | 契约行为偏差导致全部前端 API 消费者需额外拆包，违反统一处理设计目标 |
| **高** | T3 | 版本管理方式增加升级风险，修复为纯删除无副作用 |
| **高** | T4 | scope 错误可能导致构建行为异常，修复为纯删除 scope 无副作用 |
| **中** | T7 | Unused declared dependency，不影响编译运行但污染依赖分析结果 |
| **中** | T9 | 工程结构占位缺失，无运行时影响 |
| **中** | T2 | 工程结构占位缺失，无运行时影响 |
| **中** | T10 | 功能正确性不受影响，仅日志时机与 OOD 不一致 |
| **中** | T11 | 测试覆盖率不足，骨架阶段可接受但需跟踪 |
| **低** | T5 | OOD 文档问题，代码行为正确 |
| — | T8 | 误报，无需处理 |

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| P1：T5 分类内部逻辑矛盾——OOD §10.1 声明为推荐补齐不影响验收，但诊断定性为真实存在 | 已修正。将 T5 分类从"真实存在"改为"OOD文档问题"，并在总体结论表和分类修正说明中阐明 OOD 正文与阶段性声明不一致导致审查混淆的根因。 |
| P2：全量分类为"真实存在"，遗漏 OOD文档问题/其他分类维度 | 已修正。总体结论表增加分类列（真实存在/误报/OOD文档问题/其他），T5 → OOD文档问题，T8 → 误报。 |
| P3：T10 分析过浅，未评估 OOD 构造器检测方案在 Spring DI 中的可行性、当前惰性检测的工程合理性 | 已补充。分析了 Spring DI 环境下构造器阶段检测的可实现性（技术上可行），以及当前 AtomicBoolean once-only 模式的工程利弊（将 ERROR 从启动期延迟到首次调用期，功能正确但运维感知延迟）。 |
| P4：T7/T8 依赖冗余判断仅基于 OOD 依赖图推断，未讨论 Maven 工程实践中"显式声明传递性依赖"的合理性 | 已补充。T7 分析中加入了工程权衡说明（承认显式声明主张但论证本案中不适用）；T8 通过源代码级证据（permission 实体 extends BaseEntity）发现为误报，已在正文详细论证。 |
| P5：缺失可操作性修复指导——T1（需同步 @Valid）、T6（拦截器返回类型影响）、T2/T9（占位类）、T10（修复方案对比） | 已补充。T1：添加 @Valid 前置条件分析和 Controller 参数约束检查指引；T6：分析 error 回调现有处理与拆包后的联动影响；T2/T9：指明占位文件创建方式（`package-info.java`）；T10：给出方案 A（对齐 OOD）与方案 B（维持惰性+修改 OOD）的对比。 |
| P6：优先级排序缺失 | 已补充。增加优先级列和优先级排序说明表，按"影响范围 × 修复风险"划分为高/中/低三档。 |
| P7：T1 未考虑校验注解生效需 Controller 参数同时标注 @Valid 的前置条件 | 已补充。在 T1 修复者行动指引中明确指出 @Valid 前置条件及当前骨架无分页 Controller 的现状。 |
| P8：T2 指出审查描述不精确但未建议修正 todo.md 对应条目描述 | 已补充。在 T2 分析中增加"审查描述偏差"子节，明确指出 todo.md T2 描述"当前仅实现了 permission 包"不精确（dict/ 已存在），应修正为"当前缺少 config/ 包"。 |
| P9：T6 未完整分析错误拦截器路径及拆分成功拦截器后是否需要联动修改 | 已补充。分析了 error 回调现有四类处理（NETWORK_ERROR/401/403/HTTP_ERROR），论证了拆包后 error 路径无需联动修改（error 回调返回无 data 字段的对象，不触发拆包逻辑），但 `apiGet`/`apiPost` 等包装函数的返回类型需从 `Promise<ApiResponse<T>>` 调整为 `Promise<T>`。 |
| P10：T3/T4 同处 pom.xml，跨问题修复冲突未识别 | 已补充。在 T3 和 T4 分析中分别标注"修复关联：T3 与 T4 同处 pom.xml，建议合并为一个 PR 提交"。同样标注了 T2 与 T9 的合并处理建议。 |
