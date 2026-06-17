# 代码评审问题诊断报告

## 诊断信息

- **诊断时间**：2026-06-17
- **数据来源**：`Harness/reviews/202606171248_code_review/`（2轮6份审查报告 + todo + scope + review + known_issues）
- **设计依据**：`Docs/04_ood_phase0.md`（Phase 0 OOD）
- **代码基线**：`AIMedical/` 目录下全部实现代码
- **路线图**：`Docs/03_roadmap.md`

---

## todo.md 覆盖声明

本报告对 `Harness/reviews/202606171248_code_review/todo.md` 所列 **10 项待办事项**进行了逐一分析。各问题与 todo 项的对应关系见汇总表"对应 todo 项"列。经分析：

- **10 项待办已全部覆盖**，无遗漏
- **0 项为误报**（所有问题均确认为真实存在）
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

**否**。真实性确认：OOD 骨架的 dependencyManagement 包含 11 个条目，实际仅含 8 个，缺少的 5 个 starter 条目可逐行比对确认。

### 现象

OOD §2.2 "依赖管理（父 POM）"要求父 POM 的 `<dependencyManagement>` 统一声明 `spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-security`、`spring-boot-starter-validation`、`spring-boot-starter-test` 的版本（`04_ood_phase0.md:338-342`）。OOD §2.1 父 POM 骨架代码段（`04_ood_phase0.md:162-237`）更是直接包含了这些 starter 的完整 `<dependency>` 条目。但实际 `<dependencyManagement>`（`pom.xml:39-101`）仅注册了内部模块、springdoc-openapi 和 h2，**完全没有上述 5 个 starter 条目**。

### 证据链

1. **OOD 骨架代码**中 `<dependencyManagement>` 包含 11 个条目（6 内部 + 5 starter + springdoc + h2 + security + validation + test）。
2. **实际 `<dependencyManagement>`** 仅含 8 个条目（6 内部 + springdoc + h2），缺少 web、data-jpa、security、validation、test。
3. **`pom.xml:39-101`** 逐行确认无上述 starter 条目。

### 根因

实际项目依赖 Spring Boot parent POM 的 BOM 管理这些 starter 版本——即使不在 `<dependencyManagement>` 中显式列出，子模块引入这些 starter 时也能获得正确的版本。开发者可能认为无需重复声明。但 OOD 的设计意图是将它们显式列出作为"统一管理清单"——既是文档化手段，也是门禁可视化依据。根因是开发过程中未按设计骨架补齐这些条目。

### 影响范围

- 功能层面无影响。Spring Boot parent POM 的 BOM 已管理这些版本，子模块使用这些 starter 时版本解析正确。
- 设计文档的"一致性检查"页面（`dependencyManagement` 清单对照）无法准确执行，因为实际列表与设计列表不匹配。
- 新加入项目的开发者查看 `dependencyManagement` 时，无法直观感知项目的标准 starter 清单。

---

## 问题6：Common POM 引入 spring-boot-starter-validation (optional)

### 分类

**代码与 OOD 的偏差**。

### 优先级

P2（低优先级 — optional 标记避免传递扩散，当前无运行时影响）。

### 是否为误报

**否**。真实性确认：OOD §2.2 common 模块依赖规约仅列出 web 和 data-jpa，实际 common/pom.xml 包含 validation，偏差可确认。

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

- 在 application 模块上运行 `dependency:analyze` 时，如果 patient/doctor/admin 在 application 的 POM 中被声明但未使用（这在 Phase 0 不可能，因为 application 的占位 Controller 引用这些模块），会被豁免不报。
- 更关键的影响：该配置继承到所有子模块，如果在某个子模块（如 patient）中错误地将 unused 依赖添加到 POM，该机制无法检出。

---

## 问题8：GlobalExceptionHandler 缺少 HttpMessageNotReadableException / HttpMessageNotWritableException 专用处理器

### 分类

**代码缺陷**（设计明确要求实现的功能未实现）。

### 优先级

P0（Phase 1+ 场景下的最高优先级 — 引入 POST/PUT 请求体后，请求体 JSON 格式错误时将错误返回 500 而非 400，前端可能被错误引导；当前 Phase 0 下：所有 Controller 均为 `@GetMapping` 且不接收 `@RequestBody`，因此 `HttpMessageNotReadableException` 不存在触发路径，无运行时影响）。

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

---

## 问题10：ApiClient 错误拦截器未按 §3.5 实现 NETWORK_ERROR 处理

### 分类

**代码缺陷**（设计要求的功能未实现）。

### 优先级

P0（Phase 1+ 场景下的最高优先级 — 真实的 API 调用链路引入后，前端无法通过统一 code 判断网络错误，未捕获的 Promise 异常可能产生运行期报错；当前 Phase 0 下：前端仅含占位页面和健康检查，无实际 API 调用链路会触发网络错误，故无运行时影响）。

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

