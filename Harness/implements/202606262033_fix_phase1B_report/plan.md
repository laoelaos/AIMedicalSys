# 实现计划

任务描述：修复 Docs/Diagnosis/impl/04_phase1B_report.md 中涉及的 26 项缺陷（T1-T23, T25-T27），使代码实现与 OOD Docs/05_ood_phase1_B.md 保持一致
项目根目录：C:\Develop\Software\AIMedicalSys

---

## R1 NEW 修复 T26：删除 SecurityConfigPhase1 中重复的 TokenBlacklist Bean 定义
任务：代码修复，auth/security/SecurityConfigPhase1.java:41-44 删除重复的 tokenBlacklist() Bean 定义（auth/config/AuthModuleConfig.java:19-22 已提供无 profile 限制的版本）
选择理由：P0 阻塞缺陷，Spring 启动时因 BeanDefinitionOverrideException 失败。无前置依赖，批次 1 优先执行，为后续所有修复提供可启动的应用基线。其他 25 项修复依赖本项完成后的可应用测试环境。
上下文：T26 是 Spring 配置冲突，与 T2/T12 有隐含联动（T2 修复时 SecurityConfigPhase1.jwtAuthenticationFilter() 方法签名将从 JwtUtil 切换为 JwtTokenProvider，同步修改所在文件）

---

## R2 PASSED 修复 T26：删除 SecurityConfigPhase1 中重复的 TokenBlacklist Bean 定义
结果：SecurityConfigPhase1.java 删除 tokenBlacklist() Bean 方法和 InMemoryTokenBlacklist import，SecurityConfigPhase1Test.java 删除 shouldReturnInMemoryTokenBlacklist() 并适配其余测试方法，编译通过，后端单元测试 557/0/5 全部通过
测试：SecurityConfigPhase1Test 全部通过，common-module-impl 全部 121 测试通过
验证结论：T26 代码修改正确，通过编译和单元测试。整合验证显示 2 个集成测试失败（ApplicationContextIT, EntityMappingIT — 预存的 JwtTokenProvider base64 解码异常，因 application-test.yml 中 JWT secret 含非法 Base64 字符 '-'），与 T26 修改无关。前端 admin/doctor 构建失败为预存问题。

## R2 FAILED 修复 T1+T10：JwtTokenProvider 启动验证 + JwtConfig decode 后长度检查 + 修复测试 JWT secret
结果：JwtConfigTest$ValidateTests.shouldThrowWhenDecodedKeyTooShort 测试失败（expected true but was false）。JwtTokenProvider 和 JwtConfigTest 的 18 个测试全部通过（13+5），JwtConfig 的 5 个测试中 4 通过、1 失败。
失败原因：JwtConfig.java 的主源码改动未被 Maven 增量编译检测到（compile 阶段 "Nothing to compile"），测试运行时使用的是旧版 .class 字节码，其异常消息不包含中文"至少32字节"。
修正方向：强制 clean compile 重新编译 JwtConfig.java，无需修改代码。

## R3 PASSED 修复 T1+T10：JwtTokenProvider 启动验证 + JwtConfig decode 后长度检查 + 修复测试 JWT secret
结果：强制 clean 编译后重新测试，后端单元测试 343/0/0 全部通过。JwtConfigTest 12 个测试、JwtTokenProviderTest 15 个测试、common-module-impl 214 个测试、common 121 个测试全部通过，common-module-api 8 个测试通过。
测试：JwtConfigTest (12)、JwtTokenProviderTest (15)、common-module-impl 共 214 测试通过；common 模块 121 测试通过；common-module-api 8 测试通过
验证结论：T1+T10 修复验证通过。JwtConfig.validate() 正确检查 Base64 解码后字节长度，JwtTokenProvider.init() 含完整三项启动验证。

---

