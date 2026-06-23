# 诊断报告：审查报告 `todo.md` 逐条分析

## 总体结论

| 编号 | 分类 | 优先级 | 修复影响范围 | 真实存在 | 误报 | OOD文档问题 | 其他 |
|------|------|--------|-------------|---------|------|-----------|------|
| T1 | PageQuery缺少校验注解 | **高** | 所有分页接口的校验层 | ✓ | | | |
| T2 | common-module-impl缺少config包目录 | 中 | 工程结构，无运行时影响 | ✓ | | | |
| T3 | 父POM Starter冗余版本号 | **高** | 父POM依赖管理的可维护性 | ✓ | | | |
| T4 | 父POM h2误设scope | **高** | h2依赖在构建链路中的scope行为 | ✓ | | | |
| T5 | common缺少MeterRegistryCustomizer | 低 | 可观测性骨架，无关验收 | | | ✓ ✓已修复 | |
| T6 | Axios未实现Result.code拆包 | **高** | 前端API调用层的契约行为 | ✓ | | | |
| T7 | ai-impl冗余common依赖 | 中 | ai-impl的依赖维护 | ✓ | | | |
| T8 | common-module-impl冗余common依赖 | | | | ✓ | | |
| T9 | common缺少util包 | 中 | 工程结构，与T2同质 | ✓ | | | |
| T10 | FallbackAiService日志时机不一致 | 中 | FallbackAiService的日志行为 | ✓ | | | |
| T11 | BaseEntityTest未验证审计自动填充 | 中 | 单元测试覆盖率 | ✓ | | | |

### 四分类覆盖说明

需求要求的四个分类维度均已评估：T1/T2/T3/T4/T6/T7/T9/T10/T11 为**真实存在**（代码缺陷），T8 为**误报**（审查工具判断有误），T5 为**OOD文档问题**（问题根源在OOD文档呈现方式）。**其他类型**（环境配置问题、第三方依赖问题等）经逐一评估，无条目符合——当前11项问题的根因均在代码实现、审查判断或OOD文档范围内，不涉及环境或第三方因素。

### 分类修正说明

- **T5 → OOD文档问题**：OOD §10.1 在正文中以设计规范形式描述 MeterRegistryCustomizer，但同段声明"该骨架为推荐补齐项，不影响 Phase 0 骨架验收"；路线图 0.2 将其列为"推荐补齐"类。OOD 正文与阶段性声明间的矛盾导致审查工具有理由将其标记为缺失，但代码实际正确遵循了"可选"语义。根因在 OOD 文档的呈现方式，而非代码缺陷。
- **T8 → 误报**：common-module-impl 的 permission 实体（User.java:19、Role.java:15、Post.java:17、Function.java:14）均 `extends BaseEntity`，BaseEntity 来自 common 模块。虽然 common 确实可通过 common-module-api 以传递性依赖获得（common-module-api/pom.xml 以 compile scope 声明 common），但 Maven 最佳实践要求代码中直接引用的类型所在模块必须显式声明为直接依赖，否则 `mvn dependency:analyze` 会报"Used undeclared dependency"。当前显式声明是正确的，审查报告错误地将此标记为冗余。
- **T10 → 真实存在**：代码未实现 OOD §3.4 的"启动期 ERROR"要求。OOD"启动期"泛指应用初始化阶段（包括构造器阶段和 `@PostConstruct` 阶段），在 Spring DI 环境下技术可行。当前代码采用的惰性检测模式将 ERROR 从启动期延迟到首次调用期，是代码实现与 OOD 规范之间的偏差，而非 OOD 文档问题。详见 T10 分析。

---

## T1 — PageQuery 缺少 `@Min(0)` / `@Max(500)` 校验注解

**分类**：真实存在 | **优先级**：高

**现象**：`PageQuery.java:7-9` 的 `page` 和 `size` 字段仅有默认值，缺少 `jakarta.validation.constraints.Min` 和 `jakarta.validation.constraints.Max` 注解。

**根因**：OOD §3.1 明确要求 `page` 标注 `@Min(0)`、`size` 标注 `@Max(500)`，当前代码未添加。

