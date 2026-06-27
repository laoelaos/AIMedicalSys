# 任务指令（v13）

## 动作
NEW

## 任务描述
修复 T22：PermissionFunction 实体缺少 component 字段映射。
1. `PermissionFunction.java`：新增 `private String component;` 字段，添加 `@Column(name = "component")` 注解，添加 getter/setter
2. `MenuServiceImpl.java:187`：`convertToMenuResponse()` 中将硬编码的 `null` 替换为 `function.getComponent()`

涉及文件：
- `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/PermissionFunction.java`
- `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/MenuServiceImpl.java`

## 选择理由
P1 批次 6 首个任务。无前置依赖（实体字段新增 + MenuServiceImpl 中一处硬编码 null 替换），变更量小、风险可控，是后续批次 6 修复的基线。

## 任务上下文
- OOD 5.1 节已补全 component 字段映射条目：`@Column(name = "component") private String component;`
- `sys_function` 表 schema.sql 已存在 `component VARCHAR(255)` 列，无需 DDL 变更
- MenuResponse record 已定义 `String component` 字段
- MenuUpdateRequest 已有 `private String component;` + getter/setter
- data.sql 已有包含 component 列的 INSERT 语句
- 当前 `convertToMenuResponse()` 第 187 行 `null` 应改为 `function.getComponent()`
- `PermissionFunction` 未使用 Lombok，需手动添加 getter/setter
- dev 环境 `ddl-auto=update`，Hibernate 会自动新增列（但列已存在）；prod 环境需通过 schema.sql 或 DDL 脚本管理

## 已有代码上下文
- `PermissionFunction.java`（136 行）：实体类，手动编写 getter/setter，无 Lombok。已有字段：code, name, description, enabled, parent, sortOrder, visible, type, icon, path, posts
- `MenuServiceImpl.java:182-192`：`convertToMenuResponse()` 第 187 行 `null` 应改为 `function.getComponent()`
- `MenuResponse.java:5-14`：record 类型，包含 `String component` 字段
- `MenuUpdateRequest.java:24`：`private String component;` + getter/setter 已存在
- `PermissionFunctionTest.java`：现有 6 个测试，未测试 component 字段
