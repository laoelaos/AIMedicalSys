# 计划审查报告（v6 r1）

## 审查结果
REJECTED

## 发现

### [严重] task_v6.md 中 handleBusinessException 方法签名与现有代码不匹配

- **问题**：task_v6.md "修改后目标" 代码块将方法签名展示为 `public Result<Void> handleBusinessException(BusinessException e)`，但现有代码实际为 `public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e)`，并在方法体内调用 `resolveHttpStatus()` 设置 HTTP 状态码。
- **为什么是问题**：若实现者按 task 字面修改为 `Result<Void>` 返回类型，将丢失 `ResponseEntity.status(status)` 的 HTTP 状态码控制，直接导致 T25（状态码映射）无法正确实现——所有 BusinessException 将默认返回 200 而非 429/401/403 等预期状态码。此外，现有测试 `GlobalExceptionHandlerTest` 均断言 `ResponseEntity<Result<Void>>` 返回类型及状态码，修改后将全部编译失败。
- **期望修正方向**：修正 task_v6.md 的代码示例，保持返回类型 `ResponseEntity<Result<Void>>` 和 `resolveHttpStatus()` 调用结构不变，仅在 body 构造处将 `Result.fail(errorCode)` 替换为 `Result.fail(errorCode.getCode(), interpolatedMessage)`：
  ```java
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
      ErrorCode errorCode = e.getErrorCode();
      HttpStatus status = resolveHttpStatus(errorCode);
      log.warn("Business exception: code={}, message={}", errorCode.getCode(), e.getMessage());
      String message = formatMessage(errorCode.getMessage(), e.getArgs());
      return ResponseEntity.status(status)
              .body(Result.fail(errorCode.getCode(), message));
  }
  ```

### [一般] task_v6.md 中测试文件路径的包名错误

- **问题**：task_v6.md "影响文件清单" 中测试文件路径为 `common/src/test/java/com/aimedical/common/exception/GlobalExceptionHandlerTest.java`，但实际文件位于 `common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java`（`config` 包，非 `exception` 包）。
- **为什么是问题**：可能导致实现者按错误路径查找文件，浪费时间或在错误位置创建新文件。
- **期望修正方向**：将路径修正为 `common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java`。

### [轻微] task_v6.md 中现有测试方法名不精确

- **问题**：task_v6.md 第 87 行引用现有测试方法 `shouldHandleBusinessException`，但实际方法名为 `shouldHandleBusinessExceptionWith400`。
- **为什么是问题**：不影响实现正确性，但降低文档精确度。
- **期望修正方向**：将引用更新为 `shouldHandleBusinessExceptionWith400`。

## 修改要求

1. **[严重]** 修正 task_v6.md 中 `handleBusinessException` 方法签名为 `ResponseEntity<Result<Void>>`，保留 `resolveHttpStatus()` 调用结构
2. **[一般]** 修正 task_v6.md 中测试文件路径的包名
3. **[轻微]** 修正 task_v6.md 中现有测试方法名引用
