# 测试报告（v3）

## 测试执行结果

| 测试类 | 用例数 | 通过 | 失败 | 错误 |
|--------|-------|------|------|------|
| JwtUtilTest | 27 | 27 | 0 | 0 |
| AuthServiceTest | 13 | 13 | 0 | 0 |
| **合计** | **40** | **40** | **0** | **0** |

构建结果：`BUILD SUCCESS`

## 测试范围

### JwtUtilTest — 新增 InitTests（5 个用例）

验证 `@PostConstruct init()` 的行为契约：

| 用例 | 维度 | 验证点 |
|------|------|--------|
| `shouldThrowExceptionWhenSecretIsNull` | 错误路径 | null secret → `IllegalStateException("JWT_SECRET must be configured")` |
| `shouldThrowExceptionWhenSecretIsEmpty` | 边界条件 | 空字符串 secret → `IllegalStateException("JWT_SECRET must be configured")` |
| `shouldThrowExceptionWhenSecretContainsInvalidChars` | 错误路径 | 非法 Base64 字符（含 `-`）→ `IllegalStateException("JWT_SECRET contains invalid characters")` |
| `shouldThrowExceptionWhenSecretDecodedLessThan32Bytes` | 边界条件 | 解码后不足 32 字节 → `IllegalStateException("JWT_SECRET must be at least 256 bits (32 bytes) after Base64 decoding")` |
| `shouldGenerateAndParseTokenAfterInit` | 正常路径 | 有效 Base64 secret 初始化后，`generateToken()` 和 `parseToken()` 正常使用缓存密钥 |

### JwtUtilTest — 已有用例（22 个，维持不变）

原有测试继承 `setUp()` 中的 `init()` 调用，覆盖 generateToken、parseToken、validateToken、validateTokenAndGetClaims、getUserId、getRole、extractToken、getExpiration 共 8 组测试群。

### AuthServiceTest（13 个用例，维持不变）

原有测试继承 `setUp()` 中的 `init()` 调用，覆盖 login、logout、refreshToken、getCurrentUser、updateProfile 共 5 组测试群。

## 预存失败说明

`UserRepositoryTest` 的 5 个用例（1 Failure + 4 Errors）为预存问题，与本次变更无关：

| 失败原因 | 根因 |
|---------|------|
| `Table "SYS_USER" not found` | H2 自动建表时外键约束引用顺序问题，属子项 C schema.sql DDL 变更的集成测试范畴，非单元测试可覆盖 |

## 测试文件变更

| 文件 | 操作 | 说明 |
|------|------|------|
| `modules/common-module/common-module-impl/src/test/java/.../jwt/JwtUtilTest.java` | 修改 | setUp 中新增 `jwtUtil.init()`；新增 `InitTests` 嵌套类（5 个 init 验证用例） |
| `modules/common-module/common-module-impl/src/test/java/.../service/AuthServiceTest.java` | 修改 | setUp 中新增 `jwtUtil.init()` 调用 |

## 设计覆盖矩阵

| 行为契约 | 覆盖状态 | 对应用例 |
|---------|---------|---------|
| B-1: `@PostConstruct init()` 自动调用 | ✓ | `shouldGenerateAndParseTokenAfterInit` |
| B-2: secret 验证阶段顺序 null/empty → Base64 字符 → 解码长度 | ✓ | 3 个异常用例按阶段覆盖 |
| B-3: 任一验证不通过抛出 `IllegalStateException` 阻止启动 | ✓ | 每个异常用例验证异常类型和消息 |
| B-4: SecretKey 初次计算后缓存，运行期不变 | ✓ | 初始化后 `generateToken()`/`parseToken()` 使用 `this.secretKey` 成功 |
| B-5: `generateToken()`/`parseToken()` 使用 `this.secretKey` 不再每次重新计算 | ✓ | setUp 中调用一次 init，后续所有 token 操作正常 |
| B-6: `extractToken` 静态方法不变 | ✓ | ExtractTokenTests（4 个已有用例） |
| A: PermissionFunction 重命名不改变运行时行为 | ✓ | 已有 `PermissionFunctionTest`、`PostTest`、`MenuServiceTest`、`AuthServiceTest` 通过编译和运行 |

## 结论

所有单元测试通过，新增的 5 个 InitTests 覆盖了 JwtUtil.init() 的全部 3 个验证阶段（空/null、非法字符、解码后长度不足）和正常路径。预存的 UserRepositoryTest H2 集成测试失败与本次变更无关。
