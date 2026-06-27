# 测试报告（v5）

## 概述
删除 AuthServiceTest 中两个测试方法里多余的 `when(userConverter.toUserInfoResponse(any())).thenThrow(...)` stub，修复 UnnecessaryStubbingException。

## 文件变更

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AuthServiceTest.java` | 两处各删除 1 行永不执行的 mock stub |

## 行为契约验证

### AuthServiceTest.getCurrentUser

| 测试方法 | 维度 | 验证点 | 状态 |
|---------|------|--------|------|
| `getCurrentUser_shouldSucceed` | 正常路径 | userId 存在返回 UserInfoResponse，id 匹配 | ✅ 未变更 |
| `getCurrentUser_shouldThrowWhenUserNotFound` | 错误路径 | userId=999L 抛出 BusinessException(NOT_FOUND, "用户不存在") | ✅ 行为不变，删除未使用的 stub |
| `getCurrentUser_shouldThrowWhenUserIdNull` | 边界条件 | userId=null 抛出 BusinessException(NOT_FOUND) | ✅ 行为不变，删除未使用的 stub |

## 设计偏差说明
无偏差。测试行为与详细设计完全一致。
