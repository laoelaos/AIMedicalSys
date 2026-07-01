# 详细设计（v2）

## 概述
修复 consultation 模块 5 项业务逻辑缺陷（C05/C12/A08/C21/C10），涉及 4 个现有 consultation 模块文件 + 1 个 ai-api 模块 DTO 文件。各项修改互不依赖，可顺序实现。

## 文件规划
| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/consultation/src/main/java/.../exception/TriageErrorCode.java` | 修改 | 新增 `TRIAGE_FIELD_COMBINATION_INVALID` 枚举值 |
| `AIMedical/backend/modules/consultation/src/main/java/.../service/impl/TriageServiceImpl.java` | 修改 | 实现 C05 互斥校验、C10 异常转换、C21 session 快照、A08 中文化 |
| `AIMedical/backend/modules/consultation/src/main/java/.../converter/TriageConverter.java` | 修改 | 实现 C12 3000 字符截断 + TRUNCATED 标记 |
| `AIMedical/backend/modules/consultation/src/main/java/.../dialogue/DialogueSessionManager.java` | 修改 | 实现 C10 UUID v4 格式校验 |
| `AIMedical/backend/modules/ai/ai-api/src/main/java/.../dto/triage/TriageRequest.java` | 修改 | 新增 `additionalResponsesText` String 字段（为 C12 提供存储） |

## 类型定义

### 无新增类型
仅修改已有类型，不新增类/接口/枚举。

### TriageErrorCode 枚举修改
**形态**：enum（已有）
**包路径**：`com.aimedical.modules.consultation.exception`
**变更**：在 `TRIAGE_SESSION_NOT_FOUND` 之后新增一个枚举值：
```
TRIAGE_FIELD_COMBINATION_INVALID("TRIAGE_FIELD_COMBINATION_INVALID", "主诉与追问互斥或均缺失")
```

### TriageRequest DTO 修改
**形态**：class（已有）
**包路径**：`com.aimedical.modules.ai.api.dto.triage`
**变更**：新增字段及对应 getter/setter：
```
private String additionalResponsesText;

public String getAdditionalResponsesText() { return additionalResponsesText; }
public void setAdditionalResponsesText(String additionalResponsesText) { this.additionalResponsesText = additionalResponsesText; }
```

### DialogueSessionManager 修改
**形态**：class（已有）
**包路径**：`com.aimedical.modules.consultation.dialogue`
**新增成员**：
```
private static final Pattern UUID_V4_PATTERN =
    Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);
