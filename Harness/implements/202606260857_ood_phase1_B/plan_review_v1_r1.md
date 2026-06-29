# 计划审查报告（v1 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** **调用方清单错误：`MenuServiceImpl` 与 `EntityMappingIT` 并不调用 `findByUsername`**
  - 计划"预期文件路径"与"上下文"两处均将 `MenuServiceImpl`、`EntityMappingIT`（以及隐含的 `MenuServiceTest`）列为 `findByUsername` 的调用方，要求把 `user == null` 替换为 `Optional.isEmpty()` / `.orElseThrow(...)` 链式调用。
  - 实际核查结果：
    - `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/MenuServiceImpl.java`：仅在第 53 行使用 `userRepository.findById(userId)`，从未引用 `findByUsername`，无任何 null 判断需要适配。
    - `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java`：使用 `entityManager.find(User.class, ...)` 与直接构造 `User` 的方式，不依赖 `UserRepository` 方法，对应字段映射测试已就位，无需修改。
    - `MenuServiceTest.java` 也未引用 `findByUsername`（grep 结果为空）。
  - 真正的 `findByUsername` 调用方只有两处：
    - `AuthServiceImpl.java:60`（`User user = userRepository.findByUsername(request.getUsername());` 紧跟第 62 行 `if (user == null)`）。
    - `AuthServiceTest.java` 第 82/102/117/133 行（mock 调用）。
  - 影响：误导后续实现环节在无关文件中查找/改动，浪费时间；若实现者强行"适配" MenuServiceImpl / EntityMappingIT，可能引入毫无意义的修改或冲突。

- **[一般]** **预期文件路径相对项目根缺少 `AIMedical/` 前缀**
  - 计划"预期文件路径"小节使用 `backend/modules/common-module/...`，对应注释为"项目根目录：`C:/Develop/Software/AIMedicalSys`"。
  - 实际项目布局：项目根下存在 `AIMedical/` 子目录，所有 Java 源码位于 `C:/Develop/Software/AIMedicalSys/AIMedical/backend/modules/common-module/...`。`C:/Develop/Software/AIMedicalSys/backend/` 这一路径并不存在（已 `ls` 验证项目根下无 `backend` 目录）。
  - 影响：若实现者按字面理解将"项目根 + 计划路径"拼接，得到的路径不存在；如不主动核对结构，可能创建出错误的目录或在错误的相对路径上工作。任务文件 `task_v1.md` 使用相同的相对路径约定，因此这是 Planner/Task 双方共同的坐标基线错误，应在下一轮修订中统一修正为 `AIMedical/backend/...`。

- **[轻微]** **`User.passwordChangeRequired` 与 `User.tokenVersion` 字段注解细节在计划中略粗**
  - 计划只说明"`passwordChangeRequired`（`Boolean`，`@Column(nullable=false, columnDefinition="BIT(1) DEFAULT 0")`，默认值 `false`）"，与 OOD 5.1 / task_v1 一致，无矛盾；但计划未重申 `Boolean` 包装类型与 `BIT(1)` 列定义的对应关系。MySQL 下 `Boolean` 通常映射 `TINYINT(1)`，显式声明 `BIT(1)` 与既有 `schema.sql` 中 `enabled TINYINT(1) DEFAULT 1` 的列类型并不一致，建议在计划中点明这一类型差异，以便后续 DDL 任务对齐时无歧义。
  - 不影响本任务正确性，仅为后续 DDL/迁移环节的隐患提示。

- **[轻微]** **未说明集成测试新增覆盖的执行归属**
  - task_v1.md 已明确"集成测试扩展归属阶段 4，本任务仅做基础单测调整（如有需要）"；计划对此未再次声明，但与 OOD 一致。轻微提示：实施时若决定本任务不做任何单测调整，应在 detail/code 阶段明确写"无新增单测"，避免后续测试 Agent 误判为覆盖缺失。

## 修改要求

1. **修正调用方清单**：从预期文件路径与上下文段落中移除 `MenuServiceImpl`、`EntityMappingIT`、`MenuServiceTest` 三处错误引用，仅保留 `AuthServiceImpl.java:60` 与 `AuthServiceTest.java:82/102/117/133` 两个调用点。`AuthServiceImpl.login()` 的 null 分支需改为基于 `Optional` 的链式调用（参考同文件第 126/170/195 行的 `findById(...).orElseThrow(...)` 模式）；`AuthServiceTest` 第 102 行 `thenReturn(null)` 需改为 `thenReturn(Optional.empty())`，其余三处 `thenReturn(testUser)` 需改为 `thenReturn(Optional.of(testUser))`。

2. **修正预期文件路径前缀**：将所有"预期文件路径"从 `backend/...` 改为 `AIMedical/backend/...`，与项目实际布局一致；或在路径前注明"相对项目根 `C:/Develop/Software/AIMedicalSys` 的实际路径为 …"，由实现者自行解析。task_v1.md 应同步修正。

3. **澄清 `passwordChangeRequired` 字段类型映射**（建议但非必须）：在计划中明确 `Boolean` + `columnDefinition="BIT(1) DEFAULT 0"` 与既有 `schema.sql` 中 `TINYINT(1)` 列的差异，提醒后续 DDL 任务统一两种布尔类型，避免 Phase 1 schema 出现混用。

4. **明确集成测试归属**：在计划中重申"本任务不新增 User 实体字段的集成测试用例，相关覆盖由阶段 4 集成测试任务统一处理"，作为下游阶段的契约说明。