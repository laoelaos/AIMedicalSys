# 质量审查报告 — v14（第 14 轮）

## 总体评价

产出经过 13 轮迭代审议后已高度完备，整体覆盖了需求文档的全部 7 个设计维度，核心抽象定义清晰，接口契约框架完整，AI 模块的接口/降级/占位体系设计已是成熟态。以下报告侧重内部审议未充分覆盖的需求响应充分度、深度完整性和实践落地视角。

---

## 发现的问题

### 问题 1（严重）：Section 2.2 依赖方向图中的箭头方向与正文矛盾

**所在位置**：Section 2.2，依赖方向图（第 245–261 行），具体为 patient/doctor/admin 模块指向 common-module-impl 的箭头

**问题描述**：
图中 patient/doctor/admin 三组模块的箭头向上指向 `common-module-impl`，按该图箭头约定（Dependent 指向 Dependency），这表示业务模块依赖 common-module-impl。但正交明确声明：
- 第 266 行：`common-module-impl`"仅由 application 模块引入，业务模块不可见"
- 第 267 行：业务模块"依赖 common、common-module-api 和 modules/ai/ai-api"（注意是 `common-module-api`，不是 `-impl`）

这个箭头方向矛盾会让按图编码的开发者误将 `common-module-impl` 加入 patient/doctor/admin 的 POM 依赖，直接打破「编译期强制隔离」的核心架构原则。

**严重程度**：严重

**改进建议**：将图中 patient/doctor/admin 向上的箭头指向 `common-module-api`（而非 `common-module-impl`），或在图中标注 `common-module-impl` 被 **application 独占引入** 的注释。需与下方 ai-impl 的处理方式（箭头指向 application）对齐——即 common-module-impl 也应只有 application 指向它。

---

### 问题 2（一般）：父 POM dependencyManagement 缺少 spring-boot-starter-security 条目

**所在位置**：Section 2.1 父 POM 骨架（第 146–191 行）与 Section 2.2 依赖传播决策（第 279 行）

**问题描述**：
Section 2.2 将 spring-boot-starter-security 标记为 `<optional>true</optional>`，并要求需要 security 的模块"在自己的 POM 中显式声明该依赖（版本由父 POM 统一管理）"。但 Section 2.1 的父 POM dependencyManagement 示例仅列出了内部模块和 `spring-boot-starter-validation`、`spring-boot-starter-test`，**未列出 `spring-boot-starter-security`**。

虽然 spring-boot-starter-parent BOM 间接管理了该版本，但"版本由父 POM 统一管理"的措辞暗示 developer 应当能在 dependencyManagement 中找到该条目。缺少该条目时，开发者面临两个选择：(a) 依赖 BOM 的隐式版本管理，或 (b) 自行硬编码版本号。这与父 POM 统一管理的意图不一致。

**严重程度**：一般

**改进建议**：在父 POM 的 dependencyManagement 中添加 spring-boot-starter-security 条目（无需版本号，由 spring-boot-starter-parent BOM 提供），与 validation、test 的处理方式一致，保持 POM 示例的完整性。

---

### 问题 3（一般）：前端 build:all 脚本与 packages 构建定义的不兼容风险

**所在位置**：Section 2.4 packages 导出配置（第 352–380 行）与 Section 10 CI 第五阶段（第 1152 行）

**问题描述**：
CI 第五阶段定义 `npm run build:all`，根 package.json 中对应脚本为 `"build:all": "npm run build --workspaces"`。但 `packages/shared` 和 `packages/ui-core` 的 `main` 直接指向 `src/index.ts`（即源码直接引用），文件中未定义 `build` 脚本。

`npm run build --workspaces` 的行为：对于不含 `build` 脚本的 workspace，npm 会**报错退出**（非跳过）。这导致 CI 第五阶段必然失败。如果意图是让 shared 和 ui-core 不需要构建步骤，则应改用 `--if-present` 标志（`npm run build --workspaces --if-present`）或显式限定构建范围仅为 `apps/*`。

此外，`npm run build --workspaces` 在每个 workspace 的目录下独立执行 `build`，对于三端应用逐个构建是合理的，但共享包的类型导出需要在构建前就绪。如果 shared 包需要 TypeScript 编译才能被 apps 消费，则共享包必须定义 build 脚本。

**严重程度**：一般

**改进建议**：
1. 在 Section 2.4 明确 `packages/shared` 和 `packages/ui-core` 是否需要构建步骤：
   - **不需要构建**（源码直接引用）：根 package.json 的 `build:all` 脚本改为 `"npm run build --workspaces --if-present"` 或 `"npm run build --workspace @aimedical/app-patient --workspace @aimedical/app-doctor --workspace @aimedical/app-admin"`
   - **需要构建**：在 shared 和 ui-core 的 package.json 中补充 build 脚本（如 `"build": "tsc"`）
