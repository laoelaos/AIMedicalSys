# R3: OOD Section 11 交付物覆盖 + 跨模块一致性 + 设计决策遵循审查

审查时间：2026-06-17

### 审查范围

- Docs/04_ood_phase0.md（§2.2, §2.3, §7, §8.1, §11）
- AIMedical/backend/pom.xml（父 POM）
- AIMedical/backend/common/pom.xml
- AIMedical/backend/modules/ai/pom.xml, ai-api/pom.xml, ai-impl/pom.xml
- AIMedical/backend/modules/common-module/pom.xml, common-module-api/pom.xml, common-module-impl/pom.xml
- AIMedical/backend/modules/patient/pom.xml
- AIMedical/backend/modules/doctor/pom.xml
- AIMedical/backend/modules/admin/pom.xml
- AIMedical/backend/application/pom.xml
- AIMedical/backend/integration/pom.xml
- AIMedical/backend/common/src/main/java/com/aimedical/common/ (全量)
- AIMedical/backend/modules/ai/ai-api/src/main/java/com/aimedical/modules/ai/api/ (全量)
- AIMedical/backend/modules/ai/ai-impl/src/main/java/com/aimedical/modules/ai/impl/ (全量)
- AIMedical/backend/modules/common-module/common-module-api/src/main/java/ (全量)
- AIMedical/backend/modules/common-module/common-module-impl/src/main/java/ (全量)
- AIMedical/backend/modules/patient/src/main/java/com/aimedical/modules/patient/api/PatientController.java
- AIMedical/backend/modules/doctor/src/main/java/com/aimedical/modules/doctor/api/DoctorController.java
- AIMedical/backend/modules/admin/src/main/java/com/aimedical/modules/admin/api/AdminController.java
- AIMedical/backend/application/src/main/java/com/aimedical/ (全量)
- AIMedical/backend/application/src/main/resources/application.yml, application-dev.yml
- AIMedical/frontend/package.json, packages/shared/package.json, packages/ui-core/package.json, apps/patient/package.json

### 发现

#### [一般] ai-impl POM 声明了冗余的 common 直接依赖

- **位置**：`AIMedical/backend/modules/ai/ai-impl/pom.xml:17-20`
- **描述**：ai-impl POM 同时声明了 `com.aimedical:ai-api` 和 `com.aimedical:common`。根据 OOD §2.2 与 §7 设计决策，ai-impl 仅应直接依赖 ai-api（`ai-impl → ai-api`），而 ai-api 已声明依赖 common（`ai-api → common`）。因此 common 作为传递性依赖已可通过 ai-api 获得，此处显式声明 common 是冗余的，与 §2.2 规定的"ai-impl 不直接依赖 common"不一致。
- **建议**：移除 ai-impl/pom.xml 中 `com.aimedical:common` 的 `<dependency>` 声明。

#### [一般] common-module-impl POM 声明了冗余的 common 直接依赖

- **位置**：`AIMedical/backend/modules/common-module/common-module-impl/pom.xml:17-20`
- **描述**：common-module-impl POM 同时声明了 `com.aimedical:common-module-api` 和 `com.aimedical:common`。根据 OOD §2.2，common-module-impl → common-module-api，而 common-module-api → common。common 作为传递性依赖已可通过 common-module-api 获得，此处直接声明 common 是冗余的。
- **建议**：移除 common-module-impl/pom.xml 中 `com.aimedical:common` 的 `<dependency>` 声明。

#### [一般] common 模块缺少 util 包目录

- **位置**：`AIMedical/backend/common/src/main/java/com/aimedical/common/`
- **描述**：OOD §2.3 包命名规范要求 common 模块包含 `base`、`result`、`exception`、`util`、`config` 五个子包。当前实现了 `base`、`result`、`exception`、`config`，但缺少 `util` 包目录。Phase 0 虽可能暂无通用工具类，但包目录空缺与 OOD 规范描述的完整包结构不一致。
- **建议**：在 `com.aimedical.common` 下创建 `util` 空包目录，作为骨架扩展点预留。

#### [一般] common-module-impl 缺少 config 和 dict 包目录

- **位置**：`AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/`
- **描述**：OOD §2.3 规定 common-module-impl 包含 `permission`、`config`、`dict` 三个子包。当前仅实现了 `permission` 包，缺少 `config` 和 `dict` 包目录。Phase 0 虽可能暂无具体配置类和字典管理类，但包目录空缺与 OOD 规范不一致。
- **建议**：在 `com.aimedical.modules.commonmodule` 下创建 `config` 和 `dict` 空包目录，作为骨架扩展点预留。