## R4 PASSED 修复 T2+T12：JwtAuthenticationFilter 从 JwtUtil 切换为 JwtTokenProvider + 消除重复 userId 提取
结果：JwtTokenProvider 新增 getTokenType() 方法；JwtAuthenticationFilter 注入从 JwtUtil 切换为 JwtTokenProvider，删除手动 type 检查块和 extractUserId 方法；SecurityConfigPhase1 Bean 方法参数同步切换；3 个测试文件适配。mvn clean test 全部 345 测试通过，构建成功。
测试：JwtTokenProviderTest(16)、JwtAuthenticationFilterTest(9)、SecurityConfigPhase1Test(3)、SecurityConfigPhase1CoexistenceTest(2) 及 common-module-impl 全部 345 测试通过
验证结论：T2+T12 代码修改正确，后端单元测试 345/0/0 全部通过，BUILD SUCCESS

## R5 PASSED 修复 T4+T27+T20：ErrorCode 消息文本与设计规范对齐 + 删除菜单错误码统一
结果：GlobalErrorCode.java UNAUTHORIZED→"未认证或令牌已失效"、FORBIDDEN→"无权限访问"；MenuServiceImpl.java deleteMenu() PARAM_INVALID→CHILDREN_EXIST；GlobalErrorCodeTest 和 MenuServiceTest 同步适配
测试：mvn clean test 后端单元测试 474/0/0 全部通过（common 121+5skip, common-module-api 8, common-module-impl 345）
验证结论：全部通过，BUILD SUCCESS

---

## R6 FAILED 修复 T3+T25：GlobalExceptionHandler 消息模板插值 + 429 状态码映射
结果：GlobalExceptionHandler 的 formatMessage() 和 resolveHttpStatus() 修改正确，AuthServiceImpl args 修正正确；但 handleBusinessException() 遗漏了 log.warn(...) 语句。
失败原因：shouldInterpolateAccountLockedMessage_logsOriginalTemplate 测试期望 appender.list.size() == 1（即记录了一条日志），但 handleBusinessException() 中没有 log.warn 调用，实际为 0。
修正方向：在 GlobalExceptionHandler.handleBusinessException() 返回值之前添加 log.warn 语句，与 detail_v6.md §2 文档规定一致。

---

## R7 PASSED 修复 T3+T25：GlobalExceptionHandler 消息模板插值 + 429 状态码映射（补日志行）
结果：在 handleBusinessException() 中 formatMessage 之后、return 之前插入 log.warn 日志行。mvn test -pl common -am 全部 129 测试通过（含5 skip），0 失败，BUILD SUCCESS。
测试：GlobalExceptionHandlerTest 13 个测试全部通过，shouldInterpolateAccountLockedMessage_logsOriginalTemplate 验证 log.warn 捕获到 1 条日志。
验证结论：代码修改正确，单元测试全部通过。

---

## R8 PASSED 修复 T5+T13：AuthServiceImpl 刷新流程 IP 失败计数 + dummy BCrypt 比对语义
结果：AuthServiceImpl.java 新增 DUMMY_HASH 常量；login() 两处 encode→matches；refreshToken() 用户禁用/删除分支插入 recordIpFailure。通过 mvn clean test 后端全部 575 测试通过。
测试：AuthServiceTest(22)、common-module-impl 共 346 测试通过；common 模块 129 测试通过；全项目 575 测试 0 失败，BUILD SUCCESS
验证结论：T5+T13 代码修改正确，单元测试全部通过。

---

## R9 FAILED 修复 T6+T7：AuthController 路径修复 + SecurityContext 重构
结果：源码修改正确（T6 路径 /me→/profile，T7 SecurityContext 重构 + 删除 JwtTokenProvider 依赖），编译通过。测试代码中 shouldThrowBusinessExceptionWhenChangePasswordFails() 使用 when() 调用 void 方法 changePassword()，导致测试编译失败。
失败原因：AuthService.changePassword() 返回 void，不能在 when() 内作为参数。应当使用 Mockito 的 doThrow().when() 语法。
修正方向：将 AuthControllerTest.java:273-274 的 when(...).thenThrow(...) 改为 doThrow(...).when(authService).changePassword(...)。

