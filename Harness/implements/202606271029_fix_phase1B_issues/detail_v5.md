# 详细设计（v5）

## 概述

修复 AuthServiceTest 中两个错误路径测试方法的 UnnecessaryStubbingException：删除两行永远不会被调用的 `when(userConverter.toUserInfoResponse(any())).thenThrow(...)` stub。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | 修改（删除 2 行） | 移除多余 mock stub |

## 修改细节

### 1. `getCurrentUser_shouldThrowWhenUserNotFound`（第 767-775 行）

**当前代码**：
```java
@Test
void getCurrentUser_shouldThrowWhenUserNotFound() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());
    when(userConverter.toUserInfoResponse(any())).thenThrow(new RuntimeException("should not reach here")); // ← 删除此行

    BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.getCurrentUser(999L));
    assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
    assertEquals("用户不存在", ex.getArgs()[0]);
}
```

**修改后**：
```java
@Test
void getCurrentUser_shouldThrowWhenUserNotFound() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.getCurrentUser(999L));
    assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
    assertEquals("用户不存在", ex.getArgs()[0]);
}
```

**理由**：`userRepository.findById(999L)` 返回 `Optional.empty()`，`orElseThrow` 立即抛出 `BusinessException`，不会到达 `userConverter.toUserInfoResponse()`，该 stub 永不执行。

### 2. `getCurrentUser_shouldThrowWhenUserIdNull`（第 777-785 行）

**当前代码**：
```java
@Test
void getCurrentUser_shouldThrowWhenUserIdNull() {
    when(userRepository.findById(null)).thenReturn(Optional.empty());
    when(userConverter.toUserInfoResponse(any())).thenThrow(new RuntimeException("should not reach here")); // ← 删除此行

    BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.getCurrentUser(null));
    assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
}
```

**修改后**：
```java
@Test
void getCurrentUser_shouldThrowWhenUserIdNull() {
    when(userRepository.findById(null)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.getCurrentUser(null));
    assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
}
```

**理由**：同上，`findById(null)` 返回 empty，提前抛出。

## 错误处理

无变更。错误路径由 `orElseThrow` 已在 Service 实现中处理，测试验证该行为。

## 行为契约

- 不修改任何生产代码，仅删除测试中多余的 stub
- 两个测试的行为不变：仍然验证 `userId=999L` 和 `userId=null` 时返回 `NOT_FOUND`
- Mockito 不会因未使用的 stub 报错

## 依赖关系

无变更。不涉及其他文件修改。

## 修订说明（v5 r1）

| 审查意见 | 修改措施 |
|---------|---------|
| v4 验证发现 2 个 `UnnecessaryStubbingException` | 删除两处多余的 `userConverter.toUserInfoResponse(any())` stub，该 stub 因 `orElseThrow` 提前抛出而永不执行 |
