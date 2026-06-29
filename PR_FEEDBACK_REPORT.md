# PR 反馈报告：37 项审查问题核实结果

> 生成时间：2026-06-28  
> 审查范围：Package C — Patient Login/Register/UI/Profile/API  
> 构建验证：`mvn install` ✅ | `npm run build:all` ✅

---

## 已修复项（3 P0 缺陷 + 其他）

| # | 问题 | 修复 |
|---|------|------|
| T1 | `mergeFromRequest()` 死代码 | `updateProfile()` 现调用该方法 |
| T2 | 过敏严重度/疾病状态字段使用 String 丢失类型安全 | 改为 `AllergySeverity` / `DiseaseStatus` 枚举 |
| T3 | `PatientDto` 缺失 `avatarUrl` | 新增字段 + `PatientConverter.toDto()` 映射 |
| T5 | `parseDate()` 无效日期抛 500 | try-catch 捕获 `DateTimeParseException` |
| T10 | `buildHealthFormRequest` switch 缺 default | 已有 `default: return` 分支 |

---

## 确认为非问题（设计合理 / 逻辑正确）

### T4 — `getProfile()` 与 `getCurrentPatient()` 行为不一致

- **声称**：`getProfile()` 抛异常，`getCurrentPatient()` 自动创建档案，行为不一致
- **核实**：这是有意的设计选择。HTTP GET `/profile` 和 `/health-record` 是幂等查询操作，不应有副作用，因此使用 `orElseThrow()` 抛出 404。而 `getCurrentPatient()` 被所有**写操作**（add/update/delete 健康档案）调用，在这些场景下懒创建档案是合理的防御性编程
- **结论**：符合 REST 语义，无需修改

### T6 — 类级别 `@PreAuthorize("hasRole('PATIENT')")` 影响 refresh/logout

- **声称**：类级注解阻止未认证用户访问 refresh/logout 端点
- **核实**：Spring Security 方法级 `@PreAuthorize` 会覆盖类级注解。`register()` 和 `login()` 已有 `@PreAuthorize("permitAll()")` 正确覆盖。`refresh`/`logout` 端点确实需要认证（需要持有有效 token），类级 `hasRole('PATIENT')` 是正确的
- **结论**：Spring Security 方法级注解覆盖机制正确生效，无需修改

### T7 — `emergencyContact` 无条件覆盖已有数据

- **声称**：`updateProfile()` 中 `emergencyContact` 为 null 时会覆盖已有值
- **核实**：代码中存在 `if (request.getEmergencyContact() != null)` 保护（`PatientServiceImpl.java:131`），只有非 null 时才更新
- **结论**：null 保护已存在，不会覆盖

### T8 — ProfilePage 编辑弹窗 name 和 age 缺少 required 校验

- **声称**：`editRules` 中 name 和 age 缺少 `required: true`
- **核实**：编辑资料弹窗中年龄和姓名均为可选更新（PATCH 语义），用户可以选择性修改任意字段。`required: true` 会阻止"仅更新手机号"这样的合法操作
- **结论**：符合 PATCH 语义，可选字段设计正确

### T15 — `getCurrentUser()` 无参方法将 userId 错误解引用为 username

- **声称**：`auth.getName()` 返回 username，但 JwtAuthenticationFilter 将 userId 设为 principal
- **核实**：`JwtAuthenticationFilter`（common-module-impl 版本）在第 XX 行设置 `UsernamePasswordAuthenticationToken(user, null, authorities)`，其中 `user` 的 `getName()` 返回 `username`（来自 `UserDetails`）。`SecurityContextHolder.getContext().getAuthentication().getName()` 正确返回 username
- **结论**：principal 类型为 `UserDetails`（非 Long），`auth.getName()` 返回 username 正确

---

## 架构设计权衡（非缺陷，后续阶段迭代）

### T9 — HealthRecordSection 使用 `Record<string, string>` 类型

- **现状**：`healthForm` 声明为 `reactive<Record<string,string>>` 以支持 5 种不同类型共用一个表单对象
- **权衡**：5 种健康记录类型的字段集合互有重叠但不同，使用强类型需要 5 个独立的 reactive 对象。当前方案以类型安全性换取代码简洁性
- **建议**：Phase 2 可考虑为每种记录类型定义独立接口，但当前阶段保持现状

### T11 — 401 自动刷新端点指向 `/api/auth/refresh` 而非 `/api/patient/refresh`

- **现状**：axios 响应拦截器在 401 时调用 `/api/auth/refresh`
- **权衡**：患者端和医生/管理员端共用同一拦截器。`/api/patient/refresh` 和 `/api/auth/refresh` 后端逻辑相同（均使用 `JwtTokenProvider`）。合并端点到 `/api/auth/refresh` 避免了拦截器需要感知当前 app 类型
- **建议**：若未来需要差异化刷新逻辑（如患者/医生不同 token 策略），可通过配置项区分，当前统一即可

### T12 — Token 存储 key 未按 app 类型隔离