**修复者行动指引**：
1. 在 `PageQuery` 的 `page` 字段添加 `@Min(0)`，`size` 字段添加 `@Max(500)`
2. **前置条件 — common 模块编译期依赖**：`PageQuery.java` 位于 `common` 模块，需确保 `jakarta.validation-api` 在 `common` 模块编译 classpath 中可用。当前 `common/pom.xml` 仅声明了 `spring-boot-starter-web`（optional）、`spring-boot-starter-data-jpa`（optional）、`spring-boot-starter-test`（test），均不包含 `jakarta.validation-api`。需在 `common/pom.xml` 中以 `<optional>true</optional>` 添加 `spring-boot-starter-validation`（或直接添加 `jakarta.validation-api`），版本由 Spring Boot BOM 统一管理，无需显式指定。
3. **前置条件 — Controller 校验注解生效**：校验注解生效需要 Controller 方法参数同时标注 `@Valid`（或 `@Validated`）。当前骨架无分页 Controller（仅占位 ping），但修复时需确保未来所有分页接口的 Controller 参数标注 `@Valid`。OOD §3.1 已明确"所有分页 Controller 参数需标注 `@Valid`"，执行者需在新增分页 Controller 时同步添加该注解。
4. 确认 `spring-boot-starter-validation` 已作为 compile scope 依赖引入到含 Controller 的模块（patient/doctor/admin）的 pom.xml 中。

**证据**：
- `PageQuery.java:7`：`private int page = 0;` — 无 `@Min(0)`
- `PageQuery.java:9`：`private int size = 20;` — 无 `@Max(500)`

---

## T2 — common-module-impl 缺少 `config/` 包目录

**分类**：真实存在 | **优先级**：中

**现象**：OOD §2.3 包命名规范要求 common-module-impl 包含 `permission/`、`config/`、`dict/` 三个子包。实际代码中 `dict/` 和 `permission/` 已存在，但 `config/` 缺失。

**审查描述偏差**：todo.md T2 描述"当前仅实现了 permission 包"不精确——`dict/` 实际已存在（但仅有 `.gitkeep` 占位文件，无实质 Java 代码，与 `config/` 缺失性质不同但实现完成度相近）。真实问题是缺少 `config/` 而非缺少 `config/`和`dict/`。建议修正 todo.md T2 描述为"当前缺少 config/ 包"，并在评估各子包实现成熟度时注意 `dict/` 虽目录存在但尚未包含 Java 实现类。

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

**修复者行动指引**：
1. 从 `pom.xml:84-109` 中删除五个 Starter 的 `<version>3.2.5</version>` 行
2. **验证步骤**：运行 `mvn compile` 确认依赖解析正常，无版本冲突；运行 `mvn dependency:tree` 确认版本号均正确继承自 Spring Boot BOM
3. 与 T4 合并为同一 PR 提交

**证据**：
- `pom.xml:84-109`：五个 Starter 均显式声明 `<version>3.2.5</version>`

---

## T4 — 父 POM dependencyManagement 中 h2 依赖误设 scope

**分类**：真实存在 | **优先级**：高

**现象**：`pom.xml:82` 中 h2 依赖条目在 `<dependencyManagement>` 内设置了 `<scope>runtime</scope>`。

**根因**：OOD §2.2 及 §9.1 明确约定 h2 及其 scope 仅由 application 模块自行声明，父 POM 的 dependencyManagement 只负责管理版本号。当前写法强制所有继承该 dependencyManagement 的模块继承 runtime scope，与"仅 application 使用 h2"的设计矛盾。

**修复关联**：T4 与 T3 同处 `pom.xml` 的 `<dependencyManagement>` 节，建议合并处理。

**修复者行动指引**：
1. 从 `pom.xml:82` 的 h2 依赖中删除 `<scope>runtime</scope>`，仅保留版本号声明
2. **验证步骤**：运行 `mvn compile` 确认依赖解析正常；运行 `mvn dependency:tree` 确认各模块 h2 scope 符合预期（仅 application 模块在模块级 pom.xml 中声明 runtime scope，其余模块不直接持有 h2）
3. 与 T3 合并为同一 PR 提交

