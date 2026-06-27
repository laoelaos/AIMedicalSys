# 测试报告（v5）

## 概述

为 fix_phase1B_report v5 变更验证三项 error code 标准化缺陷修复：GlobalErrorCode 消息文本对齐 OOD 规范，MenuServiceImpl.deleteMenu() 错误码替换。

## 测试文件检查清单

| 测试文件 | 操作 | 说明 |
|---------|------|------|
| `common/src/test/java/.../GlobalErrorCodeTest.java` | 已更新 | UNAUTHORIZED 消息预期值 `"未认证"` → `"未认证或令牌已失效"`；FORBIDDEN 消息预期值 `"无权限"` → `"无权限访问"`。实际检查：与详细设计一致，无需修改。 |
| `modules/common-module/.../MenuServiceTest.java` | 增强 | `shouldThrowBusinessExceptionWhenHasChildren` 增加 `getErrorCode()` 断言验证错误码为 `CHILDREN_EXIST`。 |

## 行为契约覆盖

| 行为契约 | 测试方法 | 覆盖维度 |
|---------|---------|---------|
| `UNAUTHORIZED.getMessage()` → `"未认证或令牌已失效"` | `GlobalErrorCodeTest.unauthorizedShouldReturnCorrectCodeAndMessage()` | 正常路径 |
| `FORBIDDEN.getMessage()` → `"无权限访问"` | `GlobalErrorCodeTest.forbiddenShouldReturnCorrectCodeAndMessage()` | 正常路径 |
| `deleteMenu()` 子菜单存在时抛出 `CHILDREN_EXIST` | `MenuServiceTest.DeleteMenuTests.shouldThrowBusinessExceptionWhenHasChildren()` | 错误路径、状态交互 |
| 其他枚举 getCode()/getMessage() 不变 | `GlobalErrorCodeTest` 各专用测试方法 | 回归验证 |

## 设计偏差说明

无偏差。GlobalErrorCodeTest.java 中 UNAUTHORIZED 和 FORBIDDEN 断言值已由实现 agent 同步更新，与详细设计一致。

## 变更详情

### MenuServiceTest.java

- 新增 `import com.aimedical.common.exception.GlobalErrorCode;`
- 修改 `shouldThrowBusinessExceptionWhenHasChildren`：增加 `assertEquals(GlobalErrorCode.CHILDREN_EXIST, exception.getErrorCode())` 断言，验证行为契约中错误码从 `PARAM_INVALID` 变更为 `CHILDREN_EXIST` 的语义。
