# Phase 1 包 A/B/D OOD 设计 - 质量审查报告（第 7 次迭代）

审查范围：需求响应充分度、事实/逻辑正确性、深度与完整性、可落地性
审查基准：内部审议（7 轮迭代）未覆盖的维度

---

## 问题 1：异常刷新检测机制缺少可落地的实现细节

- **所在位置**：4.2 节「Refresh Token 的安全补偿策略」第三项
- **严重程度**：严重
- **问题描述**：文档声称"若检测到同一用户短时间内出现多次刷新（暗示旧 token 被重复使用），记录安全日志并触发告警"，但以下关键实现要素全部缺失：
  - 「短时间内」未定义具体时间窗口（1 秒？5 秒？）
  - 「多次」未定义阈值（2 次？5 次？）
  - 检测逻辑的放置位置未指定（AOP？AuthServiceImpl.refresh()？独立 Filter？）
  - 无对应的类/接口定义，无架构组件归属
  - 告警输出方式未说明（仅 `log.warn`？是否需对接监控系统？）
- **改进建议**：补充完整的设计契约：(a) 定义时间窗口和阈值；(b) 指定检测逻辑实现位置（如在 `AuthServiceImpl.refresh()` 中获取 `SecurityContext` 中的用户标识，检查上次刷新时间戳）；(c) 明确告警方式。或将此机制推迟到 Phase 2 并从当前设计的补偿策略列表中移除。

---

## 问题 2：H6 修复方案推荐的 `inject('router')` 在 Pinia store 中不可用

- **所在位置**：8.2 节 H6「修复方案」列、17 节修订说明（v7）第 13 项
- **严重程度**：严重
- **问题描述**：文档推荐在 Pinia store 中使用 `inject('router')` 依赖注入模式来打破循环依赖。但 `inject()` 仅在 Vue 组件 `setup()` 或 `<script setup>` 上下文中可用；在 Pinia store 的 action/method 中调用 `inject()` 没有 Vue 组件上下文，会返回 `undefined`。此方案无法在编码阶段落地。
- **改进建议**：保留现有 `createMenuStore(router, dynamicPageComponent)` 工厂模式（已在 `shared/src/stores/menu.ts` 中实现）。该模式已在 app 专用 store 层通过延迟初始化解决了循环依赖问题（见 `apps/doctor/src/stores/menu.ts` 和 `apps/admin/src/stores/menu.ts`）。修复方案应引用现有实现作为参考，而非推荐不可行的 `inject('router')` 方案。

---

## 问题 3：管理员标记密码过期的 API 端点缺失

- **所在位置**：3.4 节「密码变更强制策略」场景 2 vs 6.1 节接口清单
- **严重程度**：重要
- **问题描述**：3.4 节明确描述"系统管理员通过管理端对特定用户标记密码过期"作为两种密码变更触发场景之一，但 6.1 节接口清单中没有任何管理员端 API 端点用于设置用户的 `passwordChangeRequired = true`。此功能无实现路径。
- **改进建议**：二选一：(a) 在 6.1 节补充管理员专用端点（如 `PUT /api/users/{id}/password-expire`），并说明权限要求（ADMIN 角色）；(b) 若此功能属于管理端模块（包 D 的管理后台）或 Phase 2 范围，在 3.4 节场景 2 中明确标注"此功能属于管理端设计范围，本设计不做具体接口定义"。

---

## 问题 4：ProfileUpdateRequest.nickname 缺少空白字符串校验

- **所在位置**：5.2 节 ProfileUpdateRequest record 定义
- **严重程度**：一般
- **问题描述**：`nickname` 字段仅有 `@Size(max = 50)` 但缺少 `@NotBlank`。空字符串（`""`）能通过 `@Size(max = 50)` 校验（长度为 0 ≤ 50），导致用户可将自己的昵称设为空字符串。现有后端代码（`ProfileUpdateRequest.java`）也存在同样缺失。
- **改进建议**：在 `nickname` 字段上补充 `@NotBlank(message = "昵称不能为空")` 注解。
  ```java
  @NotBlank @Size(max = 50) String nickname
  ```

---

## 问题 5：MenuUpdateRequest 的 PUT 语义不明确（全量替换 vs 部分更新）

- **所在位置**：5.2 节 MenuUpdateRequest 定义及一致性校验说明
- **严重程度**：一般
- **问题描述**：`MenuUpdateRequest` 中除 `id` 外所有字段都是可选类型（无 `@NotNull`），暗示 PATCH 语义的局部更新。但接口方法为 `PUT /api/menu/{id}`，PUT 通常语义为全量替换。若实现为局部更新，空值应解释为"不更新此字段"；若为全量替换，缺少空值策略（设为 null 还是忽略）。
- **改进建议**：明确声明更新语义：若采用局部更新（推荐），标注为 PATCH 或补充说明"省略的字段不更新"；若坚持全量替换，为所有字段补充 Java doc 说明空值处理策略。

---

## 问题 6：登出端点在 token 过期场景下的行为未定义

- **所在位置**：3.1.4 节登出流程、4.4 节保护清单（`/api/auth/logout -> authenticated`）
- **严重程度**：一般
- **问题描述**：`/api/auth/logout` 要求 `authenticated`，意味着必须携带有效 Access Token。用户在 token 过期后无法调用登出后端接口（Filter 层无有效 token → UNAUTHORIZED → 前端 401 拦截器触发刷新）。但文档未说明在 token 过期或无效场景下，前端的登出策略是什么——是"尽力而为"（后端失败仍在前端 finally 块清除本地数据），还是需设计无 token 登出机制。
- **改进建议**：在 3.1.4 节补充说明：后端登出错时（token 过期/无效导致 401），前端 finally 块仍需清除本地 token 和用户数据。Token 黑名单的登出记录是"尽力而为"的最优努力，不应阻止本地登出。

---

## 问题 7：LoginAttemptCleaner 类定义但无行为说明

- **所在位置**：2.1 节目录结构中 `LoginAttemptCleaner.java` 条目
- **严重程度**：一般
- **问题描述**：目录结构包含 `LoginAttemptCleaner` 类，但全文没有任何地方描述此类的行为、调度策略、与 `LoginAttemptTracker` 的惰性清理之间的配合关系。4.1 节仅描述"超过锁定时间后惰性清除"。未说明 `LoginAttemptCleaner` 是否存在、是否必要。
- **改进建议**：二选一：(a) 在 9.3 节或 4.1 节补充 `LoginAttemptCleaner` 的调度机制说明（如 @Scheduled 定时清理超期记录，清理周期等）；(b) 若惰性清理已足够，从目录结构中删除 `LoginAttemptCleaner.java` 条目以避免混淆。

---

## 总结

上述 7 个问题中，**2 个严重问题**（异常刷新检测不可落地、H6 修复方案的 `inject('router')` 不可行）直接影响设计文档的可执行性，修复者无法据此进行编码实现。**1 个重要问题**（管理员标记密码过期 API 缺失）表明需求响应不充分。**4 个一般问题**属于设计完整性补强。

建议在下一轮迭代中优先修复严重问题，确保每个安全补偿措施都有明确、可落地的实现路径。
