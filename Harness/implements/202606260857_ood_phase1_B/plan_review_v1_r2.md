# 计划审查报告（v1 r2）

## 审查结果
APPROVED

## 审查依据

### 实际代码核验

| 核验项 | 计划声明 | 实际代码 | 一致性 |
|--------|---------|---------|--------|
| `User.java` 现有字段风格 | `password/nickname/username/enabled/userType` 均有 `@Column(nullable=false)`，`enabled = true` 默认值 | User.java 第 29/32/35/42/46 行均带 `@Column(nullable=false)`；第 43 行 `Boolean enabled = true` | ✅ |
| `User.java` 缺失 `passwordChangeRequired` / `tokenVersion` | 计划要求新增 | 实测无此二字段 | ✅ |
| `Role.java` `enabled` 缺 `@Column(nullable=false)` | 计划要求补加 | Role.java 第 24 行 `private Boolean enabled = true;` 无注解 | ✅ |
| `Role.java` 无 `sort` 字段 | 计划要求新增 | 实测无 sort 字段 | ✅ |
| `Post.java` `enabled` 缺 `@Column(nullable=false)` | 计划要求补加 | Post.java 第 27 行 `private Boolean enabled = true;` 无注解 | ✅ |
| `Post.java` 已存在 `sort` 字段 | 计划描述正确 | Post.java 第 29 行 `private Integer sort;` 已存在 | ✅ |
| `UserRepository.findByUsername` 返回类型 | `User`（非 Optional），需改为 `Optional<User>` | UserRepository.java 第 15 行 `User findByUsername(String username);` | ✅ |
| `findByUsername` 唯一生产调用点 | `AuthServiceImpl.java:60` | grep 全工程匹配：仅 UserRepository.java:15（声明）、AuthServiceImpl.java:60（生产）、AuthServiceTest.java:82/102/117/133（测试 mock） | ✅ |
| `MenuServiceImpl` / `MenuServiceTest` / `EntityMappingIT` 是否调用 `findByUsername` | r1 误列，r2 已剔除 | 实测三个文件均无 `findByUsername` 引用 | ✅（修正正确） |
| `findById(...).orElseThrow(...)` 模板存在 | 第 126/170/195 行 | AuthServiceImpl.java 第 126/170/195 行均使用此模式 | ✅ |
| `AuthServiceTest` mock 行号 | 第 82/102/117/133 行 | 测试文件四行精确匹配 | ✅ |

### r1 审查意见的修正落实情况

| r1 意见 | r2 修订 | 验证 |
|---------|---------|------|
| **[一般] 调用方清单错误（误列 MenuServiceImpl 等）** | 已 grep 复核，剔除无关条目，仅保留两处真实调用点 | ✅ 已落实 |
| **[一般] 预期文件路径缺 `AIMedical/` 前缀** | 全部加 `AIMedical/` 前缀，并在开头新增"路径约定"段 | ✅ 已落实 |
| **[轻微] `passwordChangeRequired` 字段类型映射需澄清** | "上下文"追加 `Boolean` + `BIT(1)` vs `TINYINT(1)` 的差异说明，明确 DDL 归一由后续任务处理 | ✅ 已落实 |
| **[轻微] 集成测试新增覆盖的执行归属未重申** | "上下文"末尾追加"集成测试覆盖归属"段 | ✅ 已落实 |

## 发现

无 [严重]、无 [一般] 发现。仅附以下 [轻微] 提示，不影响通过：

- **[轻微] Optional 改造具体写法留作实现者推演**
  - 计划仅指引"参考同文件第 126/170/195 行 `findById(...).orElseThrow(...)` 模式"，未给出完整的改写示例。`login()` 现有 3 个分支（null / disabled / password mismatch）改写为 Optional 链式调用时，需保留原错误码语义（UNAUTHORIZED "用户名或密码错误"、FORBIDDEN "用户已被禁用"）与 log.warn 副作用。
  - 不构成阻塞：实现者依模板可直接推演，task_v1.md 中"参考同文件第 126/170/195 行"已足够明确。
  - 建议（如需增强）：在 detail 阶段给出具体改写片段，例如 `userRepository.findByUsername(...).orElseThrow(() -> { log.warn(...); return new BusinessException(GlobalErrorCode.UNAUTHORIZED, "用户名或密码错误"); })`，后续 enabled/password 分支保持 `if` 即可。

- **[轻微] `Role.sort` 当前无可立即消费方**
  - 计划称"`Role.sort` 是 `UserConverter` 按优先级取主角色的数据依赖"，但 `UserConverter` 类尚未存在（grep 全工程无该类文件）。这是面向后续 OOD 5.1 / Phase 2 实施的前置字段预留。
  - 不构成阻塞：phase 1 B 后续任务（如 UserConverter 引入）将消费此字段；当前新增字段属合理前置准备。
  - 提示：实施者应确认后续 UserConverter 任务确实按 OOD 计划引入，否则此字段在 Phase 1 内无消费方。

- **[轻微] `sys_user.nickname` DDL 与 Java 注解不一致为已知遗留**
  - schema.sql 第 17 行 `nickname VARCHAR(64) DEFAULT NULL`，但 User.java 第 35 行 `@Column(nullable=false) private String nickname;`。此不一致已存在，OOD 4.3 节已标注。
  - 不在本任务范围内（计划已声明"本任务仅做 Java 注解侧，schema.sql 同步留待后续 DDL 任务统一处理"），仅作下游 DDL 任务提示。

- **[轻微] `Boolean` → `BIT(1)` vs `TINYINT(1)` 的 Hibernate 映射行为**
  - 计划正确指出 MySQL Hibernate 默认映射 `Boolean` → `TINYINT(1)`，并显式 `columnDefinition="BIT(1) DEFAULT 0"` 覆盖。
  - 提示：若 Hibernate 方言版本或 `MySQLDialect` 子类发生变更，默认映射可能不同；建议实施时同步确认 schema 列类型与 Java 注解 `columnDefinition` 严格一致，必要时在后续 DDL 任务统一归一。

## 结论

所有 r1 审查意见（2 项一般 + 2 项轻微）均已落实修订，核验结果与计划描述完全一致。计划范围清晰（实体层扩展 + 单点调用方适配），依赖关系明确（为 Phase 1 B 后续 Filter / Service / DTO 任务提供编译期依赖），边界合理（DDL 同步、集成测试归属均显式声明由后续任务承担）。无 [严重] 或 [一般] 缺陷，可进入实现阶段。