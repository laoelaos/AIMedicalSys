# 任务指令（v10）

## 动作
RETRY

## 任务描述
修复 AuthControllerTest.java 中测试编译错误：将 shouldThrowBusinessExceptionWhenChangePasswordFails() 中 `when(authService.changePassword(...)).thenThrow(...)` 改为 `doThrow(...).when(authService).changePassword(...)`。

**文件路径**：
`modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java`

**具体修改位置**：第 273-274 行

修改前：
```java
when(authService.changePassword(anyLong(), anyString(), anyString()))
        .thenThrow(new BusinessException(GlobalErrorCode.PASSWORD_MISMATCH));
```

修改后：
```java
doThrow(new BusinessException(GlobalErrorCode.PASSWORD_MISMATCH))
        .when(authService).changePassword(anyLong(), anyString(), anyString());
```

**额外 import 检查**：确认 `org.mockito.Mockito.doThrow` 已导入（或使用静态 import `doThrow`），如果未导入则添加。

## 选择理由
源码修改已验证通过（`mvn compile` 成功），仅测试代码中 Mockito 语法错误。`AuthService.changePassword()` 声明为 `void`，不能在 `when()` 内作为参数，须用 `doThrow().when()` 模式。

## 任务上下文
- R9 源码改动已全部完成并通过编译
- 验证报告 verify_v9.md 显示唯一失败原因：`AuthControllerTest.java:[273,44] 此处不允许使用 '空' 类型`
- 这是 Mockito 常见用法错误：void 方法不能用 `when().thenThrow()`，必须用 `doThrow().when()`

## 已有代码上下文
- `AuthService.java:21` 声明 `void changePassword(Long userId, String oldPassword, String newPassword)`
- `AuthControllerTest.java:264-279` 包含 shouldThrowBusinessExceptionWhenChangePasswordFails() 测试方法
- Mockito `doThrow` 静态方法在 `org.mockito.Mockito.doThrow` 下

## RETRY 说明
**失败原因**：`when(authService.changePassword(anyLong(), anyString(), anyString()))` 中 changePassword 返回 void，不能作为 when() 的参数表达式。Maven test-compile 阶段报编译错误。

**修正方向**：将 `when(...).thenThrow(...)` 替换为 `doThrow(...).when(...)...` 语法。源码（AuthController.java）无需任何改动。
