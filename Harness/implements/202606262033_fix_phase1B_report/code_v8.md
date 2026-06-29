# 实现报告（v8）

## 概述

对 `AuthServiceImpl.java` 实施 4 处修改，修复 T5（refreshToken 缺少 IP 失败记录）和 T13（dummy 比对使用 encode 而非 matches）两个安全问题。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java` | 新增 DUMMY_HASH 常量；login() 两处 encode→matches；refreshToken() 用户禁用/删除分支插入 recordIpFailure |

## 编译验证

未执行编译验证

## 设计偏差说明

无偏差。所有修改严格遵循 detail_v8.md 设计规格。

### 修改详情

1. **L52** — 新增 `private static final String DUMMY_HASH` 常量
2. **L106** — `login()` 用户不存在分支：`passwordEncoder.encode("dummy")` → `passwordEncoder.matches("dummy", DUMMY_HASH)`
3. **L114** — `login()` 用户禁用/删除分支：`passwordEncoder.encode("dummy")` → `passwordEncoder.matches("dummy", DUMMY_HASH)`
4. **L184** — `refreshToken()` 用户不存在/禁用/删除分支：在 `throw` 前插入 `loginAttemptTracker.recordIpFailure(getClientIp())`
