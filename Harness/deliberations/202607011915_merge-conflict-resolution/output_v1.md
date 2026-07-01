# 合并冲突解决方案

## 1. 合并场景概要

| 维度 | 说明 |
|------|------|
| 当前分支 | `feat/task3` (75a786a) — 已完成 `Docs/07_ood_phase2_C_3_DE.md` 开发 + Phase 2/3 包C/D/E 代码实现 |
| 目标分支 | `develop` (a92d9d8) — 领先 1 commit，含 Phase 2 包A 挂号中心 + Phase 3 包G 医嘱开立记账 |
| 共同祖先 | 16849ae |
| 差异规模 | feat/task3: +25460/-54 行（后端范围，含 283 文件改动）; develop: +10296/-1745 行（后端范围，含 191 文件改动，含 registration/medical-order 全模块） |

---

## 2. 冲突文件清单及解决策略

### 2.1 pom.xml — 高冲突

| 冲突区域 | develop | feat/task3 | 合并方案 |
|---------|---------|-----------|---------|
| `<modules>` | registration, medical-order | consultation, prescription, medical-record | **全保留**。registration 和 medical-order 是 develop 新增的外围模块，consultation/prescription/medical-record 是 feat/task3 新增的业务模块。两者无功能重叠，须共存 |
| properties: lombok.version | 1.18.32 | 已删除 | **保留 develop 版本**。全局 Lombok 版本管理为 Maven 编译所需，develop 正确 |
| properties: jjwt.version | 0.12.6 | 0.12.5 | **采用 0.12.6**（develop 更新版本） |
| properties: maven.compiler.release | ${java.version} | 已删除 | **保留 develop**，release 参数为 JDK 17 统一编译所需 |
| properties: project.build.sourceEncoding | 无 | UTF-8 | **保留 feat/task3 新增**，这是合理补充 |
| `<dependencies>` global lombok | 有 | 已删除 | **保留 develop**，全局 Lombok 声明使所有子模块自动获得 |
| jacoco-report execution | 有 | 已删除 | **保留 develop**，CI 需要 report 阶段 |
| maven-compiler-plugin | 有（含 annotationProcessorPaths） | 已删除 | **保留 develop**，annotationProcessorPaths 是 Lombok 编译的关键配置 |
| maven-surefire-plugin | 有（bytebuddy argLine） | 已删除 | **保留 develop** |
| ignoredUnusedDeclaredDependency medical-order | 有 | 已删除 | **保留 develop** |
| dependencyManagement: application | 在 medical-order 之后 | 在 patient 之前 | **按字母升序整理**（artifactId）：admin → application → common-module → consultation → doctor → medical-order → medical-record → patient → prescription → registration；与现有顺序保持一致原则 |

**操作步骤**：以 develop 的 pom.xml 为基础，插入 feat/task3 的模块声明和 `project.build.sourceEncoding`，保留所有 develop 的构建插件配置。

---

### 2.2 AiResult.java — 高冲突

| 冲突区域 | develop | feat/task3 | 合并方案 |
|---------|---------|-----------|---------|
| Lombok `@Data` | 有 | 已移除，手动 getter/setter | **采纳 feat/task3**。移除 Lombok 是项目统一方向（Phase 5 pkgG OOD 也明确不使用 Lombok），保持一致 |
| `success(T data)` | `new AiResult<>(true, data, null, false, null)` | `Objects.requireNonNull(data)` | **采纳 feat/task3**，增加非空防御 |
| `failure(errorCode)`/`degraded(...)` | 相同 | 相同 | 无冲突 |

**操作步骤**：以 feat/task3 的版本为准（无 Lombok，含 `Objects.requireNonNull`），保留所有手动 getter/setter。

> 附注：feat/task3 新增的 `AiResultFactory.java` 是独立文件，无冲突，直接保留。

---

### 2.3 TriageRequest.java — 高冲突

| 冲突区域 | develop | feat/task3 | 合并方案 |
|---------|---------|-----------|---------|
| Lombok `@Data` | 有 | 已移除 | **采纳 feat/task3**，去 Lombok |
| 字段 | 仅 `chiefComplaint` | 扩展至 8 个字段 | **采纳 feat/task3**（ChiefComplaint / additionalResponses / patientId / sessionId / ruleVersion / ruleSetId / correctedChiefComplaint / additionalResponsesText） |

**操作步骤**：直接以 feat/task3 版本覆盖（develop 的版本字段过少，无法支撑 Phase 2/3 功能需求）。

---

