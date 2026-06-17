# R3-A2: Phase 0 边界约束 + 代码质量 + 测试覆盖率 + 遗漏检查

审查时间：2026-06-17

### 审查范围

- `AIMedical/backend/application/src/main/resources/application-dev.yml`
- `AIMedical/backend/application/src/main/resources/application-prod.yml`
- `AIMedical/backend/application/src/main/resources/application.yml`
- `AIMedical/backend/application/src/main/java/com/aimedical/config/SecurityConfigPhase0.java`
- `AIMedical/backend/pom.xml`
- `AIMedical/backend/modules/patient/pom.xml`
- `AIMedical/backend/modules/doctor/pom.xml`
- `AIMedical/backend/modules/admin/pom.xml`
- `AIMedical/backend/common/src/main/java/com/aimedical/result/Result.java`
- `AIMedical/frontend/packages/shared/vitest.config.ts`
- `AIMedical/frontend/packages/shared/src/api/__tests__/interceptors.test.ts`
- `AIMedical/frontend/packages/shared/src/types/__tests__/types.test.ts`
- 后端各模块测试类存在性检查
- 后端各模块包名一致性检查

### 发现

#### [一般] common-module-impl 缺少 config 目录及 .gitkeep

- **位置**：`AIMedical/backend/modules/common-module/common-module-impl/`
- **描述**：OOD §2.3 包命名规范中定义了 `com.aimedical.modules.commonmodule.config` 目录用于存储业务级配置，但该目录在文件系统中不存在。目录下本应放置 `.gitkeep` 文件作为占位。当前仅有 `dict/` 和 `permission/` 两个子包。
- **建议**：创建 `common-module-impl/src/main/java/com/aimedical/modules/commonmodule/config/.gitkeep`（或 `.gitkeep` 中的内容参见 OOD）。

#### [一般] common 模块缺少 MeterRegistryCustomizer 占位配置

- **位置**：`AIMedical/backend/common/src/main/java/com/aimedical/common/config/`
- **描述**：OOD §10.1 要求在 common 模块的 `com.aimedical.common.config` 包中声明 `MeterRegistryCustomizer` 配置类占位，设置通用标签（如 `application=aimedical-sys`）。该文件不存在。当前 `config/` 目录下仅有 `GlobalExceptionHandler.java`、`JacksonConfig.java`、`JpaConfig.java`。
- **建议**：按 §10.1 创建 `MeterRegistryCustomizer` 占位配置类，设置 `application=aimedical-sys` 通用标签。

#### [一般] common 模块缺少 util 目录

- **位置**：`AIMedical/backend/common/src/main/java/com/aimedical/common/`
- **描述**：OOD §2.3 包命名规范中定义了 `com.aimedical.common.util` 工具包，但该目录在文件系统中不存在。当前仅包含 `base/`、`config/`、`exception/`、`result/` 四个子包。
- **建议**：如果需要，在 Phase 1+ 创建 `util/` 目录。Phase 0 可以保持现有状态（util 非硬性验收项），但与 OOD 文档不一致。

#### [轻微] CRLF/LF 换行符不一致

- **位置**：全仓库共 1498 个 LF 文件、275 个 CRLF 文件
- **描述**：仓库中存在混合换行符。大部分文件（约 84%）使用 LF，部分文件（约 16%，主要是项目配置文件）使用 CRLF（Windows 默认）。混合换行符在跨平台协作时可能导致 `git diff` 噪声。
- **建议**：在仓库根目录添加 `.gitattributes` 文件，统一约定 `*.java`、`*.xml`、`*.yml` 等文件使用 `LF`（`text eol=lf`）。

#### [轻微] 父 POM 中 spring-boot-starter-web / data-jpa 显式指定版本号

- **位置**：`AIMedical/backend/pom.xml:87-88`, `AIMedical/backend/pom.xml:91-92`
- **描述**：父 POM 的 `<dependencyManagement>` 中对 `spring-boot-starter-web` 和 `spring-boot-starter-data-jpa` 显式指定了 `<version>3.2.5</version>`。这两个依赖的版本已由 `spring-boot-starter-parent` BOM 管理，显式指定版本在功能上正确但冗余，且可能在未来升级时产生版本漂移风险（需手动同步两个地方）。
- **建议**：移除这两个依赖的 `<version>` 元素，依赖 Spring Boot BOM 自动管理版本。

#### [轻微] application-prod.yml 缺少 spring.jpa.hibernate.ddl-auto 生产环境设置

- **位置**：`AIMedical/backend/application/src/main/resources/application-prod.yml`
- **描述**：application-prod.yml 配置了 springdoc、management、h2.console 的生产安全关闭策略，但未配置 `spring.jpa.hibernate.ddl-auto`。Phase 0 使用 H2 内存数据库时 `ddl-auto: update` 在 `dev` profile 下执行，但 prod profile 未覆盖此设置，存在生产环境意外使用 `ddl-auto: update` 的风险。
- **建议**：在 `application-prod.yml` 中添加 `spring.jpa.hibernate.ddl-auto: validate`（或 `none`），确保生产环境不会自动修改数据库 schema。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 3 |
| 轻微 | 3 |

### 总评

Phase 0 边界约束全部严格遵循——`ai.mock.enabled=true` 正确配置、业务模块未注入 AiService、未引用跨模块门面、SecurityConfig 无 Phase 1+ 组件、前端无 AuthStore/Pinia 实现。测试覆盖率完整——所有 10 个模块均包含要求的占位测试类且结构良好。前端测试内容有效、覆盖拦截器与类型正确性。package 命名一致（均为 `com.aimedical`）。POM 模块声明完整。**主要缺失**为 common-module-impl 缺少 config 目录及 .gitkeep、common 模块缺少 MeterRegistryCustomizer 占位、common 模块缺少 util 目录（文档一致性）。换行符不一致和 POM 版本冗余属于轻微问题，建议在 Phase 0 验收前统一处理。