### 跨维度检查结果汇总

#### A. OOD Section 11 关联交付物覆盖（仅记录状态，不视为缺陷）

| # | 文件 | 状态 |
|---|------|------|
| 1 | CONTRIBUTING.md 或 Docs/branch-convention.md — 分支约定 | 未找到 |
| 2 | CONTRIBUTING.md 或 Docs/commit-convention.md — Commit 格式 | 未找到 |
| 3 | .github/pull_request_template.md — PR 模板 | 未找到 |
| 4 | CONTRIBUTING.md 或 Docs/cr-checklist.md — CR 必查项 | 未找到 |
| 5 | Docs/QUICKSTART.md — 新人入门引导文档 | 未找到 |

#### B. OOD Section 7 设计决策遵循 — 逐项验证

| 决策 | 验证结果 |
|------|---------|
| 模块化单体 | ✓ Maven 多模块结构正确，无微服务基础设施 |
| Maven 构建 | ✓ 无 Gradle 文件 |
| npm workspaces+Vite | ✓ workspaces: ["packages/*", "apps/*"]，各端使用 Vite |
| AiService 方法集合 | ✓ 统一 AiService 接口含 13 方法 |
| ai-api/ai-impl 拆分 | ✓ 业务模块仅依赖 ai-api，未依赖 ai-impl |
| 三级权限模型 | ✓ Role/Post/Function 实体存在且关系正确 |
| ErrorCode String 类型 | ✓ ErrorCode.code() 返回 String |
| ErrorCode interface+enum | ✓ ErrorCode 为 interface，GlobalErrorCode 为 enum implements |
| @ConditionalOnProperty | ✓ MockAiService 使用 @ConditionalOnProperty |
| npm workspace 内部包 | ✓ @aimedical/shared 和 @aimedical/ui-core 为内部包 |
| FallbackAiService 构造器注入 List<AiService> | ✓ 排除自身（`! (s instanceof FallbackAiService)`） |

#### C. 跨模块依赖方向

| 规则 | 验证结果 |
|------|---------|
| 业务模块 → common + common-module-api + ai-api | ✓ 全部符合 |
| 业务模块无 common-module-impl 依赖 | ✓ |
| 业务模块无 ai-impl 依赖 | ✓ |
| ai-impl → ai-api（不直接依赖 common） | ⚠ 冗余依赖 common（见上述发现） |
| common-module-impl → common-module-api（不直接依赖 common） | ⚠ 冗余依赖 common（见上述发现） |
| application → 所有叶模块 + ai-impl + common-module-impl | ✓ |
| 业务模块之间无互相依赖 | ✓ |

#### D. 包命名规范（§2.3）

共 9 个包路径检查项，7 项完全通过，2 项有例外：
- **common** ✓ `com.aimedical.common.{base,result,exception,config}` — 缺 `util`（见发现）
- **patient** ✓ `com.aimedical.modules.patient.{api,dto,service,repository,entity,converter}`
- **doctor** ✓ `com.aimedical.modules.doctor.{api,dto,service,repository,entity,converter}`
- **admin** ✓ `com.aimedical.modules.admin.{api,dto,service,repository,entity,converter}`
- **ai-api** ✓ `com.aimedical.modules.ai.api.{AiService, dto, degradation}`
- **ai-impl** ✓ `com.aimedical.modules.ai.impl.{mock, fallback, degradation}`
- **common-module-api** ✓ `com.aimedical.modules.commonmodule.api`
- **common-module-impl** ✓ `com.aimedical.modules.commonmodule.permission` — 缺 `config`、`dict`（见发现）
- **Application** ✓ `com.aimedical`

#### E. API 路径约定（§8.1）

| 规则 | 验证结果 |
|------|---------|
| patient Controller → /api/patient | ✓ |
| doctor Controller → /api/doctor | ✓ |
| admin Controller → /api/admin | ✓ |
| /api/ping 在 application 模块 HealthController | ✓ |
| 无版本号段 | ✓ |

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 4 |
| 轻微 | 0 |

### 总评

OOD §7 设计决策 11 项全部遵循，实现与设计一致。跨模块依赖方向基本正确，仅 ai-impl 和 common-module-impl 存在冗余的 common 直接依赖（建议移除）。包命名规范基本遵循 OOD §2.3，仅 `common/util`、`common-module-impl/config`、`common-module-impl/dict` 三个空包目录未创建。API 路径约定完全符合。OOD §11 关联交付物 5 项均未就绪，属项目级协作规范而非代码缺陷。
