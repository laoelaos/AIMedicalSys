# 测试报告（v11）

## 测试文件

`AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/MenuControllerTest.java`

## 现有测试评估

### UpdateMenuTests

| 测试方法 | 覆盖契约 | 现有 |
|---------|---------|------|
| `shouldReturnSuccessWhenUpdateMenuSucceeds` | 正常路径：`request.getId() == null` 跳过校验 → 调用 service → 成功 | ✅ 已有 |
| `shouldReturnSuccessWhenPathIdMatchesBodyId` | 正常路径：`request.getId() == path id` 校验通过 → 调用 service → 成功 | ✅ **新增** |
| `shouldReturnNotFoundWhenUpdateNonExistentMenu` | 错误路径：service 返回 null → `MENU_NOT_FOUND` | ✅ 已有 |

### PathIdConsistencyTests

| 测试方法 | 覆盖契约 | 现有 |
|---------|---------|------|
| `shouldReturnParamInvalidWhenPathIdMismatchBodyId` | 错误路径：`request.getId() != path id` → `PARAM_INVALID`，service 不被调用 | ✅ 已有 |

## 变更清单

| 变更 | 说明 |
|------|------|
| 新增 `shouldReturnSuccessWhenPathIdMatchesBodyId` | 覆盖 `request.getId() == id` 时校验通过且正常调用 service 的路径 |

## 行为契约覆盖矩阵

| 契约条目 | 正向 | 边界 | 错误 | 状态交互 |
|---------|------|------|------|---------|
| `MenuUpdateRequest` 无 id 时跳过校验，调用 service | ✅ | - | - | - |
| `MenuUpdateRequest` id == path id 时校验通过，调用 service | ✅ | ✅ | - | - |
| `MenuUpdateRequest` id != path id 时返回 PARAM_INVALID，不调 service | - | - | ✅ | ✅ |
| service 返回 null 时返回 MENU_NOT_FOUND | - | - | ✅ | - |
