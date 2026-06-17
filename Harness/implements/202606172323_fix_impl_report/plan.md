# 实现计划

任务描述：修复 @Docs\Diagnosis\impl\02_impl_report.md 诊断报告中列出的 9 个真实存在的代码缺陷（T1-T4、T6-T7、T9-T11），T5（OOD文档问题，已修复）和 T8（误报）跳过。

项目根目录：C:\Develop\Software\AIMedicalSys

---

## R1 NEW POM依赖治理（T3+T4+T7）
任务：修复父POM与模块POM中的依赖管理问题
选择理由：纯POM级别的删除操作，无运行时风险；作为底层基础修复，消除dependency:analyze噪音，为后续任务提供干净的依赖基线
上下文：
  - T3: `backend/pom.xml:84-109` 五个Starter条目均有显式 `<version>3.2.5</version>`，父POM继承自spring-boot-starter-parent:3.2.5，BOM已统一管理版本
  - T4: `backend/pom.xml:78-83` h2条目在 `<dependencyManagement>` 内设 `<scope>runtime</scope>`，与OOD §2.2 "仅application模块自行声明scope"矛盾
  - T7: `ai-impl/pom.xml:17-20` 声明了`common`依赖，但ai-impl源码无任何`com.aimedical.common.*`导入，common已通过ai-api传递获得

---

## R1 FAILED POM依赖治理（T3+T4+T7）
结果：mvn compile失败——子POM dependencyManagement覆盖父BOM版本管理，10个子模块报missing version
验证：16项测试通过8项（ParentPomVersionTest:5/5, AiImplPomCleanDependencyTest:3/5需POM修改后通过）、失败8项（ParentPomDependencyManagementCleanupTest:6/6需方案A实现后通过, AiImplPomCleanDependencyTest:2/5）

## R1 RETRY POM依赖治理（T3+T4+T7）
原因：设计假设错误——仅删除version不可行，子POM的dependencyManagement条目会覆盖父BOM（spring-boot-starter-parent）中对应条目的版本管理
修正方向：采纳code_v1方案A——完全移除dependencyManagement中5个Spring Boot Starter条目块（第84-109行），而非仅删除version；T4/T7操作与原设计一致

---

## R2 RETRY POM依赖治理（T3+T4+T7）
审查意见：task_v2验证步骤3称ParentPomVersionTest在方案A实施后仍会"保持通过"，但该测试的5条XPath断言均验证被删除的starter条目存在于dependencyManagement中，必然全量失败
修订措施：新增OP-04（标记ParentPomVersionTest.java为废弃@Disabled），修正验证步骤移除该测试条目

---

## R2 PASSED POM依赖治理（T3+T4+T7）
结果：删除5个Starter的整个dependency块（OP-01）、删除h2 runtime scope（OP-02）、删除ai-impl冗余common依赖（OP-03）、标记ParentPomVersionTest@Disabled（OP-04）
测试：ParentPomDependencyManagementCleanupTest 6项通过、AiImplPomCleanDependencyTest 5项通过、mvn compile -q 成功

---

## R3 NEW 工程结构占位补全（T2+T9）
任务：在common-module-impl创建config/目录（package-info.java占位）、在common创建util/目录（package-info.java占位）
选择理由：T2（config/缺失）与T9（util/缺失）同为OOD包命名规范未对齐的结构占位，纯目录创建无运行时风险；作为底层基础修复为后续任务提供干净的工程结构基线
上下文：OOD §2.3包命名规范要求common-module-impl包含permission/config/dict，当前缺少config/；common应包含base/result/exception/util/config，当前缺少util/

---

## R3 PASSED 工程结构占位补全（T2+T9）
结果：common-module-impl创建config/package-info.java（OP-01）、common创建util/package-info.java（OP-02）
测试：设计审查APPROVED、代码审查APPROVED（r2）、测试审查APPROVED；verify PASSED（mvn compile -q成功）

---

## R4 PASSED PageQuery校验注解补全（T1）
结果：PageQuery.page添加@Min(0)、PageQuery.size添加@Max(500)、PageQuery.sort添加@Size(max=10)；common/pom.xml添加spring-boot-starter-validation optional依赖
测试：mvn compile -pl common -q通过；验证PASSED

---

## R5 NEW Axios响应拦截器Result.code拆包（T6）
任务：修复frontend Axios响应拦截器，按OOD §4.2实现Result.code拆包逻辑（success拦截器内直接处理方案B），调整apiGet/apiPost/apiPut/apiDelete返回类型为Promise&lt;T | BusinessError&gt;，同步更新测试文件
选择理由：T6为剩余任务中唯一高优先级项；所有模块POM级基础修复已完成；与后端任务（T10/T11）无依赖关系，独立并行
上下文：OOD §4.2要求response.data.code === "SUCCESS"时返回response.data.data、否则走错误处理；当前仅return response.data；文件路径：frontend/packages/shared/src/api/index.ts