**证据**：
- `pom.xml:78-83`：h2 条目含 `<scope>runtime</scope>`
- OOD 父 POM 骨架示例（§2.1）：h2 条目无 scope

---

## T5 — common 模块缺少 MeterRegistryCustomizer 占位配置

**分类**：**OOD文档问题** | **优先级**：低 | **状态**：**已修复** ✓

**现象**：OOD §10.1 要求在 `com.aimedical.common.config` 包中声明 `MeterRegistryCustomizer` 配置类占位，当前不存在。

**根因分歧说明**：OOD §10.1 采用"设计规范 + 可选性标注"的标准文档模式——正文描述配置类的设计规格，段首同步声明"该骨架为推荐补齐项，不影响 Phase 0 骨架验收"。审查工具无法理解此类自然语言可选性声明标记，将设计规范正文中的描述误判为硬性缺失。根因并非 OOD 正文与声明间的矛盾（此为标准文档实践），而是可选性标注缺乏机制化可识别性。优化方向为增强可选性标注的机器可解析性（如使用结构化标记而非纯文本声明）。

**修复者行动指引**：需消除 OOD §10.1 正文与阶段性声明之间的语义矛盾。建议二选一：
- 方案 A：从 §10.1 设计规范正文中删除 MeterRegistryCustomizer 的具体实现描述，仅在路线图 0.2"推荐补齐"部分保留该项记录
- 方案 B：保留 §10.1 正文但在其段首增加醒目标记（如 `> **可选**` 引用块或 `⚠️` 标注），明确区分规范正文中的可选条目与硬性设计要求，避免审查工具再次误判

**执行责任归属**：此修复涉及 OOD 文档（`Docs/04_ood_phase0.md`）修改而非代码修改，应由架构师或技术负责人执行和评审，开发工程师不应自行修改设计文档。若团队流程需经架构评审委员会（ARC）审批，则应纳入架构变更流程。

**证据**：
- `common/src/main/java/com/aimedical/common/config/` 目录下现有 `GlobalExceptionHandler.java`、`JacksonConfig.java`、`JpaConfig.java`，无 MeterRegistryCustomizer 相关文件
- OOD §10.1 L1176（修复前）："该骨架为推荐补齐项，不影响 Phase 0 骨架验收"；L1180-1182 MeterRegistryCustomizer 描述细节

**用户依据：** 修复OOD

**修复执行记录（方案 B）**：

- **修复日期**：2026-06-17
- **修复文件**：`Docs/04_ood_phase0.md` §10.1（L1174-L1200）
- **修复方式**：在 §10.1 段首新增结构化 `> **⚠️ 可选 (Phase 0 跳过)**` 引用块，明确声明本节全部内容为推荐补齐项、不影响 Phase 0 骨架验收，并指示审查工具跳过对以下条目的缺失检测；同时在 §10.1 各子条目（MeterRegistryCustomizer 声明、micrometer-observation 依赖、Actuator 暴露端点配置、指标命名约定、Phase 0 验收 curl 命令、生产环境安全说明）行首添加 `[可选]` 标签，实现细粒度的机器可解析性。
- **修复效果**：
  - 段首引用块以醒目的 `⚠️` 标记 + 关键词 "可选" + "Phase 0 跳过" 组合呈现，远超原纯文本声明的机器可解析性
  - 各子条目的 `[可选]` 标签作为行内结构化标记，审查工具可基于正则（如 `\[可选\]`）精确识别可选条目与硬性要求
  - 保留 §10.1 全部设计意图，Phase 1+ 仍可按规范补齐 MeterRegistryCustomizer 等配置
  - 不涉及任何代码或 POM 变更，符合"修 OOD 不改代码"原则
- **修复后 OOD §10.1 L1176 文本**：

  > **⚠️ 可选 (Phase 0 跳过)**：本节全部内容为推荐补齐项，**不影响 Phase 0 骨架验收**。Phase 0 不强制实现 `MeterRegistryCustomizer`、不强制补充 `micrometer-observation` 依赖、不强制配置 Actuator 暴露端点；所有审查工具应以本标记为准，跳过对以下条目的缺失检测。代码中保留相应扩展点即可（`common.config` 包下未来按需新增配置类）。

