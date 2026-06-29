# 设计审查报告（v1 r1）

## 审查结果
APPROVED

## 审查背景

独立审查员视角对 `detail_v1.md` 进行审查，已交叉验证：
- `AIMedical/backend/.../permission/User.java`、`Role.java`、`Post.java`、`UserRepository.java` 实体/Repository 现状
- `AIMedical/backend/.../service/impl/AuthServiceImpl.java` 当前实现（60-65、126-127、170-171、195-196 行）
- `AIMedical/backend/.../service/AuthServiceTest.java` 测试用例（行 26 已 `import java.util.Optional`）
- `AIMedical/backend/common/.../base/BaseEntity.java`（无冲突）
- `Docs/05_ood_phase1_B.md` 第 5.1 节实体变更表、528-531 行字段规范
- `findByUsername` 全工程调用方清单（已 grep 复核，仅 `AuthServiceImpl.java:60` 生产侧 + `AuthServiceTest.java` 4 处测试侧）

## 发现

### 字段签名与 OOD 5.1 节一致性

- **`User.passwordChangeRequired`**：设计给出 `@Column(nullable = false, columnDefinition = "BIT(1) DEFAULT 0") private Boolean passwordChangeRequired = false;`，与 OOD 5.1 节 line 528 / 542 完全一致（含 `columnDefinition` 与 `= false` 默认值）。✅
- **`User.tokenVersion`**：设计给出 `@Column(nullable = false) private Integer tokenVersion = 0;`，与 OOD 5.1 节 line 529 完全一致。✅
- **`Role.enabled`**：设计为既有 `private Boolean enabled = true;` 补 `@Column(nullable = false)`，与 OOD 5.1 节 line 530 / 539 一致；既有字段值 `= true` 与既有 Role.java:24 一致，无副作用。✅
- **`Role.sort`**：设计给出 `@Column(nullable = false) private Integer sort = 0;` + getter/setter/Javadoc，与 OOD 5.1 节 line 531 / 540 完全一致。✅
- **`Post.enabled`**：设计为既有 `private Boolean enabled = true;` 补 `@Column(nullable = false)`，与 OOD 5.1 节 line 532 / 541 一致；既有字段值 `= true` 与既有 Post.java:27 一致。✅

### 调用方识别完整性

- 设计明确将 `findByUsername` 真实调用方收缩为 `AuthServiceImpl.java:60`（生产）与 `AuthServiceTest.java:82/102/117/133`（测试），与实际 grep 结果一致。✅
- 已在「不在范围」明确剔除 `MenuServiceImpl`、`MenuServiceTest`、`EntityMappingIT`（均不调用 `findByUsername`），无遗漏。✅
- 行号 82/102/117/133 已逐一比对 `AuthServiceTest.java` 当前内容，全部命中。✅

### 行号与代码片段准确性

- 设计给出 `AuthServiceImpl.java` 仅替换 60-65 行的代码片段，使用 `Optional` 链式 `.orElseThrow(() -> { log.warn(...); return new BusinessException(...); })`，与既有 lines 126-127、170-171、195-196 的 `findById(...).orElseThrow(...)` 模式风格一致。✅
- 设计给出 `AuthServiceTest.java` 4 处 Mockito stub 改写映射，line 102 改为 `Optional.empty()`，其余 3 处改为 `Optional.of(testUser)`，与 `AuthServiceTest.java:26` 已有的 `import java.util.Optional` 一致（无需新增 import）。✅

### 行为契约与向后兼容

- `AuthServiceImpl.login()` 在 `Optional.empty()` 时抛出同款 `BusinessException(GlobalErrorCode.UNAUTHORIZED, "用户名或密码错误")`，与原 `if (user == null)` 分支同款错误码/消息，未引入新的安全信息泄露路径。✅
- `AuthServiceTest#shouldThrowExceptionWhenUserNotFound`（行 99-112）断言 `GlobalErrorCode.UNAUTHORIZED`，与生产侧抛出错误码一致，无需修改断言。✅
- `AuthServiceTest#shouldThrowExceptionWhenUserDisabled`（行 129-143）依赖 `testUser.setEnabled(false)`（line 132）后 `Optional.of(testUser)` 包装并由 login() 中 `!Boolean.TRUE.equals(user.getEnabled())` 触发 FORBIDDEN 异常，断言 `GlobalErrorCode.FORBIDDEN`（line 142），逻辑链完整无误。✅

### 范围划分合理性

- `schema.sql` 变更（含 `sys_user.passwordChangeRequired`、`sys_user.token_version`、`sys_role.sort` 列 DDL）已明确划入后续 DDL 任务，本任务仅做 Java 注解侧。✅
- `PermissionFunction` 重命名（M8，5 个文件级联）已明确独立任务化，避免本任务改动面过大。✅
- `Post.sort` 字段补 `@Column(nullable=false)` 已明确不在 OOD 5.1 节 Post 变更表内，本任务不处理。✅
- 字段级集成测试用例归属（阶段 4 集成测试任务）已明确，本任务不新增覆盖。✅

### 字段/方法放置位置

- **[轻微]** 设计未显式指定 `User.passwordChangeRequired` 与 `User.tokenVersion` 字段及其 getter/setter 在 `User.java` 中的具体插入位置。建议在编码阶段参考既有风格（认证/状态相关字段相邻），例如将两字段置于 `enabled` 字段之后、`userType` 之前；getter/setter 按现有方法顺序追加在 `setEnabled` 之后。该问题纯属风格问题，不影响功能正确性，不构成驳回依据。

### `@EntityGraph` 注解归属

- **[轻微]** OOD 文档 line 925 注释提及 `findByUsername` 应配 `@EntityGraph(attributePaths = {"roles", "posts"})` 显式 fetch join，属于 8.1 节 M9 任务。task_v1.md line 4 / detail_v1.md line 21 均将 `findByUsername` 签名变更（`User` → `Optional<User>`）作为唯一 Repository 改动，未引入 `@EntityGraph`。当前设计已将 `@EntityGraph` 排除在范围外，归属正确（属于独立 M9 任务）。该问题不构成驳回依据。

### 依赖与编译期影响

- `User`/`Role`/`Post` 均继承 `BaseEntity`（已 grep 验证），新字段与 BaseEntity 现有 `id`/`createdAt`/`updatedAt`/`deleted` 无冲突。✅
- `User.tokenVersion`、`User.passwordChangeRequired`、`Role.sort` 为新增字段，不修改既有字段语义，对既有调用方零影响（Phase 2/3 才会消费）。✅
- 暴露给后续任务的公开接口（getter/setter）已逐项列出（`PasswordChangeCheckFilter`、`AuthServiceImpl.changePassword`、`JwtTokenProvider`、`UserConverter` 等），与 OOD 3.2/3.3/7.4 节交叉引用一致。✅

## 结论

设计完整覆盖 task_v1.md 五项任务要求，字段签名与 OOD 5.1 节实体变更表逐字一致；调用方清单已 grep 复核且行号准确；行为契约保留外部兼容性；范围划分（DDL、M8、Post.sort、集成测试）合理；仅余两处风格性轻微观察（字段放置位置、`@EntityGraph` 归属），均非正确性问题。无严重或一般缺陷。

APPROVED