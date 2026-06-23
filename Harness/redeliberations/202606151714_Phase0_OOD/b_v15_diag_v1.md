# 质量审查诊断报告 — v15

## 审查结论：存在 3 项需修复的问题（2 严重 / 1 一般）

---

### 问题 1（严重）：`common-module-impl` 中 `LoginUser` 缺少 `spring-boot-starter-security` 编译依赖

**问题描述**：`LoginUser`（3.3 节，归属 `common-module-impl`）实现 `UserDetails` 接口，该接口来自 `spring-boot-starter-security`。但 `spring-boot-starter-security` 在 `common` 模块中被标注为 `<optional>true</optional>`（2.2 节「Common 模块依赖传播决策」），而 `common-module-impl` 仅依赖 `common-module-api`（→ `common`）。根据 Maven 规则，optional 依赖不会被传递解析，因此 `common-module-impl` 的类路径上不存在 `UserDetails` 接口，导致编译失败。

**所在位置**：3.3 节 `LoginUser` 定义（line 528–536）、2.2 节「Common 模块依赖传播决策」security 标注（line 285）、2.2 节需显式声明 security 的模块列表未包含 `common-module-impl`（line 279）

**严重程度**：严重 — 骨架无法编译通过，属于阻断性缺陷

**改进建议**：将 `common-module-impl` 加入需显式声明 `spring-boot-starter-security` 的模块列表（2.2 节「依赖管理」段落），并在 `modules/common-module/common-module-impl/pom.xml` 中补充该依赖（scope 默认 compile，版本由父 POM 统一管理）。

---

### 问题 2（严重）：`@Profile("phase0")` 无激活机制，Phase 0 骨架无法正常启动

**问题描述**：4.5 节 SecurityConfigPhase0 标注 `@Profile("phase0")`，但全文档未定义 `spring.profiles.active=phase0` 的设置位置。

- `application.yml`（9.1 节）未设置 `spring.profiles.active`
- `application-dev.yml`（9.1 节）使用 `dev` profile，不含 `phase0`
- 9.3 节启动命令 `mvn spring-boot:run -pl application -am` 未携带 `--spring.profiles.active`
- 开发者若使用 `spring.profiles.active=dev` 启动，SecurityConfigPhase0 不会激活；若不设置任何 profile，默认 profile 不含 `phase0`，同样不激活

无 `SecurityFilterChain` Bean 时，Spring Boot 默认安全配置会拦截所有请求（包括 `/api/ping` 返回 401），直接破坏「骨架可运行」验收标准（4.1 节：`GET /api/ping` → `200 OK`）。

同时，开发者若仅设置 `spring.profiles.active=phase0`，`application-dev.yml` 不会加载（H2 数据库和 AI Mock 配置均在该文件中），数据源配置缺失导致启动失败。正确方式为 `spring.profiles.active=phase0,dev` 同时激活两个 profile，但该约束在文档中完全缺失。

**所在位置**：4.5 节 SecurityConfig 设计（line 704–724）、9.3 节启动命令（line 1133）、9.1 节配置管理（line 1076–1102）

**严重程度**：严重 — 开发者无法按文档指示启动骨架并跑通健康检查

**改进建议**：二选一：

- **方案 A（推荐）**：在 `application.yml` 的通用配置中设置 `spring.profiles.active: phase0,dev`，使 Phase 0 默认同时激活两个 profile；在 4.5 节末尾和 9.3 节启动命令旁补充注释说明
- **方案 B**：取消 `@Profile("phase0")`，改为无条件注册 permitAll SecurityConfig（Phase 1 的认证配置通过 `@Profile("!phase0")` 或在 Phase 1 时加回），保持 Spring Boot 默认行为不变

---

### 问题 3（一般）：2.2 节依赖方向图未体现业务模块对 `ai-api` 的依赖关系

**问题描述**：2.2 节正文明确说明 `patient`/`doctor`/`admin` 模块依赖 `common`、`common-module-api` 和 `modules/ai/ai-api`，但 ASCII 依赖方向图中仅显示 `ai-api` 与 `common` 和 `ai-impl` 的连线，未显示指向 `patient`/`doctor`/`admin` 的依赖箭头。阅读 ASCII 图的开发者无法直观获知业务模块需引入 `ai-api` 依赖。

**所在位置**：2.2 节依赖方向图（line 250–265）

**严重程度**：一般 — 正文已明确但图未同步，有误导风险

**改进建议**：在 ASCII 图中补充 `ai-api` 指向 `patient`/`doctor`/`admin` 的箭头，或在图下方以注释形式标注「业务模块同时依赖 common-module-api 与 ai-api」。

---

## 备注

- 内部审议关注的维度（AI 模块拆分策略、Bean 装配、FallbackAiService 自引用循环依赖、CI 构建阶段、降级策略框架等）在 v15 中已修复且无残留问题，本报告不再重复验证。
- 以上 3 项问题均已明确到可指导修复的具体位置和改法。