---

## T6 — Axios 响应拦截器未实现 OOD §4.2 规定的 Result.code 拆包逻辑

**分类**：真实存在 | **优先级**：高

**现象**：`packages/shared/src/api/index.ts:10-26` 的 Axios 响应拦截器直接返回 `response.data`（完整 `Result<T>` 包装体），未按 OOD §4.2 对 `Result.code` 做拆包。

**根因**：OOD §4.2 明确规定前端响应拦截器行为：
- `response.data.code === "SUCCESS"` → 返回 `response.data.data`
- `response.data.code !== "SUCCESS"` → 走错误处理

当前拦截器仅执行 `return response.data`，导致各 API 调用方获得的是完整包装体而非解包后的业务数据。

**现有错误拦截器路径分析**：`packages/shared/src/api/index.ts:14-25` 的 error 回调已处理四类 HTTP/网络层异常：`NETWORK_ERROR`（网络不可达）、`UNAUTHORIZED`（401）、`FORBIDDEN`（403）、`HTTP_ERROR`（其他 HTTP 状态码）。业务级错误码 `code !== "SUCCESS"` 在 HTTP 层面仍是 200 响应，不会进入此 error 拦截器。

**业务错误码路由缺口分析**：OOD §4.2 要求 `code !== "SUCCESS"` 时"走统一错误处理"。但当前架构下该路径不可达——HTTP 200 响应经过 success 拦截器（第 11~12 行）直接返回 `response.data`，不检查 `code` 字段；error 拦截器仅覆盖 HTTP/网络层异常，无法覆盖业务级 `code !== "SUCCESS"` 场景。因此代码存在中间缺口：业务层错误码需要在 success 拦截器中主动路由到错误处理路径，当前零实现。

**实现路径分析**：两种可行方向——

A. **throw 至 error 拦截器**：success 拦截器检测到 `code !== "SUCCESS"` 时 `return Promise.reject(response.data)` 将业务错误抛给 error 拦截器。此方式存在关键冲突——当前 error 拦截器 `index.ts:15` 的判断链首条件是 `error.response === undefined`，而 throw 的 `response.data`（即 `{ code: "...", message: "..." }`）不含 `response` 属性，因此 `error.response === undefined` 为 true，**所有业务错误会被错误映射为 `NETWORK_ERROR`**，无法进入预期的"新增业务错误码处理分支"。error 拦截器需增加三种异常来源的区分逻辑，改造后的判断链示例如下：

```typescript
(error): Promise<BusinessError> => {
  // ① 业务错误（从 success 拦截器 throw 的业务错误码对象）
  if (error.isBusinessError) {
    return handleBusinessError(error); // 新增业务错误码处理分支
  }
  // ② 网络错误（无 response，且非业务错误标记）
  if (error.response === undefined) {
    return Promise.resolve({ code: 'NETWORK_ERROR', message: '网络不可达，请检查网络连接' } as BusinessError);
  }
  // ③ HTTP 错误（有具体响应状态码）
  if (error.response.status === 401) { ... }
  if (error.response.status === 403) { ... }
  return Promise.resolve({ code: 'HTTP_ERROR', message: `请求失败（${error.response.status}）` } as BusinessError);
}
```

对应的 success 拦截器在 throw 时需附加 `isBusinessError` 标记：
```typescript
return Promise.reject({ isBusinessError: true, code: response.data.code, message: response.data.message });
```

此方案的联动影响：`apiGet`/`apiPost`/`apiPut`/`apiDelete` 返回类型从 `Promise<ApiResponse<T>>` 调整为 `Promise<T | BusinessError>`。**零冲击面**——经代码库实测，前端除测试文件 `packages/shared/src/api/__tests__/interceptors.test.ts` 外，不存在任何对上述四个包装函数的现有调用，真实应用代码（`packages/ui-core/`、`packages/*/src/` 不含 `__tests__/` 目录）无一使用。返回类型变更仅需同步更新上述测试文件中的类型断言和集成测试预期，无需改动其他前端代码；error 拦截器需新增业务错误码分支及区分逻辑。

