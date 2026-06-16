根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1（严重）：Section 2.2 依赖方向图中的箭头方向与正文矛盾

图中 patient/doctor/admin 三组模块的箭头向上指向 `common-module-impl`，按该图箭头约定（Dependent 指向 Dependency），表示业务模块依赖 common-module-impl。但正交明确声明：
- common-module-impl "仅由 application 模块引入，业务模块不可见"
- 业务模块 "依赖 common、common-module-api 和 modules/ai/ai-api"

箭头方向矛盾会让按图编码的开发者误将 common-module-impl 加入业务模块的 POM 依赖，直接打破「编译期强制隔离」的核心架构原则。

**改进建议**：将图中 patient/doctor/admin 向上的箭头指向 `common-module-api`（而非 `common-module-impl`），或在图中标注 `common-module-impl` 被 application 独占引入的注释。

### 问题 2（一般）：父 POM dependencyManagement 缺少 spring-boot-starter-security 条目

Section 2.2 将 spring-boot-starter-security 标记为 `<optional>true</optional>`，并要求需要 security 的模块"在自己的 POM 中显式声明该依赖（版本由父 POM 统一管理）"。但 Section 2.1 的父 POM dependencyManagement 示例仅列出了内部模块和 spring-boot-starter-validation、spring-boot-starter-test，未列出 spring-boot-starter-security。

虽然 spring-boot-starter-parent BOM 间接管理了该版本，但"版本由父 POM 统一管理"的措辞暗示 developer 应当能在 dependencyManagement 中找到该条目。

**改进建议**：在父 POM 的 dependencyManagement 中添加 spring-boot-starter-security 条目（无需版本号，由 spring-boot-starter-parent BOM 提供），与 validation、test 的处理方式一致。

### 问题 3（一般）：前端 build:all 脚本与 packages 构建定义的不兼容风险

CI 第五阶段定义 `npm run build:all`，根 package.json 中对应脚本为 `"build:all": "npm run build --workspaces"`。但 `packages/shared` 和 `packages/ui-core` 的 `main` 直接指向 `src/index.ts`（源码直接引用），文件中未定义 `build` 脚本。

`npm run build --workspaces` 的行为：对于不含 `build` 脚本的 workspace，npm 会报错退出（非跳过），导致 CI 第五阶段必然失败。

**改进建议**：
1. 明确 `packages/shared` 和 `packages/ui-core` 是否需要构建步骤：
   - 不需要构建（源码直接引用）：根 package.json 的 `build:all` 脚本改为 `"npm run build --workspaces --if-present"` 或显式限定构建范围仅为 `apps/*`
   - 需要构建：在 shared 和 ui-core 的 package.json 中补充 build 脚本
2. 在 Section 10 的 CI 说明中同步更新 build:all 的实际行为描述

### 问题 4（一般）：Section 3.4 DegradationStrategy 泛型与 AiService 具体方法的类型对齐关系未定义

`DegradationStrategy` 定义为 `<T, R> R fallback(T input)`，但 FallbackAiService 的 13 个方法各有独立的输入/输出类型。文档未说明 FallbackAiService 内部如何将 DegradationStrategy 的泛型参数与具体方法的签名对齐。

关键未覆盖的场景：
- 如果 DegradationStrategy 是跨 AI 能力共享的单一实例，其 `<T, R>` 无法同时适配所有方法的签名
- 如果每个方法持有独立的策略实例，则构造器注入 `List<DegradationStrategy>` 无法区分策略归属

**改进建议**：选择以下方向之一并在文档中显式说明：
- 方向 A（推荐）：取消 DegradationStrategy 的泛型 fallback 方法，仅保留 `shouldDegrade()` 判定逻辑；降级结果由 FallbackAiService 的每个方法自行构造 `AiResult(success=false, degraded=true, data=null)`
- 方向 B：将 DegradationStrategy 的 fallback 返回类型简化为 `AiResult<?>`（通配符）
- 方向 C：明确 FallbackAiService 内部为每个 AI 方法维护独立的 `DegradationStrategy<RequestT, AiResult<ResponseT>>` 实例

### 问题 5（轻微）：SecurityConfig Phase 0→Phase 1 切换依赖手动改代码，与 1.1 节「可演进」目标存在差距

4.5 节的 Phase 0→Phase 1 认证策略切换依赖开发者手动修改 SecurityConfig 代码：取消注释 `.anyRequest().authenticated()` 并注释 `.anyRequest().permitAll()`。虽然改动量小，但严格来说已超出"无需重构"的范畴。

**改进建议**：在 SecurityConfig 中添加 `@Profile("phase0")` 条件化配置，或采用配置属性方式通过修改 `application-dev.yml` 的配置项即可完成切换，无需触碰代码。

## 历史迭代回顾

### 已解决的问题（出现在历史反馈但当前反馈中不再提及的问题）
- 第 1-13 轮中所有除本报告 5 个问题之外的其他问题均已在前序迭代中逐一解决，包括但不限于：AI 方法名中文括号、FallbackAiService 自引用 StackOverflow、common 模块依赖策略、ErrorCode 从 enum 改为 interface、分页规范 page→PageQuery 重命名、CI 流水线 Maven 命令规范、BaseEntity 字段定义补充、装配条件汇总表完善、User→UserDTO 跨模块共享等。

### 持续存在的问题（在多轮反馈中反复出现，需重点解决）
- **问题 1（箭头方向矛盾）**：第 14 轮即已报告，本轮仍未修复。箭头指向 common-module-impl 与正文"仅 application 引入"的直接矛盾持续存在。
- **问题 2（父 POM 缺少 security-starter）**：第 14 轮即已报告，本轮仍未修复。dependencyManagement 示例中 security-starter 条目持续缺失。
- **问题 3（前端 build:all 兼容性）**：第 14 轮即已报告，本轮仍未修复。shared/ui-core 无 build 脚本与 npm run build --workspaces 冲突持续存在。
- **问题 4（DegradationStrategy 泛型对齐）**：第 14 轮即已报告，本轮仍未修复。泛型参数与具体方法签名对齐关系持续未定义。

### 新发现的问题（本轮新识别的问题）
- **问题 5（SecurityConfig Phase 0→Phase 1 手动切换）**：SecurityConfig 通过注释/取消注释代码进行阶段切换，与 1.1 节「无需重构骨架」的设计目标存在差距。建议通过 @Profile 或配置属性实现零代码切换。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\a_v14_copy_from_v13.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\requirement.md
