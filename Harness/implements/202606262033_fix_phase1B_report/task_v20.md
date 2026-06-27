# 任务指令（v20）

## 动作
NEW

## 任务描述
测试补充，`MenuServiceTest.java` 中 `GetUserMenuTreeTests` 嵌套类内新增 4 个测试方法，覆盖 `MenuServiceImpl.buildMenuTree()` 的多级菜单树构建逻辑：
- `buildMenuTree_shouldNestChildUnderParent`：一个父菜单一个子菜单
- `buildMenuTree_shouldSortSiblingsBySortOrder`：多个同级菜单验证排序
- `buildMenuTree_shouldSupportThreeLevelHierarchy`：三级嵌套
- `buildMenuTree_shouldHandleMultipleParents`：多个父菜单下各有子菜单

仅修改 `service/MenuServiceTest.java` 一个文件。

## 选择理由
批次 7 第三个任务。现有 `GetUserMenuTreeTests` 仅包含单层菜单返回验证（L80-128），无任何 parent-child 关系的树构建测试。OOD 5.2 节定义 `MenuResponse` 支持递归 `children` 结构，6.1 节定义 `/api/menu/tree` 返回树形菜单。T16 已通过验证，可启动 T17，无其他前置依赖。

## 任务上下文
### 已有测试结构
`MenuServiceTest` 使用 `@ExtendWith(MockitoExtension.class)`，`@Mock` 注入 `userRepository` 和 `functionRepository`。`GetUserMenuTreeTests` 嵌套类现有 4 个测试方法（L80-128），通过 `userRepository.findWithDetailsForMenuById()` 返回 `Optional.of(testUser)` 来间接测试 `getUserMenuTree()`。

### buildMenuTree 方法签名与行为
```java
private List<MenuResponse> buildMenuTree(List<MenuResponse> menus, Map<Long, Long> parentIdMap)
```
- 输入：展平的 MenuResponse 列表 + parentIdMap（functionId → parentFunctionId，root 为 null）
- 输出：树形结构的根节点列表，children 在 MenuResponse.children() 中
- 相同 root 级别节点按 sort 字段排序
- parentIdMap 中 parent 不在 menus 内时回退为 root

### 测试策略
由于 `buildMenuTree` 是 `private` 方法，测试应通过 `getUserMenuTree()` 间接执行。需构造多组 `PermissionFunction` 实例（设 id、parentId、sortOrder、name 等字段），通过 Post → User 关联注入，然后断言返回的 `List<MenuResponse>` 结构。

### 关键实现细节
- `PermissionFunction.parent` 是 `PermissionFunction` 实体引用，但 `buildMenuTree` 通过 `parentIdMap`（`f.getParent().getId()`）构建树，所以 mock 时必须设置 `parent` 对象（非 null），其 id 反映父子关系
- `convertToMenuResponse` 中 `component` 字段映射来自 `function.getComponent()`（T22 已修复），测试断言中 component 应预期为 `function.getComponent()` 的返回值
- 同级菜单按 `sortOrder` 升序排列

## 已有代码上下文
- `MenuServiceTest.java`（440 行）：位于 `common-module-impl/src/test/java/.../service/`，现有 `GetUserMenuTreeTests` 的 4 个方法（L80-128）仅覆盖单层/空列表场景
- `MenuServiceImpl.java`：`buildMenuTree()` 在 L195-230 实现
- `MenuResponse.id()`、`MenuResponse.name()`、`MenuResponse.children()` 为公开 API
- `UserRepository.findWithDetailsForMenuById()` 返回 `Optional<User>`，User 关联 Post，Post 关联 `Set<PermissionFunction>`