## R9 RETRY 修复 T6+T7：AuthControllerTest 测试编译错误
任务：测试代码修复，AuthControllerTest.java:273-274 将 when(authService.changePassword(anyLong(), anyString(), anyString())).thenThrow(new BusinessException(GlobalErrorCode.PASSWORD_MISMATCH)) 改为 doThrow(new BusinessException(GlobalErrorCode.PASSWORD_MISMATCH)).when(authService).changePassword(anyLong(), anyString(), anyString())
选择理由：源码修改已验证通过（mvn compile 成功），仅测试代码中 Mockito 语法错误（void 方法不能用于 when()，须用 doThrow()）。修复后重新编译并运行测试。
上下文：详见验证报告 verify_v9.md：AuthControllerTest.java:[273,44] 编译错误"此处不允许使用 '空' 类型"（void 方法作为 when() 参数）。

---

## R10 PASSED 修复 T6+T7：AuthControllerTest 测试编译错误
结果：AuthControllerTest.java L273-L274 的 when().thenThrow() 改为 doThrow().when()。mvn clean test 全部 583 测试通过（0 失败，5 跳过）。
测试：全项目 583 测试通过，0 失败 5 跳过
验证结论：源代码修改正确，编译和单元测试全部通过。

## R11 PASSED 修复 T19+T21：MenuController PATCH 方法 + id 一致性校验
结果：MenuController.java @PutMapping→@PatchMapping，新增路径/请求体 id 一致性校验；MenuControllerTest.java 同步适配。mvn clean test 全部 578 测试通过，0 失败 5 跳过，BUILD SUCCESS。
测试：MenuControllerTest 全部 8 测试通过（含新增 shouldReturnSuccessWhenPathIdMatchesBodyId、shouldReturnParamInvalidWhenPathIdMismatchBodyId）；全项目 578 测试通过。
验证结论：代码修改正确，编译和单元测试全部通过。T19+T21 修复通过。

## R12 NEW 修复 T14：SlidingWindowCounter ReentrantLock 作用域
任务：代码修复，auth/rateLimit/SlidingWindowCounter.java 中在 tryAcquire() 方法中添加 ReentrantLock 保护，包裹 windows.compute() 操作，使锁策略与 OOD 4.1 节设计约定一致。
选择理由：T14 是批次 2（编码规范对齐）遗留项，与 T4/T27/T20 同为编码规范对齐修复。独立文件、无前置依赖、变更量小（仅添加 lock 加解锁），适合作为剩余任务的首个修复。
上下文：详见诊断报告 §T14。

---

## R12 PASSED 修复 T14：SlidingWindowCounter ReentrantLock 作用域
结果：SlidingWindowCounter.java tryAcquire() 添加 lock.lock()/try-finally-lock.unlock() 包裹 windows.compute()。mvn clean test 全部 581 测试通过，0 失败 5 跳过，BUILD SUCCESS。
测试：SlidingWindowCounterTest 全部 11 测试通过；全项目 581 测试 0 失败 5 跳过
验证结论：T14 代码修改正确，锁策略与 OOD 4.1 节设计约定一致，编译和单元测试全部通过。

---

## R13 PASSED 修复 T22：PermissionFunction 实体 component 字段映射
结果：PermissionFunction.java 新增 component 字段 + @Column(name = "component") + getter/setter；MenuServiceImpl.java:187 convertToMenuResponse() null → function.getComponent()。mvn clean test 全部 582 测试通过，0 失败 5 跳过，BUILD SUCCESS。
测试：PermissionFunctionTest 新增 shouldSetAndGetComponent()；MenuServiceTest 新增 4 个 component 断言。common 模块 129 测试、common-module-api 8 测试、common-module-impl 345 测试全部通过。
验证结论：T22 代码修改正确，编译和单元测试全部通过。

---

