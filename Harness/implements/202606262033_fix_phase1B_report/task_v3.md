# 任务指令（v3）

## 动作
RETRY

## 任务描述
修复 T1+T10：强制重新编译 JwtConfig.java + JwtTokenProvider.java 并验证单元测试通过。

## 选择理由
T1+T10 的源码已在磁盘上修改正确（JwtConfig.java 含 Base64 decode + 字节长度校验，JwtTokenProvider.java 含完整启动验证，application-test.yml 已使用合法 Base64 secret，测试文件已适配），但 R2 验证时 Maven 增量编译未能检测到 JwtConfig.java 的变更（compile 阶段输出 "Nothing to compile - all classes are up to date"），测试运行的是旧版 .class 字节码，导致 JwtConfigTest$ValidateTests.shouldThrowWhenDecodedKeyTooShort 因异常消息不匹配而失败（旧版 throw 的消息不含中文"至少32字节"）。

## 任务上下文
### 失败详情
- 测试：JwtConfigTest$ValidateTests.shouldThrowWhenDecodedKeyTooShort:115
- 断言：`assertTrue(ex.getMessage().contains("至少32字节"))` → expected: true, but was: false
- 原因：JwtConfig.class 未重新编译，仍为旧版字节码（检查字符串长度而非解码后字节长度）
- 证据：Maven compile 输出 "Nothing to compile"，test-compile 输出 "Changes detected"（仅测试源码重新编译）

### 涉及文件（源码已正确，无需修改）
| 文件 | 路径 | 当前状态 |
|------|------|----------|
| JwtConfig.java | common-module-impl/.../jwt/JwtConfig.java | ✅ `validate()` 含 Base64 decode + 字节长度≥32检查 |
| JwtTokenProvider.java | common-module-impl/.../auth/jwt/JwtTokenProvider.java | ✅ `init()` 含 null/空/字符集/decode异常/字节长度检查 |
| application-test.yml | integration/src/test/resources/application-test.yml | ✅ secret 已改为合法 Base64 |
| JwtConfigTest.java | common-module-impl/.../jwt/JwtConfigTest.java | ✅ 适配解码后字节长度检查测试 |
| JwtTokenProviderTest.java | common-module-impl/.../auth/jwt/JwtTokenProviderTest.java | ✅ 新增 5 个启动验证测试 |

## 验证标准
1. `mvn clean compile -pl :common-module-impl` 强制重新编译成功
2. `mvn test -pl :common-module-impl` 全部通过（342 tests, 0 failures）
3. `mvn test -pl :common-module-impl -Dtest=JwtConfigTest` 通过全部 12 个 JwtConfigTest
4. 重点验证 `shouldThrowWhenDecodedKeyTooShort` 断言通过
5. 集成测试阶段可启用 `-pl integration` 验证 ApplicationContextIT 和 EntityMappingIT 通过

## RETRY 说明
**失败原因摘要**：JwtConfig.java 的主源码改动未被 Maven 增量编译捕获，测试运行时使用旧版字节码。

**修正方向**：
1. `mvn clean compile -pl :common-module-impl -am` — 强制清理并重编译 common-module-impl 及其依赖
2. `mvn test -pl :common-module-impl` — 运行全部单元测试验证
3. 若仍失败，检查 JwtConfig.java 的 `validate()` 方法是否确实包含 Base64 decode + 字节长度检查逻辑（参考 detail_v2.md 行为契约）
4. 若编译/测试均通过，验收标准达成。
