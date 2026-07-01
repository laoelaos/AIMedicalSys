# 计划审查报告（v14 r1）

## 审查结果
REJECTED

## 发现

### **[一般] 发现 1：D组根因分析严重不准确**

task_v14.md §D 组将失败归因于"M07 success=false 逻辑"并讨论 `isSuccess()` 断言方向，但实际错误消息明确显示失败的断言是 **line 137 `assertFalse(response.isFromFallback())`**（`expected: <false> but was: <true>`）。

**真实根因**：M06 在 `MedicalRecordServiceImpl` 中引入了 `@Value("${ai.timeout.medical-record-generate:12}")` 和 `@Value("${medical-record.visit-facade.timeout:2}")` 字段。在纯 JUnit 测试（非 Spring 上下文）中，这些字段的 Java 默认值为 **0**，导致：
1. `resolveVisitId()` 中 `future.get(0, TimeUnit.SECONDS)` 立即超时 → `visitIdFallback=true` → `response.isFromFallback()=true`
2. `callAiWithTimeout()` 中 `future.get(0, TimeUnit.SECONDS)` 立即超时 → `response.isDegraded()=true`
3. **line 135 `assertTrue(response.isSuccess())` 实际通过**（因 errorCode=MR_GEN_AI_TIMEOUT 时 success=true），与任务分析相反

**影响**：如果实施者相信了 task 的错误分析去修改 `isSuccess()` 断言，将浪费时间和方向。正确的修复路径是在测试中通过反射（或 `ReflectionTestUtils`）将 `aiTimeout` 和 `visitFacadeTimeout` 设为有效正数。

### **[一般] 发现 2：B组 dept-01 问题根因分析不完整**

task_v14.md 对 B 组 3 个测试返回 `"dept-01"` 而非 `"DEFAULT"` 仅说"需分析是 M11 导致模板选择逻辑变化还是测试数据/环境问题"，结论不明确。

**经审查确认根因**：`DatabaseTemplateConfigManager.parseRequiredFields()` 方法内部有 try-catch 捕获了所有异常并返回 `Collections.emptySet()`（而非抛出异常触发外层 catch 返回 DEFAULT_TEMPLATE）。三种场景均如此：
- `null` → 提前返回 emptySet
- `"invalid json"` → JSON 解析异常被内部 catch 消化→返回 emptySet
- `"[\"INVALID_FIELD\"]"` → `valueOf` 异常被内部 catch 消化→返回 emptySet

**这意味着生产代码行为正确**——当数据库中找到了指定科室记录但数据异常时，返回该科室 ID + 空字段集的模板，而非全局 DEFAULT 模板。测试期望（`assertEquals("DEFAULT", ...)`）是错误的，应改为 `assertEquals("dept-01", ...)`。任务应明确告知此结论，避免实施者不必要地排查生产代码。

### **[轻微] 发现 3：E组分析归因偏差**

task 说"@Column(name="content_json") 影响反序列化"，但实际根因是 `MedicalRecordContentConverter.convertToEntityAttribute()` 中 `MedicalRecordField.valueOf("UNKNOWN_FIELD")` 抛出 IllegalArgumentException，被外层 catch 捕获返回空 map。这是 converter 设计缺陷（或预存行为），非 `@Column` 变更直接导致。修正方向（匹配生产行为）正确，但归因不精确。

### **[轻微] 发现 4：C组分析缺乏字符计数验证**

C 组仅指出"不足 50 字符"，但未提供具体字符数分析。实测测试字符串约 42 字符，补足至 50+ 即可。不影响修正。

## 修改要求（REJECTED）

1. **发现 1（D 组）**：修正 task_v14.md §D 组根因分析，明确 M06 @Value 字段默认为 0 是真实根因；指导实施者通过反射设置 `aiTimeout=12`、`visitFacadeTimeout=2`；移除错误的是否修改生产代码的讨论。
2. **发现 2（B 组）**：修正 task_v14.md §B 组 dept-01 问题的分析结论为"生产代码行为正确，测试断言期望值错误，应改为 `assertEquals("dept-01", ...)`"。
