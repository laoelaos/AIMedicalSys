# 详细设计（v10）

## 概述

修复 `AuthControllerTest.java` 第 273-274 行中 `shouldThrowBusinessExceptionWhenChangePasswordFails()` 的 Mockito 语法错误：void 方法 `authService.changePassword()` 误用 `when().thenThrow()`，改为 `doThrow().when()` 模式。

这是 **v9 验证失败的唯一原因**，源码（AuthController.java）无需任何改动。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java` | 修改 | 将 L273-L274 的 `when().thenThrow()` 替换为 `doThrow().when()` |

## 类型定义

无新增类型。

## 方法变更

### 1. shouldThrowBusinessExceptionWhenChangePasswordFails() — Mockito 模式修正

**修改前**（L273-L274）：
```java
when(authService.changePassword(anyLong(), anyString(), anyString()))
        .thenThrow(new BusinessException(GlobalErrorCode.PASSWORD_MISMATCH));
```

**修改后**：
```java
doThrow(new BusinessException(GlobalErrorCode.PASSWORD_MISMATCH))
        .when(authService).changePassword(anyLong(), anyString(), anyString());
```

**变更原因**：`AuthService.changePassword(Long, String, String)` 声明的返回类型为 `void`，不能作为 `when()` 的参数表达式（Java 编译器拒绝 void 类型出现在方法调用参数位置）。`doThrow().when()` 是 Mockito 为 void 方法提供的专门语法。

**方法其余部分不变**（L264-L272，L276-L279）。

## 错误处理

不变。测试仍验证 `authService.changePassword()` 抛出 `BusinessException` 时，`AuthController.changePassword()` 正确传播该异常。

## 行为契约

- 前置条件：SecurityContext 已设置（principal=1L），同 v9 设计
- 输入：`PasswordChangeRequest("wrongOldPass", "newPass123")`
- 模拟行为：`authService.changePassword()` 抛出 `BusinessException(GlobalErrorCode.PASSWORD_MISMATCH)`
- 验证：`assertThrows(BusinessException.class, ...)` 捕获异常 + `verify(authService).changePassword(1L, "wrongOldPass", "newPass123")` 确认调用
- 后置：无变化

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `org.mockito.Mockito.doThrow` | 已有 `import static org.mockito.Mockito.*;`（L29），无需新增 import |

## 修订说明（v10 r1）

| 审查意见 | 修改措施 |
|---------|---------|
| v9 验证失败：`AuthControllerTest.java:[273,44] 此处不允许使用 '空' 类型` | 将 L273-L274 `when(authService.changePassword(...)).thenThrow(...)` 替换为 `doThrow(...).when(authService).changePassword(...)` |