B. **success 拦截器内直接处理**：在 success 拦截器内检查 `code !== "SUCCESS"` 并直接处理，不经过 error 拦截器。此方式避免了 error 拦截器区分逻辑的复杂度。具体实现轮廓如下：

```typescript
// 业务错误处理函数签名
function handleBusinessError(result: { code: string; message: string }): { code: string; message: string } {
  // 统一业务错误处理：弹窗提示或路由到错误页
  console.error(`[Business Error] ${result.code}: ${result.message}`);
  return { code: result.code, message: result.message };
}

// success 拦截器改造
(response) => {
  const body = response.data as ApiResponse<unknown>;
  if (body.code !== 'SUCCESS') {
    return Promise.resolve(handleBusinessError({ code: body.code, message: body.message }));
  }
  return body.data;  // 拆包：返回业务数据而非完整包装体
}
```

关键要点：
- `handleBusinessError()` 函数签名已明确定义：接收 `{ code: string, message: string }`，返回同结构对象
- 调用位置在 success 拦截器内部 `code !== "SUCCESS"` 分支，位于 `packages/shared/src/api/index.ts:11`
- 若同时保留方案 A，可复用同一 `handleBusinessError()` 函数，入参适配 `isBusinessError` 标记格式
- 建议在类型定义文件中定义 `BusinessError = { code: string; message: string }`（或 discriminated union，添加 `kind` 字段如 `kind: 'business' | 'network' | 'http'` 以区分错误来源）并在消费侧通过类型守卫判断
- **error 拦截器返回路径统一约束**：方案 A 中 error 拦截器的所有返回路径（`NETWORK_ERROR`、`HTTP_ERROR`、401、403 等）均需返回 `BusinessError` 类型实例，确保 `Promise<T | BusinessError>` 联合类型在各拦截器分支间一致
- 返回类型变更：`apiGet`/`apiPost`/`apiPut`/`apiDelete` 从 `Promise<ApiResponse<T>>` 调整为 `Promise<T | BusinessError>`
  - SUCCESS 分支返回 `body.data`（业务数据）→ 类型 T
  - 非 SUCCESS 分支返回 `{ code, message }` 结构 → 类型 `BusinessError`
- 测试更新：success interceptor 测试需增加 `code !== "SUCCESS"` 分支的两个测试用例（非成功 code 走业务错误处理、返回结构为 `{ code, message }`），集成测试中 `apiGet` 返回类型从 `ApiResponse` 调整为 `T | BusinessError`

**推荐优先级重新评估**：方案 A 因 error 拦截器需改造三种异常来源的鉴别逻辑，实施复杂度较高，且存在 `NETWORK_ERROR` 误映射风险。推荐调整为**优先方案 B**（success 拦截器内直接处理，改动局部、风险可控），方案 A 作为备选。

无论采用哪种方案，`apiGet`/`apiPost`/`apiPut`/`apiDelete` 等包装函数的返回类型均需从 `Promise<ApiResponse<T>>` 调整为 `Promise<T | BusinessError>`，因为拆包后去掉了外层 `Result` 包装，且业务错误分支返回 `{ code, message }`（类型 `BusinessError`）。

**证据**：
- `packages/shared/src/api/index.ts:11-12`：`(response) => { return response.data as ApiResponse<unknown> }` — 未检查 `code` 字段

---

## T7 — ai-impl POM 声明了冗余的 common 直接依赖

**分类**：真实存在 | **优先级**：中

**现象**：`ai-impl/pom.xml:17-20` 同时声明了 `ai-api` 和 `common`。ai-impl 的 Java 代码无任何对 common 类型（`com.aimedical.common.*`）的直接引用，全部依赖来自 ai-api 的接口和 DTO。

**根因**：ai-impl 的所有 import 均来自 `com.aimedical.modules.ai.api.*`、`java.*`、`org.*`，无一导入 common。`common` 作为传递性依赖可通过 ai-api 获得，无需重复声明。当前写法会被 `mvn dependency:analyze` 标记为 Unused declared dependency。

**工程权衡说明**：某些 Maven 实践主张显式声明传递性依赖以增强可读性，但 Maven 官方推荐避免重复声明。本案中因 ai-impl 代码无 common 直接引用，显式声明既不提升可读性（开发者在 ai-impl 源码中看不到对 common 的使用），又增加了 `dependency:analyze` 的误报干扰，应当移除。

