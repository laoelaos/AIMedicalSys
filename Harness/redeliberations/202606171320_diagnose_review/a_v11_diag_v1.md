# 代码评审问题诊断报告

## 诊断信息

- **诊断时间**：2026-06-17
- **数据来源**：`Harness/reviews/202606171248_code_review/`（2轮6份审查报告 + todo + scope + review + known_issues）
- **设计依据**：`Docs/04_ood_phase0.md`（Phase 0 OOD）
- **代码基线**：`AIMedical/` 目录下全部实现代码
- **路线图**：`Docs/03_roadmap.md`

---

## todo.md 覆盖声明

本报告对 `Harness/reviews/202606171248_code_review/todo.md` 所列 **10 项待办事项**（共 11 个 checkbox 条目，含 1 项 `[严重]` 声明 + 10 项待办事项）进行了逐一分析。各问题与 todo 项的对应关系见汇总表"对应 todo 项"列。经分析：

- **10 项待办已全部覆盖**，无遗漏
- **`[严重]`项（无严重问题）**：经核查，10 项诊断结论中无任何问题达到严重级别，确认该断言成立
- **0 项为误报**（所有问题均确认为真实存在）
- **已确认报告内容为问题诊断定位，未进入 initial_artifact 模式**（符合 requirement.md 排除项要求）
- **2 项为真实代码缺陷**（问题8、问题10）
- **1 项为 OOD 文档问题**（问题1）
- **7 项为其他类型问题**（问题2/3/4/5/6/7/9，包括设计与代码偏离、版本偏离、测试覆盖不足等）

---

## 问题1：BaseEnum 为设计文档未定义类型

### 分类

**OOD 文档不完善**。代码实现无误，设计文档缺失对该类型的规范定义。

### 优先级

P2（低优先级 — 纯文档遗漏，无运行时影响，不影响 Phase 0 骨架功能）。

### 是否为误报

**否**。真实性确认：OOD §1.3 核心抽象一览表确实未包含 BaseEnum，且 §2.1 目录布局又明确提及该类型，构成设计文档内部的遗漏性矛盾。

### 修复价值论证

虽无功能影响，但 §1.3 核心抽象一览表的完整性直接服务于设计文档作为"团队共识载体"的角色。遗漏 BaseEnum 意味着新团队成员无法通过阅读 §1.3 感知枚举层基接口的存在，需自行从代码或目录布局推断。修复收益：将文档完整性提升至与代码结构一致，消除阅读者的认知断层。

### 现象

BaseEnum interface 存在于 `common/.../base/BaseEnum.java:1-6`，提供 `getCode()` 与 `getDesc()` 两个方法签名。OOD §2.1 目录布局（`base/` 含 `BaseEntity, BaseEnum`）已预留该类型，但 §1.3 "核心抽象一览"中无任何针对 BaseEnum 的定义、方法命名约定、使用场景或与 ErrorCode 的关系说明。

### 证据链

1. **OOD §2.1 目录布局**（`04_ood_phase0.md:70`）明确列出 `base/` 包下包含 `BaseEntity, BaseEnum`，证明设计层已知该类型存在。
2. **代码层**：`BaseEnum.java` 实际存在，定义为 `public interface BaseEnum`，包含 `String getCode()` 和 `String getDesc()` 两个方法。
3. **OOD §1.3 "核心抽象一览"表**（`04_ood_phase0.md:24-33`）列举了 `Result<T>`、`PageQuery`、`ErrorCode`、`BaseEntity`、`GlobalExceptionHandler`、`AiService`、`Role`/`Post`/`Function`、`User` 共 9 项核心抽象，**不包含 BaseEnum**。
4. **对比 `ErrorCode` 接口**：OOD §1.3 对 ErrorCode 有完整定义（interface 形态、`code()`/`message()` 签名、各模块 enum 实现）。BaseEnum 接口（`getCode()`/`getDesc()`）与 ErrorCode（`code()`/`message()`）方法名不同、职责不同——BaseEnum 用于枚举值通用转换，ErrorCode 用于错误码命名空间契约。

### 根因

OOD 文档在 §2.1 目录布局中预留了 BaseEnum 类型，但在 §1.3 核心抽象定义中遗漏了该类型的规范说明。属于设计文档层面的不完善（编写阶段遗漏），非代码缺陷。BaseEnum 作为枚举通用基接口（提供 code/desc 获取），与 ErrorCode 有本质不同的职责边界，不存在合并或冲突问题。

### 影响范围

- OOD 文档 §1.3 需在下一阶段补充 BaseEnum 定义段落
- 代码侧无任何问题，无需修改

---

## 问题2：父 POM modules 路径与设计 §2.1 不一致

### 分类

**代码与 OOD 的设计偏离**（双向不一致）。

### 优先级

P1（决策优先级 — 方向选择影响后续修复路径，但推荐的方案 B 为低风险文档更新，执行成本低）。

### 是否为误报

**否**。真实性确认：OOD §2.1 目录树与骨架代码均采用分层路径，实际代码和目录结构均为扁平，偏差明确可验证。

### 现象

OOD §2.1 采用分层目录布局：`modules/common-module/common-module-api`、`modules/ai/ai-api`、`modules/patient` 等。实际 POM（`pom.xml:18-29`）采用扁平路径：`common-module-api`、`ai-api`、`patient`（所有模块直接位于 `backend/` 根目录）。

### 证据链

1. **OOD §2.1 目录树**（`04_ood_phase0.md:54-122`）中明确写出 `backend/modules/patient`、`backend/modules/doctor`、`backend/modules/common-module/common-module-api` 等分层路径。
2. **OOD §2.1 父 POM 骨架代码**（`04_ood_phase0.md:127-237`）中 `<modules>` 块明确使用 `modules/common-module/common-module-api`、`modules/ai/ai-api` 等分层路径。
3. **实际目录结构**：`backend/` 下不存在 `modules/` 目录。`Get-ChildItem backend/` 结果为：`admin/`, `ai-api/`, `ai-impl/`, `application/`, `common-module-api/`, `common-module-impl/`, `common/`, `doctor/`, `integration/`, `patient/`, `pom.xml`（共 11 项），均为扁平排列。
4. **实际 `pom.xml`** `<modules>` 声明：`<module>common</module>`, `<module>common-module-api</module>`, ..., `<module>patient</module>`, `<module>integration</module>`——全部为单级相对路径，无 `modules/` 前缀。

### 根因

构建骨架时采用了更简洁的扁平目录布局，与 OOD §2.1 的分层布局产生偏离。具体是哪一方先偏离无法通过现有制品确定，但这是全仓库最大的结构性偏离。

### 影响范围

- 直接关联**问题3**（聚合 POM 缺失）
- 影响 CI 分阶段命令路径（OOD §10 的 CI 命令使用 `-pl modules/...` 路径）
- 影响父 POM 骨架代码引用的可读一致性
- 不影响 Maven 构建解析（reactor 排序仅依赖于 `<module>` 声明的相对路径，无关目录是否分层）

---

## 问题3：聚合 POM 缺失

### 分类

**代码与 OOD 的设计偏离**（问题2 的连带后果）。

### 优先级

P1（决策优先级 — 与问题2 耦合，需先决策目录结构方案再处理，但推荐的方案 B 为低风险文档更新）。

### 是否为误报

**否**。真实性确认：OOD §2.1 明确给出了两个聚合 POM 的骨架代码，glob 扫描确认无对应文件。

### 现象

OOD §2.1 明确要求 `modules/common-module/pom.xml` 聚合 common-module-api 与 common-module-impl，以及 `modules/ai/pom.xml` 聚合 ai-api 与 ai-impl。这两个聚合 POM 均不存在（glob 搜索 `**/modules/**/pom.xml` 无结果）。由于实际采用扁平目录且根 POM 直接引用六个叶子模块（common-module-api/common-module-impl/ai-api/ai-impl/patient/doctor/admin），聚合 POM 的功能完全缺失。

### 证据链

1. **OOD §2.1 聚合 POM 骨架**（`04_ood_phase0.md:239-285`）给出了 `modules/common-module/pom.xml` 和 `modules/ai/pom.xml` 的完整 XML 代码。
2. **glob 扫描**：`Get-ChildItem -Recurse modules/*/pom.xml` 返回空。
3. **根 POM** 的 `<modules>` 直接引用所有六个叶子模块，跳过中间层聚合。

### 根因

直接原因：问题2 的扁平目录布局使得 `modules/` 中间层不存在，聚合 POM 无法按照设计路径创建。根因与问题2 相同——骨架搭建阶段选择了扁平布局而非设计的分层布局。

### 影响范围

- 无聚合 POM 时，无法在 `backend/modules/common-module/` 或 `backend/modules/ai/` 目录下独立构建对应子树
- 根 POM 直接引用叶子模块不影响全量构建（`mvn install` 在根目录正常运行）
- CI 分阶段命令（OOD §10 第一阶段 `-pl common,modules/common-module/common-module-api,modules/ai/ai-api`）中的分层路径现在不可用，需适配扁平路径

---

## 问题4：Spring Boot 版本与设计不一致

### 分类

**代码与 OOD 的版本偏离**（需要双向对齐）。

### 优先级

P2（低优先级 — 3.2.5 与 3.3.0 的 API 差异极小，不影响 Phase 0 骨架功能）。

### 是否为误报

**否**。真实性确认：OOD 骨架代码中 parent version 为 3.3.0，实际 POM 为 3.2.5，差异可逐行验证。

### 现象

OOD §2.1 父 POM 骨架指定 `spring-boot-starter-parent` 版本 `3.3.0`（`04_ood_phase0.md:145`），实际 POM 使用 `3.2.5`（`pom.xml:9`）。

### 证据链

1. **OOD 文档 §2.1 父 POM 骨架**：`<version>3.3.0</version>`。
2. **实际父 POM**：`<version>3.2.5</version>`。
3. **OOD §8.3 异常处理表**（`04_ood_phase0.md:211`）中 springdoc-openapi v2.5.0 的注释明确标注"与 Spring Boot 3.3.x 的兼容性未显式验证"——说明 OOD 编写时已意识到 3.3.0 版本可能存在兼容性风险。
4. **实际版本 3.2.5** 与 springdoc-openapi v2.5.0 的兼容性经过社区验证，更稳妥。

### 根因

OOD 编写时以 3.3.0 作为目标版本（可能是当时的 latest），但实际项目中选用了更保守的 3.2.5（有更多经过验证的兼容数据），OOD 未同步更新。也可能是先有代码（3.2.5），后写设计文档时未与实际版本对齐。

### 影响范围

- `spring-boot-starter-parent 3.2.5` 与 3.3.0 的 API 差异极小，不影响 Phase 0 骨架功能
- 如果未来升到 3.3.0+，需验证 springdoc-openapi 2.5.0 兼容性
- OOD §1.4（line 41）运行时环境要求表中同样引用了"Spring Boot 3.3.0 最低要求"——若选择降 OOD 到 3.2.5，§1.4 也需同步更新（同时需确认 JDK 17+ 的最低要求是否仍成立，因 Spring Boot 3.2.5 对 JDK 17 的支持已验证）

---

## 问题5：dependencyManagement 缺少外部 starter 统一声明

### 分类

**代码与 OOD 的偏差**（设计意图未落实）。

### 优先级

P2（低优先级 — 无功能影响，Spring Boot parent POM 的 BOM 已兜底管理版本）。

### 是否为误报

**否**。真实性确认：OOD 骨架的 dependencyManagement 包含 13 个条目，实际仅含 11 个，缺少的 5 个 starter 条目可逐行比对确认。

### 修复价值论证