### 2.4 TriageResponse.java — 高冲突

**与 TriageRequest 同理**，feat/task3 版本字段完备（recommendedDoctors / matchedRules / needFollowUp / followUpQuestion / confidence / degraded / sessionId / correctedChiefComplaint），直接采纳 feat/task3。

---

### 2.5 RecommendedDepartment.java — 中冲突

| 冲突区域 | develop | feat/task3 | 合并方案 |
|---------|---------|-----------|---------|
| Lombok `@Data` | 有 | 已移除 | **采纳 feat/task3**，与 §2.2 去 Lombok 决策一致 |
| 字段 | 仅 `departmentName` | 增加 `departmentId` 和 `score` | **采纳 feat/task3**（Phase 2/3 pkgC/D/E 需要 departmentId 关联科室、score 排序推荐） |
| getter/setter | 由 Lombok 生成 | 手写完整 getter/setter | **采纳 feat/task3** 手写版本 |

**操作步骤**：直接以 feat/task3 版本覆盖（含手写 getter/setter + 新增字段）。

---

### 2.6 Application.yml / application-dev.yml / application-prod.yml — 低冲突（main）/ 中冲突（test）

#### 2.6.1 `application/src/main/resources/application.yml` — 自动合并 ✅

| 冲突区域 | develop | feat/task3 | 合并方案 |
|---------|---------|-----------|---------|
| 基础配置 | 有 JPA / datasource / server 等 | 类似 | git 自动合并成功 |
| registration 相关配置 | 有 | 无 | **保留 develop** |
| consultation/prescription/medical-record 相关配置 | 无 | 有（追加末尾） | **保留 feat/task3** |

**操作步骤**：使用 git 自动合并结果即可（无 conflict marker），验证末尾追加的 `ai.timeout.*` / `consultation.doctor-facade.timeout` / `medical-record.visit-facade.timeout` 是否保留。

#### 2.6.2 `application/src/test/resources/application.yml` — content 冲突 ⚠️

| 冲突区域 | develop | feat/task3 | 合并方案 |
|---------|---------|-----------|---------|
| `jwt.*` 段 | 有（test 专用 secret / 24h token） | **已被删除** | **保留 develop**（test 资源需要 jwt 配置驱动 Spring Security 测试） |
| `ai.timeout.*` + `consultation.doctor-facade.timeout` + `medical-record.visit-facade.timeout` | 无 | 新增（与 main 一致） | **保留 feat/task3**（mock 服务的超时配置） |

**操作步骤**：以 develop 的 test/application.yml 为基础（保留 jwt 段），在末尾追加 feat/task3 的 `ai.timeout.*` / `consultation.*` / `medical-record.*` 全段。注意 develop 在 test 资源中**没有 `spring.datasource` 之外的业务段**，无需删减。

---

### 2.7 ai-impl/pom.xml — 自动合并 ✅

git 自动合并通过，主要差异是 feat/task3 添加了 `spring-boot-starter-web` 依赖、develop 调整了 `<properties>` 块的相对位置（移至 `<dependencies>` 前）。合并后**保留两者**：feat/task3 的 web 依赖 + develop 的 properties 位置。

**操作步骤**：使用 git 自动合并结果即可，验证 `<dependencies>` 段顺序为：ai-api → spring-boot-starter → spring-boot-starter-web（feat/task3 新增）→ spring-boot-starter-test。

### 2.8 `common/src/test/java/.../pom/MovedModulePomTest.java` — content 冲突 ⚠️（遗漏补充）

| 冲突区域 | develop | feat/task3 | 合并方案 |
|---------|---------|-----------|---------|
| 断言模块存在列表 | 已断言 `modules/registration` + `modules/medical-order` | 未断言 | **保留 develop**（新增 registration/medical-order 断言） |
| 根 pom 模块总数断言 | `rootPomShouldHaveExactlyTenModules` → 10 | `rootPomShouldHaveExactlyElevenModules` → 11 | **改为 13 模块**（8 基础 + 2 develop + 3 task3） |
| 模块总数测试方法名 | `...TenModules` | `...ElevenModules` | **重命名为 `...ThirteenModules`**（或 `...HasAllRequiredModules`，推荐语义化命名） |

**实测模块清单**（合并后根 pom `<modules>` 共 13 个）：
1. `common`
2. `modules/common-module`
3. `modules/ai`
4. `modules/patient`
5. `modules/doctor`
6. `modules/admin`
7. `modules/registration` （develop）
8. `modules/medical-order` （develop）
9. `modules/consultation` （feat/task3）
10. `modules/prescription` （feat/task3）
11. `modules/medical-record` （feat/task3）
12. `application`
13. `integration`

