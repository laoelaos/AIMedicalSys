# 详细设计（v14）

## 概述

修复 prescription 模块中 4 个测试文件的 8 个编译错误，解除 T9 测试构建阻断，使全量测试（含 T10 辅助开方子域 ~19 个测试）可执行通过。不涉及任何 production 代码修改。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/rule/DuplicateCheckRuleTest.java` | 修改 | 删除 ObjectMapper 字段/import，构造函数调用移除第二个参数 |
| `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/rule/ContraindicationCheckRuleTest.java` | 修改 | 同上 |
| `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/rule/AllergyCheckRuleTest.java` | 修改 | 同上 |
| `AIMedical/backend/modules/prescription/src/test/java/com/aimedical/modules/prescription/api/PrescriptionAuditControllerTest.java` | 修改 | 5 处 `isSuccess()` 调用替换为 `getCode()` 断言 |

## 修改说明

### 错误组 1：Rule 测试构造器参数不匹配（3 files × 2 changes = 6 edits）

生产代码中 `DuplicateCheckRule`、`ContraindicationCheckRule`、`AllergyCheckRule` 的构造函数不再接收 `ObjectMapper` 参数（已内联创建），测试代码需同步。

#### DuplicateCheckRuleTest.java

**第 8 行**（删除 import）：
```java
// 删除这一行：
import com.fasterxml.jackson.databind.ObjectMapper;
```

**第 27 行**（删除字段）：
```java
// 删除这一行：
private final ObjectMapper objectMapper = new ObjectMapper();
```

**第 32 行**（修改构造调用）：
```java
// 修改前：
rule = new DuplicateCheckRule(repository, objectMapper);
// 修改后：
rule = new DuplicateCheckRule(repository);
```

#### ContraindicationCheckRuleTest.java

**第 9 行**（删除 import）：
```java
// 删除这一行：
import com.fasterxml.jackson.databind.ObjectMapper;
```

**第 28 行**（删除字段）：
```java
// 删除这一行：
private final ObjectMapper objectMapper = new ObjectMapper();
```

**第 33 行**（修改构造调用）：
```java
// 修改前：
rule = new ContraindicationCheckRule(repository, objectMapper);
// 修改后：
rule = new ContraindicationCheckRule(repository);
```

#### AllergyCheckRuleTest.java

**第 11 行**（删除 import）：
```java
// 删除这一行：
import com.fasterxml.jackson.databind.ObjectMapper;
```

**第 30 行**（删除字段）：
```java
// 删除这一行：
private final ObjectMapper objectMapper = new ObjectMapper();
```

**第 35 行**（修改构造调用）：
```java
// 修改前：
rule = new AllergyCheckRule(drugAllergyMappingRepository, objectMapper);
// 修改后：
rule = new AllergyCheckRule(drugAllergyMappingRepository);
```

### 错误组 2：PrescriptionAuditControllerTest 调用 Result.isSuccess()（1 file × 5 edits）

`Result<T>`（`common/.../result/Result.java`）不存在 `isSuccess()` 方法。通过 `getCode()` 判断：成功时 code = "SUCCESS"，失败时 code ≠ "SUCCESS"。

| 行号 | 当前代码 | 修改后代码 |
|------|---------|-----------|
| 51 | `assertTrue(result.getBody().isSuccess())` | `assertEquals("SUCCESS", result.getBody().getCode())` |
| 66 | `assertFalse(result.getBody().isSuccess())` | `assertNotEquals("SUCCESS", result.getBody().getCode())` |
| 81 | `assertTrue(result.getBody().isSuccess())` | `assertEquals("SUCCESS", result.getBody().getCode())` |
| 94 | `assertFalse(result.getBody().isSuccess())` | `assertNotEquals("SUCCESS", result.getBody().getCode())` |
| 107 | `assertFalse(result.getBody().isSuccess())` | `assertNotEquals("SUCCESS", result.getBody().getCode())` |

无需新增 import，`assertEquals` / `assertNotEquals` 已由 `import static org.junit.jupiter.api.Assertions.*;` 覆盖。

## 错误处理

不涉及错误处理修改。所有修改仅涉及测试代码的构造调用和断言方法，生产代码逻辑不受影响。

## 行为契约

- 4 个测试文件编译通过，零 warning
- `DuplicateCheckRuleTest` 3 个用例全部通过
- `ContraindicationCheckRuleTest` 5 个用例全部通过
- `AllergyCheckRuleTest` 8 个用例全部通过
- `PrescriptionAuditControllerTest` 6 个用例全部通过
- prescription 模块 T9（8 tests）+ T10（~19 tests）全部通过
- 全量 `mvn test -Djacoco.skip=true -Djacoco.skip.check=true` 无回归

## 依赖关系

| 文件 | 依赖 |
|------|------|
| DuplicateCheckRuleTest | `DuplicateCheckRule`（prescription 模块） |
| ContraindicationCheckRuleTest | `ContraindicationCheckRule`（prescription 模块） |
| AllergyCheckRuleTest | `AllergyCheckRule`（prescription 模块） |
| PrescriptionAuditControllerTest | `Result`（common 模块） |

## 修订说明（v14 R1）
| 审查意见 | 修改措施 |
|---------|---------|
| 首轮设计 | 初始设计 |
