# 任务指令（v7）

## 动作
RETRY

## 任务描述
**R5 RETRY T17**: MessageInterpolator 组件 — 修正验证命令

已有代码全部正确实现，无需修改任何源文件。仅需使用正确的 Maven 命令从项目根目录验证构建。

## 选择理由
R5（T17: MessageInterpolator 组件抽取）的代码实现完全正确：
- `MessageInterpolator` 接口在 `common` 模块中 ✓
- `SimpleMessageInterpolator` 组件在 `common` 模块中 ✓
- `GlobalExceptionHandler` 注入 `MessageInterpolator` ✓
- `RestAuthenticationEntryPoint` 注入 `MessageInterpolator` ✓
- `RestAccessDeniedHandler` 注入 `MessageInterpolator` ✓
- `SecurityConfigPhase1.filterChain()` 接受 `MessageInterpolator` 参数 ✓
- 5 个测试文件已全部适配 ✓

验证失败原因：验证命令从 `modules/common-module` 聚合器目录运行，而非项目根目录。该聚合器只包含 `common-module-api` 和 `common-module-impl` 两个子模块，`common` 模块不在 reactor 中，导致 `common-module-impl` 测试编译时从本地 Maven 仓库解析到旧版 `common` JAR（不含 `MessageInterpolator`），出现 9 个编译错误。

从根目录运行 `mvn test -pl common,modules/common-module -am` 时，`-am`（also-make）标志会将 `common` 模块加入 reactor 并优先编译，正确解析 `MessageInterpolator` 类。

## 任务上下文
### 工作目录
`C:\Develop\Software\AIMedicalSys\AIMedical\backend`（项目根目录的 pom.xml 位置）

### 验证命令
1. **编译验证**：`mvn compile -pl common,modules/common-module -am`
2. **测试验证**：`mvn test -pl common,modules/common-module -am`

### 预期结果
- `common` 模块：136 tests pass, 0 failures, 5 skipped
- `common-module-impl`：391 tests pass, 0 failures, 1 skipped
- 无需修改任何源文件

### 现有文件清单（无需修改）
| 文件路径 | 操作 |
|---------|------|
| `common/src/main/java/com/aimedical/common/util/MessageInterpolator.java` | 已新建 |
| `common/src/main/java/com/aimedical/common/util/SimpleMessageInterpolator.java` | 已新建 |
| `common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java` | 已修改 |
| `common-module-impl/src/main/java/.../auth/security/RestAuthenticationEntryPoint.java` | 已修改 |
| `common-module-impl/src/main/java/.../auth/security/RestAccessDeniedHandler.java` | 已修改 |
| `common-module-impl/src/main/java/.../auth/security/SecurityConfigPhase1.java` | 已修改 |
| `common/src/test/java/.../config/GlobalExceptionHandlerTest.java` | 已修改 |
| `common-module-impl/src/test/java/.../auth/security/RestAuthenticationEntryPointTest.java` | 已修改 |
| `common-module-impl/src/test/java/.../auth/security/RestAccessDeniedHandlerTest.java` | 已修改 |
| `common-module-impl/src/test/java/.../auth/security/SecurityConfigPhase1Test.java` | 已修改 |

## RETRY 说明
失败原因摘要：验证构建命令错误 — `mvn test` 从 `modules/common-module/pom.xml` 运行（仅 3 模块 reactor），未包含 `common` 模块，导致 `common-module-impl` 测试编译时 9 个 "找不到符号 MessageInterpolator" 错误。

修正方向：使用正确的 Maven 命令 `mvn test -pl common,modules/common-module -am` 从根目录 `backend/pom.xml` 运行，`-am` 标志会自动将 `common` 模块加入 reactor 并优先构建。
