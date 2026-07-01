# 测试报告（v7）

## 测试文件清单

| 被测类型 | 测试文件路径 |
|---------|------------|
| Parent pom module 注册 | `common/src/test/java/com/aimedical/common/pom/ParentPomModuleRegistrationTest.java` |
| consultation/pom.xml | `common/src/test/java/com/aimedical/common/pom/NewModulePomTest.java` |
| prescription/pom.xml | `common/src/test/java/com/aimedical/common/pom/NewModulePomTest.java` |
| medical-record/pom.xml | `common/src/test/java/com/aimedical/common/pom/NewModulePomTest.java` |
| consultation 模块占位 | `modules/consultation/src/test/java/com/aimedical/modules/consultation/ConsultationPlaceholderTest.java` |
| prescription 模块占位 | `modules/prescription/src/test/java/com/aimedical/modules/prescription/PrescriptionPlaceholderTest.java` |
| medical-record 模块占位 | `modules/medical-record/src/test/java/com/aimedical/modules/medicalrecord/MedicalRecordPlaceholderTest.java` |

## 测试框架

JUnit 5（spring-boot-starter-test 内置），与项目已有 pom 验证测试风格一致（XML DOM + XPath 断言）。

## 覆盖维度

### ParentPomModuleRegistrationTest（5 用例）

| 行为契约 | 用例 | 覆盖 |
|---------|------|------|
| 父 pom 注册 consultation 模块 | shouldRegisterConsultationModule | ✓ |
| 父 pom 注册 prescription 模块 | shouldRegisterPrescriptionModule | ✓ |
| 父 pom 注册 medical-record 模块 | shouldRegisterMedicalRecordModule | ✓ |
| 新模块在 admin 之后 | shouldAppearAfterAdmin | ✓ |
| 新模块在 application 之前 | shouldAppearBeforeApplication | ✓ |

### NewModulePomTest（每组 8 个，共 25 用例）

每组（consultation / prescription / medical-record）各 8 个用例：

| 行为契约 | 用例 | 覆盖 |
|---------|------|------|
| parent/groupId = com.aimedical | {module}ParentShouldBeAimedicalSys | ✓ |
| parent/artifactId = aimedical-sys | {module}ParentShouldBeAimedicalSys | ✓ |
| parent/version = 0.0.1-SNAPSHOT | {module}ParentShouldBeAimedicalSys | ✓ |
| parent/relativePath = ../../pom.xml | {module}ParentShouldBeAimedicalSys | ✓ |
| artifactId 唯一 | {module}ShouldHaveCorrectArtifactId | ✓ |
| packaging = jar | {module}ShouldBeJarPackaging | ✓ |
| 依赖不声明 version（继承父 pom） | {module}ShouldNotDeclareVersionInDependencies | ✓ |
| 包含全部 8 个必需依赖 | {module}ShouldContainRequiredDependencies | ✓ |
| jacoco.skip = false | {module}ShouldHaveJacocoEnabled | ✓ |
| jacoco.skip.check = false | {module}ShouldHaveJacocoEnabled | ✓ |

额外全局验证 1 个：

| 行为契约 | 用例 | 覆盖 |
|---------|------|------|
| 三个 artifactId 互不重复 | shouldHaveUniqueArtifactIds | ✓ |

**用例总数合计：5 + 25 = 30**

### 占位测试（3 用例）

每个新模块一个空测试用例，确保模块编译时至少有一个测试入口。

## 与详细设计的一致性

- 所有测试基于详细设计（第 7、22-24、36-38、56-57、115-116、147-148、154、156-158 节）的行为契约编写
- 覆盖正常路径（parent 坐标、artifactId、packaging、依赖、jacoco 属性）
- 覆盖边界条件（新模块在 modules 列表中的位置）
- 覆盖错误路径（不声明 version 可避免与父 pom 冲突）
- 覆盖状态交互（artifactIds 互不重复确保模块独立性）

## 执行方式

```bash
# 在 common 模块下执行 pom 结构测试
mvn test -pl common -Dtest="*Pom*"
# 或在各新模块下执行占位测试
mvn test -pl modules/consultation
mvn test -pl modules/prescription
mvn test -pl modules/medical-record
```
