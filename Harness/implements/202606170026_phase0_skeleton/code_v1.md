# 实现报告（v1）

## 概述
实现了 Phase 0 骨架的 Maven 父 POM 及 common 共享基础模块，共 14 个文件。父 POM 聚合 6 个子模块并统一管理依赖版本；common 模块提供了 JPA 实体基类、统一响应包装、错误码体系、分页契约、JPA 审计配置、Jackson 配置及全局异常处理等基础类型。

## 文件变更清单
| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | backend/pom.xml | Maven 多模块聚合父 POM，聚合 6 个子模块，统一管理依赖版本，配置 maven-dependency-plugin |
| 新建 | backend/common/pom.xml | common 模块 POM |
| 新建 | backend/common/src/main/java/com/aimedical/common/base/BaseEntity.java | JPA 实体基类（@MappedSuperclass + 审计注解 + Hibernate 软删除） |
| 新建 | backend/common/src/main/java/com/aimedical/common/base/BaseEnum.java | 枚举基类接口 |
| 新建 | backend/common/src/main/java/com/aimedical/common/result/Result.java | 统一响应包装泛型类 |
| 新建 | backend/common/src/main/java/com/aimedical/common/result/PageQuery.java | 分页请求参数类 |
| 新建 | backend/common/src/main/java/com/aimedical/common/result/PageResponse.java | 分页响应泛型类 |
| 新建 | backend/common/src/main/java/com/aimedical/common/exception/ErrorCode.java | 错误码接口 |
| 新建 | backend/common/src/main/java/com/aimedical/common/exception/BusinessException.java | 业务异常基类 |
| 新建 | backend/common/src/main/java/com/aimedical/common/exception/GlobalErrorCode.java | 全局错误码枚举 |
| 新建 | backend/common/src/main/java/com/aimedical/common/config/JpaConfig.java | JPA 审计配置类 |
| 新建 | backend/common/src/main/java/com/aimedical/common/config/JacksonConfig.java | Jackson 配置类（snake_case + JavaTimeModule） |
| 新建 | backend/common/src/main/java/com/aimedical/common/config/GlobalExceptionHandler.java | 全局异常处理器 |
| 新建 | backend/common/src/test/java/com/aimedical/common/CommonPlaceholderTest.java | common 模块占位单元测试类 |

## 编译验证
未执行编译验证（目标环境未安装 Maven）。

## 设计偏差说明
| 设计规格 | 偏差原因 | 实际处理 |
|---------|---------|---------|
| 聚合 10 个子模块 | 设计的 dependencyManagement 仅列出 6 个内部模块（common, common-module-api, common-module-impl, ai-api, ai-impl, application），其余 4 个模块名称未定义 | 按 dependencyManagement 列明的 6 个模块配置 modules，其余 4 个未创建 |
| 父 POM 未指定 parent | 省略 parent 会导致无法管理 spring-boot-starter 版本 | 继承 `spring-boot-starter-parent:3.2.5`，这是管理 Spring Boot Starter 版本的标准做法 |
| 未指定 Java 版本 | 设计省略 | 使用 Java 17 |
| GlobalExceptionHandler 日志实现细节 | 设计仅要求"系统异常需记日志"，未指定实现方式 | 使用 SLF4J Logger 记录 Exception 兜底处理 |

## 设计遗漏说明
- BaseEntity 使用了 `@SQLDelete` 和 `@SQLRestriction`（`org.hibernate.annotations`），这两个 Hibernate 注解的 import 在设计类型签名中未列出，已在实现中补充。