2. 在 Section 10 的 CI 说明中同步更新 build:all 的实际行为描述

---

### 问题 4（一般）：Section 3.4 DegradationStrategy 泛型与 AiService 具体方法的类型对齐关系未定义

**所在位置**：Section 3.4 DegradationStrategy 定义（第 600 行）

**问题描述**：
`DegradationStrategy` 定义为 `<T, R> R fallback(T input)`，其中 T 为输入类型，R 为降级返回值类型。但 FallbackAiService 的 13 个方法各有独立的输入/输出类型（如 `AiResult<TriageResponse> triage(TriageRequest request)`、`AiResult<ScheduleResponse> schedule(ScheduleRequest request)` 等）。

文档未说明 FallbackAiService 内部如何将 DegradationStrategy 的泛型参数与具体方法的签名对齐。关键未覆盖的场景：
- 如果 DegradationStrategy 是跨 AI 能力**共享的单一实例**，其 `<T, R>` 无法同时适配所有方法的签名（一个实例只能绑定一组具体的 T/R）
- 如果每个方法持有**独立的策略实例**，则 FallbackAiService 需要 13 个独立的 DegradationStrategy 字段，构造器注入 `List<DegradationStrategy>` 无法区分策略归属
- 如果使用泛型擦除 + 运行时强制转换，则失去了静态类型安全的设计初衷

这是一个**设计中留空的可实现性细节**——Phase 0 的 NoOpDegradationStrategy 返回 null 所以未触发问题，但 Phase 2+ 实现者若按当前接口文档编码将遇到类型系统的结构性障碍。

**严重程度**：一般

**改进建议**：选择以下方向之一并在文档中显式说明：
- **方向 A**：取消 DegradationStrategy 的泛型 fallback 方法，仅保留 `shouldDegrade()` 判定逻辑；降级结果由 FallbackAiService 的每个方法自行构造 `AiResult(success=false, degraded=true, data=null)`，不依赖策略提供 fallback 值
- **方向 B**：将 DegradationStrategy 的 fallback 返回类型简化为 `AiResult<?>`（通配符），各方法在调用后做类型安全的转换
- **方向 C**：明确 FallbackAiService 内部为每个 AI 方法维护独立的 `DegradationStrategy<RequestT, AiResult<ResponseT>>` 实例，并在装配策略中说明按方法名注入的方式

建议方向 A——降级策略关注"是否降级"而非"降级返回什么"，fallback 值的构造责任应归属 FallbackAiService 自身。

---

### 问题 5（轻微）：SecurityConfig Phase 0→Phase 1 切换依赖手动改代码，与 1.1 节「可演进」目标存在差距

**所在位置**：Section 1.1 设计目标（第 12 行："后续阶段无需重构骨架"）与 Section 4.5 SecurityConfig 骨架（第 698–711 行）

**问题描述**：
1.1 节设计目标要求"后续阶段无需重构骨架"。但 4.5 节的 Phase 0→Phase 1 认证策略切换依赖开发者**手动修改 SecurityConfig 代码**：取消注释 `.anyRequest().authenticated()` 并注释 `.anyRequest().permitAll()`。虽然认证组件（AuthenticationEntryPoint、AccessDeniedHandler、PasswordEncoder）已预留，但代码修改本身构成了对骨架的侵入。

Phase 1 进入时的典型操作是"修改文件 → 取消注释 → 重启"，虽然改动量小，但严格来说这已超出"无需重构"的范畴。建议通过 Profile 或配置属性实现零代码切换。

**严重程度**：轻微

**改进建议**：在 SecurityConfig 中添加 `@Profile("phase0")` 条件化配置，使 Phase 0 的 permitAll 配置仅在该 profile 激活时生效；另创建一个带 `@Profile("!phase0")` 或 `phase1` 的 SecurityConfig Bean 负责 Phase 1+ 的认证策略。或采用配置属性方式：
```java
@Value("${app.security.mock-mode:true}")
private boolean mockMode;

http.authorizeHttpRequests(auth -> {
    if (mockMode) {
        auth.anyRequest().permitAll();
    } else {
        auth.requestMatchers("/api/ping").permitAll()
            .anyRequest().authenticated();
    }
});
```
通过修改 `application-dev.yml` 的配置项即可完成切换，无需触碰代码。

---

## 总结

- **事实错误**：1 项（依赖方向图箭头矛盾，严重）
- **关键遗漏**：0 项（已在前序 13 轮中全部覆盖）
- **完整性缺口**：1 项（前端 build:all 脚本兼容性问题，一般）
- **类型安全隐患**：1 项（DegradationStrategy 泛型对齐，一般）
- **可演进差距**：1 项（SecurityConfig 手动切换，轻微）
