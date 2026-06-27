# 测试报告（v13）

## 测试修改清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/permission/PermissionFunctionTest.java` | 新增 `shouldSetAndGetComponent()` 测试 |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/MenuServiceTest.java` | 在现有测试中新增 component 断言 |

## PermissionFunctionTest.java

### 新增测试

| 测试方法 | 契约验证 | 覆盖维度 |
|---------|---------|---------|
| `shouldSetAndGetComponent()` | getter/setter 正常读写 | 正常路径 |

## MenuServiceTest.java

### 新增断言

| 所属测试方法 | 断言 | 覆盖维度 |
|------------|------|---------|
| `setUp()` → `shouldGetUserMenuTreeSuccessfully` | `assertEquals("DashboardComp", menus.get(0).component())` | 正常路径（非 null component 从 entity 到 response 透传） |
| `shouldGetAllMenusSuccessfully` | `assertEquals("UserComp", ...)` / `assertEquals("RoleComp", ...)` | 正常路径（多个 entity 各带回 component） |
| `shouldGetMenuByIdSuccessfully` | `assertEquals("TestComp", response.component())` | 正常路径（单 entity 查询） |
| `shouldCreateMenuSuccessfully` | `assertNull(response.component())` | 边界条件（null component 透传） |

## 行为契约覆盖

| 行为契约 | 覆盖方式 |
|---------|---------|
| PermissionFunction.component getter/setter | `shouldSetAndGetComponent()` |
| convertToMenuResponse() 返回的 MenuResponse.component 与 function.getComponent() 一致 | 3 个正向断言 + 1 个 null 边界断言 |