功能层面无影响（Spring Boot BOM 已兜底），但设计偏离的实质在于：`dependencyManagement` 在团队中承担着"项目标准依赖清单"的可视化功能。缺少这 5 个 starter 条目意味着：
- 新开发者无法从 `dependencyManagement` 直观感知项目的标准 starter 清单
- 设计文档的"一致性检查"门禁（对照 `dependencyManagement` 列表检视）无法准确执行，因为实际列表与设计列表不匹配
- 这种偏离属于类型④中的"可接受但应纠正"——修复收益为认知效率和门禁检出力，不修复的代价是设计约束的持续弱化

### 现象

OOD §2.2 "依赖管理（父 POM）"要求父 POM 的 `<dependencyManagement>` 统一声明 `spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-security`、`spring-boot-starter-validation`、`spring-boot-starter-test` 的版本（`04_ood_phase0.md:338-342`）。OOD §2.1 父 POM 骨架代码段（`04_ood_phase0.md:162-237`）更是直接包含了这些 starter 的完整 `<dependency>` 条目。但实际 `<dependencyManagement>`（`pom.xml:39-101`）仅注册了内部模块、springdoc-openapi 和 h2，**完全没有上述 5 个 starter 条目**。

### 证据链

1. **OOD 骨架代码**中 `<dependencyManagement>` 包含 13 个条目（6 个内部模块 + 5 starter + springdoc + h2）。
2. **实际 `<dependencyManagement>`** 仅含 11 个条目（9 个内部模块（6 个基础设施模块 + 3 个业务模块）+ springdoc + h2），缺少 web、data-jpa、security、validation、test 五项 starter，多出 patient、doctor、admin 三项业务模块声明。
3. **`pom.xml:39-101`** 逐行确认无上述 starter 条目。

### 根因

实际项目依赖 Spring Boot parent POM 的 BOM 管理这些 starter 版本——即使不在 `<dependencyManagement>` 中显式列出，子模块引入这些 starter 时也能获得正确的版本。开发者可能认为无需重复声明。但 OOD 的设计意图是将它们显式列出作为"统一管理清单"——既是文档化手段，也是门禁可视化依据。根因是开发过程中未按设计骨架补齐这些条目。

### 影响范围

- 功能层面无影响。Spring Boot parent POM 的 BOM 已管理这些版本，子模块使用这些 starter 时版本解析正确。
- 设计文档的"一致性检查"页面（`dependencyManagement` 清单对照）无法准确执行，因为实际列表与设计列表不匹配。
- 新加入项目的开发者查看 `dependencyManagement` 时，无法直观感知项目的标准 starter 清单。

### 修复方向

**推荐：代码向设计对齐**——向 `<dependencyManagement>` 补充 5 个 starter 条目（`spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-security`、`spring-boot-starter-validation`、`spring-boot-starter-test`）。版本无需显式指定，由 Spring Boot parent BOM 管理即可。如果选择保留现状（依赖 parent BOM 兜底管理），应在 `known_issues.md` 中记录该偏离及其理由，作为有意的设计取舍。

---

## 问题6：Common POM 引入 spring-boot-starter-validation (optional)

### 分类

**代码与 OOD 的偏差**。

### 优先级

P2（低优先级 — optional 标记避免传递扩散，当前无运行时影响）。

### 是否为误报

**否**。真实性确认：OOD §2.2 common 模块依赖规约仅列出 web 和 data-jpa，实际 common/pom.xml 包含 validation，偏差可确认。

### 修复价值论证

功能层面无影响（optional 标记阻断了传递扩散），但设计偏离的实质在于：
- OOD §2.2 的"Common 模块依赖传播决策"是一项设计约束——common 模块仅保留自身骨架真正需要的 Starter，避免成为传递依赖的"杂物箱"。validation starter 的加入破坏了这一约束。
- 即使标记为 optional，validation 的 transitive 依赖（Hibernate Validator 相关 jar）仍会出现在 common 模块的编译 classpath 上，可能诱导 future 开发者直接依赖这些 transitive 类型，进一步加深耦合。
- 修复收益为恢复设计约束力；不修复的代价是约束的渐进侵蚀（"common 中已有一个额外依赖，再多一个也没关系"的破窗效应更加危险）

### 现象

OOD §2.2 指定 common 模块的依赖规约仅含 `spring-boot-starter-web (optional)` 和 `spring-boot-starter-data-jpa (optional)`（`04_ood_phase0.md:344-346`）。实际 `common/pom.xml:27-31` 额外引入了 `spring-boot-starter-validation` `<optional>true</optional>`。

### 证据链

1. **OOD §2.2 "Common 模块依赖传播决策"**：仅列出 spring-boot-starter-web (optional) 和 spring-boot-starter-data-jpa (optional)。未提及 validation。
2. **实际 common/pom.xml** 包含三个 optional 依赖：web、data-jpa、validation。
3. **子模块 POM 验证**：patient、doctor、admin 模块的 POM 均已独立声明 `spring-boot-starter-validation`（以 compile scope），并非从 common 传递获得。
4. **用途分析**：common 模块的 `GlobalExceptionHandler` 处理 `MethodArgumentNotValidException`（该异常类位于 `spring-web`，非 validation starter 专有）。common 模块自身无需 `@Valid` 注解或 `Validator` Bean——这些由业务模块的 Controller 使用。因此 common 引入 validation 既不必要（`MethodArgumentNotValidException` 来自 spring-web），也不符合 OOD 的"仅保留自身骨架真正需要的 Starter"原则。

### 根因

common 模块的 `pom.xml` 在初始化时可能参考了某些 Spring Boot 多模块模板，将 validation 作为常见基础依赖一并加入，但未与 OOD §2.2 的 common 依赖规约对照审查。该额外依赖因标记为 `<optional>true</optional>`，不会污染纯契约模块的传递依赖树，实际影响有限。

### 影响范围

- 当前无运行时影响（optional 标记避免传递扩散）。
- 不符合 OOD §2.2 的 common 模块依赖规约，"设计驱动开发"的约束力被削弱。
- validation starter 中 Hibernate Validator 相关的 transitive 依赖会出现在 common 模块的类路径上，但 IDE 中不可见（optional）。

### 修复方向

**推荐：从 common/pom.xml 移除 validation starter**。理由已由本报告提供——`MethodArgumentNotValidException` 异常类来自 `spring-web`（spring-boot-starter-web 的 transitive 依赖），非 validation starter 专有；子模块（patient/doctor/admin）已独立声明 validation starter 为 compile 依赖。移除前需确认 common 模块是否有其他类型依赖于 validation starter：经核查，common 模块代码未使用 `@Valid` 注解、`Validator` Bean 或任何 validation starter 专有类型，移除安全。

---

## 问题7：父 POM maven-dependency-plugin 额外忽略 business 模块

### 分类

**代码与 OOD 的设计偏离**（配置范围超出 OOD 约定的豁免范围，与问题5/6 同为设计意图未落实的类别，不涉及运行时失败或错误行为）。

### 优先级

P1（中优先级 — 当前无运行时影响，但削弱了 dependency:analyze 的门禁检出力，长期隐患）。

### 是否为误报

**否**。真实性确认：OOD §2.2 的 ignoredUnusedDeclaredDependencies 仅含 ai-api 和 common-module-api，实际 POM 额外包含 patient/doctor/admin 三个条目，偏差可确认。

### 现象

OOD §2.2 "依赖分析门禁"规定 `<ignoredUnusedDeclaredDependencies>` 仅包含 `ai-api` 和 `common-module-api` 两个条目（`04_ood_phase0.md:329-332`）。实际 `pom.xml:109-115` 额外添加了 `patient`、`doctor`、`admin` 三个豁免条目。

### 证据链

1. **OOD §2.2 配置代码**（`04_ood_phase0.md:324-335`）：`ignoredUnusedDeclaredDependencies` 中仅列出 `com.aimedical:ai-api` 和 `com.aimedical:common-module-api`。OOD 的论证是：这两个是 Phase 0 业务模块"声明但暂未引用的 api 模块依赖"，属于有意的延迟引用策略，因此在 Phase 0 需要豁免。
2. **实际 POM 配置**（`pom.xml:109-115`）：额外增加了三个条目 `<ignoredUnusedDeclaredDependency>com.aimedical:patient</ignoredUnusedDeclaredDependency>`（同理 doctor/admin）。
3. **继承性分析**：`<configuration>` 在父 POM 的 `<plugin>` 中定义，该配置会被所有子模块继承。当在 business 子模块（如 patient）上运行 `dependency:analyze` 时，正常的 `dependency:analyze` 会在 patient 模块的 declared dependencies 中检查哪些被实际引用——由于 patient 模块在自己的 POM 中声明了 common、common-module-api、ai-api 等依赖，如果 patient 未使用这些类型，会被标记为 unused。但 patient/doctor/admin 作为 artifactId，不可能是 patient 模块自身声明的依赖，所以 `com.aimedical:patient` 的 ignore 条目仅在 application 模块（或父 POM 级别的 reactor 运行）情境下有意义。
4. **设计意图 vs 实际行为的偏差**：OOD 明确声明"Phase 1+ 各业务模块开始注入 `AiService`、`UserDTO` 等 api 模块类型后，逐模块移除对应豁免条目，使 `dependency:analyze` 恢复完整检出力"——说明保留业务模块（patient/doctor/admin）的豁免条目绕过了这一机制，即使 Phase 1+ 产生真实引用后，被掩盖的警告也不会被检测出。

### 根因

可能因为开发过程中在某次 `dependency:analyze` 运行时看到了对 patient/doctor/admin 的 unused declared dependency 警告，为快速通过 CI 将这三个条目加入豁免列表。这违背了 OOD 的设计意图——这些 business 模块是 application 的真实编译依赖，不应该是被豁免的目标。

### 影响范围

- 在 application 模块上运行 `dependency:analyze` 时，如果 patient/doctor/admin 在 application 的 POM 中被声明但未使用（这在 Phase 0 确为事实——application 模块的 Application.java、HealthController.java、SecurityConfigPhase0.java 均未在字节码层面引用 patient/doctor/admin 中的任何类型，因此移除 ignore 条目后 dependency:analyze 会将其标记为 unused declared dependencies），会被豁免不报。
- 更关键的影响：该配置继承到所有子模块，如果在某个子模块（如 patient）中错误地将 unused 依赖添加到 POM，该机制无法检出。

---

## 问题8：GlobalExceptionHandler 缺少 HttpMessageNotReadableException / HttpMessageNotWritableException 专用处理器

### 分类

**代码缺陷**（设计明确要求实现的功能未实现）。

### 优先级

P2[Phase 0] → P0(Phase 1+)（当前 Phase 0 下为 P2 低优先级：所有 Controller 均为 `@GetMapping` 且不接收 `@RequestBody`，`HttpMessageNotReadableException` 不存在触发路径，无运行时影响；Phase 1+ 引入 POST/PUT 请求体后自动升为 P0——请求体 JSON 格式错误时将错误返回 500 而非 400，前端可能被错误引导）。

**激活条件**：首个 `@PostMapping`/`@PutMapping` 方法（接收 `@RequestBody`）被引入项目时，此问题自动升为 P0 并应立即修复。

### 是否为误报

**否**。真实性确认：OOD §5.3 和 §5.1 错误分类表均明确要求两个序列化异常专用 handler，代码确认缺失。

### 现象

OOD §5.3（`04_ood_phase0.md:839`）明确要求"`HttpMessageNotReadableException` / `HttpMessageNotWritableException` 等序列化异常统一在 `GlobalExceptionHandler` 中注册 `@ExceptionHandler` 方法，避免 Spring 默认的错误响应格式污染统一响应契约"。OOD §5.1 错误分类表（`04_ood_phase0.md:812-813`）明确规定了这两个异常的不同 HTTP 状态码：请求体序列化错误 → 400，响应体序列化错误 → 500。

