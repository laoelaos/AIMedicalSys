# 详细设计（v11）

## 概述

修复 `MenuController.java` 的两个 API 契约缺陷：
- **T19**：菜单更新端点错误使用 `PUT`，OOD 要求为 `PATCH`
- **T21**：`PATCH /api/menu/{id}` 缺少路径 `id` 与请求体 `id` 的一致性校验

同步适配 `MenuControllerTest.java` 中的对应测试。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/MenuController.java` | 修改 | T19: `@PutMapping` → `@PatchMapping`；T21: 新增路径/请求体 id 一致性校验 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/MenuControllerTest.java` | 修改 | 更新 PUT→PATCH 显示名；新增 T21 校验测试 |

## 类型定义

无新增类型。

## 变更详述

### T19: PUT → PATCH 方法修正

**修改位置**：`MenuController.java:117`

| 项目 | 修改前 | 修改后 |
|------|--------|--------|
| 注解 | `@PutMapping("/{id}")` | `@PatchMapping("/{id}")` |
| import | `org.springframework.web.bind.annotation.PutMapping`（L20，仅此一处使用） | 删除 `PutMapping`，新增 `import org.springframework.web.bind.annotation.PatchMapping` |

**测试适配**（`MenuControllerTest.java:90`）：
- `@DisplayName("PUT /api/menu/{id}")` → `@DisplayName("PATCH /api/menu/{id}")`

### T21: 路径 id 与请求体 id 一致性校验

**修改位置**：`MenuController.java:119`（`update()` 方法体起始处）

**修改方式**：在 `menuService.updateMenu(id, request)` 调用前插入守卫语句：

```java
if (request.getId() != null && !request.getId().equals(id)) {
    return Result.fail(GlobalErrorCode.PARAM_INVALID);
}
```

**新增 import**：
- `com.aimedical.common.exception.GlobalErrorCode`

**行为语义**：
- `MenuUpdateRequest.id` 字段类型为 `Long`，`@JsonInclude(Include.NON_NULL)` 保证空时不序列化
- 仅在 `request.getId() != null`（请求体携带了 id）时执行校验
- `GlobalErrorCode.PARAM_INVALID` 的 code 为 `"PARAM_INVALID"`，message 为 `"参数校验失败"`

**测试新增**（`MenuControllerTest.java` `UpdateMenuTests` 内）：

```java
@Test
@DisplayName("路径id与请求体id不一致返回PARAM_INVALID")
void shouldReturnParamInvalidWhenPathIdMismatchBodyId() {
    var request = new MenuUpdateRequest();
    request.setId(2L);

    Result<MenuResponse> result = menuController.update(1L, request);

    assertEquals("PARAM_INVALID", result.getCode());
    assertEquals("参数校验失败", result.getMessage());
    verify(menuService, never()).updateMenu(any(), any());
}
```

## 错误处理

以 `Result.fail(ErrorCode)` 返回错误响应，不抛出异常。控制器方法返回类型 `Result<MenuResponse>` 保持不变，错误时 `data` 字段为 `null`。

| 场景 | 错误码 | 说明 |
|------|--------|------|
| 路径 id ≠ 请求体 id | `PARAM_INVALID` | 请求体 id 可选，仅在携带时校验；不一致时返回 400 语义 |

## 行为契约

### `update(Long id, @Valid MenuUpdateRequest request)`

**前置**：
- `id` 为路径参数，必传
- `request` 经 `@Valid` 校验

**正常路径**：
1. （T21）如果 `request.getId() != null` 且与 `id` 不相等 → 立即返回 `Result.fail(GlobalErrorCode.PARAM_INVALID)`
2. 调用 `menuService.updateMenu(id, request)`，返回 `MenuResponse`
3. 若返回 `null` → `Result.fail("MENU_NOT_FOUND", "菜单不存在")`
4. 否则 → `Result.success(menu)`

**后置**：
- 当路径/请求体 id 不一致时，`menuService.updateMenu()` 不会被调用

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `org.springframework.web.bind.annotation.PatchMapping` | 新增，替换已删除的 `PutMapping` |
| `com.aimedical.common.exception.GlobalErrorCode` | 新增，T21 校验返回 PARAM_INVALID |
| `com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequest.getId()` | 已有，返回 `Long` 类型 |
