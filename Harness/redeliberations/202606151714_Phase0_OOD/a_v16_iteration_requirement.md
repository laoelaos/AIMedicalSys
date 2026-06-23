根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 问题 1（严重）：`common-module-impl` 中 `LoginUser` 缺少 `spring-boot-starter-security` 编译依赖

`LoginUser`（3.3 节，归属 `common-module-impl`）实现 `UserDetails` 接口，该接口来自 `spring-boot-starter-security`。但 `spring-boot-starter-security` 在 `common` 模块中被标注为 `<optional>true</optional>`（2.2 节「Common 模块依赖传播决策」），而 `common-module-impl` 仅依赖 `common-module-api`（→ `common`）。根据 Maven 规则，optional 依赖不会被传递解析，因此 `common-module-impl` 的类路径上不存在 `UserDetails` 接口，导致编译失败。

**改进建议**：将 `common-module-impl` 加入需显式声明 `spring-boot-starter-security` 的模块列表（2.2 节「依赖管理」段落），并在 `modules/common-module/common-module-impl/pom.xml` 中补充该依赖（scope 默认 compile，版本由父 POM 统一管理）。

### 问题 2（严重）：`@Profile("phase0")` 无激活机制，Phase 0 骨架无法正常启动

4.5 节 SecurityConfigPhase0 标注 `@Profile("phase0")`，但全文档未定义 `spring.profiles.active=phase0` 的设置位置：`application.yml`（9.1 节）未设置 `spring.profiles.active`；`application-dev.yml`（9.1 节）使用 `dev` profile；9.3 节启动命令未携带 `--spring.profiles.active`。单独激活 `phase0` 会导致 `application-dev.yml` 不加载，正确方式为 `spring.profiles.active=phase0,dev` 同时激活两个 profile，但该约束在文档中完全缺失。

**改进建议**：二选一。方案 A（推荐）：在 `application.yml` 的通用配置中设置 `spring.profiles.active: phase0,dev`；在 4.5 节末尾和 9.3 节启动命令旁补充注释说明。方案 B：取消 `@Profile("phase0")`，改为无条件注册 permitAll SecurityConfig。

### 问题 3（一般）：权限模型实体 JPA 关系映射缺失注解细节，并行开发将产生不兼容实体

3.3 节定义了 User、Role、Post、Function 四个权限实体及其概念关系，但未给出 JPA 注解细节：`User ↔ Role`（多对多）未指定 `@JoinTable`、fetch 策略、cascade 策略；`User ↔ Post`（多对多）同上；`Role ↔ Post`（一对多）未指定 `mappedBy`、cascade、`orphanRemoval`；`Post ↔ Function`（多对多）未指定 `@JoinTable`。多名开发者并行实现时，默认 JPA 命名策略产生的关联表名和列名大概率不一致，且 fetch/cascade 策略差异将导致运行时问题。

**改进建议**：在 3.3 节为每个实体补充 JPA 关系注解的伪代码骨架，至少明确：多对多关联的 `@JoinTable` 命名约定（推荐 `{entity1}_{entity2}`，如 `user_role`）；统一采用 `FetchType.LAZY`；cascade 策略推荐 Entity 端不设 cascade，由 Service 层统一管理；Role ↔ Post 的 `mappedBy` 归属推荐 Post 端为 owning side。

### 问题 4（轻微）：ASCII 依赖方向图未体现业务模块对 `ai-api` 的依赖关系

2.2 节正文明确说明 patient/doctor/admin 模块依赖 common、common-module-api 和 modules/ai/ai-api，但 ASCII 依赖方向图中仅显示 ai-api 与 common 和 ai-impl 的连线，未显示指向 patient/doctor/admin 的依赖箭头。

**改进建议**：在 ASCII 图中补充 ai-api 指向 patient/doctor/admin 的箭头，或在图下方以注释形式标注「业务模块同时依赖 common-module-api 与 ai-api」。

## 历史迭代回顾

- **已解决的问题**：AI 方法命名与中文括号（第1轮）、User 实体模块归属明确化（第1轮）、BaseEntity 字段级定义补充（第1/4轮）、MockAiService Bean 装配策略（第1/2/3/4/7/10/13轮）、SecurityConfig 骨架定义（第1/4轮）、User-UserDetails Adapter 模式（第1轮）、ai-api/ai-impl 子模块拆分（第1/10轮）、ai.mock.enabled 默认激活（第2轮）、AI DTO 类型定义（第2轮）、配置加载失败分类修正（第2轮）、ui-core 包定义补充（第2轮）、CI 流水线模块构建顺序（第2/3/11/12轮）、FallbackAiService 循环依赖与 @Primary 自引用（第3/4/7/13轮）、DegradationStrategy 签名与 DegradationContext（第3/12轮）、嵌套 DTO 字段定义（第3轮）、@EnableJpaAuditing 配置（第4轮）、ErrorCode enum→interface（第4轮）、BusinessException 继承层次（第5轮）、PageRequest→PageQuery 重命名（第5轮）、H2 数据库策略（第6轮）、Vite 代理配置（第6轮）、spring-boot-starter-web/test/validation 依赖声明（第8轮）、前端 workspace 与内部包引用（第8轮）、PermissionService 返回 DTO 而非实体（第9轮）、UserRegisteredEvent 普通 POJO（第9轮）、PageQuery @Max 约束（第9轮）、ApiClient 错误处理（第9轮）、Integration 模块 repackage classifier（第11轮）、依赖方向图箭头指向 common-module-api（第14轮）、父 POM dependencyManagement 添加 security（第14轮）。

- **持续存在的问题**：安全依赖管理相关的问题在多轮迭代中反复出现——common 模块将 `spring-boot-starter-security` 设为 `<optional>true</optional>` 后，依赖 common 或 common-module-api 的子模块如果使用到 Security 相关类型（如 UserDetails、SecurityFilterChain 等），必须显式声明该依赖。上一轮修复了父 POM 的 dependencyManagement 条目和 application 等模块的声明，但本轮发现 `common-module-impl` 同样需要使用 `UserDetails`（通过 LoginUser），却未被列入显式声明列表。该问题的本质是：任何使用 Security 类型的模块均需在自身 POM 中显式声明 security 依赖，但设计文档缺乏系统性的检查清单来确保所有受影响模块都被覆盖。

- **新发现的问题**：问题 1（common-module-impl 缺少 security 编译依赖）是持续安全问题的新实例；问题 2（@Profile("phase0") 无激活机制）为本轮首次发现；问题 3（权限实体 JPA 关系映射缺失注解细节）为本轮首次发现；问题 4（ASCII 图缺少 ai-api 到业务模块的连线）为本轮首次发现。

## 上一轮产出路径
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\a_v15_design_v1.md

## 用户需求
C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606151714_Phase0_OOD\requirement.md