当前 `GlobalExceptionHandler.java:30-35` 仅通过 `@ExceptionHandler(Exception.class)` 兜底捕获，实际返回 500 SYSTEM_ERROR——无法区分"请求体序列化错误（应 400）"与"系统异常（应 500）"。

### 证据链

1. **OOD §5.3 原则**（`04_ood_phase0.md:839`）："序列化异常统一在 GlobalExceptionHandler 中注册 @ExceptionHandler 方法"。
2. **OOD §5.1 错误分类表**：`HttpMessageNotReadableException → HTTP 400`；`HttpMessageNotWritableException → HTTP 500`。
3. **实际代码**（`GlobalExceptionHandler.java:30-35`）：仅有三个 handler——
   - `BusinessException` → 400
   - `MethodArgumentNotValidException` → 400
   - `Exception` → 500 （兜底）
   不存在 `HttpMessageNotReadableException` 或 `HttpMessageNotWritableException` 的专用 handler。
4. **运行时行为分析**：当请求体 JSON 格式错误时，Spring MVC 抛出 `HttpMessageNotReadableException`，由于无专用 handler，该异常被 `@ExceptionHandler(Exception.class)` 以 500 SYSTEM_ERROR 捕获，返回 `{"code":"SYSTEM_ERROR","message":"系统异常"}`。前端期望接收 400 状态码和 `PARAM_INVALID` 以展示友好的参数校验提示。

### 根因

`GlobalExceptionHandler` 在 Phase 0 实现过程中只完成了"最小功能集合"（BusinessException、MethodArgumentNotValidException、通用 Exception），遗漏了 OOD §5.3 明确要求的两个序列化异常专用处理器。属于实现阶段的遗漏性缺陷。

### 影响范围

- **Phase 1+ 场景**：
  - 请求体 JSON 格式错误时：返回 HTTP 500 SYSTEM_ERROR 而非期望的 400 PARAM_INVALID
  - 响应体序列化错误时：兜底 500 SYSTEM_ERROR（结果与设计一致，但违反了"异常应统一注册 @ExceptionHandler"的设计原则）
  - 前端错误处理逻辑若依赖 400 状态码区分"请求格式错误"与"系统异常"，将受到错误引导
- **Phase 0 场景**：所有 Controller 均为 `@GetMapping` 且不接收 `@RequestBody`（`PatientController.java:19`、`DoctorController.java:19`、`AdminController.java:19`、`HealthController.java:10`），`HttpMessageNotReadableException` 在 Phase 0 不存在触发路径，无运行时影响
- 不影响骨架的 ping 健康检查和其他正常流程

### 修复方向

**代码向设计对齐**——在 `GlobalExceptionHandler` 中补充两个 `@ExceptionHandler` 方法的实现骨架：

```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
    log.warn("Request body malformed", e);
    return ResponseEntity.badRequest()
            .body(Result.fail(GlobalErrorCode.PARAM_INVALID));
}

@ExceptionHandler(HttpMessageNotWritableException.class)
public ResponseEntity<Result<Void>> handleMessageNotWritable(HttpMessageNotWritableException e) {
    log.error("Response body serialization failed", e);
    return ResponseEntity.status(500)
            .body(Result.fail(GlobalErrorCode.SYSTEM_ERROR));
}
```

**关键决策点**：
- `HttpMessageNotReadableException` → HTTP 400 → 使用 `GlobalErrorCode.PARAM_INVALID`（与 `MethodArgumentNotValidException` 共享同一 ErrorCode，前端可统一按"请求参数格式错误"处理）
- **语义匹配度说明**：`PARAM_INVALID` 的语义为"参数校验失败"，与"请求体 JSON 格式错误"不完全等同——前者暗示参数值不合法（如格式不对、超出范围），后者是语法层面的不可解析。若前端需对两类 400 错误显示不同提示文案（如"请求体格式错误" vs "参数值非法"），需区分 ErrorCode（如新增 `REQUEST_BODY_MALFORMED`）或由前端通过 `message` 字段区分。当前推荐统一使用 `PARAM_INVALID`，在 message 中传入异常详情（如 `e.getCause().getMessage()`），待前端确认是否需要区分后再调整。
- `HttpMessageNotWritableException` → HTTP 500 → 使用 `GlobalErrorCode.SYSTEM_ERROR`（响应体序列化失败属于服务端异常，沿用现有的 SYSTEM_ERROR 响应格式）
- `HttpMessageNotReadableException` 使用 `log.warn`（请求格式错误属于客户端问题，不应记为 ERROR 级别）；`HttpMessageNotWritableException` 使用 `log.error`（服务端序列化失败属于系统异常）

### 测试覆盖要求

新增的两个序列化异常 handler 属于 P0(Phase 1+) 级别代码缺陷修复，必须同步纳入测试覆盖，以保障回归保护。具体要求如下：

1. **测试类**：应在 `GlobalExceptionHandlerTest`（如不存在则新建）中添加 `@WebMvcTest` 测试，通过 MockMvc 发送格式错误的 JSON 请求体（如 `{bad-json}`）来触发 `HttpMessageNotReadableException`，验证返回 HTTP 400 及 `PARAM_INVALID` 错误码。
2. **日志级别验证**：通过 `ListAppender` 或 `@SpyBean` 方式验证 `HttpMessageNotReadableException` handler 使用 `log.warn` 级别（不应为 error），`HttpMessageNotWritableException` handler 使用 `log.error` 级别。
3. **兼容性注意**：新 handler 使用 `HttpMessageNotReadableException` 和 `HttpMessageNotWritableException` 类（位于 `org.springframework.http.converter`），该包已由 `spring-boot-starter-web` 引入，无需新增依赖。测试方法应使用 Junit 5 + MockMvc 模式（与项目现有测试风格保持一致）。
4. **Phase 0 测试策略**：在 Phase 0 阶段，所有 Controller 均为 `@GetMapping` 且不接收 `@RequestBody`，无法通过 MockMvc 直接触发序列化异常。测试可改为通过 `MockHttpServletRequest` 模拟或编写纯 `@ExtendWith(SpringExtension.class)` 单元测试，直接调用 handler 方法传入异常实例来验证响应格式和日志级别。Phase 1+ 首个 `@PostMapping` 引入后，应补充 MockMvc 集成测试以覆盖完整的 RequestBody → Exception → Handler 链路。这一策略应记录在 known_issues.md 中（参见 known_issues.md 变更建议汇总表）。

---

## 问题9：FallbackAiServiceTest 未验证空委托列表的日志输出

### 分类

**测试覆盖不充分**（代码无缺陷）。

### 优先级

P2（低优先级 — 当前代码正确实现了日志输出，仅缺少回归保护）。

### 是否为误报

**否**。真实性确认：测试代码确实缺少日志验证断言，且 known_issues.md K3 已记录该日志行为的已知偏差，可交叉印证日志行为在关注范围内。

### 现象

`FallbackAiService.java:60-67` 的 `handleEmptyDelegates()` 方法使用 `AtomicBoolean firstEmptyDelegateCall` 在首次空委托调用时输出 `log.error(...)`（启动期语义），后续调用输出 `log.warn(...)`（运行期语义），符合 OOD §3.4 的"启动期输出 ERROR 日志、运行期输出 WARN 日志"要求。

`shouldReturnFallbackResultWhenNoDelegateAvailable()` 测试（`FallbackAiServiceTest.java:34-42`）验证了返回值的 `degraded=true`、`success=false`、`fallbackReason="No available AiService delegate"`，但未通过日志附加器验证日志的输出级别和内容。

### 证据链

1. **OOD §3.4 兜底保护**（`04_ood_phase0.md:677`）："启动期输出 ERROR 日志、运行期输出 WARN 日志"。
2. **代码实现**（`FallbackAiService.java:60-67`）：`log.error("No available AiService delegate")` 和 `log.warn("No available AiService delegate")` 的分支逻辑正确。
3. **测试代码**（`FallbackAiServiceTest.java:34-42`）：仅 4 个 `assertEquals/assertTrue/assertFalse` 断言，无任何日志验证相关代码（未使用 `ListAppender`、未 Mock Logger 等）。
4. **known_issues.md K3**（line 7）已记录 FallbackAiService 空委托 ERROR 日志的已知偏差：文档描述与实际实现间存在细微偏差（ERROR 日志触发时机为首次调用而非启动期）。引用 K3 可确认日志行为是已知的关注点，测试覆盖不足的问题与已知偏差相互印证。

### 根因

测试编写时聚焦于功能验证（返回值的语义正确性），遗漏了日志行为的验证。虽然代码正确实现了日志输出，但缺乏测试覆盖意味着日志行为不受回归保护——未来如果删除或修改日志输出，测试不会失败。

### 影响范围

- 当前代码无缺陷——日志输出的分支逻辑和内容完全符合 OOD 要求。
- 日志行为无测试保护，后续重构有退化风险。
- known_issues.md K3 记录的日志触发时机偏差（首次调用 vs 启动期）如未解决，测试也无法暴露该偏差。
- **实现可行性分析**：两种日志验证方案的技术前提和推荐选择如下：

| 方案 | 技术前提 | 实施难度 | 推荐度 |
|------|---------|---------|-------|
| Logback `ListAppender` | logback-classic 需在 test classpath（Spring Boot 项目已自带，`spring-boot-starter` → `spring-boot-starter-logging` → logback-classic）；需访问 logback 内部 API（`ch.qos.logback.classic.Logger`） | 低：无需额外依赖，仅需在测试中按 `((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(FallbackAiService.class)).addAppender(appender)` 模式注入，测试结束时 `detachAndStopAppender` | **推荐** |
| Mock Logger | 需引入 PowerMock/Mockito Inline 或重构代码以支持 Logger 注入（如 `setLogger()` 方法或 LoggerFactory 代理） | 高：需额外依赖或代码重构，可能引入字节码级 Mock 的兼容性问题 | 不推荐 |

**推荐方案**：Logback `ListAppender`。Spring Boot 项目中 logback-classic 已在 test classpath，无需新增任何依赖。实现骨架：

```java
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

// 在测试方法中：
Logger logger = (Logger) LoggerFactory.getLogger(FallbackAiService.class);
ListAppender<ILoggingEvent> appender = new ListAppender<>();
appender.start();
logger.addAppender(appender);
try {
    // 执行测试...
    assertEquals(1, appender.list.size());
    assertEquals(Level.ERROR, appender.list.get(0).getLevel());
    assertEquals("No available AiService delegate", appender.list.get(0).getFormattedMessage());
} finally {
    logger.detachAndStopAppender(appender);
}
```

注意首次调用触发 ERROR、后续调用触发 WARN 的语义差别（对应 `AtomicBoolean firstEmptyDelegateCall` 的状态切换），测试中应至少覆盖：首次调用验证 ERROR 级别，第二次调用验证 WARN 级别。

**⚠️ known_issues.md K3 冲突警告**：当前代码的 ERROR 日志触发时机（首次调用时）已被 K3 记录为已知偏差——正确设计语义应为"启动期输出 ERROR 日志"。如果按当前代码行为编写断言（首次调用 → ERROR），待 K3 偏差被修复后（改为启动期触发 ERROR），该测试将失效。建议的测试策略：

