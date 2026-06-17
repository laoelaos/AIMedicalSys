# 详细设计（v5）

## 概述

实现 backend/common-module-api 和 backend/common-module-impl 的子模块骨架。common-module-api 提供共享枚举类型（UserType），common-module-impl 提供四级权限实体（User/Role/Post/Function）及 UserRepository 骨架。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| backend/common-module-api/pom.xml | 修改 | 添加 common（compile scope）依赖、spring-boot-starter-test（test scope）依赖 |
| backend/common-module-api/src/main/java/com/aimedical/modules/commonmodule/api/UserType.java | 新建 | 用户类型枚举 |
| backend/common-module-impl/pom.xml | 修改 | 添加 common-module-api、common、spring-boot-starter-data-jpa、spring-boot-starter-test 依赖 |
| backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java | 新建 | 用户实体 |
| backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Role.java | 新建 | 角色实体 |
| backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Post.java | 新建 | 岗位实体 |
| backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Function.java | 新建 | 功能权限实体 |
| backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/UserRepository.java | 新建 | 用户 Repository |
| backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dict/.gitkeep | 新建 | 字典占位目录 |
| backend/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/CommonModulePlaceholderTest.java | 新建 | 占位单元测试 |

## 类型定义

### UserType
**形态**：enum implements BaseEnum
**包路径**：com.aimedical.modules.commonmodule.api
**职责**：区分三种用户类型
```java
public enum UserType implements BaseEnum {
    DOCTOR("DOCTOR", "医生"),
    PATIENT("PATIENT", "患者"),
    ADMIN("ADMIN", "管理员");

    // getCode() -> code, getDesc() -> desc
}
```
**公开接口**：getCode() : String, getDesc() : String（来自 BaseEnum）
**构造方式**：枚举常量

### User
**形态**：class extends BaseEntity
**包路径**：com.aimedical.modules.commonmodule.permission
**职责**：统一用户实体，通过 userType 区分类型
```java
@Entity
@Table(name = "sys_user")
public class User extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String username;

    private String password;

    private String nickname;

    private String phone;

    private String email;

    private Boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserType userType;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_post", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "post_id"))
    private Set<Post> posts;
}
```
**公开接口**：getter/setter 全部字段（无业务方法）
**构造方式**：默认空参构造（JPA）
**类型关系**：extends BaseEntity, @ManyToMany ↔ Role, @ManyToMany ↔ Post

### Role
**形态**：class extends BaseEntity
**包路径**：com.aimedical.modules.commonmodule.permission
**职责**：粗粒度角色，一对多 Post，多对多 User
```java
@Entity
@Table(name = "sys_role")
public class Role extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String code;

    private String name;

    private String description;

    private Boolean enabled;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private Set<Post> posts;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users;
}
```
**公开接口**：getter/setter
**构造方式**：默认空参构造
**类型关系**：extends BaseEntity, @OneToMany → Post, @ManyToMany(mappedBy) ↔ User

### Post
**形态**：class extends BaseEntity
**包路径**：com.aimedical.modules.commonmodule.permission
**职责**：细粒度岗位，关联 Role、Function、User
```java
@Entity
@Table(name = "sys_post")
public class Post extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String code;

    private String name;

    private String description;

    private Boolean enabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "post_function", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "function_id"))
    private Set<Function> functions;

    @ManyToMany(mappedBy = "posts", fetch = FetchType.LAZY)
    private Set<User> users;
}
```
**公开接口**：getter/setter
**构造方式**：默认空参构造
**类型关系**：extends BaseEntity, @ManyToOne → Role, @ManyToMany → Function, @ManyToMany(mappedBy) ↔ User

### Function
**形态**：class extends BaseEntity
**包路径**：com.aimedical.modules.commonmodule.permission
**职责**：最细粒度操作权限
```java
@Entity
@Table(name = "sys_function")
public class Function extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String code;

    private String name;

    private String description;

    private Boolean enabled;

    @ManyToMany(mappedBy = "functions", fetch = FetchType.LAZY)
    private Set<Post> posts;
}
```
**公开接口**：getter/setter
**构造方式**：默认空参构造
**类型关系**：extends BaseEntity, @ManyToMany(mappedBy) ↔ Post

### UserRepository
**形态**：interface
**包路径**：com.aimedical.modules.commonmodule.permission
**职责**：用户 Repository 骨架，提供 JPA CRUD 能力
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
```
**公开接口**：继承 JpaRepository 的方法
**构造方式**：Spring Data 代理

## 错误处理

本项目不涉及自定义错误处理。实体校验通过 JPA 注解（@Column(nullable = false) 等）约束，违反时由 Hibernate 运行时抛出异常。

## 行为契约

1. 所有实体默认空参构造，setter 注入关联关系
2. 双向关联维护端：User（roles/posts 维护端），Post（role/functions 维护端），Function/Role 端 mappedBy
3. 所有 FetchType.LAZY，无 cascade 配置
4. 软删除由 BaseEntity 的 @SQLDelete/@SQLRestriction 统一处理，子实体无需额外配置
5. UserRepository 在 Phase 0 仅作占位，无自定义查询方法

## 依赖关系

| 模块 | 依赖 |
|------|------|
| common-module-api | common（获取 BaseEnum）、spring-boot-starter-test（test scope） |
| common-module-impl | common（获取 BaseEntity）、common-module-api（获取 UserType）、spring-boot-starter-data-jpa、spring-boot-starter-test |

暴露给后续任务的公开接口：
- `com.aimedical.modules.commonmodule.api.UserType` — 业务模块可直接引用
- `com.aimedical.modules.commonmodule.permission.UserRepository` — 后续业务 Service 可注入

## 修订说明（v5 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| verify_v4 报告：common-module-api 测试编译失败，23 个编译错误，缺失 JUnit 测试依赖 | 在 common-module-api/pom.xml 中添加 spring-boot-starter-test（test scope），版本由父 POM 管理；同步更新文件规划表和依赖关系表 |
