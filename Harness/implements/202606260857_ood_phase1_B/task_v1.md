# 任务指令（v1）

## 动作
NEW

## 任务描述
实施包 A 数据建模缺陷修复与字段扩展（P0 阶段第一步），覆盖 OOD 文档 5.1 节与 8.1/8.3 节的 A1/A3/H1 修复点：

1. `permission/User.java`：新增 `passwordChangeRequired`（`Boolean`，`@Column(nullable=false, columnDefinition="BIT(1) DEFAULT 0")`，默认值 `false`）与 `tokenVersion`（`Integer`，`@Column(nullable=false)`，默认值 `0`），连同 getter/setter 与字段 Javadoc。
2. `permission/Role.java`：为 `enabled` 字段补 `@Column(nullable=false)`；新增 `sort` 字段（`Integer`，`@Column(nullable=false)`，默认值 `0`）用于主角色优先级排序，连同 getter/setter。
3. `permission/Post.java`：为 `enabled` 字段补 `@Column(nullable=false)`。
4. `permission/UserRepository.java`：将 `findByUsername(String)` 返回类型改为 `Optional<User>`。
5. 同步修复所有调用方：把 `user == null` 替换为 `Optional.isEmpty()` / `.orElseThrow(...)` 链式调用。**经 grep 复核，实际调用方仅两处**：
   - `AuthServiceImpl.java:60` `login()` 方法中的 `User user = userRepository.findByUsername(...);` 紧跟 `if (user == null)` 分支，参考同文件第 126/170/195 行 `findById(...).orElseThrow(...)` 模式改写。
   - `AuthServiceTest.java` 第 82/102/117/133 行的 Mockito stub：第 102 行 `thenReturn(null)` 改为 `thenReturn(Optional.empty())`，其余三行 `thenReturn(testUser)` 改为 `thenReturn(Optional.of(testUser))`。
   - 已确认 `MenuServiceImpl`（仅用 `findById`）、`MenuServiceTest`、`EntityMappingIT`（用 `entityManager.find`）均不调用 `findByUsername`，**不在本任务修改范围**。

