# 任务指令（v14）

## 动作
RETRY

## 任务描述
修复 T9 遗留的 8 个编译错误，解除 prescription 模块构建阻断，使全量测试（含 T10 辅助开方子域 19 个测试文件）可执行通过。

## 选择理由
T10（prescription 辅助开方子域）在 v13 验证中因 T9 测试文件编译错误被阻断，R12/R13 连续两次失败均非 T10 代码问题。本次为第 2 次重试，需彻底修复底层编译问题。

## 任务上下文

### 编译错误详情（8 errors）

**错误组 1：Rule 测试构造器参数不匹配（3 errors）**

v11 中 Rule 生产代码重构，构造函数不再接受 `ObjectMapper` 参数（改为内联创建），但测试代码未同步更新：

| 文件 | 行号 | 当前构造调用 | 正确构造 |
|------|------|-------------|---------|
| `DuplicateCheckRuleTest.java` | 32 | `new DuplicateCheckRule(repository, objectMapper)` | `new DuplicateCheckRule(repository)` |
| `ContraindicationCheckRuleTest.java` | 33 | `new ContraindicationCheckRule(repository, objectMapper)` | `new ContraindicationCheckRule(repository)` |
| `AllergyCheckRuleTest.java` | 35 | `new AllergyCheckRule(drugAllergyMappingRepository, objectMapper)` | `new AllergyCheckRule(drugAllergyMappingRepository)` |

同时删除各文件中不再使用的 `objectMapper` 字段（DuplicateCheckRuleTest:27, ContraindicationCheckRuleTest:28, AllergyCheckRuleTest:30）。

**错误组 2：PrescriptionAuditControllerTest 调用 Result.isSuccess()（5 errors）**

`Result<T>` 类（`common/src/main/java/com/aimedical/common/result/Result.java`）不存在 `isSuccess()` 方法。测试文件在以下位置调用了不存在的方法：

| 文件 | 行号 | 当前断言 | 修改后断言 |
|------|------|---------|-----------|
| `PrescriptionAuditControllerTest.java` | 51 | `assertTrue(result.getBody().isSuccess())` | `assertEquals("SUCCESS", result.getBody().getCode())` |
| `PrescriptionAuditControllerTest.java` | 66 | `assertFalse(result.getBody().isSuccess())` | `assertNotEquals("SUCCESS", result.getBody().getCode())` |
| `PrescriptionAuditControllerTest.java` | 81 | `assertTrue(result.getBody().isSuccess())` | `assertEquals("SUCCESS", result.getBody().getCode())` |
| `PrescriptionAuditControllerTest.java` | 94 | `assertFalse(result.getBody().isSuccess())` | `assertNotEquals("SUCCESS", result.getBody().getCode())` |
| `PrescriptionAuditControllerTest.java` | 107 | `assertFalse(result.getBody().isSuccess())` | `assertNotEquals("SUCCESS", result.getBody().getCode())` |

### 修改文件清单
| 操作 | 文件路径 |
|------|---------|
| 修改 | `prescription/src/test/java/com/aimedical/modules/prescription/rule/DuplicateCheckRuleTest.java` |
| 修改 | `prescription/src/test/java/com/aimedical/modules/prescription/rule/ContraindicationCheckRuleTest.java` |
| 修改 | `prescription/src/test/java/com/aimedical/modules/prescription/rule/AllergyCheckRuleTest.java` |
| 修改 | `prescription/src/test/java/com/aimedical/modules/prescription/api/PrescriptionAuditControllerTest.java` |

## 验证标准
1. 4 个修改后的测试文件编译通过
2. `mvn test -Djacoco.skip=true -Djacoco.skip.check=true` 全量测试 0 失败
3. prescription 模块 T9（8 tests）+ T10（~19 tests）全部通过
4. 所有其他模块无回归

## RETRY 说明
v13 验证 FAILED：POM 测试已修复（通过），但 prescription 模块 T9 测试代码存在 8 个编译错误（3 个 Rule 构造器参数不匹配 + 5 个 `Result.isSuccess()` 不存在），构建阻断未解除。

修正方向：修复上述 4 个测试文件中的 8 个编译错误，不做任何生产代码变更。
