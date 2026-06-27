# R2-A: 服务层与 Provider 层测试审查 — 测试覆盖率、正确性及与 OOD 设计的一致性

审查时间：2026-06-27

### 审查范围

- `service/AuthServiceTest.java`
- `service/MenuServiceTest.java`
- `auth/jwt/JwtTokenProviderTest.java`
- `jwt/JwtConfigTest.java`
- `jwt/JwtUtilTest.java`
- `jwt/DebugJwtTest.java`
- `auth/UserFacadeImplTest.java`
- `auth/converter/UserConverterTest.java`
- `auth/config/AuthModuleConfigTest.java`

参考设计：`Docs/05_ood_phase1_B.md`，实现源码：`AuthServiceImpl.java`、`JwtTokenProvider.java`、`UserConverter.java`、`AuthModuleConfig.java`

### 发现

#### [一般] AuthServiceTest 存在重复测试方法

- **位置**：`service/AuthServiceTest.java:266` 和 `service/AuthServiceTest.java:291`
- **描述**：`login_shouldThrowUserDeleted`（行 266）和 `login_shouldThrowLoginFailed_whenUserDeleted`（行 291）测试完全相同的业务场景——已删除（deleted=true）用户登录应抛出 LOGIN_FAILED + ACCOUNT_DELETED。两者差异仅在于前者额外 mock 了 `passwordEncoder.matches(anyString(), anyString()).thenReturn(true)`（返回值实际未被业务代码消费，属于无效mock）。断言逻辑完全相同，属于重复测试。
- **建议**：删除 `login_shouldThrowLoginFailed_whenUserDeleted`（行 291-313），或合并为一个测试方法。

#### [一般] JwtTokenProviderTest 缺乏篡改 Token 测试

- **位置**：`auth/jwt/JwtTokenProviderTest.java`
- **描述**：`validateToken` 方法验证签名时依赖 `SecretKey`，但测试中缺少"使用不同密钥签名的 token 应被拒绝"的用例。`validateToken_withExpiredToken_shouldReturnNull`（行 80）仅覆盖了过期场景，未覆盖签名篡改（SignatureException）路径。按代码逻辑，篡改 token 应返回 null，但无测试保障。
- **建议**：新增测试方法，用不同密钥生成 JWT 后调用 `validateToken` 验证返回 null。

#### [一般] UserFacadeImplTest 使用真实 UserConverter 而非 Mock，未完全隔离 Facade 测试

- **位置**：`auth/UserFacadeImplTest.java:26`
- **描述**：测试使用 `new UserConverter()` 真实转换器实例。虽能验证"委托 UserConverter"的设计要求，但使 Facade 测试与 Converter 实现耦合：若 `UserConverter.toUserInfoResponse()` 行为变更，UserFacadeImplTest 会连锁失败，无法单独定位问题。OOD 设计明确要求"UserFacadeImpl.toUserInfoResponse() 必须委托 UserConverter"，当前测试方式虽验证了委托事实，但粒度偏粗。
- **建议**：引入 `@Mock UserConverter` 并用 `when(userConverter.toUserInfoResponse(any())).thenReturn(...)` 隔离。同时保留当前方式作为集成验证，或依赖 UserConverterTest 单独覆盖转换逻辑。

#### [一般] DebugJwtTest.java 为 main 方法调试类而非单元测试

- **位置**：`jwt/DebugJwtTest.java`
- **描述**：该文件包含 `public static void main(String[] args)` 方法，没有 `@Test` 注解，不属于 JUnit 测试类。放置在 `src/test/java` 下可能被构建工具误识别为测试类。且文件名为 `DebugJwtTest`，按命名规范应改为 `DebugJwtMain` 或移入 `src/main` 下的工具包。
- **建议**：将文件重命名并移入 `src/main` 下的 `util` 或 `tool` 包，或在文件名和类名上明确 `Main` 后缀，避免被 Maven Surefire 插件识别为测试类。

#### [轻微] AuthServiceTest 中无效 mock：deleted 测试的 passwordEncoder 返回值未被消费

- **位置**：`service/AuthServiceTest.java:272`
- **描述**：`login_shouldThrowUserDeleted`（行 266）中对 `passwordEncoder.matches(anyString(), anyString()).thenReturn(true)` 的 mock 是无效的。在 deleted 分支中 `passwordEncoder.matches("dummy", DUMMY_HASH)` 的返回值未被业务代码消费（仅用于时序混淆），该 mock 不贡献于测试验证。类似的模式也出现在 `login_shouldThrowUserDisabled`（行 196）。
- **建议**：移除 `when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)` 行，或使用 `verify(passwordEncoder).matches(eq("dummy"), anyString())` 替代 `when` 来验证 dummy BCrypt 被调用即可。

#### [轻微] AuthServiceTest changePassword 测试未覆盖 passwordChangeService.clearChangeRequired 验证