---

## 总体分析（按 requirement.md 四类标准归类）

依据 `requirement.md` 定义的四种问题类型，对全部 10 项诊断结论的归类统计如下：

| 问题类型 | 包含问题 | 数量 |
|---------|---------|------|
| **1. 真实代码缺陷**（设计要求的功能未实现或实现错误） | 问题8（GlobalExceptionHandler 缺少序列化异常处理器）、问题10（ApiClient 错误拦截器未实现 NETWORK_ERROR） | **2 项** |
| **2. 误报**（审查报告提出的问题实际不存在） | 无 | **0 项** |
| **3. OOD 文档问题**（设计文档存在矛盾、偏差、不完善或错误） | 问题1（BaseEnum 未在 §1.3 定义） | **1 项** |
| **4. 其他类型问题**（不属于以上三类的问题） | 问题2/3（目录结构与设计偏离）、问题4（版本偏离）、问题5/6/7（POM 配置偏离设计规约）、问题9（测试覆盖不充分） | **7 项** |

**补充说明**：问题2/3/4/5/6/7 虽归入"其他类型"，但共享同一根因模式——POM 骨架搭建阶段偏离了 OOD §2.1/§2.2 的设计规范，详见"跨问题根因模式整合"。

---

## 汇总

| 序号 | 优先级 | 问题摘要 | 分类 | 是否为误报 | 对应 todo 项 | 根因所在 |
|------|--------|---------|------|-----------|-------------|---------|
| 1 | P2 | BaseEnum 未在 §1.3 定义 | OOD 文档不完善（类型③） | 否 | todo 第2项 | `Docs/04_ood_phase0.md` §1.3 |
| 2 | P1 | POM modules 路径与设计 §2.1 不一致 | 代码与设计双向偏离（类型④） | 否 | todo 第3项 | 代码目录结构 vs 设计目录约定 |
| 3 | P1 | 聚合 POM 缺失 | 代码偏离设计，问题2 的连带后果（类型④） | 否 | todo 第4项 | 代码目录结构 vs 设计目录约定 |
| 4 | P2 | Spring Boot 版本 3.2.5 vs 3.3.0 | 代码与设计版本偏离（类型④） | 否 | todo 第5项 | 版本决策未同步（含 §1.4 运行时环境表） |
| 5 | P2 | dependencyManagement 缺少 starter 声明 | 代码偏离设计（类型④） | 否 | todo 第6项 | `pom.xml` 未按骨架实现 |
| 6 | P2 | Common POM 多引入 validation (optional) | 代码偏离设计（类型④） | 否 | todo 第7项 | `common/pom.xml` 额外依赖 |
| 7 | P1 | maven-dependency-plugin 额外忽略 business | 代码与设计偏离（类型④） | 否 | todo 第8项 | `pom.xml` 豁免配置过宽 |
| 8 | P0 | GlobalExceptionHandler 缺少序列化异常处理器 | 代码缺陷（类型①） | 否 | todo 第9项 | `GlobalExceptionHandler.java` 功能遗漏 |
| 9 | P2 | FallbackAiServiceTest 未验证日志输出 | 测试覆盖不充分（类型④） | 否 | todo 第10项 | `FallbackAiServiceTest.java` 未覆盖日志（关联 known_issues.md K3） |
| 10 | P0 | ApiClient 错误拦截器未实现 NETWORK_ERROR | 代码缺陷（类型①） | 否 | todo 第11项 | `api/index.ts` 功能遗漏 |

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

第3步（可与第2步并行）：
  ├── 修复代码缺陷：
  │   ├── 问题8（P0，独立，可最先执行）
  │   └── 问题10（P0，独立，可最先执行）
  ├── 修复 POM 配置：
  │   ├── 问题7（P1，依赖于第1步的 POM 修改结果）
  │   ├── 问题5（P2，可在此轮一并调整）
  │   └── 问题6（P2，可在此轮一并调整）
  └── 修复测试 & 文档：
      ├── 问题9（P2，独立，不依赖任何前置步骤）
      └── 问题1（P2，独立，不依赖任何前置步骤）
```

**并行机会说明**：
- 问题8 和 问题10 完全独立，可第一时间并行分配
- 问题9 和 问题1 独立于其他所有步骤，可随时并行执行
- 问题5/6/7 可合并为"POM 配置一轮对齐"任务，由同一修复者处理
- 问题2/3/4 的决策链路构成串行依赖，是整体修复路径的关键路径

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

**备选方案**（推荐）：不在父 POM 中直接移除这三个 ignore 条目，而是将 `patient`、`doctor`、`admin` 的 `<ignoredUnusedDeclaredDependency>` 从父 POM 的 `<plugin><configuration>` 移入 `application/pom.xml` 自身的 `maven-dependency-plugin` 配置中，限定豁免范围仅对 application 模块生效。这样既：
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