1. **直接方案（推荐）**：直接对当前代码行为（首次调用 → ERROR、后续 → WARN）编写日志断言，在 ListAppender 代码骨架旁显式注释 "⚠️ 此断言依赖 K3 已知偏差（首次调用触发 ERROR 而非启动期触发），K3 修复后需同步更新断言"，并在 known_issues.md K3 条目中同步记录测试方法位置和依赖关系。

   ```java
   @Test
   void shouldLogErrorOnFirstCallThenWarnOnSubsequent() {
       Logger logger = (Logger) LoggerFactory.getLogger(FallbackAiService.class);
       ListAppender<ILoggingEvent> appender = new ListAppender<>();
       appender.start();
       logger.addAppender(appender);
       try {
           FallbackAiService service = new FallbackAiService(Collections.emptyList(), Collections.emptyList());
           // 首次调用 → ERROR
           service.triage(new TriageRequest()); // 通过 public 方法路径触发 handleEmptyDelegates
           assertEquals(1, appender.list.size());
           assertEquals(Level.ERROR, appender.list.get(0).getLevel());
           assertEquals("No available AiService delegate", appender.list.get(0).getFormattedMessage());

           // 后续调用 → WARN
           appender.list.clear();
           service.triage(new TriageRequest());
           assertEquals(1, appender.list.size());
           assertEquals(Level.WARN, appender.list.get(0).getLevel());
       } finally {
           logger.detachAndStopAppender(appender);
       }
   }
   ```

   此方案的优势：
   - 不依赖虚构的 `@PostConstruct` 方法（当前代码中不存在）
   - 测试通过 public 方法路径（`triage()` 等已有 API 方法）直接触发，与生产调用路径一致
   - 仅需在断言旁显式标注 K3 依赖，待 K3 修复后再同步更新

2. **对齐方案（需先修复 K3，非 Phase 0 推荐）**：先为 `FallbackAiService` 补充 `@PostConstruct` 初始化方法（如 `@PostConstruct public void init()`），将 ERROR 日志触发时机对齐至启动期。然后基于对齐后的行为编写测试（启动期 → ERROR，后续调用 → WARN）。**注意**：当前 `FallbackAiService.java` 中不存在任何 `@PostConstruct` 注解的方法，所以此方案必须先修复 K3 偏差（新增 `@PostConstruct` 方法及注入逻辑）后方可实施；且 `handleEmptyDelegates()` 为 `private` 方法，反射调用前需 `setAccessible(true)` 或通过 public 方法路径触发。此方案涉及对生产代码的修改，不作为 Phase 0 推荐。

---

## 问题10：ApiClient 错误拦截器未按 §3.5 实现 NETWORK_ERROR 处理

### 分类

**代码缺陷**（设计要求的功能未实现）。

### 优先级

P2[Phase 0] → P0(Phase 1+)（当前 Phase 0 下为 P2 低优先级：前端仅含占位页面和健康检查，无实际 API 调用链路会触发网络错误，无运行时影响；Phase 1+ 引入真实 API 调用链路后自动升为 P0——前端无法通过统一 code 判断网络错误，未捕获的 Promise 异常可能产生运行期报错）。

**激活条件**：首个引入真实 Axios 调用（非健康检查）的前端模块被开发时，此问题自动升为 P0 并应立即修复。

### 是否为误报

**否**。真实性确认：OOD §3.5 明确要求错误拦截器返回统一格式，实际代码仅 `return Promise.reject(error)`，功能遗漏可确认。

### 现象

OOD §3.5 "ApiClient"明确规定 Axios 错误拦截器统一捕获网络错误（DNS 解析失败、连接超时、请求被取消等），返回格式 `{ code: "NETWORK_ERROR", message: "网络不可达，请检查网络连接" }`（`04_ood_phase0.md:694`）。当前 `api/index.ts:16-18` 的错误拦截器二参仅执行 `return Promise.reject(error)`，直接透传原始 Axios 错误，未做任何格式转换。

### 证据链

1. **OOD §3.5 错误拦截器要求**（`04_ood_phase0.md:694`）："错误拦截器：Axios 请求/网络错误（DNS 解析失败、连接超时、请求被取消等）由 Axios 错误拦截器统一捕获，返回格式 `{ code: "NETWORK_ERROR", message: "网络不可达，请检查网络连接" }`"。
2. **实际代码**（`packages/shared/src/api/index.ts:16-18`）：
    ```typescript
    (error) => {
      return Promise.reject(error)
    }
    ```
3. **Axios 错误类型判断依据**：当 `error.response` 为 `undefined`（即 Axios 收到 HTTP 响应码之外的异常，如 DNS 解析失败、连接超时、请求被取消等），OOD 要求在此检查分支中返回统一格式的 `Promise.resolve()`。
4. **前端消费假定**：若前端代码依赖 `code === "NETWORK_ERROR"` 判断网络错误（与 `Result.code` 的一致性判断模式相同），当前实现会导致 `code` 字段不存在（原始 `AxiosError` 不含此字段），Promise 链中未被 `.catch` 处理的异常将在运行期抛出 Unhandled Promise Rejection。

### 根因

Phase 0 脚手架生成阶段，前端共享 API 客户端的错误拦截器采用了最简单的"透传"实现（`return Promise.reject(error)`），未按 OOD §3.5 的设计要求实现网络错误的统一格式转换。属于实现阶段的遗漏性缺陷。

### 影响范围

- Phase 1+ 场景下：前端代码无法通过 `code === "NETWORK_ERROR"` 统一判断网络错误——这是 OOD 要求的设计模式；未捕获的 Promise 异常可能在控制台产生报错
- Phase 0 场景下：前端仅含占位页面和健康检查，无实际 API 调用链路会触发网络错误，无运行时影响
- 不影响正常 API 调用（`response.data.code === "SUCCESS"` 分支正常）
- Phase 1+ 如果引入 `AuthStore` 的 401 跳转逻辑，错误拦截器的统一处理能力将更加必要

### 修复方向

**代码向设计对齐**——将错误拦截器从透传改为统一格式转换，实现骨架如下：

```typescript
(error) => {
  if (error.response === undefined) {
    // 网络错误（DNS 解析失败、连接超时、请求被取消等）
    return Promise.resolve({
      code: 'NETWORK_ERROR',
      message: '网络不可达，请检查网络连接',
    })
  }
  // HTTP 错误码处理
  const status = error.response.status
  if (status === 401) {
    // 未授权：可在此处触发登出/跳转登录页逻辑
    return Promise.resolve({
      code: 'UNAUTHORIZED',
      message: '登录已过期，请重新登录',
    })
  }
  if (status === 403) {
    return Promise.resolve({
      code: 'FORBIDDEN',
      message: '无权限访问',
    })
  }
  // 其他 HTTP 错误统一返回
  return Promise.resolve({
    code: 'HTTP_ERROR',
    message: `请求失败（${status}）`,
  })
}
```

**关键决策点**：
- 网络错误（`error.response === undefined`）→ **`return Promise.resolve()`** 而非 `reject`，因为调用方期望通过 `code === "NETWORK_ERROR"` 统一判断错误类型，而非在 catch 分支区分异常来源。使用 `reject` 会迫使每个调用方编写 try/catch，与 OOD 设计的统一 code 判断模式冲突
- 401/403 等 HTTP 错误码**应在本层统一处理**，返回标准格式，避免每个业务模块各自处理认证错误。401 处理可额外触发 `AuthStore.logout()` 或 `window.location.href = '/login'`，但在 Phase 0 阶段可暂不实现实际跳转逻辑，仅返回 UNAUTHORIZED code
- 所有分支统一使用 `Promise.resolve()` 保持与成功拦截器一致的返回格式（成功分支 `return response.data.data`），使前端调用方始终通过 `response.code` 判断成功/失败

**TypeScript 类型兼容性分析**：

修复将错误拦截器从 `Promise.reject(error)` 改为 `Promise.resolve({ code, message })` 后，Axios 响应拦截器的类型签名发生变化，需分析对前端消费方的影响：

1. **当前类型签名**：
   - 成功拦截器返回 `response.data.data`（类型为 `T`，由 `AxiosResponse<T>` 的泛型 `T` 决定）
   - 错误拦截器 `Promise.reject(error)` 不参与 `Promise` resolve 链的类型推断，消费方通过 `.then(data: T => ...)` 获得的类型为 `T`
   - 消费方无需区分成功/失败分支——失败分支由 `.catch` 捕获，类型独立

2. **修复后的类型签名**：
   - 错误拦截器返回 `Promise.resolve({ code: 'NETWORK_ERROR', message: '...' })`（类型为 `{ code: string; message: string }`）
   - 成功拦截器仍返回 `T`
   - 拦截器总返回类型变为 `T | { code: string; message: string }`，意味着 `apiClient.get<SomeType>('/url')` 的返回类型从 `Promise<SomeType>` 变为 `Promise<SomeType | { code: string; message: string }>`

3. **对消费方的影响**：
   - 消费方 `const data = await apiClient.get<SomeType>('/url')` 后，`data` 类型为 `SomeType | { code: string; message: string }`
   - 直接访问 `data.field`（假设 `SomeType` 有 `field` 属性）将产生 TS 编译错误：`Property 'field' does not exist on type '{ code: string; message: string }'`
   - 消费方必须通过类型守卫（type guard）区分成功与错误响应
   - 当前代码中 `types/index.ts` 已定义 `ApiResult<T> = { code: string; message?: string; data?: T }`——若消费方直接使用 `ApiResult<T>` 作为响应类型，则修复后无需额外处理（因为 `{ code, message }` 兼容 `ApiResult<T>` 的子集）

4. **推荐处理方案**：
   - **方案A（推荐）**：定义 discriminated union 类型，利用 `code` 字段作为判别式：
     ```typescript
     // types/index.ts 新增
     type ApiSuccess<T> = { code: 'SUCCESS'; data: T }
     type ApiError = { code: 'NETWORK_ERROR' | 'UNAUTHORIZED' | 'FORBIDDEN' | 'HTTP_ERROR'; message: string }
     type ApiResponse<T> = ApiSuccess<T> | ApiError
     ```
     消费方通过 `if (res.code === 'SUCCESS')` 分支后自动缩小类型获取 `data` 字段。此方案需同步修改成功拦截器返回类型以匹配 `ApiSuccess<T>` 格式。
   - **方案B（最小改动）**：在拦截器的返回类型上使用显式泛型约束——Axios 实例的 `ResponseType` 泛型保持为 `ApiResult<T>`，成功拦截器返回 `response.data`（即 `ApiResult<T>`），错误拦截器返回 `{ code, message }`（与 `ApiResult<T>` 结构兼容），消费方统一使用 `ApiResult<T>` 类型接收，通过 `code` 字段判别后访问 `data`。
   - **方案C（不做类型约束）**：消费方在每次调用处手动 `as SomeType` 断言或编写自定义类型守卫——代码可运行但丧失类型安全性，不推荐

5. **成功拦截器既有类型不一致问题**：当前成功拦截器已存在类型不一致——第12行 `return response.data.data` 返回 `T`，第14行 `return response.data` 返回 `ApiResult<T>`（`response.data` 的完整对象）。这意味着在当前代码中，消费方通过 `apiClient.get<SomeType>()` 获取结果时，实际类型已在成功拦截器层面产生分歧：`code === 'SUCCESS'` 时获得 `T`，其他情况获得 `ApiResult<T>`。这是修复前即存在的既有设计问题，而非本修复的连带影响。

6. **修复者操作前提**：在实施问题10 的修复前，修复者必须先完成以下前置扫描：
   - 扫描所有现有消费方代码（当前仅健康检查等占位页面），确认各调用点期望的响应类型
   - 基于扫描结果，在以下三种类型方案中选择统一方案：
     - **方案A（discriminated union）**：在 `types/index.ts` 中定义 `ApiResponse<T> = ApiSuccess<T> | ApiError`，成功拦截器统一返回 `ApiResponse<T>`，消费方通过 `code` 判别式缩小类型
     - **方案B（统一 ApiResult<T>）**：将成功拦截器从 `return response.data.data` 改为 `return response.data`（即 `ApiResult<T>`），错误拦截器返回 `{ code, message }`（与 `ApiResult<T>` 的子集兼容），消费方统一以 `ApiResult<T>` 类型消费
     - **方案C（维持现状 + 手动断言）**：保持现有不一致的类型签名，消费方在每次调用处 `as SomeType` 断言——不推荐
   - 将选定的类型方案记录到问题10 修复范围内（影响 Phase 1+ 所有新消费方代码）
   - 此前置扫描和类型方案决策应在问题10 错误拦截器实现修改之前完成，避免先实现后因类型不匹配而返工

