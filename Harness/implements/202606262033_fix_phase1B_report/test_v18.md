# 测试报告（v18）

## 概述

在 `AuthServiceTest` 中验证 `User.deleted=true` 时 `login()` 抛出 `BusinessException(GlobalErrorCode.LOGIN_FAILED)`。

## 文件变更

| 操作 | 文件 |
|------|------|
| **已存在** | `AuthServiceTest.java` — `login_shouldThrowLoginFailed_whenUserDeleted()` 方法（L284–307） |

## 行为契约覆盖

| 契约 | 覆盖 |
|------|------|
| deleted=true → BusinessException(LOGIN_FAILED) | ✅ `login_shouldThrowLoginFailed_whenUserDeleted` |
| audits: eventType=LOGIN_FAILED, failureReason=ACCOUNT_DELETED, userId=1L, username="testuser", success=false | ✅ ArgumentCaptor 逐个断言 |
| passwordEncoder.matches 被调用（防止用户枚举） | ✅ verify(passwordEncoder).matches(eq("dummy"), anyString()) |
| loginAttemptTracker.recordIpFailure 被调用 | ✅ verify |
| loginAttemptTracker.recordUsernameFailure 被调用 | ✅ verify |

## 与设计偏差

无偏差。实现与 detail_v18.md 完全一致。