- **位置**：`service/AuthServiceTest.java:806-830`
- **描述**：`changePassword_shouldSucceed` 验证了 `passwordChangeService.clearChangeRequired(1L)` 被调用（行 820），但 `changePassword_shouldThrowOnOldPasswordMismatch`（行 833）和 `changePassword_shouldThrowOnPolicyViolation`（行 843）未验证失败路径下 `clearChangeRequired` 和 `SecurityContextHolder.clearContext()` 不会被调用。
- **建议**：在失败路径测试中添加 `verify(passwordChangeService, never()).clearChangeRequired(any())` 验证。

#### [轻微] MenuServiceTest 三级嵌套菜单测试被 @Disabled

- **位置**：`service/MenuServiceTest.java:217`
- **描述**：`buildMenuTree_shouldSupportThreeLevelHierarchy` 被 `@Disabled` 标记，原因注释为"三级嵌套受 MenuResponse 不可变 record 限制暂不可用"。这是已知的设计限制被文档化标记，但涉及核心菜单树功能的重要测试被禁用。
- **建议**：在 issue 跟踪系统中记录此限制，并在修复后及时启用测试。

### 覆盖完整性评价

| 审查重点 | 覆盖状态 | 说明 |
|----------|---------|------|
| 登录成功路径 | ✅ 完整 | login_shouldSucceed + login_shouldSetPasswordChangeRequired |
| 登录失败路径 | ✅ 完整 | 限流/IP锁定/用户名锁定/不存在/禁用/删除/密码错误 7 条全覆盖 |
| 双维度锁定检测 | ✅ 完整 | IP_LOCKED + USERNAME_LOCKED 分离测试，含审计事件验证 |
| dummy BCrypt | ✅ 验证 | 用户不存在/禁用/删除场景均验证 `passwordEncoder.matches(eq("dummy"), ...)` |
| tokenVersion 刷新检查 | ✅ 完整 | 版本匹配成功 + 版本不匹配 + 版本未找到（TokenVersionNotFound） |
| 异常刷新检测 | ✅ 完整 | 超限阻断 + 未超限放行 + 过期条目清理后放行 + 混合场景 |
| 登出审计 | ✅ 完整 | 正常登出 + token null 不审计 + token 无效审计 + refreshTokenMasked 验证 |
| 密码变更 tokenVersion 递增 | ✅ 验证 | assertEquals(2, testUser.getTokenVersion()) 在 changePassword_shouldSucceed 中 |
| 用户不存在 | ✅ 覆盖 | login + refreshToken + getCurrentUser |
| 用户禁用 | ✅ 覆盖 | login + refreshToken |
| 用户删除 | ✅ 覆盖 | login（重复） + refreshToken |
| 账户锁定 | ✅ 覆盖 | IP 锁定 + 用户名锁定 + refresh 锁定 |
| 密码过期 | ✅ 覆盖 | login passwordChangeRequired + refresh PasswordChangeRequiredException |
| Mock 合理性 | ⚠️ 良好但有冗余 | 外部依赖均已 mock，但 deleted 测试含无效 mock，UserFacadeImplTest 使用 real converter |
| 测试命名清晰度 | ✅ 良好 | camelCase 方法名 + @DisplayName 中文描述 |
| 断言具体性 | ✅ 良好 | 验证 errorCode、event 字段、mock 调用参数等 |
| JWT 生成 token | ✅ 覆盖 | generateAccessToken + generateRefreshToken |
| JWT 验证 token | ✅ 覆盖 | 正确 token、type 校验、过期 token |
| JWT 篡改 token | ❌ 缺失 | 无不同密钥签名 token 的拒绝测试 |
| UserConverter 角色排序取首个 | ✅ 覆盖 | sort=1 优先于 sort=2 |
| UserConverter null sort | ✅ 覆盖 | sort=null 角色被正确处理（不 NPE） |
| UserConverter 已禁用角色过滤 | ✅ 覆盖 | disabled role 被排除 |
| UserConverter 已禁用权限过滤 | ✅ 覆盖 | disabled permission 被排除 |
| UserFacadeImpl 委托 UserConverter | ✅ 验证 | 使用真实 UserConverter 实例验证委托 |
| AuthModuleConfig Bean 创建 | ✅ 覆盖 | 4 个 @Bean 均验证非 null |
| @EntityGraph N+1 测试验证 | ❌ 未覆盖 | 可选要求，测试层未验证 |

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 4 |
| 轻微 | 4 |

### 总评

测试代码整体质量较高，覆盖了 OOD 设计第 3 节规定的全部主要业务流程和边界条件。AuthServiceTest 是亮点：15 个测试方法覆盖了登录的 7 条失败路径、刷新 token 的 9 种场景、登出的 4 种变体、密码变更的正反路径，且每个方法都精细验证了审计事件的字段正确性。Mock 使用总体合理，外部依赖被 mock 而业务逻辑得以保留。测试命名和断言质量达到生产级标准。

存在的主要问题是：(1) 一条重复测试（deleted 登录失败），浪费维护成本；(2) JwtTokenProviderTest 缺少篡改 Token 测试；(3) UserFacadeImplTest 使用 real converter 而非 mock，降低了隔离性；(4) DebugJwtTest 是 main 类而非测试类，位置不当。上述问题均为一般/轻微级别，不影响当前测试套件的有效性和覆盖率。

JwtUtilTest 和 JwtConfigTest 设计清晰、覆盖全面。
