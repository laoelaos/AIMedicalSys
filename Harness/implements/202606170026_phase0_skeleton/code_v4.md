# 实现报告（v1）

## 概述

实现了 common-module-api 和 common-module-impl 两个子模块的骨架代码。common-module-api 提供 UserType 枚举；common-module-impl 提供 User/Role/Post/Function 四个 JPA 实体及 UserRepository 接口。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | backend/common-module-api/pom.xml | 添加 common 依赖（compile scope） |
| 新建 | backend/common-module-api/src/main/java/com/aimedical/modules/commonmodule/api/UserType.java | 用户类型枚举，实现 BaseEnum |
| 修改 | backend/common-module-impl/pom.xml | 添加 common-module-api、common、spring-boot-starter-data-jpa、spring-boot-starter-test 依赖 |
| 新建 | backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java | 用户实体，关联 Role（@ManyToMany）、Post（@ManyToMany） |
| 新建 | backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Role.java | 角色实体，关联 Post（@OneToMany）、User（@ManyToMany mappedBy） |
| 新建 | backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Post.java | 岗位实体，关联 Role（@ManyToOne）、Function（@ManyToMany）、User（@ManyToMany mappedBy） |
| 新建 | backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Function.java | 功能权限实体，关联 Post（@ManyToMany mappedBy） |
| 新建 | backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/UserRepository.java | 用户 Repository 骨架，继承 JpaRepository |
| 新建 | backend/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dict/.gitkeep | 字典占位目录 |
| 新建 | backend/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/CommonModulePlaceholderTest.java | 占位单元测试 |

## 编译验证

`mvn compile -pl common-module-api,common-module-impl -am -q` 静默通过，编译成功。

## 设计偏差说明

无偏差。
