# 任务指令（v5）

## 动作
NEW

## 任务描述
修复 T4、T27、T20 三项 error code 标准化缺陷：

1. **T4** — `common/.../GlobalErrorCode.java` 第9行，UNAUTHORIZED 消息 `"未认证"` → `"未认证或令牌已失效"`
2. **T27** — `common/.../GlobalErrorCode.java` 第10行，FORBIDDEN 消息 `"无权限"` → `"无权限访问"`
3. **T20** — `MenuServiceImpl.java` 第165行（约），deleteMenu() 中 `GlobalErrorCode.PARAM_INVALID` → `GlobalErrorCode.CHILDREN_EXIST`

影响文件：
- `common/GlobalErrorCode.java` — 2 处消息文本修改
- `common-module-impl/.../auth/security/SecurityConfigPhase1Test.java` — 若有断言引用 FORBIDDEN/UNAUTHORIZED 消息，需同步更新
- `common-module-impl/.../service/impl/MenuServiceImpl.java` — 1 处错误码枚举替换

## 选择理由
- 三项均为 Batch 2（编码规范对齐）中的纯文本/枚举值变更，无运行时行为变更风险
- T4/T27 均为 GlobalErrorCode.java 同一文件的文案修正，可同步完成
- T20 为同一模块中 MenuServiceImpl 的枚举替换，与 T4/T27 无冲突
- 无前置依赖，可在 R4 完成的基线上安全执行

## 任务上下文
### T4: UNAUTHORIZED 消息
- OOD 10.2 节明确规定 UNAUTHORIZED 消息为 `"未认证或令牌已失效"`，实现者仅写了 `"未认证"`
- 代码位置：`GlobalErrorCode.java:9`，`UNAUTHORIZED("UNAUTHORIZED", "未认证")`
- 修改方式：改为 `UNAUTHORIZED("UNAUTHORIZED", "未认证或令牌已失效")`

### T27: FORBIDDEN 消息
- OOD 10.2 节规定 FORBIDDEN 的错误消息为 `"无权限访问"`，实现者写为 `"无权限"`
- 代码位置：`GlobalErrorCode.java:10`，`FORBIDDEN("FORBIDDEN", "无权限")`
- 修改方式：改为 `FORBIDDEN("FORBIDDEN", "无权限访问")`

### T20: 删除菜单错误码
- OOD 10.1 节和 6.1 节要求有子菜单阻止删除时返回 `CHILDREN_EXIST`，实现者使用了 `PARAM_INVALID`
- 代码位置：`MenuServiceImpl.java:165`（约），`throw new BusinessException(GlobalErrorCode.PARAM_INVALID, ...)`
- 修改方式：替换为 `GlobalErrorCode.CHILDREN_EXIST`
- `CHILDREN_EXIST` 枚举值已存在于 `GlobalErrorCode.java`（第23行），消息为 `"存在子菜单，无法删除"`

## 已有代码上下文
### GlobalErrorCode.java 相关枚举定义
```java
// 第8-13行（约）
UNAUTHORIZED("UNAUTHORIZED", "未认证"),
FORBIDDEN("FORBIDDEN", "无权限"),
// ...
// 第23行
CHILDREN_EXIST("CHILDREN_EXIST", "存在子菜单，无法删除"),
```

### MenuServiceImpl.deleteMenu() 片段
```java
// 检查是否有子菜单
if (hasChildren) {
    throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "存在子菜单，无法删除，请先删除子菜单");
}
```

### 测试关注点
- `GlobalErrorCodeTest` 中若有断言引用 UNAUTHORIZED 或 FORBIDDEN 的消息文本，需同步更新预期值
- `SecurityConfigPhase1Test` 中若有断言引用 FORBIDDEN/UNAUTHORIZED 消息，需同步更新预期值
- `MenuServiceTest` 中若有测试验证 deleteMenu 异常错误码为 PARAM_INVALID，需改为 CHILDREN_EXIST
