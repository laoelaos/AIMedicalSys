# 任务指令（v2）

## 动作
RETRY

## 任务描述
修复父 POM `backend/pom.xml` 构建失败问题，使 common 模块编译通过，并完成编译验证。

## 选择理由
R1 任务 `后端父 POM + common 模块` 构建失败，需要在当前代码基础上修复并验证通过，才能进入下一轮。

## 任务上下文
- 父 POM：`AIMedical/backend/pom.xml` — 继承 spring-boot-starter-parent:3.2.5，聚合 6 个子模块
- common 模块：`AIMedical/backend/common/pom.xml` — 当前 POM 定义正确
- 所有 14 个 Java 源文件和 9 个测试文件已在上一轮创建并确认

## 已有代码上下文
### 当前文件结构
```
backend/
├── pom.xml                    # 父 POM（需修复）
├── common/pom.xml             # 子 POM（正确）
├── common-module-api/pom.xml  # 占位
├── common-module-impl/pom.xml # 占位
├── ai-api/pom.xml             # 占位
├── ai-impl/pom.xml            # 占位
├── application/pom.xml        # 占位
└── common/src/
    ├── main/java/com/aimedical/common/
    │   ├── base/BaseEntity.java, BaseEnum.java
    │   ├── result/Result.java, PageQuery.java, PageResponse.java
    │   ├── exception/ErrorCode.java, BusinessException.java, GlobalErrorCode.java
    │   └── config/JpaConfig.java, JacksonConfig.java, GlobalExceptionHandler.java
    └── test/java/com/aimedical/common/
        ├── CommonPlaceholderTest.java
        ├── base/BaseEntityTest.java, BaseEnumTest.java
        ├── result/ResultTest.java, PageQueryTest.java, PageResponseTest.java
        ├── exception/BusinessExceptionTest.java, GlobalErrorCodeTest.java
        └── config/GlobalExceptionHandlerTest.java
```

### 父 POM 当前问题
父 POM `dependencyManagement` 中第 69-90 行声明了 5 个 `spring-boot-starter-*`，但未指定 `<version>`（见行 73, 77, 81, 85, 89）。由于这些声明位于最贴近的 `dependencyManagement` 中，Maven 优先使用它们而非 `spring-boot-starter-parent:3.2.5` 传递下来的版本管理，导致 common 模块依赖解析失败。

## RETRY 说明
- **失败原因摘要**：父 POM dependencyManagement 声明了 spring-boot-starter-web / data-jpa / security / validation / test 但没有版本号，遮蔽了 spring-boot-starter-parent 传递的版本管理，构建失败。
- **修正方向**：删除父 POM `dependencyManagement` 中的「External starters」区块（行 69-90），这些依赖的版本已由 `spring-boot-starter-parent:3.2.5` 通过 `<dependencyManagement>` 传递管理，无需在父 POM 中重复声明。
- **验证方式**：在 `AIMedical/backend/` 目录执行 `mvn compile -pl common -am` 确认编译通过。
