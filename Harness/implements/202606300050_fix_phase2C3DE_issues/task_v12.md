# 任务指令（v12）

## 动作
RETRY

## 任务描述
修复 medical-record 模块 3 个测试文件的编译错误，使模块构建通过：
- `MissingFieldDetectorImplTest.java` — 适配 MedicalRecordConverter 构造参数变更（R11 M11 引入）
- `MedicalRecordControllerTest.java` — 修复 `result.isSuccess()` → `"SUCCESS".equals(result.getCode())`（预存）
- `MedicalRecordServiceImplTest.java` — StubAiService 返回类型对齐 AiService 接口（预存）

## 选择理由
R11 生产代码修改（M04-M07/M09-M11）全部正确。验证 FAILED 源于 3 个测试文件编译错误——1 个由 R11 引入（MissingFieldDetectorImplTest），2 个在 R11 前已存在但被先前轮次（仅跑 consultation/ai-api 模块）掩盖。

## 任务上下文

### 需求来源
实现报告 `Docs/Diagnosis/impl/06_phase2C3DE_report.md` 中病历模块 7 项问题（M04-M07, M09-M11）

### 失败原因分析

| 文件 | 行号 | 错误 | 引入者 | 修正方案 |
|------|------|------|--------|---------|
| `MissingFieldDetectorImplTest.java` | 22 | `new MedicalRecordConverter()` 无参 → 需 ObjectMapper | R11 M11 | 改为 `new MedicalRecordConverter(new ObjectMapper())` |
| `MedicalRecordControllerTest.java` | 30,46,62 | `result.isSuccess()` 不存在 | 预存（Result 类无 isSuccess） | 改为 `"SUCCESS".equals(result.getCode())` |
| `MedicalRecordServiceImplTest.java` | 306-341 | StubAiService 方法返回 `CompletableFuture<X>` 而非 `CompletableFuture<AiResult<X>>` | 预存（AiService 返回类型 AiResult 化未同步） | 将所有 stub 方法返回类型包装为 `CompletableFuture<AiResult<X>>`; 新增 `discussionConclusion` 方法；类声明改为非 abstract |

### 已有代码上下文

**MissingFieldDetectorImplTest.java:22**:
```java
private final MedicalRecordConverter converter = new MedicalRecordConverter();
// R11 M11 后需：
private final MedicalRecordConverter converter = new MedicalRecordConverter(new ObjectMapper());
```
需新增 import: `com.fasterxml.jackson.databind.ObjectMapper`

**MedicalRecordControllerTest.java:30/46/62**:
```java
// 当前：
assertFalse(result.isSuccess());  // 无此方法
assertTrue(result.isSuccess());   // 无此方法

// 改为：
assertEquals("SUCCESS", result.getCode());
assertNotEquals("SUCCESS", result.getCode());
```

**MedicalRecordServiceImplTest.java** (StubAiService, L298-342):
```java
private static class StubAiService implements AiService {
    CompletableFuture<AiResult<MedicalRecordGenResponse>> resultFuture;

    @Override
    public CompletableFuture<AiResult<MedicalRecordGenResponse>> generateMedicalRecord(...) {
        return resultFuture;
    }

    // 以下 12 个方法返回类型当前为 CompletableFuture<X>
    // 需改为 CompletableFuture<AiResult<X>>
    @Override
    public CompletableFuture<AiResult<TriageResponse>> triage(...) { return null; }
    // ... 所有方法同理，均返回 CompletableFuture<AiResult<X>>
    // 新增 discussionConclusion 方法
}
```

### 涉及文件（均位于 test 目录）
| 文件路径 | 修改内容 |
|---------|---------|
| `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/detector/MissingFieldDetectorImplTest.java` | MedicalRecordConverter 构造传递 ObjectMapper |
| `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/api/MedicalRecordControllerTest.java` | isSuccess() → getCode() 判断 |
| `AIMedical/backend/modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/service/impl/MedicalRecordServiceImplTest.java` | StubAiService 返回类型 AiResult 化，新增 discussionConclusion |

## RETRY 说明
R11 生产代码正确（已验证 `mvn compile -pl modules/medical-record -am -q` 通过），仅测试文件编译失败：
1. **R11 引入**: MissingFieldDetectorImplTest 未同步 MedicalRecordConverter 构造参数变更
2. **预存**: MedicalRecordControllerTest 依赖 Result.isSuccess() 但 Result 类仅有 getCode()
3. **预存**: MedicalRecordServiceImplTest.StubAiService 未同步 AiService 接口的 AiResult 化

仅修改测试文件，不动生产代码。