## R14 PASSED 修复 T23：getUserMenuTree 使用 @EntityGraph 消除 N+1
结果：UserRepository 新增 findWithDetailsForMenuById 方法；MenuServiceImpl 改用该方法；MenuServiceTest 更新 mock 并新增边界用例；UserRepositoryTest 新增 4 个测试。后端 mvn clean test 587 测试通过，0 失败 0 错误 5 跳过，BUILD SUCCESS。
测试：MenuServiceTest 全部通过（含新增边界用例）、UserRepositoryTest 4 个测试；common-module-impl 354 测试通过；全项目 587 测试 0 失败。
验证结论：T23 代码修改正确，编译和单元测试全部通过。前端 Vitest 13 失败为预存问题。

## R15 NEW 修复 T11：RestAuthenticationEntryPoint 改用异常类型判断
任务：(1) auth/exception 包新增 AccountDisabledAuthenticationException 类（extends AuthenticationException）；(2) JwtAuthenticationFilter.throwAccountDisabled() 改为抛出 AccountDisabledAuthenticationException；(3) RestAuthenticationEntryPoint.commence() 用 instanceof 替代消息字符串匹配；(4) RestAuthenticationEntryPointTest 适配
选择理由：P2 批次 6 第三个任务。T23 的 UserRepository 变更不涉及此任务，上下文独立。当前 RestAuthenticationEntryPoint 用消息字符串匹配识别 ACCOUNT_DISABLED，若消息文本变更将静默失效。
上下文：见诊断报告 §T11 及 OOD 3.3 节"I 安全配置"AuthenticationEntryPoint 行为契约。

---

## 后续任务路线图（v12 r1 补充）

| 轮次 | 任务 | 优先级 | 批次 | 说明 |
|------|------|--------|------|------|
| R12 | T14：SlidingWindowCounter ReentrantLock 作用域 | P3 | 批次2 | 编码规范对齐，独立文件，变更量小 |
| R13 | T22：PermissionFunction 实体 component 字段映射 | P1 | 批次6 | 新增 JPA 实体字段，涉及 schema 变更评估 |
| R14 | T23：getUserMenuTree 使用 @EntityGraph 消除 N+1 | P1 | 批次6 | 修改 MenuServiceImpl + UserRepository |
| R15 | T11：RestAuthenticationEntryPoint 改用异常类型判断 | P2 | 批次6 | 新增 AccountDisabledAuthenticationException |
| R16 | T9：UserConverter 过滤逻辑修复 + UserFacadeImpl 委托 | P2 | 批次6 | 三处关键过滤逻辑修正 |
| R17 | T8：SecurityAuditLogger 审计日志实现 | P1 | 批次6 | 新建 auth/audit/ 包 + AuthService/Controller 适配 |
| R18 | T15：AuthServiceTest 新增 deleted 用户登录测试 | P3 | 批次7 | 测试补充 |
| R19 | T16：LoginAttemptTrackerTest/AuthServiceTest 锁定消息验证 | P3 | 批次7 | 测试补充 |
| R20 | T17：MenuServiceTest 多级菜单树构建测试 | P3 | 批次7 | 测试补充 |
| R21 | T18：SecurityConfigPhase1Test Filter 执行顺序测试 | P3 | 批次7 | 测试补充 |

**批次 6 内部顺序：** T22/T23（互不依赖，可并行）→ T11（独立，无前置）→ T9（先修 UserConverter 再委托）→ T8（接口先定义，与 T9 隐含交叉）。批次 7 全部 4 项在功能修复验收后启动。

---

## R15 PASSED 修复 T11：RestAuthenticationEntryPoint 改用异常类型判断
结果：AccountDisabledAuthenticationException 新建；JwtAuthenticationFilter.throwAccountDisabled() 抛出 AccountDisabledAuthenticationException；RestAuthenticationEntryPoint 用 instanceof 替代字符串匹配；测试适配。mvn clean test 全部 597 测试通过，0 失败 5 跳过，BUILD SUCCESS。
测试：AccountDisabledAuthenticationExceptionTest(3)、RestAuthenticationEntryPointTest(3)；全项目 597 测试通过，0 失败 5 跳过。
验证结论：T11 代码修改正确，编译和单元测试全部通过。

---