预期文件路径（实际位于 `AIMedical/backend/...`）：
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Role.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Post.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/UserRepository.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java`（调用方适配）
- `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java`（测试侧 mock 适配）

## 选择理由
- 与已完成任务的依赖关系：Phase 1 全部实体/DTO/Repository 改造尚处于起步，本任务是其中最基础的一步。用户实体 `passwordChangeRequired` 与 `tokenVersion` 字段是 Phase 2 `PasswordChangeCheckFilter`、`Refresh Token tokenVersion 比对`、`AuthServiceImpl.changePassword()` 的编译期依赖。`Role.sort` 是 Phase 3 `UserConverter` 按优先级取主角色的数据依赖（替代原 `primaryRole` 字段）。`UserRepository` 返回 Optional 是 Phase 2/3 安全审计与异常处理路径的前置条件。
- 当前优先级：在 OOD 12 节"阶段 1：实体 / DTO / Repository / ErrorCode 对齐（P0）"中，A1/A3/H1 三条 P0 修复点必须在 DTO 重构、Filter 改造之前完成。PermissionFunction 重命名（M8）虽然也是 P0，但其级联影响（5 个文件）适合作为独立任务处理，避免本任务改动面过大。

## 任务上下文
需求/设计/约束摘录（来源：`Docs/05_ood_phase1_B.md`）：
- 5.1 节 User 实体变更表：passwordChangeRequired 新增 `@Column(nullable=false, columnDefinition="BIT(1) DEFAULT 0") private Boolean passwordChangeRequired = false;`；tokenVersion 新增 `@Column(nullable=false) private Integer tokenVersion = 0;`。
- 5.1 节 Role 实体变更表：enabled 补 `@Column(nullable=false)`；sort 新增字段（`@Column(nullable=false) private Integer sort = 0;`）。
- 5.1 节 Post 实体变更表：enabled 补 `@Column(nullable=false)`。
- 5.1 节 UserRepository 变更表：`User findByUsername(String username)` → `Optional<User> findByUsername(String username)`。
- 8.3 节 A1/A3 问题：DDL 与 Java 注解对齐；本任务仅做 Java 注解侧，schema.sql 同步留待后续 DDL 任务统一处理。
- 7.3 节"UserRepository 返回 Optional 对包 B 的影响"：AuthServiceImpl.login() 调用方式从 null 检查改为 Optional.map/orElseThrow。

**类型映射说明**：MySQL 下 `Boolean` 默认映射为 `TINYINT(1)`，与既有 `schema.sql` 中 `enabled TINYINT(1) DEFAULT 1` 一致；显式声明 `columnDefinition="BIT(1) DEFAULT 0"` 是 OOD 5.1 节统一布尔类型的约定。后续 DDL 迁移任务应统一将两种布尔类型归一（本任务不处理）。

## 已有代码上下文
- `User.java` 现有风格：手写 getter/setter、`@Column(nullable=false)`、`enabled = true` 默认值；继承 `BaseEntity`（已含 id / createdAt / updatedAt / deleted 字段）。
- `Role.java`：`enabled = true` 默认值已存在，缺 `@Column(nullable=false)`；无 sort 字段。`Post.java` 已有 `sort` 字段可作为 Role.sort 的格式参考。
- `Post.java`：`enabled = true` 默认值已存在，缺 `@Column(nullable=false)`。
- `UserRepository.java`：当前 `User findByUsername(String username)`，唯一生产调用方为 `AuthServiceImpl.login()` 行 60-65（`User user = userRepository.findByUsername(request.getUsername());` + `if (user == null)` + `log.warn` + `throw new BusinessException(...)`），需改为 `Optional<User>` 链式调用。`AuthServiceImpl.refreshToken()` 行 126、`getCurrentUser()` 行 170、`updateProfile()` 行 195 已使用 `findById(...).orElseThrow(...)` 模式，可作为本任务改写的模板。
- 测试侧 `AuthServiceTest.java` 第 82/102/117/133 行有 4 处 Mockito stub 调用 `when(userRepository.findByUsername(...))`，需同步调整返回类型以匹配新签名。`MenuServiceTest.java` 与 `EntityMappingIT.java` 经 grep 确认不涉及 `findByUsername`，无需修改。
- **集成测试覆盖归属**：本任务不新增 `User` 实体字段（`passwordChangeRequired` / `tokenVersion`）的集成测试用例，相关覆盖由"阶段 4 集成测试任务"统一处理；本任务仅做 `AuthServiceTest` 中 mock 返回类型的最小化调整。

## 修订说明（v1 r1）

| 审查意见 | 修改措施 |
|---------|---------|
| **[一般] 调用方清单错误**：误列 `MenuServiceImpl`、`EntityMappingIT`、`MenuServiceTest` 为 `findByUsername` 调用方 | 已 grep 复核，移除无关条目；调用方清单收缩为 `AuthServiceImpl.java:60`（生产）与 `AuthServiceTest.java:82/102/117/133`（测试）两处，并给出具体改写指令 |
| **[一般] 预期文件路径缺少 `AIMedical/` 前缀** | 所有"预期文件路径"已统一加 `AIMedical/` 前缀，与项目实际布局一致 |
| **[轻微] `passwordChangeRequired` 字段类型映射需澄清** | "任务上下文"新增"类型映射说明"段，点明 `Boolean` + `BIT(1)` 与既有 `TINYINT(1)` 列的差异，并标注后续 DDL 任务统一归一 |
| **[轻微] 集成测试新增覆盖的执行归属未重申** | "已有代码上下文"末尾新增"集成测试覆盖归属"段，明确本任务不新增字段级集成测试，统一由阶段 4 集成测试任务处理 |