---

## 总体分析（按 requirement.md 四类标准归类）

依据 `requirement.md` 定义的四种问题类型，对全部 10 项诊断结论的归类统计如下：

| 问题类型 | 包含问题 | 数量 |
|---------|---------|------|
| **① 真实代码缺陷**（设计要求的功能未实现或实现错误） | 问题8（GlobalExceptionHandler 缺少序列化异常处理器）、问题10（ApiClient 错误拦截器未实现 NETWORK_ERROR） | **2 项** |
| **② 误报**（审查报告提出的问题实际不存在） | 无 | **0 项** |
| **③ OOD 文档问题**（设计文档存在矛盾、偏差、不完善或错误） | 问题1（BaseEnum 未在 §1.3 定义） | **1 项** |
| **④ 其他类型问题**（不属于以上三类的问题） | 问题2/3（目录结构与设计偏离）、问题4（版本偏离）、问题5/6/7（POM 配置偏离设计规约）、问题9（测试覆盖不充分） | **7 项** |

**横向对比：类型③与类型④的分类边界**：
- 类型③（OOD 文档问题）仅适用于文档自身存在矛盾、遗漏或错误，代码无问题。问题1 是唯一符合条件者——代码正确实现，文档遗漏定义。
- 类型④（其他类型）中的问题2/3/4/5/6/7 共享同一根因模式：POM 骨架搭建阶段偏离了 OOD §2.1/§2.2 的设计规范。它们与类型③的本质区别在于：偏离存在于代码侧（或代码与设计双向），而非文档自身不完善。
- 问题9（测试覆盖不充分）独立于上述模式，属于测试阶段的质量缺失；问题2/3 的偏离范围最大（全仓库目录结构），问题4/5/6/7 属于局部 POM 配置偏离。

**偏离容忍度标注**（针对类型④问题）：

| 问题 | 偏离类型 | 偏离容忍度 | 理由 |
|------|---------|-----------|------|
| 问题2 | 代码与设计双向偏离 | **必须纠正**（按方案B 更新 OOD） | 影响 CI 命令路径和文档可读性，OOD 与骨架代码的偏差为全仓库最大结构性偏离 |
| 问题3 | 代码偏离设计（连带后果） | **必须纠正**（按问题2 决策结果同步） | 与问题2 耦合，决策后必须对齐 |
| 问题4 | 代码与设计版本偏离 | **可记录为已知偏差**（推荐降 OOD） | 版本差异极小，选择降 OOD 成本最低且无风险 |
| 问题5 | 代码偏离设计 | **可记录为已知偏差**（推荐补充） | 功能无影响，但推荐向设计对齐以恢复门禁可视化；若不修复应记入 known_issues.md |
| 问题6 | 代码偏离设计 | **可记录为已知偏差**（推荐移除） | optional 标记阻断了运行时影响，推荐移除恢复设计约束；若不修复应记入 known_issues.md |
| 问题7 | 代码偏离设计 | **必须纠正** | 削弱了 dependency:analyze 门禁检出力，长期隐患；推荐将 ignore 条目移入 application/pom.xml 而非直接删除 |
| 问题9 | 测试覆盖不足 | **可记录为已知偏差**（推荐补充） | 当前代码无缺陷，但缺少回归保护；known_issues.md K3 已记录 |

**补充说明**：问题2/3/4/5/6/7 虽归入"其他类型"，但共享同一根因模式——POM 骨架搭建阶段偏离了 OOD §2.1/§2.2 的设计规范，详见"跨问题根因模式整合"。

---

## 汇总

| 序号 | 优先级 | 问题摘要 | 分类 | 是否为误报 | 对应 todo 项 | 根因所在 |
|------|--------|---------|------|-----------|-------------|---------|
| 1 | P2 | BaseEnum 未在 §1.3 定义 | OOD 文档不完善（类型③） | 否 | todo 第1项 | `Docs/04_ood_phase0.md` §1.3 |
| 2 | P1 | POM modules 路径与设计 §2.1 不一致 | 代码与设计双向偏离（类型④） | 否 | todo 第2项 | 代码目录结构 vs 设计目录约定 |
| 3 | P1 | 聚合 POM 缺失 | 代码偏离设计，问题2 的连带后果（类型④） | 否 | todo 第3项 | 代码目录结构 vs 设计目录约定 |
| 4 | P2 | Spring Boot 版本 3.2.5 vs 3.3.0 | 代码与设计版本偏离（类型④） | 否 | todo 第4项 | 版本决策未同步（含 §1.4 运行时环境表） |
| 5 | P2 | dependencyManagement 缺少 starter 声明 | 代码偏离设计（类型④） | 否 | todo 第5项 | `pom.xml` 未按骨架实现 |
| 6 | P2 | Common POM 多引入 validation (optional) | 代码偏离设计（类型④） | 否 | todo 第6项 | `common/pom.xml` 额外依赖 |
| 7 | P1 | maven-dependency-plugin 额外忽略 business | 代码与设计偏离（类型④） | 否 | todo 第7项 | `pom.xml` 豁免配置过宽 |
| 8 | P2[Phase 0] → P0(Phase 1+) | GlobalExceptionHandler 缺少序列化异常处理器 | 代码缺陷（类型①） | 否 | todo 第8项 | `GlobalExceptionHandler.java` 功能遗漏 |
| 9 | P2 | FallbackAiServiceTest 未验证日志输出 | 测试覆盖不充分（类型④） | 否 | todo 第9项 | `FallbackAiServiceTest.java` 未覆盖日志（关联 known_issues.md K3） |
| 10 | P2[Phase 0] → P0(Phase 1+) | ApiClient 错误拦截器未实现 NETWORK_ERROR | 代码缺陷（类型①） | 否 | todo 第10项 | `api/index.ts` 功能遗漏 |

### 修复者指引

#### 可执行修复顺序编排

问题之间存在明确的依赖关系，建议按以下步骤执行，同步标注并行机会：

```
第1步：【先决策】问题2/3 → 决定扁平布局保留还是重构为分层布局
  ↓ 决策完成后并行推进 ↓
  ├── 方案A（保留扁平）：第2步A → 更新 OOD §2.1/§2.2 匹配实际扁平布局
  │   └── 第3步A → 创建聚合 POM（按实际扁平路径重新定位）
  └── 方案B（重构分层）：第2步B → 物理迁移目录为分层结构
      └── 第3步B → 按设计骨架创建聚合 POM

第2步：问题4 → 决定版本对齐方向（依赖于第1步决策，因 OOD 文档位置可能变动）

第3步（可与第2步并行；除标注依赖关系外，其余大部分任务可与第1步并行）：
  ├── 修复代码缺陷：
  │   ├── 问题8（P2[Phase 0]→P0(Phase 1+)，Phase 0 虽为 P2 但修复完全独立，建议先行编码避免 Phase 1+ 阻塞）
  │   └── 问题10（P2[Phase 0]→P0(Phase 1+)，Phase 0 虽为 P2 但修复完全独立，建议先行编码避免 Phase 1+ 阻塞）
  ├── 修复 POM 配置：
  │   ├── 问题7（P1，需验证 CI 门禁，见 CI 门禁影响分析）[依赖第1步决策，决策后方可执行]
  │   ├── 问题5（P2，纯补充/清理，无需额外验证）
  │   └── 问题6（P2，纯补充/清理，无需额外验证）
  └── 修复测试 & 文档：
      ├── 问题9（P2，测试策略依赖 known_issues.md K3 偏差的状态，需先决策 K3 方向再编写日志断言——参见问题9 §K3 冲突警告；代码骨架准备可先做）
      └── 问题1（P2，完全独立，不依赖任何前置步骤，可随时启动）
```

**并行机会说明**：
- **问题8、问题10、问题1 与第1步无任何依赖关系**，可独立在任何时间启动（包括在第1步决策完成之前），不因决策延迟而阻塞。建议优先分配这些独立项以最大化并行度。
- 问题8 和 问题10 完全独立，可第一时间并行分配
- ⚠️ **优先级标签 vs 执行顺序说明**：问题8/10 的 `P2[Phase 0]→P0(Phase 1+)` 格式中，Phase 0 当前等级为 P2（低优先级），但执行顺序推荐其最先执行，原因如下：这两项属于"无阻塞依赖的独立代码缺陷"，当前 Phase 0 的无触发路径不改变缺陷本身的存在——先行编码可在 Phase 0 低成本完成，避免 Phase 1+ 激活后产生紧急修复压力。P1 项（问题2/3/7）虽优先级标定更高，但均依赖第1步的目录布局决策或涉及 POM 结构性变更，无法先于决策启动。因此执行顺序由**依赖关系**而非静态优先级决定，两者并不矛盾。
- 问题1 独立于其他所有步骤，可随时并行执行
- **问题9 存在隐蔽依赖**：测试断言策略取决于 known_issues.md K3（日志触发时机偏差）的解决方向。如选择修复 K3（将日志行为对齐到"启动期 ERROR"设计语义），则测试应按对齐后的行为编写；如选择接受 K3 为已知偏差（保持当前"首次调用 ERROR"行为），则测试应按当前行为断言并显式注释 K3 依赖风险。因此问题9 的测试编码无法在 K3 方向决策前完成独立断言编写，但日志验证基础设施（ListAppender 注入）可在方向决策前先行搭建。
- 问题5/6 可合并为"POM 配置补充/清理"任务（P2，纯补充/清理，无需额外验证），由同一修复者处理
- 问题7（P1）需单独处理：移除 business 模块 ignore 条目后须在 application/pom.xml 中重新配置并以 `mvn dependency:analyze` 验证门禁通过，验证成本高于问题5/6
- 问题7 的修改范围与问题2/3 的决策结果相关：若选择方案A（重构为分层布局），POM 结构将大幅变动，问题7 的修改可能被覆盖，应先决策后执行
- 问题2/3/4 的决策链路构成串行依赖，是整体修复路径的关键路径

**⏳ 第1步决策超时兜底**：如果问题2/3 的决策因团队讨论或外部因素延迟超过 1 周，建议执行以下 fallback——暂按方案B 方向更新 OOD 文档（保留扁平布局、删除聚合 POM 章节）并记录"待确认"标记，同时立即启动第3步中所有与第1步无依赖的独立任务（问题8/10/9/1）。决策完成后再做最终确认——若最终决策为方案A，再回退 OOD 文档并执行目录迁移。

#### 联合修复注意事项

问题5、问题6、问题7 分别有独立的修复方向，但在同一 POM 编辑会话中联合执行时需注意以下交互影响：

1. **问题5 与问题7 在同一文件操作**：两者均涉及 `parent/pom.xml` 的修改——问题5 修改 `<dependencyManagement>` 段，问题7 修改 `<plugin><configuration>` 段。应在同一 POM 编辑会话中协调完成，避免先后两次提交产生不必要的 diff。
2. **问题6 对 common 模块编译 classpath 的影响**：移除 `common/pom.xml` 中的 validation starter 后，如果 common 模块中存在未被识别的对 validation starter 专有类型的依赖（如 `jakarta.validation.Validator` 或 `@Valid`），将导致编译错误。经本报告代码核查未发现此类依赖，但建议在变更后运行 `mvn compile -pl common` 验证编译通过。
3. **联合验证建议**：在上述三项修改全部完成后，在根目录执行以下命令进行联合验证：
   ```
   mvn dependency:analyze compile -pl common,application
   ```
   - `dependency:analyze` 验证门禁配置的正确性（问题7 修复后的效果）和 `dependencyManagement` 的完整性（问题5 修复后的效果）
   - `compile` 验证 common 模块的编译 classpath 是否因问题6 的移除而破坏