**修复者行动指引**：
1. 从 `ai-impl/pom.xml` 中移除 `<dependency>common</dependency>` 块（第 17-20 行）
2. 运行 `mvn dependency:analyze` 确认无新的 Used undeclared dependency 警告
3. 运行 ai-impl 全量测试确认功能不受影响

**证据**：
- `ai-impl/pom.xml:17-20`：同时声明 `ai-api` 和 `common`
- `ai-api/pom.xml:13-16`：`common` 在 `ai-api` 中以 compile scope 声明
- ai-impl 全量 import 检查：无 `com.aimedical.common` 导入

---

## T9 — common 模块缺少 util 包目录

**分类**：真实存在 | **优先级**：中

**现象**：OOD §2.3 包命名规范要求 common 模块包含 `base`、`result`、`exception`、`util`、`config` 五个子包，当前 `util` 包缺失。

**根因**：代码实现未覆盖 OOD 指定的全部子包。

**修复关联**：T9（缺少 util 包）与 T2（缺少 config 包）同属"OOD 包命名规范未完全对齐"类，均为 Phase 0 工程结构占位缺失。建议合并处理，在对应目录创建 `package-info.java` 占位。

**修复者行动指引**：
1. 在 `common/src/main/java/com/aimedical/common/` 下创建 `util/` 目录
2. 放置 `package-info.java` 占位文件作为 Phase 0 占位
3. 与 T2（config 包占位）合并为同一提交

**证据**：
- 目录 `common/src/main/java/com/aimedical/common/` 下仅有 `base/`、`config/`、`exception/`、`result/`，无 `util/`

---

## T10 — FallbackAiService ERROR 日志触发时机与 OOD §3.4 不一致

**分类**：真实存在 | **优先级**：中（日志行为）

**现象**：OOD §3.4 规定"启动期输出 ERROR 日志、运行期输出 WARN 日志"，但当前 `FallbackAiService.java:60-67` 的 `handleEmptyDelegates()` 在首次调用时触发 ERROR，而非在构造器/启动期检查。后续调用触发 WARN。

**根因**：OOD §3.4 规定"启动期输出 ERROR 日志"。"启动期"泛指应用初始化阶段（包括构造器阶段和 `@PostConstruct` 阶段），而当前代码将 ERROR 触发时机延迟到了"首次调用阶段"，偏离了 OOD 的启动期要求。

**Spring DI 环境可行性分析**：OOD"启动期"要求可通过多种方式实现。构造器阶段：在 Bean 创建时 `List<AiService>` 已可用，直接检查 `delegates.isEmpty()` 并输出 ERROR 日志是技术可行的；`@PostConstruct` 阶段：在 `@PostConstruct` 方法中检查同样满足"启动期"语义，且在依赖注入全部完成后执行，语义更精确。两种方案均优于当前"首次调用"模式。OOD §3.4 的规范是合理的，根因在代码实现未对齐规范，而非 OOD 文档存在工程约束遗漏。

**当前惰性检测的工程合理性评估**：当前 AtomicBoolean once-only 模式是一种典型的"惰性首次检测"实现，可有效防止重复 ERROR 日志刷屏。但其缺陷是：
- 将启动期的配置错误信号延迟到首次调用时才暴露，运维人员无法在应用启动后立即感知"AI 模块异常配置"状态
- Phase 0 中 FallbackAiService 的兜底保护行为（返回 `AiResult.degraded()`）不依赖该日志时机，功能正确性不受影响

**Phase 0 生效范围约束**：当前 Phase 0 默认配置 `ai.mock.enabled=true` 下，`delegates` 列表包含 MockAiService 实例而非空集合，`handleEmptyDelegates()` 在正常流程中不会被调用。该日志行为差异仅当 `ai.mock.enabled=false` 时暴露（当前受 OOD §3.4 约束禁止手动关闭 mock）。因此本项在 Phase 0 实际影响为零，可推迟至 Phase 2+ 引入真实 AiService 时一并修复。Phase 0 中建议标记为"跟踪项"，在路线图中记录修复窗口期。

