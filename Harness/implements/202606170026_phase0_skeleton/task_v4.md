# 任务指令（v4）

## 动作
NEW

## 任务描述
实现 `backend/common-module-api` 和 `backend/common-module-impl` 子模块骨架，包含以下文件：

### common-module-api
- `common-module-api/pom.xml` — 更新依赖：添加 `common`（compile scope）
- `src/main/java/com/aimedical/modules/commonmodule/api/UserType.java` — 用户类型枚举（DOCTOR/PATIENT/ADMIN）

### common-module-impl
- `common-module-impl/pom.xml` — 更新依赖：添加 `common-module-api`、`common`、`spring-boot-starter-data-jpa`、`spring-boot-starter-test`
- `src/main/java/com/aimedical/modules/commonmodule/permission/User.java` — 用户实体（继承 BaseEntity，关联 Role/Post）
- `src/main/java/com/aimedical/modules/commonmodule/permission/Role.java` — 角色实体（继承 BaseEntity，一对多 Post，多对多 User）
- `src/main/java/com/aimedical/modules/commonmodule/permission/Post.java` — 岗位实体（继承 BaseEntity，多对多 User/Function，多对一 Role）
- `src/main/java/com/aimedical/modules/commonmodule/permission/Function.java` — 功能权限实体（继承 BaseEntity，多对多 Post）
- `src/main/java/com/aimedical/modules/commonmodule/permission/UserRepository.java` — 用户 Repository（extends JpaRepository<User, Long>）
- `src/main/java/com/aimedical/modules/commonmodule/dict/.gitkeep` — 字典占位目录
- 单元测试类（至少占位测试）

## 选择理由
common-module 是 common 之后的下一层依赖基础。业务模块（patient/doctor/admin）都依赖 common-module-api 获取 UserType 等共享类型；common-module-impl 提供 User/Role/Post/Function 四级权限实体骨架，是后续认证与权限控制的基础。父 POM 已声明 common-module-api 和 common-module-impl 作为子模块并管理版本。

## 任务上下文

### OOD 权限模型核心抽象（§3.3）
- User：统一用户实体，通过 userType（枚举 DOCTOR/PATIENT/ADMIN）区分类型，多对多关联 Role 和 Post
- Role：粗粒度角色（如"患者"、"门诊医生"、"管理员"），一对多 Post，多对多 User
- Post：细粒度岗位（如"门诊医生-神经内科"），多对多 Function，多对一 Role，多对多 User
- Function：最细粒度操作权限，多对多 Post

### JPA 关系映射约定
- User ↔ Role：`@ManyToMany`，User 端 `@JoinTable(name = "user_role")`，Role 端 mappedBy
- User ↔ Post：`@ManyToMany`，User 端 `@JoinTable(name = "user_post")`，Post 端 mappedBy
- Role ↔ Post：Post 端 `@ManyToOne` + `@JoinColumn(name = "role_id")`，Role 端 `@OneToMany(mappedBy = "role")`
- Post ↔ Function：Post 端 `@ManyToMany` + `@JoinTable(name = "post_function")`，Function 端 mappedBy
- 所有 FetchType.LAZY，无 cascade
- 所有实体继承 BaseEntity（在 common 模块中定义）

### 包命名规范
- common-module-api: `com.aimedical.modules.commonmodule.api`
- common-module-impl: `com.aimedical.modules.commonmodule.permission`、`.dict`

## 已有代码上下文
- `backend/pom.xml` — 父 POM 已聚合 common-module-api 和 common-module-impl 子模块，并管理其版本（${project.version}）
- `backend/common/` — 已实现 BaseEntity（@MappedSuperclass，含 id/createdAt/updatedAt/deleted）、BaseEnum、Result 等共享基础类型
- `backend/common-module-api/pom.xml` — 仅含骨架，无任何依赖声明
- `backend/common-module-impl/pom.xml` — 仅含骨架，无任何依赖声明
