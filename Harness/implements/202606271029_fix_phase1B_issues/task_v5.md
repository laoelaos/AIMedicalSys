# 任务指令（v5）

## 动作
RETRY

## 任务描述
修复 AuthServiceTest 中两个错误路径测试方法的 UnnecessaryStubbingException：
- `getCurrentUser_shouldThrowWhenUserNotFound`（第 769 行）
- `getCurrentUser_shouldThrowWhenUserIdNull`（第 780 行）

具体修改：在以上两个方法中各删除一行 `when(userConverter.toUserInfoResponse(any())).thenThrow(...)` — 该 stub 因 `orElseThrow` 提前抛出而永远不会被调用，Mockito 严格模式报 UnnecessaryStubbingException。

## 选择理由
R4 验证失败（391 run, 2 Errors），均为同一个问题。不需要修改 AuthService 接口、AuthServiceImpl 实现、AuthController 或 AuthControllerTest。只需在 AuthServiceTest 中删除两行多余的 mock stub。

## 任务上下文
- 影响文件：`AuthServiceTest.java`（仅删除两行）
- `getCurrentUser_shouldThrowWhenUserNotFound`（第 766-775 行）：
  - 有问题的 stub：`when(userConverter.toUserInfoResponse(any())).thenThrow(new RuntimeException("should not reach here"));`
  - 原因：`userRepository.findById(999L)` 返回 `Optional.empty()`，`orElseThrow` 立即抛出 BusinessException，never reaches converter
- `getCurrentUser_shouldThrowWhenUserIdNull`（第 777-785 行）：
  - 有问题的 stub：`when(userConverter.toUserInfoResponse(any())).thenThrow(new RuntimeException("should not reach here"));`
  - 原因：同上，`userId=null` 时 `findById(null)` 返回 empty，提前抛出
- 不涉及其他文件修改

## 已有代码上下文
文件路径：`common-module-impl/src/test/java/.../service/AuthServiceTest.java`
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

@Test
void getCurrentUser_shouldThrowWhenUserIdNull() {
    when(userRepository.findById(null)).thenReturn(Optional.empty());
    when(userConverter.toUserInfoResponse(any())).thenThrow(new RuntimeException("should not reach here")); // ← 删除此行
    BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.getCurrentUser(null));
    assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
}
```

## RETRY 说明
验证报告中 2 个 Error 为 `UnnecessaryStubbingException`：
- `AuthServiceTest.getCurrentUser_shouldThrowWhenUserNotFound` — AuthServiceTest.java:769
- `AuthServiceTest.getCurrentUser_shouldThrowWhenUserIdNull` — AuthServiceTest.java:780

修正方向：删除两处多余的 `userConverter.toUserInfoResponse(any())` stub，重新运行测试验证全部通过。