**修复者行动指引**：推荐方案 B（优先采用）——将 ERROR 日志移到构造器末尾直接输出，保留 `handleEmptyDelegates()` 仅输出 WARN。具体改动：
1. 在构造器第 57~58 行之间（`this.delegates` 赋值完成后）增加：
   ```java
   if (this.delegates.isEmpty()) {
       log.error("No available AiService delegate");
   }
   ```
2. `handleEmptyDelegates()` 移除 `firstEmptyDelegateCall` 的 AtomicBoolean once-only 模式，方法体简化为：
   ```java
   private <T> CompletableFuture<AiResult<T>> handleEmptyDelegates() {
       log.warn("No available AiService delegate");
       return CompletableFuture.completedFuture(AiResult.degraded("No available AiService delegate"));
   }
   ```
3. 移除 `private final AtomicBoolean firstEmptyDelegateCall = new AtomicBoolean(true);` 字段声明
4. **同步更新测试文件** `FallbackAiServiceTest.java`：`shouldLogErrorOnFirstCallThenWarnOnSubsequent()`（第 117-142 行）需重构以验证新行为——构造器阶段输出 ERROR、首次调用仅输出 WARN。当前测试在构造 `FallbackAiService` 前已附加 ListAppender（第 119-121 行），因此构造器输出的 ERROR 会被捕获。修复后执行流程变为：第 124 行构造时产生 1 条 ERROR → 第 126 行首次 triage 产生 1 条 WARN → 第 127 行 `appender.list.size()` 从期望 1 变为 2，断言需同步调整。建议重构为两个独立测试方法：`shouldLogErrorOnConstruction()` 验证构造器阶段输出 ERROR，`shouldLogWarnOnSubsequentCalls()` 验证首次及后续调用仅输出 WARN，并移除第 130 行的 `⚠️` 待办注释标记。
5. **修复后验证闭环**：
   - 运行 `FallbackAiServiceTest` 全量测试，确认拆分后的测试方法均通过：`shouldLogErrorOnConstruction()` 验证构造器阶段输出 1 条 ERROR、`shouldLogWarnOnSubsequentCalls()` 验证首次调用输出 1 条 WARN 且后续调用不再输出 ERROR
   - 确认 logback 配置（`logback.xml`、`logback-spring.xml`）中未对 `com.aimedical.modules.ai.impl.fallback.FallbackAiService` 类或该包设置 `<filter>` 过滤规则阻止 ERROR 级别日志输出。在 logback 级别层次中 ERROR(40000) > WARN(30000)，因此 ERROR 在 `<root level="WARN">` 下必然可见。若存在 `<root level="ERROR">` 或 `<root level="OFF">` 的限制，则需单独为该包开放 ERROR 级别（如 `<logger name="com.aimedical.modules.ai.impl.fallback" level="ERROR"/>`）

**推荐理由**：方案 B 改动量小（修改 3 处源代码 + 1 处测试 + 1 处验证）；与 OOD §3.4 "启动期"原意一致；构造器执行在 Spring 单线程启动期（基于当前 Spring 单线程 Bean 创建的实现行为，非 API 契约保证，但在 Phase 0 框架版本下实际风险极低），无需 AtomicBoolean 等线程安全机制。

**备选启动期方案——@PostConstruct**：若团队倾向于更精确的"启动期"语义（OOD 原文），可使用 `@PostConstruct` 方案替代方案 B。`@PostConstruct` 在依赖注入全部完成后执行，满足"启动期"要求且语义更精确，不会与构造器中的字段赋值逻辑混合。但代价是增加一个独立生命周期方法，代码结构不如方案 B 紧凑。两种启动期方案（构造器 / `@PostConstruct`）均优于当前"首次调用"模式，团队可根据编码风格偏好选择。

**证据**：
- `FallbackAiService.java:52-58`：构造器仅完成 delegates 过滤和 strategies 赋值，不做空检测
- `FallbackAiService.java:60-67`：`handleEmptyDelegates()` 在第一次调用时输出 ERROR

---

## T11 — BaseEntityTest 未验证审计字段自动填充

**分类**：真实存在 | **优先级**：中（测试覆盖）

