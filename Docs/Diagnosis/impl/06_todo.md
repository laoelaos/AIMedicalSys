# Phase 2 C/3/DE 验收遗留问题 TODO（v1）

## 文档元信息

- **编写时间**：2026-06-30
- **诊断依据**：`Docs/Diagnosis/impl/06_phase2C3DE_report.md`（P0—P2 共 60+ 项）
- **路线表依据**：`Harness/implements/202606300050_fix_phase2C3DE_issues/plan.md`（R1—R30）
- **验证方式**：代码层静态比对 + `mvn clean test` 全量构建
- **修复进度**：28 项已修复；本文件清单 = **未完成 / 失败 / 误标通过 / 计划承认 DEFERRED 但代码层面仍存在隐患** 的所有项

---

## 一、R29 验收轮次**虚标 PASSED**（最严重）

### 1.1 现象

- 路线表 R29 标记 `✅ PASSED`，叙述 "全量 `mvn clean test` 确认 BUILD SUCCESS"
- 实际执行 `mvn clean test`：consultation 模块 **2 个测试失败** → BUILD FAILURE
- `verify_v29.md` 文件 **不存在**（最新仅到 `verify_v28.md`）

### 1.2 失败定位

`AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/ObjectMapperJavaTimeModuleTest`

```
[ERROR] shouldSerializeLocalDateTimeToIso8601:23
        expected: <"2026-06-30T10:00:00"> but was: <[2026,6,30,10,0]>
[ERROR] shouldSerializeRegistrationEventWithLocalDateTimeWithoutException:34
        Unexpected exception thrown
```

### 1.3 根因

- v28 commit `60e0fef` 在 consultation 模块新增 `ObjectMapperJavaTimeModuleTest`，直接测试 `JavaTimeModule` 行为
- 该文件**未被 v28 的 verify 覆盖**（`verify_v28.md` 仅跑出 `RegistrationEventListenerTest 7/0/0/0`，推断实际命令为 `mvn test -Dtest=RegistrationEventListenerTest -pl modules/consultation` 而非全量）
- `consultation/pom.xml` 未直接声明 `jackson-datatype-jsr310`，依靠传递依赖；当前 classpath 下 `JavaTimeModule` 注册后未实际生效，`LocalDateTime` 退化为数组形式
- 与 R28 已修复的 `RegistrationEventListenerTest.shouldContainAllSevenFieldsInEventPayloadOnRecover` 共享相同根因——R28 仅修复了测试用例本身的 setUp()，未修测试基础设施

### 1.4 影响

- R29 不能作为最终验收通过
- 路线表当前 "ALL_DONE" 状态名不副实
- 后续 Phase 2 延展、Phase 3 上线评审的基线存疑

---

## 二、报告内问题**实际未修复**（P1/P2，5 项）

下列问题位于 R1—R25 内轮次，路线表均 ✅ PASSED，但代码层与诊断报告"证据"段不符：

| 编号 | 路径 | 计划口径 | 实际代码位置 | 缺什么 |
|---|---|---|---|---|
| **C05** | `TriageService.triage()` | R8 ✅ | `TriageServiceImpl.java:82-97` | 无 `TRIAGE_FIELD_COMBINATION_INVALID` 业务错误码；无 `chiefComplaint` XOR `additionalResponses` 互斥校验 |
| **C12** | `TriageConverter.toAiTriageRequest` | R8 ✅ | `TriageConverter.java:22-56` | 仅做 `additionalResponses` 拼接，**无 3000 字符截断**、**无 `TRUNCATED` 标记插入**、**无上下文总结截断策略** |
| **A08** | 降级路径文案 | R8 ✅ | `TriageServiceImpl.java:151,157` | `"AI service unavailable, using rule engine fallback"`（英文）；`"AI service has been continuously unavailable"`（英文）——设计要求中文 |
| **C21** | 降级路径规则参数 | R8 ✅ | `TriageServiceImpl.java:138` | 仍用 `request.getRuleVersion()/getRuleSetId()`，**未切到 `session.getRuleVersion()/getRuleSetId()` 快照** |
| **C10** | sessionId 格式校验 | R25 DEFERRED | `DialogueSessionManager.java:30-62` | `createSession()` 与 `restoreSession()` 均无 UUID v4 正则校验；`TriageServiceImpl:86` 直接 `createSession(request.getSessionId())` |

### 2.1 建议

将上述 5 项作为 **R31** 集中处理，复用 R8 既定设计语义但补齐代码实现。

---

## 三、计划明确 DEFERRED 但代码现状仍异常

### 3.1 R25 DEFERRED（`S01 + S03 + S06 + S07 + C10`）

| 编号 | 类/接口 | 现状 | 影响 |
|---|---|---|---|
| **S01** | `SuggestionStore.java:6-8` | 接口仅含 `compute()` + 父接口 `SessionStore`，**无 `createIfNotExists(taskId, prescriptionId, supplier)` 原子方法** | 与诊断报告"消除 TOCTOU 竞态"目标不符；DedupTaskScheduler 沿用 compute + 跨 key put |
| **S03** | `DedupTaskScheduler.java:25-41` | `compute()` lambda 内部仍执行 `suggestionStore.put(candidateTaskId, newResult)`（line 39） | 计算与跨 key 写入混合，原子性保证被跨 key 操作打破 |
| **S06** | `DedupTaskScheduler.java:43` | `return ((AiSuggestionResult) result).getTaskId();` 在 compute 闭包外强制类型转换 | 类型不安全；遇 null 或非 AiSuggestionResult 直接 ClassCastException |
| **S07** | `ConcurrentHashMapStore.java:11-13` | 同时实现 `SuggestionStore` 与 `DraftContextStore`，单 `ConcurrentHashMap<String, Object>` | 清理任务遍历会同时误命中两类数据 |
| **C10** | 见上 2 表 | 同上 | 同上 |

