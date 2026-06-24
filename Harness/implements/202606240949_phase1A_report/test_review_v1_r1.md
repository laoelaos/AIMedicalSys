# 测试审查报告（v1 r1）

## 审查结果
REJECTED

## 发现
- **[严重]** `Harness/implements/202606240949_phase1A_report/test_v1.md` — 测试报告文件不存在，对应此修复的测试代码未被创建或提交。行为契约（null password → DataIntegrityViolationException）无任何测试覆盖。

- **[严重]** `AIMedical/backend/integration/src/test/java/com/aimedical/integration/EntityMappingIT.java` — 集成测试类中缺少 `user_shouldRejectNullPassword()` 或等效的 password NOT NULL 约束验证测试。现有 UserTest.java 仅包含纯 getter/setter 单元测试，不涉及 JPA 持久化，无法验证 `@Column(nullable = false)` 的运行期约束行为。

- **[严重]** `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/permission/UserTest.java` — 单元测试中无持久化测试，无法验证 Hibernate 在 password 为 null 时抛出 DataIntegrityViolationException。

## 修改要求（仅 REJECTED 时）
1. **`需要新创建测试代码`** — 必须在集成测试类（EntityMappingIT.java）中新增 `user_shouldRejectNullPassword()` 测试方法：persist 一个未设置 password 的 User，预期抛出 `DataIntegrityViolationException`。同时补充正向测试 `user_shouldPersistWithPassword()`：设置非 null password 后 persist 应成功。
   - 问题：无测试覆盖行为契约的核心场景
   - 期望：参照 EntityMappingIT.java 中现有 `@SpringBootTest` + `@Transactional` + `EntityManager` 模式新增两个测试方法，覆盖正反两种场景。

2. **`test_v1.md`** — 创建测试报告文件，按规范记录新增测试的清单、运行结果和与行为契约的对应关系。