**现象**：OOD §3.2 明确 `createdAt` 由 `@CreatedDate` + `AuditingEntityListener` 自动填充，`updatedAt` 由 `@LastModifiedDate` 自动填充。`BaseEntityTest.java:46-48` 仅通过 `new TestEntity()` 验证了 POJO 级 setter/getter 默认值行为，未在 Spring Data JPA 上下文（`@SpringBootTest` + `@EntityListeners`）中验证审计监听器是否按预期自动填充时间戳。

**修复者行动指引**：需添加 `@SpringBootTest`（或 `@DataJpaTest`）测试类或测试方法，在 JPA 上下文中持久化实体并验证 `createdAt` 和 `updatedAt` 在 `@PrePersist` / `@PreUpdate` 阶段被正确赋值。当前纯 JUnit 5 测试不可用于验证审计行为。

**前置条件检查**：
- `common/pom.xml` 当前未声明 `com.h2database:h2` 依赖。`@DataJpaTest` 依赖嵌入式数据库自动配置（`spring.boot.test.autoconfigure.AutoConfigureTestDatabase`），该机制需要 H2 driver 在 test classpath 中可用。需在 `common/pom.xml` 的 `<scope>test</scope>` 中补充声明 H2：
  ```xml
  <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>test</scope>
  </dependency>
  ```
  版本由父 POM `<h2.version>`（2.2.224）统一管理，无需显式指定。
- 若选择使用 `@SpringBootTest` 而非 `@DataJpaTest`，且通过 `@ActiveProfiles("test")` 加载独立数据源配置，则需同时确认 `application-test.yml` 或 `application-test.properties` 文件中 H2 数据源、JPA ddl-auto 等配置已就绪。

**证据**：
- `BaseEntityTest.java`：纯 JUnit 5 测试类，无 `@SpringBootTest` 或 `@DataJpaTest` 注解
- `shouldCreateWithDefaultValues()` 验证 `assertNull(entity.getCreatedAt())` — 此为 POJO 默认 null，与审计填充后的行为相反
- `shouldSetAndGetTimestamps()` 手动 set 时间戳，绕过了审计机制

---

## 优先级排序说明

基于"影响范围 × 修复风险 × 修复窗口期"三维评估：

- **影响范围**：当前代码库中缺陷影响的模块/接口/调用方数量
- **修复风险**：修复引入回归或其他副作用的概率和潜在影响
- **修复窗口期**：当前阶段距离该缺陷首次产生实际影响的时间窗口。窗口越小（即缺陷尚未被触发前），修复的优先级越高。例如 T6 的 Axios 拆包缺失当前冲击面为零（无现有 API 调用方），但一旦后续开发接入 API，所有消费者都将收到错误的响应结构，修复成本将因调用方数量增长而放大。因此"零冲击面"不是降低优先级的理由，恰恰是修复窗口期仍在开放的标志——应在冲击面非零前完成修复。

| 优先级 | 条目 | 理由 |
|--------|------|------|
| **高（安全）** | T1 | 校验缺失可能引入安全风险（恶意大分页 OOM），修复需确认 @Valid 链路 |
| **高（功能）** | T6 | 契约行为偏差直接改变 API 消费者得到的响应结构，违反统一处理设计目标 |
| **高（维护）** | T3 | 版本管理方式增加升级风险，修复为纯删除无副作用 |
| **高（维护）** | T4 | scope 错误可能导致构建行为异常，修复为纯删除 scope 无副作用 |
| **中（依赖治理）** | T7 | Unused declared dependency，不影响编译运行但污染依赖分析结果 |
| **中（结构占位）** | T9 | 工程结构占位缺失，无运行时影响 |
| **中（结构占位）** | T2 | 工程结构占位缺失，无运行时影响 |
| **中（日志行为）** | T10 | 功能正确性不受影响，仅日志时机与 OOD 不一致 |
| **中（测试覆盖）** | T11 | 测试覆盖率不足，骨架阶段可接受但需跟踪 |
| **低** | T5 | OOD 文档问题，代码行为正确（已修复 §10.1 可选性标记） |
| — | T8 | 误报，无需处理 |
