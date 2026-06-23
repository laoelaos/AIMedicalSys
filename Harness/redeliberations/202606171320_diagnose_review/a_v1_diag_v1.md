# 代码评审问题诊断报告

## 诊断信息

- **诊断时间**：2026-06-17
- **数据来源**：`Harness/reviews/202606171248_code_review/`（2轮6份审查报告 + todo + scope + review + known_issues）
- **设计依据**：`Docs/04_ood_phase0.md`（Phase 0 OOD）
- **代码基线**：`AIMedical/` 目录下全部实现代码
- **路线图**：`Docs/03_roadmap.md`

---

## 问题1：BaseEnum 为设计文档未定义类型

### 分类

**OOD 文档不完善**。代码实现无误，设计文档缺失对该类型的规范定义。

### 现象

BaseEnum interface 存在于 `common/.../base/BaseEnum.java:1-6`，提供 `getCode()` 与 `getDesc()` 两个方法签名。OOD §2.1 目录布局（`base/` 含 `BaseEntity, BaseEnum`）已预留该类型，但 §3.x "核心抽象规范"中无任何针对 BaseEnum 的定义、方法命名约定、使用场景或与 ErrorCode 的关系说明。

### 证据链

1. **OOD §2.1 目录布局**（`04_ood_phase0.md:70`）明确列出 `base/` 包下包含 `BaseEntity, BaseEnum`，证明设计层已知该类型存在。
2. **代码层**：`BaseEnum.java` 实际存在，定义为 `public interface BaseEnum`，包含 `String getCode()` 和 `String getDesc()` 两个方法。
3. **OOD §3.x "核心抽象一览"表**（`04_ood_phase0.md:24-33`）列举了 `Result<T>`、`PageQuery`、`ErrorCode`、`BaseEntity`、`GlobalExceptionHandler`、`AiService`、`Role`/`Post`/`Function`、`User` 共 9 项核心抽象，**不包含 BaseEnum**。
4. **对比 `ErrorCode` 接口**：OOD §3.1 对 ErrorCode 有完整定义（interface 形态、`code()`/`message()` 签名、各模块 enum 实现）。BaseEnum 接口（`getCode()`/`getDesc()`）与 ErrorCode（`code()`/`message()`）方法名不同、职责不同——BaseEnum 用于枚举值通用转换，ErrorCode 用于错误码命名空间契约。

### 根因

OOD 文档在 §2.1 目录布局中预留了 BaseEnum 类型，但在 §3.x 核心抽象定义中遗漏了该类型的规范说明。属于设计文档层面的不完善（编写阶段遗漏），非代码缺陷。BaseEnum 作为枚举通用基接口（提供 code/desc 获取），与 ErrorCode 有本质不同的职责边界，不存在合并或冲突问题。

### 影响范围

- OOD 文档 §3.x 需在下一阶段补充 BaseEnum 定义段落
- 代码侧无任何问题，无需修改

---

## 问题2：父 POM modules 路径与设计 §2.1 不一致

### 分类

**代码与 OOD 的设计偏离**（双向不一致）。

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
- 建议根据实际决策二选一：升代码到 3.3.0 或降 OOD 到 3.2.5

---

## 问题5：dependencyManagement 缺少外部 starter 统一声明

### 分类

**代码与 OOD 的偏差**（设计意图未落实）。

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

**代码缺陷**（该配置可能导致依赖问题被掩盖）。

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
- 修复措施：移除 patient/doctor/admin 三个条目，保留 ai-api 和 common-module-api 两个设计认可的豁免。

---

## 问题8：GlobalExceptionHandler 缺少 HttpMessageNotReadableException / HttpMessageNotWritableException 专用处理器

### 分类

**代码缺陷**（设计明确要求实现的功能未实现）。

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

- 请求体 JSON 格式错误时：返回 HTTP 500 SYSTEM_ERROR 而非期望的 400 PARAM_INVALID
- 响应体序列化错误时：兜底 500 SYSTEM_ERROR（结果与设计一致，但违反了"异常应统一注册 @ExceptionHandler"的设计原则）
- 前端错误处理逻辑若依赖 400 状态码区分"请求格式错误"与"系统异常"，将受到错误引导
- 不影响骨架的 ping 健康检查和其他正常流程

---

## 问题9：FallbackAiServiceTest 未验证空委托列表的日志输出

### 分类

**测试覆盖不充分**（代码无缺陷）。

### 现象

