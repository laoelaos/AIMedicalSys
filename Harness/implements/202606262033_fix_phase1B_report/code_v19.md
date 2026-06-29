# 实现报告（v19）

## 概述
在 `AuthServiceTest.login_shouldThrowIpLocked()` 和 `login_shouldThrowUsernameLocked()` 方法末尾各追加一条 `ex.getArgs()` 断言，验证 `BusinessException` 携带正确的锁定时间参数。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java | 两个方法末尾追加 args 断言 |

## 编译验证
未执行编译验证

## 设计偏差说明
无偏差
