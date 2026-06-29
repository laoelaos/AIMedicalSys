# 测试报告（v14）

## 概述

为 T23（`MenuServiceImpl.getUserMenuTree()` N+1 修复）编写和更新测试。基于行为契约覆盖新增的 `findWithDetailsForMenuById` 方法及 `getUserMenuTree` 方法变更。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `modules/common-module/common-module-impl/src/test/java/.../service/MenuServiceTest.java` | `getUserMenuTree` 测试 mock 从 `findById` 改为 `findWithDetailsForMenuById`（3 处）；新增岗位无权限边界用例 |
| 修改 | `modules/common-module/common-module-impl/src/test/java/.../permission/UserRepositoryTest.java` | 新增 `findWithDetailsForMenuById` 方法存在性、EntityGraph 注解验证、存在/不存在数据查询共 4 个测试 |

## 变更详情

### MenuServiceTest.java

**已有测试 mock 更新（3 处）**：

| 测试方法 | 行号 | 修改前 | 修改后 |
|---------|------|--------|--------|
| `shouldGetUserMenuTreeSuccessfully` | 84 | `findById(1L)` | `findWithDetailsForMenuById(1L)` |
| `shouldReturnEmptyListWhenUserNotFound` | 98 | `findById(999L)` | `findWithDetailsForMenuById(999L)` |
| `shouldReturnEmptyListWhenUserHasNoPosts` | 110 | `findById(1L)` | `findWithDetailsForMenuById(1L)` |

**新增边界用例**：

| 测试方法 | 行号 | 覆盖维度 | 说明 |
|---------|------|---------|------|
| `shouldReturnEmptyListWhenUserHasPostsWithoutFunctions` | 118-128 | 边界条件 | 用户有岗位，但岗位的 functions 为 null → 返回空列表 |

### UserRepositoryTest.java

| 测试方法 | 覆盖维度 | 说明 |
|---------|---------|------|
| `shouldHaveFindWithDetailsForMenuByIdMethod` | 正常路径 | 反射验证方法存在且返回 `Optional<User>` |
| `shouldHaveEntityGraphAnnotationOnFindWithDetailsForMenuById` | 正常路径 | 反射验证 `@EntityGraph(attributePaths = {"roles", "posts", "posts.functions"})` |
| `shouldFindWithDetailsForMenuByIdReturnUserWhenExists` | 正常路径 | 集成测试：持久化用户后按 ID 查询成功返回 |
| `shouldFindWithDetailsForMenuByIdReturnEmptyWhenNotFound` | 错误路径 | 集成测试：不存在的 ID 返回 `Optional.empty()` |

## 测试覆盖矩阵

| 行为契约 | 覆盖维度 | 测试方法 |
|---------|---------|---------|
| `findWithDetailsForMenuById` 方法存在且返回 `Optional<User>` | 正常路径 | `shouldHaveFindWithDetailsForMenuByIdMethod` |
| `@EntityGraph` 含正确属性路径 | 正常路径 | `shouldHaveEntityGraphAnnotationOnFindWithDetailsForMenuById` |
| 存在 ID 返回对应用户 | 正常路径 | `shouldFindWithDetailsForMenuByIdReturnUserWhenExists` |
| 不存在 ID 返回 empty | 错误路径 | `shouldFindWithDetailsForMenuByIdReturnEmptyWhenNotFound` |
| `getUserMenuTree` 正常返回菜单树 | 正常路径 | `shouldGetUserMenuTreeSuccessfully` |
| 用户不存在返回空列表 | 错误路径 | `shouldReturnEmptyListWhenUserNotFound` |
| 用户无岗位返回空列表 | 边界条件 | `shouldReturnEmptyListWhenUserHasNoPosts` |
| 岗位无 functions 返回空列表 | 边界条件 | `shouldReturnEmptyListWhenUserHasPostsWithoutFunctions` |

## 不受影响文件

- `JwtAuthenticationFilterTest.java` — 仍使用 `findWithDetailsById`，无变更
- `AuthServiceTest.java` — 仍使用 `findByUsername`，无变更
- 其他测试文件不受本次变更影响

## 设计偏差说明

- 新增 `shouldReturnEmptyListWhenUserHasPostsWithoutFunctions` 边界用例，设计文档未明确列出但符合行为契约覆盖要求
- 新增 `UserRepositoryTest.java` 中 `findWithDetailsForMenuById` 的 4 个测试，覆盖反射验证和集成查询，设计文档未明确列出但属于合理补充
- 无其他偏差
