# 设计审查报告（v4 r2）

## 审查结果
APPROVED

## 发现

- **[轻微]** 测试异常实例缺少构造参数：设计仅描述"直接创建 `HttpMessageNotReadableException` 实例"/"直接创建 `HttpMessageNotWritableException` 实例"，但两个类（位于 `org.springframework.http.converter` 包，Spring 5.3.x）均无无参构造器，至少需要 `String msg` 参数。现有测试（如 `shouldHandleValidationExceptionWith400`）显式传入了构造参数。建议测试描述中明确写出如 `new HttpMessageNotReadableException("test")` 或 `new HttpMessageNotReadableException("test", new RuntimeException())`，避免编码时遗漏。

- **[轻微]** 响应体 null 断言未显式声明：设计步骤第 5 步"验证状态码=400、body.getCode()=..."未在 `body.getCode()` 前显式编写 `assertNotNull(body)`。虽然第 6 步的 `assertNull(body.getData())` 间接要求 body 非空，但与现有 `shouldHandleBusinessExceptionWith400`、`shouldHandleGenericExceptionWith500` 的显式 `assertNotNull(body)` 模式不一致。建议统一。

## 修改要求
无 — 以上均为**[轻微]**级别，不影响编码可行性，不触发驳回。
