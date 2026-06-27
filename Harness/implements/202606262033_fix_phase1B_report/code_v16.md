# 实现报告（v16）

## 概述
实现 T9 修复：UserConverter 补充三处过滤逻辑（禁用角色过滤、null-safe sort、禁用权限过滤），UserFacadeImpl 将转换职责委托给 UserConverter，删除冗余私有方法。同步更新测试。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `common-module-impl/.../auth/converter/UserConverter.java` | `resolveRole()` 补充 `.filter(Role::getEnabled)` + null-safe sort；`resolvePermissions()` 两层循环补充 `Boolean.TRUE.equals(function.getEnabled())` 过滤 |
| 修改 | `common-module-impl/.../auth/UserFacadeImpl.java` | 注入 `UserConverter`，`toUserInfoResponse()` 委托给 `userConverter`，删除 `resolvePrimaryRole`、`resolvePosition`、`resolvePermissions` 三个私有方法及对应的 import |
| 修改 | `common-module-impl/.../auth/converter/UserConverterTest.java` | 新增 `shouldFilterDisabledRole`、`shouldHandleNullSort`、`shouldFilterDisabledPermission` 三个测试 |
| 修改 | `common-module-impl/.../auth/UserFacadeImplTest.java` | 注入 `UserConverter` mock，5 个受影响的测试方法增加 mock 设置 |

## 编译验证
编译通过，20 tests run（UserConverterTest 8 + UserFacadeImplTest 12），0 failures，0 errors。

## 设计偏差说明
无偏差。
