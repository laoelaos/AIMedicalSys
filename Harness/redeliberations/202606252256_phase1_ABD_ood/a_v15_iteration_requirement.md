根据以下审查结果，迭代上一轮的产出，形成新版的文件，从而更好地满足用户需求。

## 当前审查结果

### 逻辑矛盾（第二章）

1. **[重要] ACCOUNT_LOCKED 错误消息与 ErrorCode 表不一致**（2.1 节）
   - 位置：3.1.1 节步骤 3 vs 10.2 节 ErrorCode 表
   - 问题：3.1.1 节为 IP 维度和用户名维度锁定分别定义了不同的错误消息（"请 30 分钟后重试" vs "请 15 分钟后重试"），但 10.2 节 ErrorCode 表中 ACCOUNT_LOCKED 仅有一条固定消息"请 15 分钟后重试"。两处描述语义矛盾
   - 改进建议：二选一统一：(a) 在 10.2 节为 ACCOUNT_LOCKED 新增"账户已锁定，请 30 分钟后重试"消息行对应 IP 维度；(b) 在 10.2 节补充注释说明"ACCOUNT_LOCKED 消息根据锁定维度动态生成"

2. **[重要] tokenVersion 比对步骤存在设计歧义**（2.2 节）
   - 位置：3.1.3 节步骤 5 vs 步骤 9
   - 问题：步骤 5 从 DB 加载用户（含 tokenVersion），步骤 9 要求"验证 Refresh Token claims 中的 tokenVersion 与 DB 中当前 tokenVersion 一致"。未明确步骤 9 是从 DB 重新查询 tokenVersion，还是复用步骤 5 已加载的实体字段值。若复用则存在细竞态窗口
   - 改进建议：明确实现决策：(a) 若复用步骤 5 值（允许微竞态），在设计决策表记录此权衡；(b) 若重新查询以保证强一致性，步骤 9 改为"重新从 DB 加载用户当前 tokenVersion 并比对"，同时移除步骤 5 中 tokenVersion 的预加载

### 深度与完整性（第三章）

3. **[严重] UserFacade 门面接口缺少完整定义**（3.1 节，同 v1 诊断）
   - 位置：2.2 节依赖规则、1.3 节核心抽象一览、2.1 节目录结构
   - 问题：UserFacade 未出现在 1.3 节核心抽象一览表、未出现在 2.1 节目录结构、未定义任何方法签名、未说明与 CurrentUser 的职责分工
   - 改进建议：在 1.3 节补充 UserFacade 条目；在 2.1 节 common-module-api 目录树中增加 UserFacade.java；定义至少 2-3 个方法签名；补充 UserFacade 与 CurrentUser 的分工说明

4. **[重要] PasswordChangeCheckFilter 白名单路径匹配策略未定义**（3.2 节，同 v1 诊断）
   - 位置：3.3 节 PasswordChangeCheckFilter 行为契约
   - 改进建议：明确指定使用 AntPathRequestMatcher 进行路径匹配，并在 SecurityConfig 中给出 Filter 注册的示例代码段

5. **[重要] MenuUpdateRequest 的 PATCH 语义与 Java record 反序列化存在歧义**（3.3 节，同 v1 诊断）
   - 位置：5.2 节 MenuUpdateRequest
   - 改进建议：二选一：(a) 改用传统 POJO + 字段标记组；(b) 正式声明降级为全量替换语义（PUT）

6. **[重要] Refresh 端点在禁用用户场景不递增 IP 维度失败计数**（3.4 节，同 v1 诊断）
   - 位置：3.1.3 节步骤 7
   - 改进建议：在步骤 7 中增加 IP 维度的失败计数递增

7. **[一般] 登录成功后 IP 维度计数器清空前置条件未定义**（3.5 节）
   - 位置：3.1.1 节步骤 8
   - 问题：未说明清除操作的前置假设。NAT/代理共享 IP 场景下，用户 A 登录成功将清除同一公网 IP 下所有用户的 IP 维度失败计数，导致用户 B 的 IP 锁定被意外绕过
   - 改进建议：在 11 节设计决策表增加一行记录此局限性，或补充说明"IP 维度的失败计数以请求来源 IP 为准（若经过反向代理，应取 X-Forwarded-For 或 X-Real-IP 头，由 GlobalRateLimitFilter 和 LoginAttemptTracker 统一从 RequestHelper.getClientIp() 获取）"

8. **[一般] ProfileUpdateRequest.phone 可选性未声明**（3.6 节，同 v1 诊断）
   - 位置：5.2 节 ProfileUpdateRequest
   - 改进建议：在 phone 和 email 字段补充注释说明"可选字段"

9. **[一般] PasswordChangeRequest.oldPassword 缺少约束**（3.7 节，同 v1 诊断）
   - 位置：5.2 节 PasswordChangeRequest
   - 改进建议：为 oldPassword 补充 @Size(max = 128) 约束

## 历史迭代回顾

分析历史迭代反馈（第 1-14 轮）与当前审查结果的关系：

- **已解决的问题**（出现在历史反馈但当前反馈中不再提及的问题）：第 1-13 轮中大量问题（包括登录流程时序侧信道、密码变更流程恢复、黑名单设计、多 Tab 竞态、ErrorCode 设计、版本号不一致等）已在之前迭代中修复，当前诊断不再提及，视为已解决

- **持续存在的问题**（在多轮反馈中反复出现，需重点解决）：
  - `UserFacade` 门面缺少完整定义——首次出现在第 14 轮第 1 条，当前第 15 轮仍存在（问题 3）
  - ACCOUNT_LOCKED 消息不一致——首次出现在第 14 轮第 2 条，当前第 15 轮仍存在（问题 1）
  - tokenVersion 比对歧义——首次出现在第 14 轮第 3 条，当前第 15 轮仍存在（问题 2）
  - PasswordChangeCheckFilter 路径匹配策略——第 14 轮第 4 条，当前仍存在（问题 4）
  - MenuUpdateRequest PATCH 语义——第 14 轮第 5 条，当前仍存在（问题 5）
  - Refresh 端点递增失败计数——第 14 轮第 6 条，当前仍存在（问题 6）
  - IP 计数器清空前置条件——第 14 轮第 7 条，当前仍存在（问题 7）
  - ProfileUpdateRequest.phone——第 14 轮第 8 条，当前仍存在（问题 8）
  - PasswordChangeRequest.oldPassword——第 14 轮第 9 条，当前仍存在（问题 9）

- **新发现的问题**（本轮新识别的问题）：无——本轮所有问题均为第 14 轮 v1 诊断已识别但未修复的遗留问题

## 上一轮产出路径
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/a_v14_design_v2.md

## 用户需求
c:/Develop/Software/AIMedicalSys/Harness/redeliberations/202606252256_phase1_ABD_ood/requirement.md
