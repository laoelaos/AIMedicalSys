# 任务指令（v14）

## 动作
NEW

## 任务描述
修复 T23：getUserMenuTree 使用 @EntityGraph 消除 N+1 查询

涉及文件：
1. `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/UserRepository.java`
   - 新增一个专用查询方法 `findWithDetailsForMenuById`，带扩展后的 `@EntityGraph(attributePaths = {"roles", "posts", "posts.functions"})`
   - 保持现有 `findWithDetailsById`（仅 `{"roles", "posts"}`）不变，不影响 `JwtAuthenticationFilter.java:85` 调用方

2. `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/MenuServiceImpl.java`
   - L44：`userRepository.findById(userId)` → `userRepository.findWithDetailsForMenuById(userId)`

## 选择理由
P1 批次 6 第二个任务。T22（component 字段映射）已通过验证。变更仅两个文件，无交叉依赖风险。方案 A（新增专用方法而非修改现有方法）避免了影响 `JwtAuthenticationFilter` 调用者的副作用。

## 任务上下文

### 问题现象
`MenuServiceImpl.getUserMenuTree()` 使用 `userRepository.findById(userId)`（无 EntityGraph），随后遍历 `user.getPosts()`（触发 N 次懒加载查询）和 `post.getFunctions()`（触发额外 N 次查询）。

### 交叉依赖说明
`findWithDetailsById` 同时被 `JwtAuthenticationFilter.java:85` 调用。若直接修改其 EntityGraph 新增 `posts.functions`，将导致每次认证请求也强制 JOIN `posts.functions`，造成不必要的性能开销。因此采用方案 A：新增专用查询方法 `findWithDetailsForMenuById`，仅 `MenuServiceImpl` 使用。

## 已有代码上下文

### UserRepository.java（当前）
```java
@EntityGraph(attributePaths = {"roles", "posts"})
Optional<User> findWithDetailsById(Long id);
```

### MenuServiceImpl.java:44（当前）
```java
Optional<User> userOptional = userRepository.findById(userId);
```

### 修改后 UserRepository.java：新增专用方法
```java
@EntityGraph(attributePaths = {"roles", "posts"})
Optional<User> findWithDetailsById(Long id);

@EntityGraph(attributePaths = {"roles", "posts", "posts.functions"})
Optional<User> findWithDetailsForMenuById(Long id);
```

### 修改后 MenuServiceImpl.java:44
```java
Optional<User> userOptional = userRepository.findWithDetailsForMenuById(userId);
```

## 验证方式
- `mvn clean test` 全部测试通过
- 可开启 `spring.jpa.show-sql=true` 验证 `getUserMenuTree()` 只产生 1 条 JOIN 查询

## 修订说明（v14 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| `findWithDetailsById` 同时被 `JwtAuthenticationFilter.java:85` 调用，修改 EntityGraph 会无差别影响所有调用者；"无交叉依赖"声明与事实不符 | 采用方案 A：新增 `findWithDetailsForMenuById` 方法（`@EntityGraph` 含 `posts.functions`），保持 `findWithDetailsById` 不变。`MenuServiceImpl` 改为调用新方法。修正任务描述中的"无交叉依赖"表述并补充交叉依赖说明。 |

## 修订说明（v14 r2）
| 审查意见 | 修改措施 |
|---------|---------|
| plan.md R14 NEW 节的方法描述与 task_v14.md 已修订方案存在严重不一致（描述为"扩展 @EntityGraph attributePaths 增加 posts.functions""将 findById 替换为 findWithDetailsById"，暗示修改现有方法） | 更新 plan.md R14 NEW 节：(1) 任务描述改为"新增 findWithDetailsForMenuById 方法"而非修改现有方法；(2) "findById → findWithDetailsForMenuById"；(3) 删除"无交叉依赖"声明，补充交叉依赖说明（findWithDetailsById 被 JwtAuthenticationFilter 调用） |