`FallbackAiService.java:60-67` 的 `handleEmptyDelegates()` 方法使用 `AtomicBoolean firstEmptyDelegateCall` 在首次空委托调用时输出 `log.error(...)`（启动期语义），后续调用输出 `log.warn(...)`（运行期语义），符合 OOD §3.4 的"启动期输出 ERROR 日志、运行期输出 WARN 日志"要求。

`shouldReturnFallbackResultWhenNoDelegateAvailable()` 测试（`FallbackAiServiceTest.java:34-42`）验证了返回值的 `degraded=true`、`success=false`、`fallbackReason="No available AiService delegate"`，但未通过日志附加器验证日志的输出级别和内容。

### 证据链

1. **OOD §3.4 兜底保护**（`04_ood_phase0.md:677`）："启动期输出 ERROR 日志、运行期输出 WARN 日志"。
2. **代码实现**（`FallbackAiService.java:60-67`）：`log.error("No available AiService delegate")` 和 `log.warn("No available AiService delegate")` 的分支逻辑正确。
3. **测试代码**（`FallbackAiServiceTest.java:34-42`）：仅 4 个 `assertEquals/assertTrue/assertFalse` 断言，无任何日志验证相关代码（未使用 `ListAppender`、未 Mock Logger 等）。

### 根因

测试编写时聚焦于功能验证（返回值的语义正确性），遗漏了日志行为的验证。虽然代码正确实现了日志输出，但缺乏测试覆盖意味着日志行为不受回归保护——未来如果删除或修改日志输出，测试不会失败。

### 影响范围

- 当前代码无缺陷——日志输出的分支逻辑和内容完全符合 OOD 要求。
- 日志行为无测试保护，后续重构有退化风险。
- 建议在测试中追加 Logback `ListAppender` 或 Mock Logger 的日志验证。

---

## 问题10：ApiClient 错误拦截器未按 §3.5 实现 NETWORK_ERROR 处理

### 分类

**代码缺陷**（设计要求的功能未实现）。

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

- 前端代码无法通过 `code === "NETWORK_ERROR"` 统一判断网络错误——这是 OOD 要求的设计模式
- 未捕获的 Promise 异常可能在控制台产生报错
- 不影响正常 API 调用（`response.data.code === "SUCCESS"` 分支正常）
- Phase 1+ 如果引入 `AuthStore` 的 401 跳转逻辑，错误拦截器的统一处理能力将更加必要

---

## 汇总

| 序号 | 问题摘要 | 分类 | 根因所在 |
|------|---------|------|---------|
| 1 | BaseEnum 未在 §3.x 定义 | OOD 文档不完善 | `Docs/04_ood_phase0.md` §3.x |
| 2 | POM modules 路径与设计 §2.1 不一致 | 代码与设计双向偏离 | 代码目录结构 vs 设计目录约定 |
| 3 | 聚合 POM 缺失 | 代码偏离设计（问题2 的连带后果） | 代码目录结构 vs 设计目录约定 |
| 4 | Spring Boot 版本 3.2.5 vs 3.3.0 | 代码与设计版本偏离 | 版本决策未同步 |
| 5 | dependencyManagement 缺少 starter 声明 | 代码偏离设计 | `pom.xml` 未按骨架实现 |
| 6 | Common POM 多引入 validation (optional) | 代码偏离设计 | `common/pom.xml` 额外依赖 |
| 7 | maven-dependency-plugin 额外忽略 business | 代码缺陷 | `pom.xml` 豁免配置过宽 |
| 8 | GlobalExceptionHandler 缺少序列化异常处理器 | 代码缺陷 | `GlobalExceptionHandler.java` 功能遗漏 |
| 9 | FallbackAiServiceTest 未验证日志输出 | 测试覆盖不充分 | `FallbackAiServiceTest.java` 未覆盖日志 |
| 10 | ApiClient 错误拦截器未实现 NETWORK_ERROR | 代码缺陷 | `api/index.ts` 功能遗漏 |

### 修复者指引

- **修复代码**：问题 7、8、10 需修改代码；问题 5、6 按设计对齐代码或由设计侧确认后保留（需同步更新 OOD）；问题 2、3、4 需在代码重构与 OOD 更新间二择其一。
- **修复 OOD 文档**：问题 1 需在 §3.x 补充 BaseEnum 定义。
- **修复测试**：问题 9 需在测试中追加日志验证。

各问题的具体代码位置和触发条件已在各节的"证据链"中定位到文件/行号级别，修复者可据此识别"改哪里"和"为什么"。
