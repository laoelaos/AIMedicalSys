# 实现计划

任务描述：完成 Docs\Diagnosis\impl\03_phase1A_report.md 描述的 Phase 1A 四个 Issue 的代码修复与测试补充
项目根目录：C:\Develop\Software\AIMedicalSys

---

## 实施路线（v5 最终）

| # | 任务项 | 对应 Issue | 优先级 | 涉及文件 | 复杂度 | v1 状态 | v2 状态 | v3 状态 | v4 状态 | v5 状态 |
|---|--------|-----------|--------|---------|-------|---------|---------|---------|---------|---------|
| 1 | User.password 添加 @Column(nullable=false) + schema.sql password NOT NULL | Issue 2 (P0) | P0 | User.java:28, schema.sql:16 | 低(2行) | ✅ DONE | ✅ DONE | ✅ DONE | ✅ DONE | ✅ DONE |
| 2 | 修复 common-module-impl 测试基础设施（添加 H2 依赖） | 基础设施 | P0 | common-module-impl/pom.xml | 低(3行) | ❌ FAILED | ✅ DONE | ✅ DONE | ✅ DONE | ✅ DONE |
| 3 | schema.sql 16张表 deleted 列添加 NOT NULL | Issue 3 (P1) | P1 | schema.sql | 低(批量替换) | ☐ SKIPPED | ✅ DONE | ✅ DONE | ✅ DONE | ✅ DONE |
| 4 | enabled/visible 字段添加 Java 默认值 true | Issue 4 (P2) | P2 | User.java:37, Role.java:28, Post.java:30, Function.java:30/54 | 低(5行) | ☐ SKIPPED | ✅ DONE | ✅ DONE | ✅ DONE | ✅ DONE |
| 5 | EntityMappingIT 新增 User/Role/Post 集成测试 | Issue 1 (P3) | P3 | EntityMappingIT.java | 中(9个方法) | ➖ PARTIAL | ✅ DONE | ✅ DONE | ✅ DONE | ✅ DONE |
| 6 | 修复 EntityMappingIT.java 2个测试缺陷（userType缺失+异常类型） | 修复缺陷 | P0 | EntityMappingIT.java:250-271 | 低(2行) | — | ❌ FAILED | ✅ DONE | ✅ DONE | ✅ DONE |
| 7 | 修复 UserRepositoryTest 2个测试（异常类型+H2大小写敏感） | 修复缺陷 | P0 | UserRepositoryTest.java:47-71 | 低(2行) | — | ❌ FAILED | ❌ FAILED | ✅ DONE | ✅ DONE |
| 8 | 验证全链路（common-module-impl+integration 全部通过） | 验证 | P0 | — | 低 | — | ❌ FAILED | ❌ FAILED | ❌ FAILED | ✅ DONE |

## v1~v3 失败根因分析

**v1 FAILED**：common-module-impl 缺少 H2 依赖 → UserRepositoryTest 上下文加载失败 → 阻断 reactor 链 → integration 模块未执行。

**v2 FAILED**：H2 依赖已添加，但仍有 **4 个测试失败分布在 2 个模块**：
1. common-module-impl: UserRepositoryTest.shouldRejectNullPassword — 期望 DataIntegrityViolationException，实际 Hibernate 抛出 PropertyValueException
2. common-module-impl: UserRepositoryTest.shouldHaveNotNullConstraintOnPasswordColumn — INFORMATION_SCHEMA 查询因 H2 大写存储无结果 → EmptyResultDataAccessException
3. integration: EntityMappingIT.user_shouldPersistWithPassword — 未设 userType（列 NOT NULL）
4. integration: EntityMappingIT.user_shouldRejectNullPassword — 未设 userType + 异常类型不匹配

**v2 分析失误**：仅诊断了 integration 的 2 个失败（#3 #4），**遗漏了 common-module-impl 的 2 个上游失败（#1 #2）**。这导致后续修复方向错误。

**v3 FAILED**：仅修复了 integration 的 2 个测试（#3 #4——已在 EntityMappingIT.java 中正确修正）。但 common-module-impl 的 2 个测试仍失败（#1 #2），上游模块构建失败 → integration 模块仍然被阻断。验证报告显示 EntityMappingIT 从未被执行。

**v4 修正方向**：修复 UserRepositoryTest 的 2 个测试缺陷，然后全链路验证。

---

## R1 PASSED User.password 添加 NOT NULL 约束（含 schema.sql 同步修改）
任务：
  - 在 User.java:28 添加 `@Column(nullable = false)` ✅
  - schema.sql:16 `DEFAULT NULL` → `NOT NULL` ✅
  - 编译验证：`mvn compile -pl modules/common-module/common-module-impl -am` 编译成功 ✅