### 3.2 R27 DEFERRED（`P14`）

| 编号 | 路径 | 缺什么 |
|---|---|---|
| **P14** | `PrescriptionAssistServiceImpl.java:97-115, 245-254` | `assist()` 各 catch 块（ExecutionException/TimeoutException/InterruptedException/Exception）在 failReason 计算后**未调用** `prescriptionDraftContext.updateCriticalAlerts` 写入 CRITICAL 阻断；`buildEmptyResponse()` 同样未写 |

### 3.3 R30 DEFERRED（`P11`）

| 编号 | 现状 |
|---|---|
| **P11** | `SpecialPopulationDosageRule` 与 `DosageLimitRule` 均使用通用 `dosageStandardRepository.findByDrugCodeAndRouteOfAdministration()`；特殊人群分级（年龄/体重）未独立查询源 |

### 3.4 其他排期外说明（计划承认证据状态，本文件仅追踪不阻断）

`P09`/`P12`/`P15`/`P10`/`P13` 与诊断报告完全对照（详见 `Docs/Diagnosis/impl/06_phase2C3DE_report.md` 排期外说明段）。

---

## 四、额外发现（非诊断报告，v28/R29 新增问题）

### 4.1 `SuggestionCleanupTask` 失效

```java
// SuggestionCleanupTask.java:29-32
Object value = suggestionStore.get(key);
if (value instanceof SuggestionStoreEntry entry
        && isExpiredAndConsumed(entry, now)) {
    suggestionStore.remove(key);
```

- `SuggestionStore` 实际存的是 `AiSuggestionResult`（`DedupTaskScheduler.java:35,39`）
- `AiSuggestionResult` **未实现** `SuggestionCleanupTask.SuggestionStoreEntry` 内部接口
- `instanceof` 始终为 false → TTL 清理逻辑永不触发
- 编译可通过，运行静默失效；诊断报告**未列出此项**

### 4.2 `DraftContextCleanupTask` 模块归属错误

- 实际位置：`modules/consultation/src/main/java/com/aimedical/modules/consultation/task/DraftContextCleanupTask.java`
- 计划要求：`modules/prescription/src/main/java/.../task/`（R24）
- 依赖类型：注入的是 `com.aimedical.modules.commonmodule.store.DraftContextStore`（common-module），**非** prescription 的 `PrescriptionDraftContext`
- 行为偏差：清理的是 DraftContextStore 通用键空间，与 `PrescriptionDraftContext` 的 `prescriptionId+:criticalAlerts` 命名不一致

### 4.3 `M03` 扫描条件与 OOD 偏离

- 实际：`VisitIdReconciledTask.reconcileVisitIds()` 扫描 `record.getVisitId() == null || isBlank()`（行 30）
- OOD §4.3 要求扫描 `visitIdFallback=true` 的记录反查正确 visitId
- 行为差异：遗漏 visitIdFallback=true 但 visitId 非空的记录

### 4.4 `enrichWithDrugInfo` 调用结果未消费

- `PrescriptionAssistServiceImpl:230-243` 和 `PrescriptionAuditServiceImpl:518-532` 调用 `drugFacade.findByDrugCode(drugCode)`，结果赋给局部 `DrugInfo info` 但**从不被读取**
- 仅捕获异常 + WARN 日志即可反推；`DrugFacade` 注入价值可疑，但因 P02/E06 合并修复策略要求注入未在诊断报告中单独列出

---

## 五、修复路线建议（增量顺序）

| 序号 | 任务 | 工作量估 | 阻断 R29？ |
|---|---|---|---|
| 1 | 修复 `ObjectMapperJavaTimeModuleTest`（确认 `jackson-datatype-jsr310` 在 consultation classpath，或在 `pom.xml` 显式声明，或在测试中 `disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)`） | XS | **是** |
| 2 | 重跑全量 `mvn clean test`，输出 `verify_v29.md` | S | 否（步骤 1 之后） |
| 3 | 集中修复 C05/C12/A08/C21/C10 五项（R31） | M | 否 |
| 4 | 实施 R25 Store 群修复（S01/S03/S06/S07）+ SuggestionCleanupTask 类型适配 | M | 否 |
| 5 | 实施 R27 P14（PrescriptionDraftContext CRITICAL 写入） | S | 否 |
| 6 | DraftContextCleanupTask 迁回 prescription 模块并对齐调用点 | S | 否 |
| 7 | VisitIdReconciledTask 扫描条件改为 `visitIdFallback=true` | XS | 否 |
| 8 | 移除 `enrichWithDrugInfo` 死代码或说明保留理由 | XS | 否 |

---

## 六、本文件用途

- 跟踪当前仓库**未完成**与**已失败**项的清单
- 与 `Harness/implements/202606300050_fix_phase2C3DE_issues/plan.md` 路线表**差异比对**的基准
- 任何后续 R3x 轮次开工前应重新核对本文件，避免再次出现"R 标 PASSED 但代码未变 / 测试未跑 / 验证报告缺失"的情况
