# 详细设计（v13）

## 概述

修复 T22：PermissionFunction 实体缺少 component 字段映射。在实体类中新增 `component` 字段及对应 JPA 列映射，并在 MenuServiceImpl 的 `convertToMenuResponse()` 中将硬编码的 `null` 替换为 `function.getComponent()`。

- `sys_function` 表 schema.sql 已存在 `component VARCHAR(255)` 列，无需 DDL 变更
- `MenuResponse` record 已定义 `String component` 字段
- `MenuUpdateRequest` 已有 `private String component;` + getter/setter
- `data.sql` 已有包含 `component` 列的 INSERT 语句

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/PermissionFunction.java` | 修改 | 新增 `component` 字段、getter、setter |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/MenuServiceImpl.java` | 修改 | `convertToMenuResponse()` 中第 187 行 `null` 替换为 `function.getComponent()` |

## 类型定义

### PermissionFunction（已有实体，字段新增）

**形态**：JPA @Entity
**包路径**：`com.aimedical.modules.commonmodule.permission`
**职责**：映射 `sys_function` 表，新增 component 字段对应表中 `component VARCHAR(255)` 列

**新增字段**：
```java
@Column(name = "component")
private String component;
```

**插入位置**：在现有 `private String path;`（L44）之后、`@ManyToMany private Set<Post> posts;`（L46-47）之前。

**新增方法**：
```java
public String getComponent() {
    return component;
}

public void setComponent(String component) {
    this.component = component;
}
```

**插入位置**：在现有 `setPath()`（L125-127）之后、`getPosts()`（L129-131）之前。

**现有 import 覆盖**：`jakarta.persistence.Column` 已在 L4 导入，无需新增 import。

### MenuServiceImpl.convertToMenuResponse()（方法修改）

**形态**：private 方法
**包路径**：`com.aimedical.modules.commonmodule.service.impl.MenuServiceImpl`
**职责**：将 `PermissionFunction` 实体转换为 `MenuResponse` record

**修改位置**：`MenuServiceImpl.java:182-193`，`new MenuResponse(...)` 构造调用

**修改点**：
| 参数位置 | 当前值 | 修改后 |
|---------|-------|-------|
| 第 4 个参数（component） | `null`（L187） | `function.getComponent()` |

**不涉及 import 变更**：`PermissionFunction` 已在 L8 导入。

## 错误处理

不涉及。新增字段为 `String` 类型，可为 `null`，`getComponent()` 返回 `null` 时 `MenuResponse` 构造器正常接收。无新异常抛出。

## 行为契约

### PermissionFunction.component

- **前置**：字段无校验约束，可 null
- **存储**：对应 `sys_function.component` 列（VARCHAR(255)），通过 `@Column(name = "component")` 映射
- **后置**：getter 返回当前值，setter 更新当前值

### convertToMenuResponse()

- **前置**：`function` 非 null
- **修改行为**：返回的 `MenuResponse.component` 与 `function.getComponent()` 一致，不再硬编码为 `null`
- **后置**：方法签名与返回类型不变；仅第 4 个构造参数语义变化

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `jakarta.persistence.Column` | PermissionFunction 已有，无需新增 |
| `com.aimedical.modules.commonmodule.dto.response.MenuResponse` | MenuServiceImpl 已有，无需新增 |
| `com.aimedical.modules.commonmodule.permission.PermissionFunction` | MenuServiceImpl 已有，无需新增 |

## 测试建议

- `PermissionFunctionTest.java`：新增 `shouldSetAndGetComponent()` 测试方法，验证 getter/setter 正常
- `MenuServiceImpl`：无单独单元测试文件，现有集成测试自动覆盖 component 字段映射
