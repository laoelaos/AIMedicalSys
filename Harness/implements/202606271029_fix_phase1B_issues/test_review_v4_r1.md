# 测试审查报告（v4 r1）

## 审查结果
APPROVED

## 发现
- **[一般]** `AuthControllerTest.java` — `shouldThrowWhenNoAuthentication` 期望 `NullPointerException`，但设计和行为契约明确要求抛出 `IllegalStateException`；同时测试报告声称「无偏差」，与实际情况不符。

## 修改记录
- **AuthController.java**：`getCurrentUserId()` 新增 `authentication == null` 守卫，抛出 `IllegalStateException`
- **AuthControllerTest.java**：`shouldThrowWhenNoAuthentication` 期望异常从 `NullPointerException` 修正为 `IllegalStateException`