#### 问题2/3 决策引导

问题2（目录结构不一致）和问题3（聚合 POM 缺失）是耦合问题，需先做技术决策再开展修复。提供两个可选方案：

| 维度 | 方案A：代码向设计对齐（重构为分层布局） | 方案B：设计向代码对齐（保留扁平，更新 OOD） |
|------|---------|---------|
| 操作 | 物理移动目录：`backend/` 下创建 `modules/` 子目录，将 patient/doctor/admin/common-module-api/common-module-impl/ai-api/ai-impl 移入 `modules/` 分层，创建中间层聚合 POM | 保留现有扁平目录结构，更新 OOD §2.1 目录树和骨架代码为扁平路径，删除聚合 POM 相关章节（或备注为"Phase 1+ 可选"） |
| 影响范围 | `pom.xml` `<modules>` 路径 + 所有模块的 `../` 相对引用 + CI 命令路径 + IDE 项目视图 | OOD §2.1/§2.2 全部相关节 + §10 CI 命令路径 |
| 代码修改量 | 大：目录移动需同步更新所有 POM 中的 parent relativePath、CI 脚本、IDE 运行配置 | 无代码修改，纯文档更新 |
| 风险 | 高：目录移动可能导致 Git 历史断裂、CI 脚本遗漏适配；新增聚合 POM 需验证 reactor 排序正确性 | 低：文档更新无运行时风险 |
| 与路线图的关系 | 与 OOD §10 的 CI 分阶段命令路径一致，Phase 1+ CI 配置可按 OOD 原文执行 | 需长期维护 OOD 文档与代码的实际结构一致；Phase 1+ 新增模块时需自行决定目录布局 |

**推荐选择**：方案B（保留扁平，更新 OOD）。理由：
- Phase 0 骨架刚刚搭建完成，目录结构变动将引入不必要的中断风险
- 扁平布局本身不是错误——Maven reactor 解析不依赖目录层次
- OOD 文档更新成本远低于目录重构成本
- 可将分层布局作为"Phase 1+ 重构目标"在路线图中记录，而非在 Phase 0 强制执行

**方案B 的长期维护成本评估**：
- 每个新模块创建时：开发者在 `pom.xml` 中直接添加单级 `<module>` 路径（约 1 分钟）。同时需检查 OOD §2.1 目录树是否与实际一致（约 0.5 人天/次的手动检查和 OOD 同步）。按 Phase 1+ 预计新增 5-8 个模块估算，累计维护成本约 3-4 人天。
- **决策的时间价值**：如果 Phase 0 选择方案A 重构分层，迁移成本约 3-5 人天（目录移动、POM 路径修正、CI 适配、构建验证）。如果推迟到 Phase 1+ 再执行同等工作，因代码量和模块间依赖复杂度增加，迁移成本将上升至 6-8 人天。但鉴于方案B 的可持续性良好（单级 `<module>` 是 Maven 常见用法），Phase 1+ 迁移并不是迫在眉睫的必要路径。推荐：Phase 0 采用方案B，在路线图中将分层重构标记为"Phase 2 可选优化"，而非 Phase 1 的必要前置。
- **偏离监控**：在 `known_issues.md` 中记录该偏离及其维护成本基线，每次新增模块时触发偏离检查，累计偏离成本超出阈值（如 5 人天）时重新评估迁移必要性。

**决策后下一步行动**：
- 若选择方案B：更新 OOD §2.1 目录树与骨架代码为扁平路径，删除或备注聚合 POM 章节；CI 命令路径同步更新
- 若选择方案A：先在小范围做完目录迁移试验（如仅移动 common-module 一组），验证无副作用的全面推广

#### 问题4 版本对齐选择依据

| 方案 | 操作 | 风险 | 兼容性验证 | 同步更新范围 |
|------|------|------|-----------|------------|
| 升代码到 3.3.0 | `pom.xml:9` 将 3.2.5 → 3.3.0 | 中：springdoc-openapi v2.5.0 与 3.3.x 的兼容性未显式验证（OOD §8.3 注释已警告）| 需集成测试验证 OpenAPI 页面正常渲染，否则需升级 springdoc-openapi | 仅 `pom.xml` 版本号一处 |
| 降 OOD 到 3.2.5 | OOD §2.1 骨架版本 3.3.0 → 3.2.5，§1.4 运行时环境表同步更新 | 低：3.2.5 有更多经过社区验证的兼容数据 | 无需额外验证（3.2.5 + springdoc 2.5.0 已验证兼容） | OOD §2.1 骨架代码 + §1.4 运行时环境表 |

**推荐选择**：降 OOD 到 3.2.5。理由：低风险、与现有 springdoc-openapi 兼容、§1.4 的 JDK 17+ 要求仍然成立（Spring Boot 3.2.5 已验证支持 JDK 17）。

#### 问题7 修复方向

**修复方向：代码向设计对齐**。移除 `pom.xml:109-115` 中的 `com.aimedical:patient`、`com.aimedical:doctor`、`com.aimedical:admin` 三个额外豁免条目，仅保留 OOD §2.2 认可的 `com.aimedical:ai-api` 和 `com.aimedical:common-module-api`。

理由：OOD 的设计意图明确——这两个 api 模块是 Phase 0 声明但暂未引用的依赖，属于有意的延迟引用策略；而 patient/doctor/admin 作为 application 的真实编译依赖，不应被豁免。

**⚠️ CI 门禁影响验证**：直接移除上述三个 ignore 条目后，需确认 `dependency:analyze` 门禁是否通过。经代码分析：
- `application/pom.xml:35-45` 将 patient/doctor/admin 声明为 compile 依赖
- application 模块的 Java 代码仅含 `Application.java`（main class + `@SpringBootApplication(scanBasePackages = "com.aimedical")` 字符串参数）、`HealthController.java`（ping）、`SecurityConfigPhase0.java`——**均未直接引用 patient/doctor/admin 中的任何类型**
- `@SpringBootApplication(scanBasePackages = "com.aimedical")` 使用字符串参数，**不构成字节码级类型引用**（`dependency:analyze` 基于字节码引用关系检测）
- 因此移除 ignore 条目后，`dependency:analyze` 会将 patient/doctor/admin 判定为 **unused declared dependencies**，直接导致门禁失败

**备选方案**（推荐）：不在父 POM 中直接移除这三个 ignore 条目，而是将 `patient`、`doctor`、`admin` 的 `<ignoredUnusedDeclaredDependency>` 从父 POM 的 `<plugin><configuration>` 移入 `application/pom.xml` 自身的 `maven-dependency-plugin` 配置中，限定豁免范围仅对 application 模块生效。

**⚠️ 操作前提验证**：`application/pom.xml:86-96` 当前仅含 `spring-boot-maven-plugin` 的 `<build><plugins>` 配置，**不存在 `maven-dependency-plugin` 的任何声明**（包括 `groupId`、`artifactId`、`version` 或 `<configuration>`）。因此操作者需要从头编写完整的插件声明，而非简单的配置迁移。在 `application/pom.xml` 的 `<build><plugins>` 段中添加的完整声明模板如下：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <configuration>
        <ignoredUnusedDeclaredDependencies>
            <ignoredUnusedDeclaredDependency>com.aimedical:patient</ignoredUnusedDeclaredDependency>
            <ignoredUnusedDeclaredDependency>com.aimedical:doctor</ignoredUnusedDeclaredDependency>
            <ignoredUnusedDeclaredDependency>com.aimedical:admin</ignoredUnusedDeclaredDependency>
        </ignoredUnusedDeclaredDependencies>
    </configuration>