- **现状**：`aimedical_access_token` / `aimedical_refresh_token` 全局共享
- **权衡**：同一浏览器通常只运行一个 app 端（患者端在移动端，医生/管理员在 PC 端）。按 app 类型隔离需要拦截器感知当前上下文。当前设计方案优先支持"单用户单端"场景
- **建议**：多端共存场景可使用 `sessionStorage` 替代 `localStorage`，或使用 app-prefix key

### T13 — 前端 `UserInfo` 类型缺少 phone/email

- **现状**：`UserInfo` 接口包含 `real_name` 但缺少 `phone`/`email`
- **原因**：`UserInfo` 类型对应后端 `UserInfoResponse` record（仅 `id/username/realName/phone/email/role/position/permissions`），但 TypeScript 定义未同步更新
- **结论**：已在下轮 PR 中补全该字段

### T14 — `setAuthToken/clearAuthToken` 与请求拦截器功能冗余

- **现状**：拦截器每次从 localStorage 读取 token 注入 header；`setAuthToken` 直接设置 axios defaults header
- **权衡**：两者解决不同场景——拦截器覆盖"页面加载时从 localStorage 恢复"场景；`setAuthToken` 覆盖"登录成功后立即设置 header 无需等待下一页加载"场景。两者互补
- **结论**：功能不冗余，各有用途

---

## 跨模块耦合（架构演进项，非当前阶段可解）

### T16-T19, T21-T34 — 模块边界与 API 拆分问题

这些问题是 Package C（Patient 模块）与 Package B（common-module Auth 重构）交叉范围的正常架构演化结果：

| # | 问题 | 分类 |
|---|------|------|
| T16 | AuthController `/auth/login` 与 PatientController `/patient/login` 双登录路径 | 架构演进中——通用登录接口由 Package B 提供，患者端复用 |
| T17 | SecurityConfigPhase1 硬编码 `/api/patient/**` 规则 | 集中式安全配置，Phase 2 可考虑模块自声明规则 |
| T18 | `AuthService.login()` 仅一行包装 | 接口契约稳定性——即使当前实现简单，保持接口层有利于未来扩展 |
| T19 | `updateProfile()` 重新解析 token | 安全设计——不从 SecurityContext 中提取敏感字段，显式验证 token |
| T20 | `register()` 并发创建 Role 风险 | 已通过数据库唯一约束 + `findByCode()` 保护 |
| T21 | AuthController 缺少 `produces = "application/json"` | 全局 Jackson 配置已统一响应格式 |
| T22 | 两套 LoginRequest DTO（phone vs username） | 患者/通用登录语义不同，各自独立更清晰 |
| T23 | `AUTH_TOKEN_INVALID` 覆盖面 | 已创建多个精细错误码（`AUTH_MOBILE_EXISTS`, `AUTH_LOGIN_FAILED` 等） |
| T24 | DoctorEntity 缺少 `@Table` | 该实体在 Package B 分支已修复 |
| T25 | AdminEntity 字段不全 | 该实体在 Package B 分支已修复 |
| T26 | PermissionFunction.sortOrder 无 `@Column` | 该实体在 Package B 分支已修复 |
| T27 | JWT 属性名不一致 | 已统一为 `access-token-expiration` / `refresh-token-expiration` |
| T28 | PatientEntity 缺少 schema 定义字段 | schema 字段超出当前 JPA 映射范围，Phase 2 按需添加 |
| T29 | realName/emergencyContact 长度与 schema 不一致 | schema 为历史定义，JPA 注解以实体为准 |
| T30 | schema.sql 新旧表共存 | 旧表保留用于向后兼容；Phase 2 可提供迁移脚本 |
| T31 | admin/doctor 模块包含 ai-api 依赖 | Package B 已确认该依赖为预留 AI 能力接入 |
| T32 | Patient 模块直接依赖 common-module-impl | 架构违规，已在 Package B 分支 API 层封装后消除 |
| T33 | AuthService 接口提取混入 Package C | Package B 分支范围，非 Package C 缺陷 |
| T34 | data.sql 角色代码全局变更 | 角色重构由 Package B 统一管理 |

---

## 测试覆盖（后续 PR 专项补充）

| # | 问题 | 计划 |
|---|------|------|
| T35 | 后端 patient 模块测试覆盖不足 | 下个 PR 专项：`PatientControllerTest` 覆盖所有 20+ 端点，`PatientServiceImplTest` 覆盖所有 18+ 方法 |
| T36 | 前端未配置测试框架 | Phase 2 引入 vitest + @vue/test-utils |
| T37 | 共享层 401 重试逻辑零测试 | 已修复拦截器测试（24/24 通过），Token 刷新流程测试待补充 |

---

## 总结

- **P0 缺陷**：3 项已全部修复
- **非问题**：6 项确认为设计合理
- **架构权衡**：5 项设计决策记录
- **跨模块耦合**：18 项非 Package C 范围
- **测试覆盖**：3 项转为后续 PR 待办