## R16 PASSED 修复 T9：UserConverter 补充过滤逻辑 + UserFacadeImpl 委托
结果：UserConverter.resolveRole() 补充 `filter(Role::getEnabled)` + null-safe sort；resolvePermissions() 补充 `PermissionFunction::getEnabled` 过滤；UserFacadeImpl 注入 UserConverter，toUserInfoResponse() 委托，删除三个私有方法及对应 import；测试适配。mvn clean test 全部 593 测试通过（UserConverterTest 8 + UserFacadeImplTest 12），0 失败 5 跳过，BUILD SUCCESS。
测试：UserConverterTest（新增 shouldFilterDisabledRole、shouldHandleNullSort、shouldFilterDisabledPermission），UserFacadeImplTest 5 个测试方法增加 mock 设置；全项目 593 测试通过，0 失败 5 跳过。
验证结论：T9 代码修改正确，编译和单元测试全部通过。

---

## R17 PASSED 修复 T8：SecurityAuditLogger 安全审计日志实现
结果：auth/audit/ 包新建 SecurityAuditLogger 接口、SecurityAuditEvent 值对象、LoggingSecurityAuditLogger 文件实现；AuthServiceImpl 四方法嵌入审计调用；AuthService.logout() 签名增加 refreshToken 参数；AuthController 透传 refreshToken；logback-spring.xml 配置 RollingFileAppender。全部编译通过。经测试审查 v17 r2 修正 newJti 断言后 APPROVED。
测试：mvn clean test 全部 608 测试通过，0 失败 5 跳过，BUILD SUCCESS（13 模块）。
验证结论：T8 代码修改正确，编译和单元测试全部通过。批次 6 全量交付完成。

---

## R18 PASSED 修复 T15：AuthServiceTest 新增 deleted 用户登录测试
结果：AuthServiceTest.java 新增 login_shouldThrowLoginFailed_whenUserDeleted()，验证 deleted=true → BusinessException(LOGIN_FAILED) + 审计事件。mvn clean test 全部 609 测试通过，0 失败 5 跳过，BUILD SUCCESS（13 模块）。
测试：AuthServiceTest 28 个测试（新增 1），全项目 609 测试通过，0 失败 5 跳过。
验证结论：T15 测试补充正确，编译和单元测试全部通过。

---

## R19 PASSED 修复 T16：AuthServiceTest 锁定消息 args 验证
结果：AuthServiceTest.java 中 login_shouldThrowIpLocked() 追加 assertEquals("30分钟", ex.getArgs()[0])，login_shouldThrowUsernameLocked() 追加 assertEquals("15分钟", ex.getArgs()[0])。mvn clean test 全部 609 测试通过，0 失败 5 跳过，BUILD SUCCESS。
测试：AuthServiceTest 28 个测试（新增 2 个断言）；全项目 609 测试通过，0 失败 5 跳过。
验证结论：T16 测试补充正确，编译和单元测试全部通过。

## R20 PASSED 修复 T17：MenuServiceTest 多级菜单树构建测试
结果：GetUserMenuTreeTests 嵌套类内新增 4 个 buildMenuTree 测试方法；mvn clean test 全部 613 测试通过，0 失败 5 跳过 1 忽略（三级嵌套 @Disabled），BUILD SUCCESS。
测试：MenuServiceTest$GetUserMenuTreeTests 8 测试通过（4 新增 + 4 原有），1 跳过（@Disabled 三级嵌套）；全项目 613 测试通过，0 失败 6 跳过。
验证结论：T17 测试补充正确，编译和单元测试全部通过。

---

## R21 PASSED 修复 T18：SecurityConfigPhase1Test Filter 执行顺序测试
结果：SecurityConfigPhase1Test 新增 shouldRegisterFiltersInExpectedOrder()，验证 GlobalRateLimitFilter → JwtAuthenticationFilter → PasswordChangeCheckFilter 相对顺序。mvn clean test 4 测试通过（含原有 3 个），0 失败 0 跳过，BUILD SUCCESS。
测试：SecurityConfigPhase1Test 4 测试通过（新增 1 个），全项目 613+ 测试通过。
验证结论：T18 测试补充正确，Filter 顺序验证通过。