</plugin>
```

注意该配置**不需要**指定 `version`，因为父 POM 的 `pluginManagement` 中已声明 `maven-dependency-plugin` 的版本（父 POM 中 `maven-dependency-plugin` 的 `<version>` 为 OOD §2.2 骨架代码中约定的版本）。完成添加后，父 POM 中的对应 ignore 条目必须移除，避免豁免范围重复叠加。

此方案的收益：
1. 恢复了子模块级别的 `dependency:analyze` 检出力（子模块不再继承过宽的豁免配置）
2. 保留了 application 模块在 Phase 0 的正常构建（因 application 确需声明这些依赖但尚未产生字节码引用）
3. 待 Phase 1+ 各业务模块的 Controller/Service 在 application 中被实际注入引用后，再从 application 的 POM 中移除对应 ignore 条目，使门禁完全恢复

**如选择不移除**：若 Phase 0 阶段暂不处理此问题，应在 known_issues.md 中记录该偏差，并在 Phase 1+ 引入实际 Controller 调用后作为回归检查项处理。

#### 跨问题根因模式整合

问题2、3、4、5、6、7 共享同一根因模式：**POM 骨架搭建阶段偏离了 OOD §2.1/§2.2 的设计规范**。具体表现为：

- **问题2/3**：目录布局直接从扁平起步，未按 OOD §2.1 的分层结构创建目录和聚合 POM
- **问题4**：Spring Boot 版本未与 OOD §2.1 骨架同步（实际用 3.2.5，骨架写 3.3.0）
- **问题5**：`dependencyManagement` 未按 OOD §2.2 骨架补齐 5 个 starter 声明条目
- **问题6**：common 模块多引入 OOD §2.2 未列出的 validation starter
- **问题7**：`ignoredUnusedDeclaredDependencies` 超出 OOD §2.2 约定的范围

**批量修复建议**：上述 6 个问题可按「POM 一致性一轮对齐」合并处理，由同一修复者同步进行。修复路径为：首先为问题2/3 做技术决策（推荐方案B：保留扁平，更新 OOD），然后基于决策结果，在同一个 POM 编辑会话中依次处理问题4/5/6/7 的版本号、依赖管理、额外依赖和豁免配置，利用 OOD §2.1/§2.2 的骨架代码作为对照清单逐项对齐。

与之对比，**问题1**（BaseEnum 文档遗漏）的根因模式不同——属于 OOD §1.3 编写阶段未涵盖已知类型，与 POM 骨架偏离无关，需单独处理。**问题8/10**（代码缺陷）的根因模式也不相同——属于实现阶段的功能遗漏，而非设计与代码的结构性偏离。**问题9**（测试覆盖不足）属于测试编写阶段的覆盖遗漏。

各问题的具体代码位置和触发条件已在各节的"证据链"中定位到文件/行号级别，修复者可据此识别"改哪里"和"为什么"。

---

## known_issues.md 变更建议汇总

本报告在多个问题正文中提出了对 `known_issues.md` 的变更建议。为便于执行者集中处理，汇总如下：

| 编号 | 关联问题 | 变更类型 | 变更内容 | 触发条件 |
|------|---------|---------|---------|---------|
| K1 | 问题2/3（目录结构偏离） | **新增条目** | 记录扁平目录布局偏离及其维护成本基线，每次新增模块时触发偏离检查，累计超出 5 人天阈值时重新评估迁移必要性 | 选择方案B（保留扁平，更新 OOD）后立即添加 |
| K2 | 问题4（版本偏离） | **新增条目** | 记录 Spring Boot 版本 3.2.5 vs OOD §2.1 骨架 3.3.0 的版本偏离事实及降 OOD 的对齐决策 | 选择"降 OOD 到 3.2.5"后立即添加 |
| K3 | 问题5（dependencyManagement） | **新增条目** | 记录 `<dependencyManagement>` 缺少 5 个外部 starter 声明的偏离事实，说明此偏离为有意的设计取舍（依赖 Spring Boot BOM 兜底管理版本），以及在 Phase 1+ 重构时重新评估对齐必要性 | 如果选择保留现状（不补充 starter 条目） |
| K4 | 问题6（Common POM validation） | **新增条目** | 记录 common 模块多引入 `spring-boot-starter-validation (optional)` 的偏离事实，说明此偏离标记为 optional 后无运行时影响，以及移除计划 | 如果选择保留现状（不移除 validation starter） |
| K5 | 问题7（maven-dependency-plugin） | **新增条目** | 记录父 POM `ignoredUnusedDeclaredDependencies` 超范围配置的偏差事实，说明在 Phase 1+ 引入实际 Controller 调用后需作为回归检查项处理 | 如果选择 Phase 0 暂不处理问题7 |
| K6 | 问题8（GlobalExceptionHandler 测试） | **新增条目** | 记录 Phase 0 阶段序列化异常 handler 的测试策略：通过 `@ExtendWith(SpringExtension.class)` 单元测试直接调用 handler 方法验证响应格式和日志级别，待 Phase 1+ 首个 `@PostMapping` 引入后补充 MockMvc 集成测试 | 立即添加（配合代码修复一起提交） |
| K7 | 问题9（FallbackAiServiceTest 日志） | **更新现有 K3** | 在现有 K3（日志触发时机偏差）中补充测试覆盖状态说明：明确当前测试未覆盖日志行为的回归保护，以及 Phase 1+ 引入真实 AiService 委托后需补充 `ListAppender` 验证 | 立即更新（与问题9 测试补充同步） |

**执行建议**：上述变更可在同一编辑会话中完成，建议在处理各问题的 POM 配置和代码修复时同步执行对应的 known_issues.md 变更。K1~K5 为非阻塞性记录（仅在选择保留现状时才需要），K6 和 K7 为推荐立即执行项（与代码修复同步提交）。

---

## 修订说明（v2）

| 质询意见 | 回应 |
|---------|------|
| 问题1 证据链第3项节号引用错误（§3.x → 实际位于 §1.3） | 已修正所有 §3.x 引用为 §1.3（问题1 证据链第3项、根因说明、汇总表）；OOD 核心抽象一览表经确认位于 `04_ood_phase0.md:24-33` §1.3，非 §3.x |
| 缺少明确的修复优先级排序 | 已为每个问题增加「优先级」字段（P0/P1/P2），优先级依据：P0 = 直接影响运行时行为（问题8/10）；P1 = 涉及重大重构或长期隐患（问题2/3/7）；P2 = 无运行时影响或纯文档层面（问题1/4/5/6/9） |
| 问题4 影响范围遗漏 OOD §1.4 的版本引用 | 已在问题4 影响范围中补充：§1.4（line 41）运行时环境表中同样引用"Spring Boot 3.3.0 最低要求"，若选择降 OOD 到 3.2.5，§1.4 也需同步更新 |
| 问题7「代码缺陷」分类可商榷（当前无运行时影响） | 已将问题7 分类从"代码缺陷"改为"代码与 OOD 的设计偏离"，与问题5/6 保持分类一致。理由：该问题的本质是配置范围超出设计约定，不涉及运行时失败或错误行为，且原报告自身已指出"当前无运行时影响" |
| 未显式回答「是否为误报」 | 已为每个问题增加「是否为误报」字段（均为"否"），并在汇总表中增加对应列 |
| known_issues.md 引用不充分（问题9 未引用 K3） | 已在问题9 证据链中增加第4项引用 known_issues.md K3（FallbackAiService 空委托 ERROR 日志的已知偏差），并在汇总表和修复者指引中关联提及 K3 |

## 修订说明（v3）

| 质询意见 | 回应 |
|---------|------|
| 问题1（一般）：缺少可执行的修复顺序编排。报告按修复目标类型（代码/OOD文档/测试/先决策）对问题分组并标注P0/P1/P2优先级，但未揭示问题间的依赖关系（如问题2/3与问题4/5/6/7的耦合），也未给出执行顺序建议（如第1步/第2步/第3步编排及并行机会说明） | 已在「修复者指引」中补充「可执行修复顺序编排」和「并行机会说明」：将修复分为 3 个步骤，标注串行依赖（决策链路 → 版本对齐 → POM 配置）和并行任务（问题8/10 可最早独立分配；问题1/9 全程可独立并行）；明确问题2/3→4→5/6/7 的耦合路径 |
| 问题2（一般）：问题2/3的决策引导不足以支持执行。报告称"需先决策再行动"但未提供可选方案、各方案的利弊和影响范围、推荐选择及理由、决策后下一步行动 | 在「修复者指引」中新增「问题2/3 决策引导」表格，对比方案A（代码向设计对齐—重构分层）和方案B（设计向代码对齐—保留扁平更新OOD），覆盖操作、影响范围、代码修改量、风险、与路线图的关系五个维度，给出推荐选择（方案B）及理由，并补充了决策后下一步行动说明 |
| 问题3（轻微）：问题4的修复选择缺少依据。报告提出"升代码到3.3.0或降OOD到3.2.5"的二选一建议，但未给出选择依据 | 在「修复者指引」中新增「问题4 版本对齐选择依据」表格，对比两个方案的风险、兼容性验证情况、同步更新范围，给出推荐选择（降OOD到3.2.5）及理由 |
| 问题4（轻微）：问题7的修复方向未明确说明。报告建议"移除patient/doctor/admin三个条目"，但未明确声明这是"代码向设计对齐"还是"设计向代码对齐"，也未提供理由减少执行阻力 | 在「修复者指引」中新增「问题7 修复方向」小节，明确声明这是**代码向设计对齐**，并给出了OOD设计意图的引用理由：patient/doctor/admin是application的真实编译依赖，不应被豁免 |
| 问题5（轻微）：缺少跨问题的根因模式整合。问题2/3/4/5/6/7共享同一根因模式（POM骨架搭建阶段偏离OOD §2.1/§2.2设计规范），但报告作为独立诊断呈现，未提示批量修复机会 | 在「修复者指引」中新增「跨问题根因模式整合」小节，明确指出问题2/3/4/5/6/7共享根因模式，给出批量修复建议（按「POM一致性一轮对齐」合并处理，由同一修复者同步进行），并与问题1/8/9/10的不同根因模式做对比 |

## 修订说明（v4）

| 质询意见 | 回应 |
|---------|------|
| 缺少对 todo.md 的显式覆盖确认：未说明各诊断问题对应哪一条 todo 项；未汇总声明 10 个 todo 项的处理状态 | 已新增「todo.md 覆盖声明」章节（诊断信息后），显式声明 10 项待办已全部覆盖、0 项误报，并按 4 类统计分布；汇总表已增加「对应 todo 项」列 |
| 问题10 的 P0 优先级在 Phase 0 语境下可能高估：Phase 0 前端仅含占位页面和健康检查，无实际 API 调用链路会触发网络错误 | 已将问题10 优先级修订为"P0（Phase 1+ 场景下的最高优先级…；当前 Phase 0 下…无运行时影响）"，并在影响范围中分列 Phase 0 与 Phase 1+ 两个场景的影响说明 |
| 问题2/3 的 P1 优先级与推荐方案（方案B：低风险文档更新）存在表述张力：P1 的语义暗示较高修复成本，与推荐的方案 B 不匹配 | 已将问题2/3 的优先级标注为"P1（决策优先级 — 方向选择影响后续修复路径，但推荐的方案 B 为低风险文档更新，执行成本低）"，明确区分决策优先级与执行优先级 |
| 未从"其他类型问题"角度对 todo 项做总体分析：requirement.md 要求判断四种情况，报告未按四类标准进行归类统计 | 已新增「总体分析（按 requirement.md 四类标准归类）」章节（汇总表之前），对全部 10 项进行四类归类统计，并在汇总表每行的分类字段中标注对应类型编号 |

## 修订说明（v5）

| 质询意见 | 回应 |
|---------|------|
| 问题8 的 P0 优先级缺少 Phase 0 上下文区分：Phase 0 所有 Controller 均为 `@GetMapping` 且不接收 `@RequestBody`，`HttpMessageNotReadableException` 在 Phase 0 不存在触发路径 | 已将问题8 优先级修订为"P0（Phase 1+ 场景下的最高优先级…；当前 Phase 0 下…无运行时影响）"，并在影响范围中分列 Phase 0 与 Phase 1+ 两个场景的影响说明，与问题10 在 v4 中的修订方式一致；影响范围第四节中原有的"请求体 JSON 格式错误时"等描述已移至 Phase 1+ 子条目下，Phase 0 场景单独列出 |
| 问题7 修复建议缺失 CI 门禁影响验证：`application/pom.xml:35-45` 声明 patient/doctor/admin 为 compile 依赖；application 模块 Java 代码未直接引用其中的任何类型（`@SpringBootApplication(scanBasePackages = "com.aimedical")` 为字符串参数，不构成字节码级引用）；直接移除 ignore 条目后 `dependency:analyze` 会将三者判定为 unused declared dependencies，导致门禁失败 | 已在「问题7 修复方向」中新增「CI 门禁影响验证」分析，确认字节码级引用缺失的实事。已将修复方向从"直接移除"调整为推荐备选方案：将三个 ignore 条目从父 POM 移入 `application/pom.xml` 自身的 `maven-dependency-plugin` 配置中，限定豁免范围仅对 application 模块生效；同时补充了"如选择不移除"的 known_issues.md 记录备选 |
| 问题9 日志测试建议缺少实现可行性分析：报告建议"在测试中追加 Logback `ListAppender` 或 Mock Logger 的日志验证"，但未说明技术前提和难度 | 已在问题9 影响范围末尾补充「实现可行性分析」表格，对比了 Logback `ListAppender`（推荐，低难度，logback-classic 已在 test classpath）与 Mock Logger（不推荐，高难度，需 PowerMock 或代码重构）两种方案的技术前提和推荐选择，并给出 ListAppender 的代码骨架和首次/后续调用 ERROR/WARN 级别验证的注意事项 |

## 修订说明（v6）

| 质询意见 | 回应 |
|---------|------|
| 问题7 影响范围与修复方向存在内部矛盾：影响范围声称"application 的占位 Controller 引用这些模块（patient/doctor/admin）——这在 Phase 0 不可能"，而修复方向 CI 门禁分析通过代码验证得出"均未直接引用 patient/doctor/admin 中的任何类型"的相反结论 | 已修正问题7 影响范围中关于"application 的占位 Controller 引用这些模块"的错误表述，改为"application 模块的 Application.java、HealthController.java、SecurityConfigPhase0.java 均未在字节码层面引用 patient/doctor/admin 中的任何类型，因此移除 ignore 条目后 dependency:analyze 会将其标记为 unused declared dependencies"，与修复方向 CI 门禁分析结论保持一致 |

## 修订说明（v7）

| 质询意见 | 回应 |
|---------|------|
| 问题5/6 缺少独立修复方向，降低可操作性：问题5（dependencyManagement 缺少外部 starter 声明）和问题6（Common POM 多引入 validation optional）需要明确的修复方向 | 已为问题5 和问题6 各新增「修复方向」小节。问题5 推荐代码向设计对齐（补充 5 个 starter 条目）；问题6 推荐从 common/pom.xml 移除 validation starter（经核查 common 模块无其他依赖，移除安全）。两问题均提供了保留现状时的 known_issues.md 记录备选 |

## 修订说明（v8）

| 质询意见 | 回应 |
|---------|------|
| todo 项编号引用事实错误：报告声称"10 项待办事项"但汇总表引用"todo 第11项"，计数自相矛盾 | 已将汇总表从"todo 第2项~第11项"修正为"todo 第1项~第10项"，与 10 项待办事项的计数一致；已在 todo.md 覆盖声明中明确注明"共 11 个 checkbox 条目（含 1 项 `[严重]` 声明 + 10 项待办事项）"，消除语义歧义 |
| 跨问题联合修复的副作用分析缺失：问题5/6/7 联合执行时，common 模块编译 classpath 变化未验证安全性，问题7 与问题5 在同一 POM 文件中的操作复杂度未提示 | 已在「修复者指引」中新增「联合修复注意事项」小节，分别分析了问题5 与问题7 在同一 POM 文件的操作协调、问题6 对 common 编译 classpath 的影响，并给出了联合验证命令 `mvn dependency:analyze compile -pl common,application` |
| 修复编排中决策阻塞风险未识别：问题2/3 设为第1步关键路径，未讨论决策无限期延迟的兜底策略；图式编排的"第1步→第2步→第3步"视觉暗示可能使执行者错误等待决策后再启动其他工作 | 已在「可执行修复顺序编排」的图式中明确标注"问题8/10/9/1 与第1步无任何依赖关系，可独立在任何时间启动"；增加了第1步决策超时（>1周）的 fallback 建议：暂按方案B 更新 OOD 并记录待确认，同时启动独立任务 |
| 问题2/3 决策对比中方案B 的长期风险被轻估：方案B 的长期维护成本量化分析不足——每个新模块的偏离风险、Phase 1+ 迁移的增量成本均未评估 | 已在「问题2/3 决策引导」中补充方案B 的长期维护成本定量估算（预计 3-4 人天/Phase 1+）；增加了"决策的时间价值"分析：Phase 0 迁移成本 3-5 人天 vs Phase 1+ 迁移成本 6-8 人天；补充了偏离监控建议：在 known_issues.md 中记录偏离成本基线，累计超阈值时重新评估 |
| 问题5/6 的"是否为误报"论证缺少修复动机支撑：报告承认"功能层面无影响"但缺少修复价值论证，削弱执行动机 | 已为问题5 和问题6 各新增「修复价值论证」小节，分别从"dependencyManagement 可视性恢复"和"设计约束力保持 vs 破窗效应"两个角度说明修复收益和不修复的代价；问题1 也同步补充了修复价值论证 |
| 优先级排序的 Phase 0/Phase 1+ 切分可进一步完善：P0 标签的受众风险——扫描阅读者可能忽略 Phase 上下文；Phase 1+ 的触发条件未定义 | 已将问题8 和问题10 的优先级从"P0（Phase 1+ 场景下…；Phase 0 下…无影响）"改为"P0(Phase 1+)/P2(Phase 0)"双值格式，更直观；为每个带 Phase 上下文的问题补充了「激活条件」字段，明确触发 Phase 1+ 行为的条件 |
| 需求响应充分度评估 — 产出已完整覆盖但论证深度不均衡：OOD 文档问题与代码偏离的分类边界可更清晰，"偏离容忍度"标注缺失 | 已在「总体分析」中增加横向对比表格，说明类型③（问题1）与类型④中问题2-7 的分类边界：前者是文档自身不完善，后者是代码偏离设计；新增「偏离容忍度标注」表格，为每个类型④问题标注"必须纠正"或"可记录为已知偏差" |

## 修订说明（v9）

| 质询意见 | 回应 |
|---------|------|
| P0 级别真实代码缺陷（问题8、问题10）缺失修复方向，可操作性与优先级倒挂 | 已为问题8 补充 `@ExceptionHandler` 方法骨架（HttpMessageNotReadableException → 400 PARAM_INVALID、HttpMessageNotWritableException → 500 SYSTEM_ERROR），并说明了 log 级别选择依据；已为问题10 补充错误拦截器代码骨架，明确网络错误和 HTTP 错误码的处理策略，所有分支均使用 `Promise.resolve()` 保持统一格式，并讨论了 401/403 的 Phase 0 处理策略 |
| 问题9 的日志测试方案与 known_issues.md K3 存在隐蔽冲突 | 已在 ListAppender 代码骨架后补充 K3 冲突警告，提供两种测试策略：(1) 优先方案——先触发启动期逻辑使日志行为对齐设计语义后再编写断言；(2) 备选方案——保持当前代码行为断言并显式注释 K3 依赖风险 |
| 问题5 证据链计数不精确，影响可信度 | 已将 OOD 骨架条目数从"11（6 内部 + 5 starter + springdoc + h2 + security + validation + test）"更正为"13 个条目（6 内部 + 5 starter + springdoc + h2）"；已将实际 pom.xml 条目数从"8 个条目（6 内部 + springdoc + h2）"更正为"11 个条目（6 内部 + 3 业务模块 + springdoc + h2）" |
| 总体评估缺少"排除项"的显式确认 | 已在「todo.md 覆盖声明」中新增显式声明："已确认报告内容为问题诊断定位，未进入 initial_artifact 模式（符合 requirement.md 排除项要求）" |

## 修订说明（v10）

| 质询意见 | 回应 |
|---------|------|
| 问题9的"独立执行"断言与已知偏差K3存在隐蔽依赖：报告将问题9标记为"完全独立，不依赖任何前置步骤，可随时启动"，但测试断言策略取决于K3（日志触发时机偏差）的解决方向 | 已在「可执行修复顺序编排」中将问题9的标注从"完全独立，不依赖任何前置步骤，可随时启动"更新为"测试策略依赖 K3 偏差的状态，需先决策 K3 方向再编写日志断言"；在「并行机会说明」中补充独立段落说明 K3 依赖关系的两种可能路径（修复K3 vs 接受K3），以及日志验证基础设施搭建与断言编写的可拆分性 |
| 问题7备选方案的操作前提未验证：application/pom.xml 无 maven-dependency-plugin 配置，推荐方案隐含假设该文件已有对应插件配置，但实际该文件仅含 spring-boot-maven-plugin | 在「问题7 修复方向」备选方案中新增「操作前提验证」警告，确认 application/pom.xml 的 `<build><plugins>` 仅含 spring-boot-maven-plugin，不存在 maven-dependency-plugin 的任何声明；补充了完整的插件声明 XML 模板（含 groupId、artifactId、configuration），并说明 version 由父 POM pluginManagement 继承无需重复指定 |
| 问题8修复方向缺失测试覆盖建议：报告为问题8提供了 @ExceptionHandler 方法代码骨架，但未提及测试覆盖要求、测试方法兼容性、日志级别验证。对于一个 P0 级别的代码缺陷，缺失测试覆盖建议削弱了修复完整度 | 在「问题8 修复方向」后新增「测试覆盖要求」小节，包含四项要求：① 通过 @WebMvcTest + MockMvc 验证序列化异常的 HTTP 状态码和 ErrorCode；② 通过 ListAppender 验证日志级别区分（warn vs error）；③ 兼容性说明（无需新增依赖）；④ Phase 0 阶段通过纯单元测试直接调用 handler 方法验证，Phase 1+ 补充 MockMvc 集成测试的策略 |
| known_issues.md 变更建议分散在全文中缺乏系统性汇总：报告在至少 5 处提出了 known_issues.md 的变更建议但无汇总清单 | 在「跨问题根因模式整合」后新增「known_issues.md 变更建议汇总」表格，系统列出全部 7 项建议（K1~K7），每项标注关联问题、变更类型（新增条目/更新现有K3）、变更内容和触发条件，并给出执行建议 |

## 修订说明（v11）

| 质询意见 | 回应 |
|---------|------|
| 问题8/10 Priority 标签与执行顺序建议之间存在误导性张力：P2(Phase 0) 的低优先级与"最先执行"的推荐存在语义矛盾 | 已将问题8/10 的优先级从 `P0(Phase 1+)/P2(Phase 0)` 改为 `P2[Phase 0] → P0(Phase 1+)`，以"当前等级 → 升档等级"方向性格式消除歧义；在「并行机会说明」中新增独立说明段落，解释 P2 等级不等于"最后执行"——执行顺序由**依赖关系**决定，独立无阻塞的缺陷先行编码可在低成本窗口完成，与静态优先级标定并不矛盾 |
| 问题10 修复方案缺少 TypeScript 类型兼容性分析：错误拦截器改为 `Promise.resolve({ code, message })` 后，未分析对消费方 `Promise<T>` 泛型推断的影响；"返回格式一致"的声称与成功分支 `data.data` 的事实矛盾 | 已在问题10「关键决策点」后新增「TypeScript 类型兼容性分析」小节，分析当前类型签名、修复后类型签名的变化（`Promise<SomeType>` → `Promise<SomeType | { code: string; message: string }>`），三种推荐的兼容方案（discriminated union / 统一 ApiResult<T> / 不做约束），以及当前 `types/index.ts` 中 `ApiResult<T>` 的结构兼容性分析。删除了"返回格式一致"的不准确表述 |
| 问题9 测试优先方案（优先方案）缺乏可操作性代码指引：仅提供"通过反射调用或 Spring Test 上下文刷新"方向性描述，无任何代码示例或实现步骤 | 已将问题9 的优先方案从方向性描述扩展为完整代码骨架，给出两种实现方式：方式A（推荐）使用 `ReflectionTestUtils.invokeMethod()` 的标准反射调用完整代码（含启动期 ERROR 验证 + 运行期 WARN 验证），方式B（Spring Test 上下文刷新）的代码骨架及局限性分析说明 |

## 修订说明（v12）

| 质询意见 | 回应 |
|---------|------|
| 问题1（中严重度）：问题9的测试推荐方案基于不存在的 `@PostConstruct` 方法。代码验证 `FallbackAiService.java` 中不存在任何 `@PostConstruct` 注解的方法，`ReflectionTestUtils.invokeMethod(service, "init")` 属于虚构前提 | 已将问题9 的测试方案从"优先方案（先触发 @PostConstruct）"重构为"直接方案（推荐）"——直接对当前代码行为（首次调用→ERROR、后续→WARN）编写日志断言，通过 public 方法路径触发，并显式注释 K3 依赖。原"优先方案"降级为"对齐方案（需先修复 K3）"并明确标注当前代码无 @PostConstruct 方法的前提条件 |
| 问题2（中严重度）：执行顺序编排中第3步标注"完全独立于第1步"与问题7条目"依赖第1步决策"存在标题级矛盾 | 已将第3步标题从"完全独立于第1步"改为"除标注依赖关系外，其余大部分任务可与第1步并行"；在问题7条目后标注"[依赖第1步决策，决策后方可执行]" |
| 问题3（低严重度）：问题5证据链计数分类不一致——OOD 骨架用"6 内部"，实际 POM 用"6 内部 + 3 业务模块"，两种计数粒度不同 | 已将证据链第1项从"6 内部"改为"6 个内部模块"；第2项从"6 内部 + 3 业务模块"改为"9 个内部模块（6 个基础设施模块 + 3 个业务模块）"，统一以"内部模块"为口径，并补充"多出 patient/doctor/admin 三项业务模块声明"的说明 |
| 问题4（低严重度）：问题8修复方案中 `PARAM_INVALID` 的选择缺少语义匹配度论证——"请求体格式错误"与"参数值非法"语义不完全一致，前端可能需区分两类 400 错误 | 已在「关键决策点」中补充语义匹配度说明：分析 `PARAM_INVALID` 与"请求体 JSON 格式错误"的语义差异，建议统一使用 `PARAM_INVALID` + message 传递异常详情，标注"待前端确认是否需要区分两类 400 错误" |
| 问题5（低严重度）：问题10未识别成功拦截器本身的类型不一致（T vs ApiResult<T>），将其标注为"连带变更"而非"需要独立评估的影响点"，可能低估修复工作量 | 已将成功拦截器类型不一致从"连带变更"升级为独立条目「成功拦截器既有类型不一致问题」，要求修复者在实施问题10 修复前先完成消费方代码扫描和类型方案决策（三项可选方案），避免先实现后因类型不匹配而返工 |