```
**createSession(String sessionId)** 开头追加校验：
```
if (sessionId == null || !UUID_V4_PATTERN.matcher(sessionId).matches()) {
    throw new IllegalArgumentException("Invalid UUID v4 format for sessionId: " + sessionId);
}
```
**restoreSession(String sessionId)** 开头追加相同校验。

### TriageServiceImpl.triage() 修改
**形态**：class（已有）
**包路径**：`com.aimedical.modules.consultation.service.impl`

**变更顺序（自上而下）：**

1. **C05 互斥校验**（插入在 line 83 之前，即 session 创建之前）：
   ```
   boolean hasChiefComplaint = request.getChiefComplaint() != null && !request.getChiefComplaint().trim().isEmpty();
   boolean hasAdditional = request.getAdditionalResponses() != null && !request.getAdditionalResponses().isEmpty();
   if (hasChiefComplaint == hasAdditional) {
       throw new BusinessException(TriageErrorCode.TRIAGE_FIELD_COMBINATION_INVALID,
           request.getSessionId());
   }
   ```

2. **C10 异常转换**（包裹 lines 84-87 session 创建/恢复区域）：
   将 `restoreSession` 和 `createSession` 调用包裹在 try-catch 中：
   ```
   DialogueSession session;
   try {
       session = sessionManager.restoreSession(sessionId);
       if (session == null) {
           session = sessionManager.createSession(sessionId);
       }
   } catch (IllegalArgumentException e) {
       throw new BusinessException(TriageErrorCode.TRIAGE_SESSION_NOT_FOUND, sessionId);
   }
   ```
   **新增 import**：`java.lang.IllegalArgumentException`（JDK 内置，无需显式导入）

3. **C21 session 快照**（请求→session 传值 + fallback 切引用）：
   - 在 session 创建/恢复之后、`session.setChiefComplaint` 之前（lines 88-89 之间），新增将 request 的 ruleVersion/ruleSetId 同步至 session：
     ```
     if (session.getRuleVersion() == null && request.getRuleVersion() != null) {
         session.setRuleVersion(request.getRuleVersion());
     }
     if (session.getRuleSetId() == null && request.getRuleSetId() != null) {
         session.setRuleSetId(request.getRuleSetId());
     }
     ```
   - line 138：`request.getRuleVersion()` → `session.getRuleVersion()`
   - line 138：`request.getRuleSetId()` → `session.getRuleSetId()`

4. **A08 中文化**（lines 151, 157）：
   - line 151：`"AI service unavailable, using rule engine fallback"` → `"AI 服务不可用，已切换至规则引擎降级"`
   - line 157：`"AI service has been continuously unavailable"` → `"AI 服务持续不可用，建议稍后重试"`

### TriageConverter.toAiTriageRequest() 修改
**形态**：class（已有）
**包路径**：`com.aimedical.modules.consultation.converter`
**变更**：在现有 items 列表构建完成后（`aiRequest.setAdditionalResponses(items)` 之后），追加截断逻辑：
1. 将所有 items 拼接为连续文本：
   ```
   StringBuilder sb = new StringBuilder();
   for (AdditionalResponseItem item : items) {
       sb.append("Q: ").append(item.getQuestion() != null ? item.getQuestion() : "")
         .append(" A: ").append(item.getAnswer() != null ? item.getAnswer() : "")
         .append(" ");
   }
   ```
2. 若 `sb.length() > 3000`，截断至 3000 字符：
   ```
   if (sb.length() > 3000) {
       sb.setLength(3000);
       sb.append(" [TRUNCATED]");
   }
   ```
3. 设置到 `aiRequest`：
   ```
   aiRequest.setAdditionalResponsesText(sb.toString());
   ```

## 错误处理

### C05 校验异常
- **异常类型**：`BusinessException`（已有）
- **错误码**：`TriageErrorCode.TRIAGE_FIELD_COMBINATION_INVALID`（新增）
- **传播策略**：直接抛出，由全局 `@RestControllerAdvice` 异常处理器统一处理

### C10 UUID 格式异常
- **校验层异常**：`IllegalArgumentException`（由 `DialogueSessionManager` 抛出）
- **服务层转换**：在 `TriageServiceImpl.triage()` 中捕获 `IllegalArgumentException`，转换为 `BusinessException(TRIAGE_SESSION_NOT_FOUND, sessionId)`
- **传播策略**：对调用方透明，统一以 `BusinessException` 形态返回

### C12 截断逻辑
- **无异常**：纯字符串操作，不引入新异常类型
- **边界**：拼接后恰好 3000 字符时不截断、不追加 `[TRUNCATED]`；仅在 `> 3000` 时触发

## 行为契约

### C05 — 字段互斥校验在 session 创建之前
- **前置**：`request` 非 null
- **后置**：`hasChiefComplaint != hasAdditional` 时正常继续；否则抛出 `BusinessException`
- **校验时序**：triage() 方法体的第一条业务逻辑（在 session 创建/恢复之前）

### C12 — 截断顺序和格式
- **拼接格式**：`"Q: {question} A: {answer} "`（每项后有一个尾随空格）
- **截断优先级**：先截断至 3000 字符，再追加 `" [TRUNCATED]"`，最终长度可能为 3000 + 12 = 3012
- **空列表**：若 `items` 为空，`sb.toString()` 为空字符串，不截断

### C21 — session 快照优先级
- **传值策略**：仅当 `session.getRuleVersion() == null` 时才从 request 复制（不覆盖已恢复的 session 值）
- **fallback 引用**：`triageRuleEngine.match()` 的第二个参数使用 `session.getRuleVersion()`，第三个参数使用 `session.getRuleSetId()`

### C10 — UUID v4 校验入参
- **null 处理**：`sessionId == null` 视为格式不合法，抛出 `IllegalArgumentException`
- **大小写**：正则使用 `Pattern.CASE_INSENSITIVE`，不区分大小写

### A08 — 仅替换字符串常量
- 不改变任何控制流逻辑
- 保持原有 `fallbackResponse.setReason()` 和 `fallbackResponse.setFallbackHint()` 调用结构不变

## 依赖关系

### 新增模块间依赖
- **consultation → ai-api**：`TriageRequest.additionalResponsesText` 字段为 consultation 模块使用，不影响其他消费者
- **ai-api 模块**：纯 DTO 字段新增，无行为变更，向后兼容

### 暴露给后续任务的接口
无新增公开接口。所有修改为内部实现变更：
- `TriageErrorCode` 新增枚举值（同包可见）
- `TriageConverter.toAiTriageRequest()` 签名不变（参数/返回类型不变）
- `DialogueSessionManager.createSession/restoreSession` 签名不变（参数/返回类型不变，新增校验副作用）
- `TriageServiceImpl.triage()` 签名不变
