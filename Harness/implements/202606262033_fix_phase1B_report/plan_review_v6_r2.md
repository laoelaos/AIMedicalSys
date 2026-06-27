# 计划审查报告（v6 r2）

## 审查结果
REJECTED

## 发现

- **[严重]** `resolveHttpStatus()` 的"当前代码"示例与实际源代码严重不一致。任务 `T25: 429 状态码映射` 节展示 switch 表达式返回 `int`：
  ```java
  return switch (code) { case "UNAUTHORIZED" -> 401; ... default -> 400; };
  ```
  但实际源代码使用 if-else 链返回 `HttpStatus` 枚举：
  ```java
  if (GlobalErrorCode.UNAUTHORIZED.getCode().equals(code)) { return HttpStatus.UNAUTHORIZED; }
  ```
  这导致"修改后"指导 `case "RATE_LIMITED", "ACCOUNT_LOCKED" -> 429;` 在实际代码中不适用——实现者若照搬将写出无法编译或不符合代码结构的代码（返回类型不匹配、switch/case 与 if-else 不兼容）。**必须修正代码示例使其如实反映当前代码，并给出正确的修改指导（在 if-else 链中增加 `RATE_LIMITED` / `ACCOUNT_LOCKED` 到 `HttpStatus.TOO_MANY_REQUESTS` 的映射）。**

## 修改要求
1. **[严重] resolveHttpStatus 代码示例修正**：将 T25 节中"当前代码"和"修改后"的代码块修正为与实际 if-else + `HttpStatus` 结构一致。实际代码为 6 个 if 语句 + 1 个 default return，建议修改方案为在 default return 前插入：`if (GlobalErrorCode.RATE_LIMITED.getCode().equals(code) || GlobalErrorCode.ACCOUNT_LOCKED.getCode().equals(code)) { return HttpStatus.TOO_MANY_REQUESTS; }`。