**操作步骤**：
1. 在 `rootPomShouldHaveExactlyTenModules`/`...ElevenModules` 的断言位置改为 `assertEquals(13, ...)`，并将方法名改为 `rootPomShouldHaveExactlyThirteenModules`；
2. 在 `assertTrue(exists(rootPom, "/project/modules/module[.='modules/admin']"));` 之后追加 develop 贡献的两行：
   ```java
   assertTrue(exists(rootPom, "/project/modules/module[.='modules/registration']"));
   assertTrue(exists(rootPom, "/project/modules/module[.='modules/medical-order']"));
   ```
3. **必须在合并后立即执行** `cd AIMedical/backend/common && mvn test -Dtest=MovedModulePomTest` 验证断言通过。

> ⚠️ 这是 CI 门禁测试，断言失败会阻塞构建。如未同步更新，develop CI 也会立即失败。

---

## 3. 新增文件清单（无冲突，直接保留）

所有 feat/task3 独有但 develop 中不存在的文件：

| 模块 | 路径 | 说明 |
|------|------|------|
| consultation | `modules/consultation/` 全部 | 智能分诊模块（Controller/Service/DTO/Entity） |
| prescription | `modules/prescription/` 全部 | 处方审核 + 辅助开方模块 |
| medical-record | `modules/medical-record/` 全部 | 病历生成模块 |
| ai-api 扩展 | `ai-api/.../dto/prescription/`、`medicalrecord/` | 新增 AI DTO |
| ai-api 扩展 | `ai-api/AiResultFactory.java` | AiResult 工厂类 |
| ai-api 扩展 | `ai-api/.../degradation/DegradationContext.java` | 降级上下文 |
| ai-api 扩展 | `ai-api/.../degradation/DegradationStrategy.java`（**补充**：原方案遗漏，feat/task3 引入，develop 也存在同名文件，需确认合并后两者签名一致） |
| common-module-api | `common-module-api/.../doctor/AvailableDoctor.java` | 医生门面 DTO |
| common-module-api | `common-module-api/.../doctor/DoctorFacade.java`（**补充**：原方案遗漏） | 医生门面接口（含超时配置入口） |
| common-module-api | `common-module-api/.../store/` 全部 | Store 抽象层接口（DraftContext / Session / Suggestion） + impl |
| ai-impl 扩展 | `modules/ai/ai-impl/.../controller/`、`mock/`、`rule/` | AiMockController / TimeBasedMockRule 等 |
| Docs | `Docs/Diagnosis/impl/` 诊断报告 | 无冲突文档 |

develop 独有的文件（无冲突，直接保留）：
| 模块 | 路径 | 说明 |
|------|------|------|
| registration | `modules/registration/` 全部 | 挂号中心模块 |
| medical-order | `modules/medical-order/` 全部 | 医嘱开立记账模块 |

---

## 4. 重复/重叠功能处理方案

以下识别两边实现了相似功能但未产生 Git 冲突的区域，需人工裁决。

### 4.1 分诊概念的语义重叠

| develop (registration 模块) | feat/task3 (consultation 模块) |
|----------------------------|-------------------------------|
| `TriageRecord` Entity + `TriageRecordRepository` | `TriageController` + `TriageService` |
| 护士到诊分诊（登记台）— 手动分诊，记录生命体征 | AI 智能分诊 — 多轮对话自动推荐科室 |
| 分诊作为挂号流程的附属步骤 | 分诊作为独立 AI 能力，输出 RegistrationEvent |
| `TriageLevel` (LEVEL_1~5) | 无对应概念，用 `confidence` / `degraded` 表达 |

**处理方案**：**两者共存，对接而非合并**。

- `TriageLevel` (develop) 和 `recommendedDepartments` (feat/task3) 是不同粒度的分诊概念，不存在功能重复
- develop 的 `TriageRecord` 是挂号到诊后的护士分诊记录
- feat/task3 的 DialogueSession 是 AI 分诊对话的会话状态
- **集成点**：feat/task3 的 `TriageServiceImpl.selectDepartment()` 写入最终科室选择后，可通过 `RegistrationEvent` 触发 registration 模块创建 `Registration` 记录
- 需确保 `DialogueSession` 中记录的 `finalDepartmentId` 能传递到 registration 模块的 `Registration.department`

### 4.2 Registration 模块的存留问题

