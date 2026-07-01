# 任务指令（v2）

## 动作
NEW

## 任务描述
修复 consultation 模块 5 项问题（C05/C12/A08/C21/C10），均为第一阶段 R8/R25 已标 PASSED 但代码未实际修复的缺陷：

### C05: 新增业务错误码 + 字段互斥校验
- **文件**: `TriageErrorCode.java` + `TriageServiceImpl.java`
- **需求**:
  1. 在 `TriageErrorCode` 枚举中新增 `TRIAGE_FIELD_COMBINATION_INVALID("TRIAGE_FIELD_COMBINATION_INVALID", "主诉与追问互斥或均缺失")`
  2. 在 `TriageServiceImpl.triage()` 方法开头，校验 `request.getChiefComplaint()` 与 `request.getAdditionalResponses()` 互斥——二者不能同时为空，也不能同时非空；违反时抛出 `BusinessException(TriageErrorCode.TRIAGE_FIELD_COMBINATION_INVALID, ...)`

### C12: 3000 字符截断 + TRUNCATED 标记
- **文件**: `TriageConverter.toAiTriageRequest()`
- **需求**: 对 `additionalResponses` 的拼接文本做 3000 字符截断：
  1. 将所有 `additionalResponses` 拼接为连续文本（格式：`"Q: {question} A: {answer} "` 逐条拼接）
  2. 若拼接后超过 3000 字符，截断至 3000 字符并在末尾追加 `" [TRUNCATED]"` 标记
  3. 将截断后的文本设置到 `aiRequest` 的某个合适字段（建议复用 `additionalResponses` 结构或新增字段；若 `TriageRequest` 已有 `additionalResponses` 字段则直接覆盖）

### A08: 降级文案中文化
- **文件**: `TriageServiceImpl.java` lines 151, 157
- **需求**:
  - line 151: `"AI service unavailable, using rule engine fallback"` → `"AI 服务不可用，已切换至规则引擎降级"`
  - line 157: `"AI service has been continuously unavailable"` → `"AI 服务持续不可用，建议稍后重试"`

### C21: 降级路径切回 session 快照
- **文件**: `TriageServiceImpl.java` line 138
- **需求**: 将 fallback 路径中的 `request.getRuleVersion()` 和 `request.getRuleSetId()` 替换为 `session.getRuleVersion()` 和 `session.getRuleSetId()`
- **背景**: 当前 `session` 已在 triage() 开头创建/恢复（lines 84-87），但 fallback 路径仍读 request 而非 session 快照；规则引擎应当基于 session 的快照版本匹配

### C10: UUID v4 格式校验
- **文件**: `DialogueSessionManager.java` + `TriageServiceImpl.java`
- **需求**:
  1. 在 `DialogueSessionManager` 中新增 UUID v4 正则常量：`^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`（不区分大小写）
  2. 在 `createSession(String sessionId)` 开头校验 sessionId 格式，不合法则抛出 `IllegalArgumentException("Invalid UUID v4 format for sessionId: " + sessionId)`
  3. 在 `restoreSession(String sessionId)` 开头同样校验
  4. 在 `TriageServiceImpl.triage()` 的 `sessionManager.createSession(request.getSessionId())` 处捕获 `IllegalArgumentException` 并转换为 `BusinessException(TriageErrorCode.TRIAGE_SESSION_NOT_FOUND, ...)`（或新增专用错误码）

## 选择理由
5 项问题均位于 consultation 模块，修改范围集中（4 个文件），互不依赖，可一次实现。T1 基础设施修复已验证通过，T2 为最高优先级业务逻辑修复，完成后将覆盖 P1/P2 级别缺陷。

## 任务上下文
- **TriageErrorCode.java**: 当前仅有 `TRIAGE_SESSION_NOT_FOUND` 一个枚举值
- **TriageServiceImpl.triage()**: 入口方法 lines 82-168，先恢复/创建 session，然后调用 AI，失败时走 fallback 路径；校验应加在 session 创建逻辑之前
- **TriageConverter.toAiTriageRequest()**: lines 22-56，拼接 additionalResponses 为 `AdditionalResponseItem` 列表，当前无字符数限制
- **DialogueSessionManager.createSession()/restoreSession()**: lines 30-38 / 44-62，当前 sessionId 直接使用，无格式校验
- **降级路径**: `TriageServiceImpl.triage()` 的 lines 133-161，其中 line 138 使用了 `request.getRuleVersion()/getRuleSetId()`

## 已有代码上下文
文件路径（均相对于 AIMedical/backend/modules/consultation）：
- `src/main/java/.../exception/TriageErrorCode.java`
- `src/main/java/.../service/impl/TriageServiceImpl.java`
- `src/main/java/.../converter/TriageConverter.java`
- `src/main/java/.../dialogue/DialogueSessionManager.java`
- `src/main/java/.../dialogue/DialogueSession.java`
- `src/main/java/.../dto/DialogueCreateRequest.java`

所有文件当前代码内容详见 detail_v1.md 同目录的上一轮上下文记录。
