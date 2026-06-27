# 任务指令（v11）

## 动作
NEW

## 任务描述

修复 MenuController.java 中 **T19**（PUT → PATCH）和 **T21**（路径 id 与请求体 id 一致性校验）两项缺陷，同步适配 MenuControllerTest.java。

### 涉及文件

| 操作 | 文件路径 |
|------|---------|
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/MenuController.java` |
| 修改 | `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/MenuControllerTest.java` |

## 选择理由

T19+T21 同为 MenuController 修改，属于批次 5（Controller 层修复）剩余项（T6+T7 已在 R9/R10 完成）。两处修改在同一文件 MenuController.java，可一次性提交。与刚完成的 T6+T7（AuthController 修复）类型一致，均为 Controller 层 API 契约对齐。

## 任务上下文

### T19: 菜单更新端点使用 PUT 而非 PATCH 方法（P2）

- **位置**：`MenuController.java:117`
- **当前代码**：`@PutMapping("/{id}")`
- **OOD 要求**：`PATCH /api/menu/{id}`（OOD 4.4 节保护清单和 6.1 节接口清单均规定 PATCH）
- **修改方式**：
  - 将 `@PutMapping("/{id}")` 替换为 `@PatchMapping("/{id}")`
  - 新增 import `org.springframework.web.bind.annotation.PatchMapping`
  - 删除 import `org.springframework.web.bind.annotation.PutMapping`（若不再被其他方法使用）
- **API 契约变更**：前端请求方法需从 PUT 变更为 PATCH，否则产生 405

### T21: 缺少路径 id 与请求体 id 一致性校验（P2）

- **位置**：`MenuController.java:119`（update 方法体起始处）
- **当前代码**：未校验 `request.getId()` 与路径 `id` 是否一致
- **OOD 要求**：OOD 5.2 节要求 `PATCH /api/menu/{id}` 的路径参数 `{id}` 与请求体中的 `id` 字段（若携带）必须相同，不一致时返回 400（PARAM_INVALID）
- **修改方式**：在 `menuService.updateMenu(id, request)` 调用前插入校验：
  ```java
  if (request.getId() != null && !request.getId().equals(id)) {
      return Result.fail(GlobalErrorCode.PARAM_INVALID);
  }
  ```
- **注意**：`MenuUpdateRequest` 中 `id` 为 `Long` 类型，与路径参数类型一致；请求体中 id 可选（PATCH 语义），仅在携带时校验

## 已有代码上下文

### MenuController.java 当前结构

- **L17**：已 import `PutMapping`
- **L20**：import `PutMapping`（将在修改后替换为 `PatchMapping`）
- **无** `import com.aimedical.common.exception.GlobalErrorCode;`（需新增）
- **L117**：`@PutMapping("/{id}")` 标识的 `update()` 方法
- **L119-125**：`update()` 方法体——直接调用 `menuService.updateMenu(id, request)`；返回值为 `Result<MenuResponse>`

### MenuControllerTest.java 当前结构

- **L89-121**：`UpdateMenuTests` 嵌套类，`@DisplayName("PUT /api/menu/{id}")`
  - `shouldReturnSuccessWhenUpdateMenuSucceeds()`（L93-106）
  - `shouldReturnNotFoundWhenUpdateNonExistentMenu()`（L108-120）
- 需将 PUT 引用更新为 PATCH，并新增 T21 校验测试（路径 id=1 但请求体 id=2 应返回 PARAM_INVALID）

## RETRY 说明

无（首次执行）。