---

## R5 FAILED Axios响应拦截器Result.code拆包（T6）
结果：2 tests failed — success interceptor non-SUCCESS分支返回`Promise.resolve(...)`而测试未await
验证：35 passed, 2 failed（verify_v5.md）

## R5 RETRY Axios响应拦截器Result.code拆包（T6）
任务：移除success拦截器non-SUCCESS分支的Promise.resolve()包装，使返回值保持同步以对齐SUCCESS分支行为
选择理由：Promise.resolve()包装导致non-SUCCESS分支返回Promise对象，但SUCCESS分支（return body.data）为同步返回值，测试也以同步方式断言；两分支行为不一致导致2个测试因收到Promise而非BusinessError对象而失败
修正方向：
  - api/index.ts:14 — 移除Promise.resolve()，直接返回BusinessError对象
  - 测试文件无需修改（同步返回值直接匹配现有断言模式）

## R5 PASSED Axios响应拦截器Result.code拆包（T6）
结果：实现success拦截器code拆包逻辑（方案B）、BusinessError类型、4个包装函数返回类型从Promise&lt;ApiResponse&lt;T&gt;&gt;变更为Promise&lt;T | BusinessError&gt;
测试：37 passed（interceptors.test.ts 24 + types.test.ts 13）
验证：verify_v6.md PASSED（mvn -q编译成功）

---

## R6 NEW BaseEntityTest审计字段自动填充验证（T11）
任务：新建BaseEntityAuditTest.java（@DataJpaTest + @Import(JpaConfig.class)），验证@CreatedDate/@LastModifiedDate审计自动填充；同步在common/pom.xml中添加h2 test scope依赖
选择理由：T11为剩余任务中底层依赖最深的（common模块），修复不影响生产代码；先补全底层测试基础设施，再处理T10行为修改
上下文：
  - common/pom.xml缺少h2 test依赖，@DataJpaTest需嵌入式数据库
  - JpaConfig已标注@EnableJpaAuditing，审计配置就绪
  - BaseEntityTest现有4个纯JUnit5测试，需新增JPA上下文测试

---

## R6 FAILED BaseEntityTest审计字段自动填充验证及回归测试修复（T11）
结果：BaseEntityAuditTest ERROR（@DataJpaTest无法找到@SpringBootConfiguration——common为库模块无入口类）、CommonPomTest 2项失败（依赖计数3→5因R4新增validation+R6新增h2、shouldNotContainValidationStarter因R4新增validation失效）、ParentPomTest 2项失败（dependencyManagementShouldContainAllSpringBootStarters因R1删除starter块失效、testStarterShouldHaveTestScope因dependencyManagement中starter已删除失效）
验证：94 passed, 5 failed (4 Failures, 1 Error)

## R6 RETRY BaseEntityTest审计字段自动填充验证及回归测试修复（T11）
任务：3项修复——（1）BaseEntityAuditTest添加@SpringBootConfiguration内部配置类使@DataJpaTest可引导；（2）CommonPomTest更新依赖计数和validation starter断言；（3）ParentPomTest更新dependencyManagement starter行断言
选择理由：3项失败均为同一模块(common)的测试基础设施问题，一次RETRY统一修复效率最高
修正方向：
  - BaseEntityAuditTest.java: 移除@ExtendWith(SpringExtension.class)（@DataJpaTest已包含），在类内添加@SpringBootApplication静态内部配置类，使@DataJpaTest可正常引导Spring应用上下文
  - CommonPomTest.java: dependencyCountShouldBeExactlyThree断言值3→5，shouldNotContainValidationStarter移除整条测试方法（validation starter为R4有意新增）
  - ParentPomTest.java: dependencyManagementShouldContainAllSpringBootStarters中移除5个已被删除的Spring Boot Starter断言行（web/data-jpa/security/validation/test），testStarterShouldHaveTestScope移除整条测试方法（test starter已在dependencyManagement中删除）

---

## R6 PASSED BaseEntityTest审计字段自动填充验证及回归测试修复（T11）
结果：修改3个测试文件（BaseEntityAuditTest/@SpringBootApplication内部配置类、CommonPomTest/断言值3→5+移除validation断言、ParentPomTest/移除5条Starter断言行+移除test scope方法）
测试：98 passed, 0 failed, 5 skipped（verify_v8.md PASSED）

---

## R7 PASSED FallbackAiService日志时机修复（T10）
结果：构造器末尾增加ERROR日志；移除firstEmptyDelegateCall字段；handleEmptyDelegates()始终WARN；测试重构为shouldLogErrorOnConstruction()+shouldLogWarnOnSubsequentCalls()
测试：29 passed, 0 failed（verify_v9.md PASSED）

