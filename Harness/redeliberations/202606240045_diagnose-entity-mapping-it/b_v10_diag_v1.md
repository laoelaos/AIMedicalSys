# 质量审查报告：a_v10_diag_v1.md

## 审查范围

- 审查对象：`a_v10_diag_v1.md`（第 10 轮迭代产出）
- 审查维度：需求响应充分度、事实准确性、逻辑一致性、深度/完整性、可操作性
- 审查原则：聚焦内部审议未充分覆盖的维度，避免重复验证已确认的技术细节

---

## 发现的问题

### 问题 1：[中等] 映射点表 M11 描述与测试实际验证能力不一致

**问题描述**：映射点表 M11 将 Post.code 描述为"唯一约束"并在备注列标注"post_shouldMapManyToManyFunctions 中验证"。然而 `post_shouldMapManyToManyFunctions` 仅验证了基本字段映射（`assertEquals("POST_WITH_FUNCS", found.getCode())`），并未验证唯一约束的强制力（即 persist 两个相同 code 的 Post 应抛出 `DataIntegrityViolationException`）。同样模式在 M1（v8 修订说明第 2 条）和 M7（v10 修订说明第 1 条）中已先后被修正和补充测试，但 M11 未被同步修正。

**所在位置**：第 31-32 行（M11 行）

**严重程度**：中等 — 与已修复的 M1/M7 模式重复，降低修复指引的整体一致性

**改进建议**：将 M11 备注列改为与 M1 对齐的描述，如"仅验证基本字段映射，约束强制力验证需另加测试"，或补充独立的 `post_shouldEnforceCodeUniqueConstraint` 测试方法。

---

### 问题 2：[中等] Post 测试示例缺失 `deleted` 字段断言

**问题描述**：产出第 35 行声明"实体均继承 BaseEntity，需验证 id 自增、deleted NOT NULL、createdAt/updatedAt 审计字段的映射行为"。User 和 Role 的测试示例均包含 `assertNotNull(found.getDeleted())` 和 `assertFalse(found.getDeleted())` 对 deleted 字段的双重断言。但 Post 组的两个测试示例（`post_shouldMapManyToOneRole` 和 `post_shouldMapManyToManyFunctions`）均未对 `deleted` 做任何断言，仅验证了 `enabled`、`role`、`sort`、`code` 和关系字段。

**所在位置**：第 256-260 行（post_shouldMapManyToOneRole）、第 283-288 行（post_shouldMapManyToManyFunctions）

**严重程度**：中等 — 三个实体的测试覆盖标准不统一，Post 的 BaseEntity 字段验证存在缺口

**改进建议**：在两个 Post 测试方法体中补充 `assertNotNull(found.getDeleted())` 和 `assertFalse(found.getDeleted())`，与 User/Role 测试保持一致。

---

### 问题 3：[中等] 交叉对比表"是否已有生产脏数据"维度在 Issue 4 与 Issue 2 间论证标准不对称

**问题描述**：交叉对比分析表（第 718-726 行）中 Issue 4 的"是否已有生产脏数据"维度标注为"是"，而 Issue 2 在 v8 修订说明第 1 条中已从"是"更正为"可能（未经验证）"。Issue 4 的副作用分析（第 676-689 行）仅确认了"不存在任何生产代码路径依赖 enabled == null"，并未对实际生产数据库中是否存在 NULL 记录做预检或提供证据支撑，与 Issue 2 的论证层级完全相同（代码路径排查 → 无生产路径创建 → 遗留数据可能性），结论却不同。

**所在位置**：第 724 行（交叉对比分析表 Issue 4 行，"是否已有生产脏数据"列）

**严重程度**：中等 — 同样的论证标准得出不同结论，降低交叉对比的可信度

**改进建议**：将 Issue 4 该维度值改为"可能（未经验证）"，与 Issue 2 保持一致；或在排序理由中明确标注 Issue 4 脏数据存在性为推定（而非确认），例如"按逻辑推定已存在（因无 Java 默认值 + 未显式 setEnabled 的代码路径可能写入 NULL）"。

---

### 问题 4：[低] `userType NOT NULL` 约束验证策略已描述但未提供测试示例

**问题描述**：第 297 行的"NOT NULL 约束验证策略"明确指出"User.userType 标注了 `@Column(nullable = false)`，测试应通过 persist 一个未设置 userType 的 User 来验证 `DataIntegrityViolationException`"。但 Issue 1 提供的 7 个测试示例中，所有涉及 User 的测试均显式设置了 `userType`（`UserType.DOCTOR`/`PATIENT`/`ADMIN`），不存在未设置 userType 的测试。该策略描述与提供的测试示例之间存在落差。

**所在位置**：第 297 行（策略描述）及所有 User 测试示例

**严重程度**：低 — 不影响基本映射验证，但"说了没做"使策略章节的指导性打折扣

**改进建议**：二选一 — (A) 补充一个 `user_shouldRejectNullUserType` 测试示例（与 `user_shouldRejectNullPassword` 模式一致）；(B) 在策略章节中明确标注"userType NOT NULL 约束测试已由 `user_shouldRejectNullPassword` 模式类推，不在此处逐一提供"。

---

### 问题 5：[低] BaseEntity 审计字段验证在指引中提出但未在测试中兑现

**问题描述**：第 35 行提出"需验证 ... createdAt/updatedAt 审计字段的映射行为"，但 User、Role、Post 三组的 7 个测试示例中没有任何一个对 `createdAt` 或 `updatedAt` 做断言。此外，`createdAt`/`updatedAt` 由 `@CreatedDate`/`@LastModifiedDate` + `AuditingEntityListener` 管理，需要在测试上下文中启用 `@EnableJpaAuditing` 才能自动填充。产出未说明测试环境中审计功能的启用状态，也未解释为何将其排除在测试范围之外。

**所在位置**：第 35 行（指引）及 Issue 1 全部测试示例

**严重程度**：低 — 不阻塞修复执行，但指引与实现之间的缺口可能误导后续维护者期望这些字段被覆盖

**改进建议**：在"需验证的映射点"表的 BaseEntity 备注行或"测试环境能力边界说明"中，明确标注`createdAt`/`updatedAt` 是否在当前测试环境中可验证，以及是否需要补充断言。如果审计功能未启用，建议在类级注释中说明。

---

## 整体评价

产出经过 10 轮迭代审议，已基本覆盖用户需求的全部 4 个问题，修复指引可操作性高，生产迁移方案和副作用分析系统全面。上述 5 个问题均为残余性局部缺陷，集中在测试示例的覆盖一致性（问题 1、2、4、5）和交叉对比论证标准统一性（问题 3）上，不影响产出作为修复执行依据的有效性。
