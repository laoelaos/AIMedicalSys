# 实现报告（v18）

## 概述
在 `AuthServiceTest` 中新增 `login_shouldThrowLoginFailed_whenUserDeleted()` 测试方法，覆盖 `User.deleted=true` 时 `AuthServiceImpl.login()` 抛出 `BusinessException(GlobalErrorCode.LOGIN_FAILED)` 的场景。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | 在 `login_shouldThrowUserDeleted()` 后插入新 `@Test` 方法 |

## 编译验证
通过：`mvn compile test-compile -pl common-module-impl -am -q` 返回 BUILD_OK

## 设计偏差说明
无偏差
