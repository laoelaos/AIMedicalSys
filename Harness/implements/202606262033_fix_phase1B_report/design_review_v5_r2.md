# 设计审查报告（v5 r2）

## 审查结果
REJECTED

## 发现

- **[一般]** 设计第74-76行对运行时消息行为的分析有事实性错误。原文声称 `BusinessException(ErrorCode, String)` 的第二个参数 `"存在子菜单，无法删除，请先删除子菜单"` 会"覆盖枚举 message 作为返回体的 message 字段"，从而"避免 API 响应报文变化"。但检查 `GlobalExceptionHandler` (L23-27) 和 `Result.fail(ErrorCode)` (L37-39) 后发现：handler 调用的是 `Result.fail(errorCode)`，该函数使用 `errorCode.getMessage()` 构造响应。`BusinessException` 的 `args` 参数（第二个参数）仅存储在异常对象中，**从未被 handler 用于构造响应体**。实际响应 message 由枚举的 `getMessage()` 决定：
  - 修改前：`errorCode = PARAM_INVALID` → 响应 message = `"参数校验失败"`
  - 修改后：`errorCode = CHILDREN_EXIST` → 响应 message = `"存在子菜单，无法删除"`
  
  响应 message 确实会发生变化（从 `"参数校验失败"` 变为 `"存在子菜单，无法删除"`），这与设计声称的"避免 API 响应报文变化"矛盾。**不过**，变化后的 message (`"存在子菜单，无法删除"`) 恰好符合 OOD 规范，故修改方向正确。设计应修正其分析表述，如实说明消息行为。

- **[轻微]** 设计第99行引用方法名为 `GlobalExceptionHandler.getHttpStatus`，但实际方法名为 `resolveHttpStatus`（GlobalExceptionHandler.java:38）。

## 修改要求

1. 修正第74-76行关于 `BusinessException` 消息参数的运行时行为分析。澄清第二个参数 `"存在子菜单，无法删除，请先删除子菜单"` 不会被 handler 用于响应体；响应 message 来自枚举的 `getMessage()`，因此从 `"参数校验失败"` 变为 `"存在子菜单，无法删除"`（此变化符合 OOD，可接受）。
2. 将第99行 `getHttpStatus` 修正为 `resolveHttpStatus`。
