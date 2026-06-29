# 详细设计（v14）

## 概述

修复 T23：`MenuServiceImpl.getUserMenuTree()` 使用 `userRepository.findById(userId)`（无 EntityGraph）导致 N+1 查询。采用方案 A：新增专用查询方法 `findWithDetailsForMenuById`（`@EntityGraph` 含 `posts.functions`），保持现有 `findWithDetailsById` 不变以避免影响 `JwtAuthenticationFilter` 调用方。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/UserRepository.java` | 修改 | 新增 `findWithDetailsForMenuById` 方法 |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/MenuServiceImpl.java` | 修改 | L44 `findById(userId)` → `findWithDetailsForMenuById(userId)` |

## 类型定义

### UserRepository（已有 JPA Repository 接口）

**形态**：interface（`JpaRepository<User, Long>`）
**包路径**：`com.aimedical.modules.commonmodule.permission`
**职责**：用户数据访问，新增菜单查询专用方法

**新增方法**：
```java
@EntityGraph(attributePaths = {"roles", "posts", "posts.functions"})
Optional<User> findWithDetailsForMenuById(Long id);
```

**插入位置**：在现有 `findWithDetailsById`（L17）之后、`findTokenVersionById`（L19）之前。

**import 变更**：无需新增 import，`@EntityGraph` 已在 L3 导入，`Optional` 已在 L9 导入。

**保持现有方法不变**：
```java
@EntityGraph(attributePaths = {"roles", "posts"})
Optional<User> findWithDetailsById(Long id);
```

### MenuServiceImpl.getUserMenuTree()（方法修改）

**形态**：`@Transactional(readOnly = true) public` 方法
**包路径**：`com.aimedical.modules.commonmodule.service.impl.MenuServiceImpl`
**职责**：根据用户 ID 获取菜单树，消除 N+1 查询

**修改位置**：`MenuServiceImpl.java:44`

| 当前 | 修改后 |
|------|--------|
| `Optional<User> userOptional = userRepository.findById(userId);` | `Optional<User> userOptional = userRepository.findWithDetailsForMenuById(userId);` |

**不涉及 import 变更**：`UserRepository` 已在 L12 导入。

## 错误处理

不涉及。`findWithDetailsForMenuById` 返回类型与 `findById` 一致（`Optional<User>`），返回 `Optional.empty()` 时现有 L45-47 的判空逻辑不变。无新异常抛出。

## 行为契约

### findWithDetailsForMenuById(Long id)

- **前置**：`id` 非 null（Long 类型 JPARepository 默认行为）
- **EntityGraph**：LEFT JOIN FETCH `roles`、`posts`、`posts.functions`，确保 `getUserMenuTree()` 中遍历 `user.getPosts()` 和 `post.getFunctions()` 时不再触发懒加载
- **返回**：`Optional<User>`，与 `findById` / `findWithDetailsById` 语义一致
- **后置**：仅 `MenuServiceImpl.getUserMenuTree()` 调用，不影响其他调用方

### getUserMenuTree()（行为变化）

- **前置**：不变
- **性能变化**：1 条 JOIN 查询替代 N+1 条懒加载查询
- **返回行为**：不变（仍返回 `List<MenuResponse>`）

### 交叉依赖约束

- `findWithDetailsById` 被 `JwtAuthenticationFilter.java:85` 调用，其 EntityGraph `{"roles", "posts"}` **不得修改**
- 新方法 `findWithDetailsForMenuById` 仅供 `MenuServiceImpl.getUserMenuTree()` 使用

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `org.springframework.data.jpa.repository.EntityGraph` | UserRepository 已有，无需新增 |
| `com.aimedical.modules.commonmodule.permission.UserRepository` | MenuServiceImpl 已有，无需新增 |

## 测试适配

`MenuServiceTest.java` 中 `getUserMenuTree` 测试用例（L83-116）需将 mock 方法从 `findById` 改为 `findWithDetailsForMenuById`：

| 测试方法 | L | 修改前 | 修改后 |
|---------|---|--------|--------|
| `shouldGetUserMenuTreeSuccessfully` | 84 | `when(userRepository.findById(1L)).thenReturn(...)` | `when(userRepository.findWithDetailsForMenuById(1L)).thenReturn(...)` |
| `shouldReturnEmptyListWhenUserNotFound` | 98 | `when(userRepository.findById(999L)).thenReturn(...)` | `when(userRepository.findWithDetailsForMenuById(999L)).thenReturn(...)` |
| `shouldReturnEmptyListWhenUserHasNoPosts` | 110 | `when(userRepository.findById(1L)).thenReturn(...)` | `when(userRepository.findWithDetailsForMenuById(1L)).thenReturn(...)` |

其他测试文件（`JwtAuthenticationFilterTest`、`AuthServiceTest` 等）使用 `findWithDetailsById` 或 `findByUsername`，不受本次变更影响。
