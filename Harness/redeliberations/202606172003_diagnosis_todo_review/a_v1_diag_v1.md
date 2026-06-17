# 诊断报告：审查报告 `todo.md` 逐条分析

## 总体结论

| 编号 | 分类 | 真实存在 | 误报 | OOD文档问题 | 其他 |
|------|------|---------|------|-----------|------|
| T1 | PageQuery缺少校验注解 | ✓ | | | |
| T2 | common-module-impl缺少包目录 | ✓ | | | |
| T3 | 父POM Starter冗余版本号 | ✓ | | | |
| T4 | 父POM h2误设scope | ✓ | | | |
| T5 | common缺少MeterRegistryCustomizer | ✓ | | | |
| T6 | Axios未实现Result.code拆包 | ✓ | | | |
| T7 | ai-impl冗余common依赖 | ✓ | | | |
| T8 | common-module-impl冗余common依赖 | ✓ | | | |
| T9 | common缺少util包 | ✓ | | | |
| T10 | FallbackAiService日志时机不一致 | ✓ | | | |
| T11 | BaseEntityTest未验证审计自动填充 | ✓ | | | |

---

## T1 — PageQuery 缺少 `@Min(0)` / `@Max(500)` 校验注解

**分类**：真实存在

**现象**：`PageQuery.java:7-9` 的 `page` 和 `size` 字段仅有默认值，缺少 `jakarta.validation.constraints.Min` 和 `jakarta.validation.constraints.Max` 注解。

**根因**：OOD §3.1 明确要求 `page` 标注 `@Min(0)`、`size` 标注 `@Max(500)`，当前代码未添加。

**证据**：
- `PageQuery.java:7`：`private int page = 0;` — 无 `@Min(0)`
- `PageQuery.java:9`：`private int size = 20;` — 无 `@Max(500)`

---

## T2 — common-module-impl 缺少 `config/` 包目录

**分类**：真实存在（审查描述存在不精确之处）

**现象**：OOD §2.3 包命名规范要求 common-module-impl 包含 `permission/`、`config/`、`dict/` 三个子包。实际代码中 `dict/` 和 `permission/` 已存在，但 `config/` 缺失。审查描述称"当前仅实现了 permission 包"不准确（`dict/` 已存在），但核心问题（缺失 `config/`）真实存在。

**根因**：代码实现未覆盖 OOD 指定的全部子包。

**证据**：
- 目录 `common-module-impl/src/main/java/com/aimedical/modules/commonmodule/` — 包含 `dict/`、`permission/`，缺少 `config/`

---

## T3 — 父 POM dependencyManagement 中 Starter 依赖冗余显式版本号

**分类**：真实存在

**现象**：`pom.xml:84-109` 中 `spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-security`、`spring-boot-starter-validation`、`spring-boot-starter-test` 均显式标注了 `<version>3.2.5</version>`。

**根因**：父 POM 继承自 `spring-boot-starter-parent:3.2.5`，该 parent 的 BOM 已统一管理所有 Spring Boot Starter 的版本。dependencyManagement 中重复标注版本号违反"版本由 BOM 统一管理"的原则，且增加升级时遗漏同步的风险。

**证据**：
- `pom.xml:86-87`：`<artifactId>spring-boot-starter-web</artifactId><version>3.2.5</version>`
- `pom.xml:91-92`：`<artifactId>spring-boot-starter-data-jpa</artifactId><version>3.2.5</version>`
- `pom.xml:96-97`：`<artifactId>spring-boot-starter-security</artifactId><version>3.2.5</version>`
- `pom.xml:101-102`：`<artifactId>spring-boot-starter-validation</artifactId><version>3.2.5</version>`
- `pom.xml:106-107`：`<artifactId>spring-boot-starter-test</artifactId><version>3.2.5</version>`

---

## T4 — 父 POM dependencyManagement 中 h2 依赖误设 scope

**分类**：真实存在

**现象**：`pom.xml:82` 中 h2 依赖条目在 `<dependencyManagement>` 内设置了 `<scope>runtime</scope>`。

**根因**：OOD §2.2 及 §9.1 明确约定 h2 及其 scope 仅由 application 模块自行声明，父 POM 的 dependencyManagement 只负责管理版本号，scope 由各消费模块自行决定。当前写法强制所有继承该 dependencyManagement 的模块继承 runtime scope，与"仅 application 使用 h2"的设计矛盾。

**证据**：
- `pom.xml:78-83`：`<groupId>com.h2database</groupId><artifactId>h2</artifactId><version>${h2.version}</version><scope>runtime</scope>`
- OOD 父 POM 骨架：h2 条目无 scope（`<scope>` 仅出现于 starter-test 条目）

---

## T5 — common 模块缺少 MeterRegistryCustomizer 占位配置

**分类**：真实存在（属推荐补齐项，不影响骨架验收）

**现象**：OOD §10.1 要求在 `com.aimedical.common.config` 包中声明 `MeterRegistryCustomizer` 配置类占位（设置通用标签如 `application=aimedical-sys`），当前不存在。

**根因**：代码未实现 OOD §10.1 设计。OOD 同时声明该骨架为"推荐补齐项，不影响 Phase 0 骨架验收"（§10.1 首段），且路线图 Phase 0.2 将其列为"推荐补齐"项。

**证据**：
- 目录 `common/src/main/java/com/aimedical/common/config/` — 包含 `GlobalExceptionHandler.java`、`JacksonConfig.java`、`JpaConfig.java`，无 `MeterRegistryCustomizer` 相关文件

---

## T6 — Axios 响应拦截器未实现 OOD §4.2 规定的 Result.code 拆包逻辑

**分类**：真实存在