feat/task3 的 consultation 模块的 OOD（§1.1a）声明依赖 registration 模块的 `RegistrationEvent`：

> "registration 模块：RegistrationEvent 的发布能力...consultation 模块消费"

但 feat/task3 分支上实际上**不存在 registration 模块**（文件级别上被删除，原因是在 feat/task3 中从未创建）。

**处理方案**：**恢复并保留 registration 模块**（来自 develop）。Consultation 模块按 OOD 契约注册监听 `RegistrationEvent`。两个模块的联合工作流：

```
[前端] → consultation:TriageController → 得出科室推荐
  → consultation:TriageServiceImpl.selectDepartment()
  → 确认挂号 → 发布 RegistrationEvent
  → registration:RegistrationServiceImpl.createRegistration() 消费事件
```

### 4.3 Schema.sql 表重叠（重要：原方案描述错误）

**实测结论**：
- `git diff $(merge-base)..feat/task3 -- AIMedical/backend/**/schema.sql` 输出为空 → **feat/task3 完全未修改 schema.sql**
- `git diff $(merge-base)..develop -- AIMedical/backend/**/schema.sql` 输出 333 文件 diff → **所有 schema.sql 改动均来自 develop**

**develop 单独引入的 schema 变更**：
- **新增表 11 张**：`patient_allergy`、`patient_chronic_disease`、`patient_family_history`、`patient_surgery_history`、`patient_medication_history`、`registration`、`triage_record`、`medical_order`、`medical_order_item`、`charge_pre_order`、`charge_pre_order_item`
- **修改已有表**（字段/索引调整）：
  - `sys_user`：新增 `gender`、`age` 字段；新增 `uk_phone`、`uk_email` unique key
  - `sys_role` / `sys_post` / `sys_function` / `sys_dict_type`：新增 `version` 乐观锁字段；`enabled` / `deleted` 由 `NOT NULL DEFAULT 1/0` 改为 `DEFAULT 1/0`；`sort` 字段名/注释微调
  - `sys_role` / `sys_post`：删除 `idx_code` 索引（保留 `uk_code`）
  - `sys_patient`：新增 `uk_id_card`、`idx_phone`
  - `sys_doctor`：新增 `uk_license_no`、`idx_department`
- **SET 子句**：新增 `SET REFERENTIAL_INTEGRITY FALSE`（H2 兼容）

**合并方案**：**直接以 develop 的 schema.sql 为准**。feat/task3 无 schema 改动，因此 git 会自动 fast-forward 该文件，不会产生冲突。

**操作步骤**：无需手动处理 schema.sql；如 review 后发现 phase 2/3 业务需要 consultation/prescription/medical-record 专用表（rule_version / prescription_audit / medical_record 等），需**作为补充迁移脚本**单独立 PR，**不在本次合并范围**。

> ⚠️ 原方案 §4.3 错误声称"feat/task3 新增 consultation/prescription/medical-record 相关表"，经实测不成立，已纠正。

### 4.4 DosageStandard 实体归属

| develop | feat/task3 |
|---------|-----------|
| 无 DosageStandard | `common/entity/DosageStandard.java`（OOD 要求 DosageStandard 实体迁移至 common 模块） |

feat/task3 在 `common/` 中新增了 `DosageStandard.java`，develop 无此文件。**无冲突，直接保留**。

### 4.5 已删除的 develop 构建配置

feat/task3 从 pom.xml 中删除了以下 develop 的配置，合并后全部恢复：
- maven-compiler-plugin（含 Lombok annotationProcessorPaths）
- maven-surefire-plugin（bytebuddy argLine）
- jacoco-report execution
- 全局 Lombok dependency
- `maven.compiler.release` property

这些是 CI 构建和代码覆盖度门禁的必要配置，**不应删除**。

---

## 5. 合并执行步骤（**feat/task3 → develop**）

> **方向**：本方案目标是将 feat/task3 合并到 develop（用户明示"task 合入 develop"）。