结果：Issue 2 代码修复完成，编译通过。预添加的 EntityMappingIT 测试方法 (user_shouldPersistWithPassword, user_shouldRejectNullPassword) 已就位但因 build 阻断未执行。

---

## R2 FAILED User.password NOT NULL - 集成测试验证
结果：验证失败 ❌
原因：
  - common-module-impl 的 UserRepositoryTest（`@DataJpaTest`）尝试替换 DataSource 为嵌入式 H2 时失败
  - 根因：common-module-impl/pom.xml 未声明 h2 依赖，而 root pom.xml 虽在 dependencyManagement 中定义了 h2，但子模块未显式引用
  - 该测试失败阻断 mvn test 的 reactor 构建链，integration 模块的 EntityMappingIT 未能执行
修正方向：在 common-module-impl/pom.xml 中添加 h2 (test/runtime scope) 依赖

## R2 PASSED Phase 1A 剩余全部代码修复 + 测试补充
结果：
  - ✅ common-module-impl/pom.xml 添加 H2 runtime 依赖
  - ✅ schema.sql 16 张表 deleted → `NOT NULL DEFAULT 0`
  - ✅ User.java/Role.java/Post.java/Function.java 添加 `= true` 默认值
  - ✅ EntityMappingIT.java 新增 9 个测试方法（User 5 + Role 3 + Post 2）
  - ✅ 代码审查全部 APPROVED（design/code/test 三份审查报告）
  - ❌ 验证失败：v1 引入的 2 个测试有缺陷

---

## R3 RETRY 修复 v1 遗留的 2 个测试缺陷
任务：修复 EntityMappingIT.java 中 user_shouldPersistWithPassword 和 user_shouldRejectNullPassword
失败原因：
  - `user_shouldPersistWithPassword`（第 250 行）：未设置 userType（列有 @Column(nullable=false)），H2 下抛出 ConstraintViolationException
  - `user_shouldRejectNullPassword`（第 262 行）：① 未设置 userType，实际异常源于 userType 非空而非 password；② 预期 `DataIntegrityViolationException`，但 EntityManager 直操作抛出 `ConstraintViolationException`
修正方向：
  - user_shouldPersistWithPassword：添加 `user.setUserType(UserType.ADMIN)` 
  - user_shouldRejectNullPassword：添加 `user.setUserType(UserType.PATIENT)` + 将异常断言改为 `ConstraintViolationException`
验证方式：`mvn test -pl integration -am`

---

## R4 PASSED 修复 UserRepositoryTest 2个测试缺陷
任务：修复 common-module-impl 的 UserRepositoryTest 中 2 个测试
失败原因：
  - `shouldRejectNullPassword`（第 47 行）：`DataIntegrityViolationException` → `PropertyValueException`（Hibernate 直接抛出 PropertyValueException 而非 Spring 包装的 DataIntegrityViolationException）
  - `shouldHaveNotNullConstraintOnPasswordColumn`（第 65 行）：H2 INFORMATION_SCHEMA.COLUMNS 默认将表名列名存储为大写，`TABLE_NAME = 'sys_user'` 匹配不上 `'SYS_USER'` → EmptyResultDataAccessException
修正方向：
  - shouldRejectNullPassword：将 expected exception 改为 `PropertyValueException`，添加 `org.hibernate.PropertyValueException` import
  - shouldHaveNotNullConstraintOnPasswordColumn：使用 `UPPER(TABLE_NAME) = 'SYS_USER' AND UPPER(COLUMN_NAME) = 'PASSWORD'`
验证方式：`mvn test -pl common-module-impl,integration -am`（一次全链路验证，确保 integration 不再被阻断）
结果：PASSED ✅
  - common-module-impl: 39 tests, 0 failures ✅
  - integration: EntityMappingIT 成功执行 ✅
  - 全链路 13 模块 BUILD SUCCESS，242 通过，0 失败，5 跳过 ✅

---

## R5 PASSED 全任务完成
所有 8 项任务已实现并通过全链路测试验证：
- ✅ Issue 2: User.password NOT NULL 约束（Java + SQL）
- ✅ 基础设施: common-module-impl H2 依赖
- ✅ Issue 3: 16 张表 deleted NOT NULL
- ✅ Issue 4: enabled/visible Java 默认值
- ✅ Issue 1: EntityMappingIT 9 个集成测试
- ✅ 测试缺陷修复: EntityMappingIT 2 个 + UserRepositoryTest 2 个
- ✅ 全链路验证: 242 passed, 0 failed, 5 skipped, BUILD SUCCESS

**Phase 1A 全部完成。**