**现象**：`frontend/packages/shared/src/api/index.ts:10-26` 的 Axios 响应拦截器直接返回 `response.data`（完整 `Result<T>` 包装体），未按 OOD §4.2 对 `Result.code` 做拆包。

**根因**：OOD §4.2 明确规定前端响应拦截器行为：
- `response.data.code === "SUCCESS"` → 返回 `response.data.data`
- `response.data.code !== "SUCCESS"` → 走错误处理

当前拦截器仅执行 `return response.data`，导致各 API 调用方获得的是完整包装体而非解包后的业务数据，各调用处需自行解包，违反"统一处理"设计目标。

**证据**：
- `index.ts:11-12`：`(response) => { return response.data as ApiResponse<unknown> }` — 未检查 `code` 字段，未返回 `data` 字段

---

## T7 — ai-impl POM 声明了冗余的 common 直接依赖

**分类**：真实存在

**现象**：`ai-impl/pom.xml:17-20` 同时声明了 `ai-api` 和 `common`。`ai-api` 已依赖 `common`（编译期可见），因此 `common` 作为传递性依赖已可通过 `ai-api` 获得。

**根因**：OOD §2.2 定义 `ai-impl → ai-api` 的单向依赖关系。`ai-api` 自身依赖 `common`（在 `ai-api/pom.xml` 中声明的 compile scope），因此 `ai-impl` 中的 `common` 声明是冗余的传递性依赖，增加不必要的维护负担。

**证据**：
- `ai-impl/pom.xml:17-20`：同时声明 `ai-api` 和 `common`
- `ai-api/pom.xml:13-16`：`common` 在 `ai-api` 中以默认 compile scope 声明

---

## T8 — common-module-impl POM 声明了冗余的 common 直接依赖

**分类**：真实存在

**现象**：`common-module-impl/pom.xml:17-20` 同时声明了 `common-module-api` 和 `common`。`common-module-api` 已依赖 `common`（编译期可见），因此 `common` 作为传递性依赖已可通过 `common-module-api` 获得。

**根因**：OOD §2.2 定义 `common-module-impl → common-module-api` 的单向依赖关系。`common-module-api` 自身依赖 `common`（在 `common-module-api/pom.xml` 中声明的 compile scope），因此 `common-module-impl` 中的 `common` 声明冗余。

**证据**：
- `common-module-impl/pom.xml:17-20`：同时声明 `common-module-api` 和 `common`
- `common-module-api/pom.xml:13-16`：`common` 在 `common-module-api` 中以默认 compile scope 声明

---

## T9 — common 模块缺少 util 包目录

**分类**：真实存在

**现象**：OOD §2.3 包命名规范要求 common 模块包含 `base`、`result`、`exception`、`util`、`config` 五个子包，当前 `util` 包缺失（目录下仅有 `base/`、`config/`、`exception/`、`result/`）。

**根因**：代码实现未覆盖 OOD 指定的全部子包。

**证据**：
- 目录 `common/src/main/java/com/aimedical/common/` — 有 `base/`、`config/`、`exception/`、`result/`，无 `util/`

---

## T10 — FallbackAiService ERROR 日志触发时机与 OOD §3.4 不一致

**分类**：真实存在

**现象**：OOD §3.4 规定"启动期输出 ERROR 日志、运行期输出 WARN 日志"，但当前 `FallbackAiService.java:60-67` 的 `handleEmptyDelegates()` 方法在首次调用时触发 ERROR，而非在构造器/启动期检查。后续调用触发 WARN。

**根因**：代码仅在方法调用时惰性检测 `delegates` 是否为空并输出相应日志。OOD 要求 ERROR 在启动期（构造器阶段）发出，WARN 在运行期发出。当前实现将 ERROR 延后到了首次调用时。

**差异分析**：
- OOD 意图：`FallbackAiService` 构造完成后立即检测 `delegates` 为空 → 输出 ERROR（启动期标识配置异常）；后续每次调用仅输出 WARN（运行时不影响功能但提示运维）。
- 当前实现：构造器不检测，`handleEmptyDelegates()` 使用 `AtomicBoolean` 在第一次调用时输出 ERROR、后续输出 WARN。

**证据**：
- `FallbackAiService.java:52-58`：构造器仅完成 `delegates` 过滤和 `strategies` 赋值，不做空检测和日志输出
- `FallbackAiService.java:60-67`：`handleEmptyDelegates()` 在第一次调用时输出 ERROR（`firstEmptyDelegateCall.getAndSet(false)` 首次返回 `true`）

---

## T11 — BaseEntityTest 未验证审计字段自动填充

**分类**：真实存在

**现象**：OOD §3.2 明确 `createdAt` 由 `@CreatedDate` + `AuditingEntityListener` 自动填充，`updatedAt` 由 `@LastModifiedDate` 自动填充。`BaseEntityTest.java:46-48` 仅通过 `new TestEntity()` 验证了 POJO 级 setter/getter 默认值行为，未在 Spring Data JPA 上下文（`@SpringBootTest` + `@EntityListeners`）中验证审计监听器是否按预期自动填充时间戳。

**根因**：当前测试仅覆盖 POJO 层（无 Spring 上下文），无法验证 JPA 审计生命周期回调。这是 Phase 0 骨架测试的典型占位行为，但确实未覆盖 OOD 规定的审计字段自动填充契约。

**证据**：
- `BaseEntityTest.java`：纯 JUnit 5 测试类，无 `@SpringBootTest` 或 `@DataJpaTest` 注解，无 JPA 上下文
- 测试 `shouldCreateWithDefaultValues()` 验证 `assertNull(entity.getCreatedAt()`，这恰好是 POJO 默认值（null），与审计填充后的行为相反
- 测试 `shouldSetAndGetTimestamps()` 手动 set 时间戳，绕过了审计机制