```
1. git checkout develop && git pull
2. git fetch origin feat/task3
3. git merge --no-ff origin/feat/task3 -m "merge: phase2/3 pkgC/D/E into develop"
4. 解决冲突文件（按 §2 对照表）：
   - pom.xml                       → 全保留模块 + develop 构建配置（§2.1）
   - AiResult.java                 → 采纳 feat/task3（去 Lombok + Objects.requireNonNull）（§2.2）
   - TriageRequest.java            → 采纳 feat/task3（8 字段扩展）（§2.3）
   - TriageResponse.java           → 采纳 feat/task3（扩展字段）（§2.4）
   - RecommendedDepartment.java    → 采纳 feat/task3（手写 + 新增字段）（§2.5）
   - application.yml (main)        → 验证 git 自动合并结果（§2.6.1）
   - application.yml (test)        → 保留 develop jwt 段 + 末尾追加 feat/task3 超时配置（§2.6.2）
   - ai-impl/pom.xml               → 验证 git 自动合并结果（§2.7）
   - MovedModulePomTest.java       → 模块数 11→13，新增 registration/medical-order 断言（§2.8）
5. git add 解决后的所有冲突文件
6. git commit --no-edit  (或自定义合并信息)
7. 验证：
   - mvn -pl common test -Dtest=MovedModulePomTest   # 必须通过
   - mvn -pl application test -Dtest=ApplicationContextTest
   - mvn clean compile -DskipTests
   - mvn test
   - 检查 schema.sql = develop 版本（自动继承，无冲突）
   - 检查 modules/registration/、modules/medical-order/ 目录存在
8. push origin develop
```

**重要：不要在 feat/task3 上合并 develop**（与原方案方向相反）。如需在 feat/task3 上同步 develop 的最新提交以避免后续 rebase 痛苦，可独立执行：

```bash
git checkout feat/task3
git merge --no-ff origin/develop -m "sync develop into feat/task3"
```

**但不推荐**——因 feat/task3 已有 13 个 squash commit 链，重新合并会产生大量冲突；建议 feat/task3 作为一次性历史保留，develop 继续前进即可。

---

## 6. 冲突风险评级汇总

| 风险等级 | 文件 | 解决难度 |
|---------|------|---------|
| 🔴 高 | `pom.xml` | 需逐区块判断取舍，不可自动接受任一方 |
| 🔴 高 | `MovedModulePomTest.java`（**新增评级**：CI 门禁，断言值必须改对） | 模块数 11→13，方法名同步重命名 |
| 🟡 中 | `AiResult.java` | 功能差异小，原则坚定即可 |
| 🟡 中 | `TriageRequest/Response.java` | 版本差异大但目标明确，直接覆盖 |
| 🟡 中 | `application.yml (test 资源)`（**新增评级**：原方案误评为低） | jwt 段保留 + ai/consultation/medical-record 全段追加 |
| 🟢 低 | `RecommendedDepartment.java` | 直接采纳 feat/task3（含手写 getter/setter） |
| 🟢 低 | `application.yml (main 资源)` | git 自动合并通过 |
| 🟢 低 | `ai-impl/pom.xml` | git 自动合并通过 |
| 📋 设计重复 | 分诊 & registration 集成 | 需确认对接方案 |
| 📋 数据迁移 | schema.sql | 直接继承 develop 版本，feat/task3 无冲突；后续若需新增 consultation/prescription/medical-record 业务表，另开迁移 PR |

---

## 7. 修订记录（v1 → v1.1）

| 修订项 | 优先级 | 内容 |
|--------|--------|------|
| §5 执行步骤方向 | P0 | 反转：原方案将 develop 合入 task3，用户明示"task 合入 develop"，现改为 `develop` 上 `merge feat/task3` |
| 新增 §2.6.2 test/application.yml 冲突处理 | P0 | 原方案仅评估 main 资源，遗漏 test 资源的 content 冲突 |
| 新增 §2.8 MovedModulePomTest.java 冲突处理 | P0 | CI 门禁测试，断言模块数需从 11→13 并新增 registration/medical-order 断言 |
| §2.5 RecommendedDepartment 补 Lombok 说明 | P1 | 与 §2.2 去 Lombok 决策保持一致 |
| §3 补充 DoctorFacade.java / DegradationStrategy.java / ai-impl mock 文件 | P1 | 原方案文件清单不全 |
| §2.6 标题改为双文件 | P1 | main 资源 / test 资源分开评级 |
| §1 差异行数刷新 | P2 | 实测后端范围：feat/task3 +25460/-54；develop +10296/-1745 |
| §4.3 纠正 schema.sql 描述 | P2 | 原方案错误声称 feat/task3 新增 consultation/prescription/medical-record 表，实测 feat/task3 完全未改 schema.sql；所有 schema 改动来自 develop；现方案直接以 develop 为准 |
| §2.1 dependencyManagement 顺序说明 | P2 | 改为"按字母升序"明确规则 |
| §6 风险评级表 | P1 | 新增 MovedModulePomTest（🔴高）、test/application.yml（🟡中） |
