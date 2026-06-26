根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 🔴 严重问题

1. **Refresh 端点可绕过 passwordChangeRequired 强制约束**：v13 将 `/api/auth/refresh` 加入 PasswordChangeCheckFilter 白名单后，`AuthServiceImpl.refresh()` 中不存在 passwordChangeRequired 状态检查。用户登录后即使不修改密码，也可通过 `POST /api/auth/refresh` 反复获取新令牌，维持认证状态。这是 v13 修订引入的回归问题。所在位置：3.1.3 节 Token 刷新流程、3.3 节 PasswordChangeCheckFilter 白名单、3.4 节 passwordChangeRequired 访问控制。改进建议：在 `AuthServiceImpl.refresh()` 中增加 passwordChangeRequired 检查，若用户在 DB 中 `passwordChangeRequired=true`，拒绝刷新并返回 `PASSWORD_CHANGE_REQUIRED`（403）。

### 🟡 重要问题

2. **3.4 节白名单与 3.3 节/3.1.2 节白名单不一致（内部矛盾）**：3.4 节「passwordChangeRequired 访问控制」的白名单仅列出两个端点，但 3.1.2 节和 3.3 节的白名单包含三个端点（含 `/api/auth/refresh`）。3.4 节未随 v13 修订同步更新。所在位置：3.4 节。改进建议：将 3.4 节白名单补充 `POST /api/auth/refresh` 条目。

3. **PasswordChangeService 和 CurrentUser 接口缺少方法签名（可落地性缺陷）**：两个接口仅有职责描述而无完整方法签名。v13 已修复 PasswordPolicy 缺少方法签名的问题，但 PasswordChangeService 和 CurrentUser 遗漏。所在位置：1.3 节、3.4 节。改进建议：补充 `PasswordChangeService` 方法签名（isChangeRequired/markChangeRequired/clearChangeRequired）和 `CurrentUser` 方法签名（getUserId/getUsername/getUserType）。

4. **包 A 实体移至 common-module-impl 的结构变更影响评估不完整**：2.1 节将包 A 实体放在 `common-module-impl/permission/` 下，但未评估现有代码中业务模块是否已直接引用包 A 实体。7.2 节声称业务模块通过 `common-module-api` 访问共享类型，但 User 等实体不属于 API 模块——业务模块如何获取用户数据未说明。所在位置：2.1 节、2.2 节、7.1 节、7.2 节。改进建议：补充评估现有外部引用；明确业务模块访问用户数据的途径；补充迁移步骤。

5. **SlidingWindowCounter 声明为 public 但包结构仍存在困惑**：InMemoryRateLimitGuard 和 GlobalRateLimitFilter 均描述为使用 `ConcurrentHashMap<String, SlidingWindowCounter>`，表述雷同，开发者可能误以为两者共享同一个计数器实例。所在位置：4.1 节。改进建议：明确注释两套计数器实例相互独立。

### 🔵 一般问题

6. **3.4 节"403 FORBIDDEN"与 PasswordChangeCheckFilter 的 PASSWORD_CHANGE_REQUIRED 描述不一致**：3.4 节描述为"返回 403 FORBIDDEN"，但 PasswordChangeCheckFilter 行为契约（3.3 节）定义的返回值是 ErrorCode.PASSWORD_CHANGE_REQUIRED + HTTP 403。所在位置：3.4 节。改进建议：将"返回 403 FORBIDDEN"修正为"返回 403 + ErrorCode.PASSWORD_CHANGE_REQUIRED"。

7. **refresh 端点携带 Authorization header 时的服务端行为未完整定义**：3.1.3 节和 7.2 节要求 refresh 请求不应携带 Authorization header，但未定义携带时的服务端处理策略。所在位置：3.1.3 节步骤 1、7.2 节。改进建议：补充说明服务端行为——JwtAuthenticationFilter 正常处理但 refresh 逻辑不依赖前置认证；同时应等待问题 1 的修复确认此场景的 passwordChangeRequired 行为。

8. **login() 锁检查的 IP 维度与 GlobalRateLimitFilter 重复提及，分工不够清晰**：3.1.1 节步骤 2 称为"RateLimitGuard 检查"，未明确标注是 InMemoryRateLimitGuard（登录专用限流）还是 Filter 层面共同作用。所在位置：3.1.1 节步骤 2、4.1 节。改进建议：在 3.1.1 节步骤 2 明确标注"AuthServiceImpl 内部调用 InMemoryRateLimitGuard.tryAcquire()"。

## 历史迭代回顾

根据 iteration_history.md 中第 1~13 轮迭代反馈与当前审查结果对比分析：

- **已解决的问题**：第 1~12 轮历史反馈中的所有问题（涉及 NOT NULL 约束、DTO 定义、Filter 分离、Token 设计、密码策略、前端补偿、模块迁移、锁机制等各方面）已在 v12/v13 中得到有效修复，当前审查中不再提及。

- **持续存在的问题**：以下问题在第 13 轮反馈中首次出现，并在本轮审查中再次被确认尚未完全解决：
  - Refresh 端点绕过 passwordChangeRequired（问题 1，第 13 轮已报告但 v13 的修复不完整，引入了回归）
  - 3.4 节白名单内部矛盾（问题 2，与问题 1 关联，同为 v13 修订不完整所致）
  - PasswordChangeService/CurrentUser 缺少方法签名（问题 3）
  - 包 A 实体迁移影响评估不完整（问题 4）
  - SlidingWindowCounter 包结构困惑（问题 5）

- **新发现的问题**：以下问题在本轮审查中首次被识别（未在第 13 轮历史反馈中出现）：
  - 3.4 节"403 FORBIDDEN"与 3.3 节 PASSWORD_CHANGE_REQUIRED 描述不一致（问题 6）
  - refresh 端点携带 Authorization header 时的行为未定义（问题 7）
  - login() IP 限流分工不够清晰（问题 8）

## 上一轮产出路径
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/a_v13_copy_from_v12.md

## 用户需求
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/requirement.md
