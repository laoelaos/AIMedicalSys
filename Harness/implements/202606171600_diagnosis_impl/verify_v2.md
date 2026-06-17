# 验证报告（v2）

## 结果
FAILED

## 统计
- 通过：2
- 失败：2

| 测试项 | 结果 | 说明 |
|--------|------|------|
| 父 POM 验证 (`mvn validate -N`) | ✅ PASSED | 父 POM `aimedical-sys` 结构正确，`dependencyManagement` 中 5 个 starter 版本号生效 |
| 全模块依赖解析 (`mvn validate`) | ✅ PASSED | 11/11 模块全部 SUCCESS，dependencyManagement 版本号及版本继承均正确 |
| Maven 编译 (`mvn compile -DskipTests`) | ❌ FAILED | `common` 模块 `PageQuery.java` 编译失败——缺少 `spring-boot-starter-validation` 依赖（v1 预存问题，非 v2 变更引入） |
| Java XPath 单元测试 (`mvn test -pl common -Dtest=ParentPomTest`) | ❌ FAILED | 被预存编译错误阻塞，无法到达测试执行阶段 |

## 失败原因摘要
1. **`PageQuery.java` 编译失败（预存问题）**：`common/pom.xml` 在 v1 中移除了 `spring-boot-starter-validation`，第 3-4 行的 `import jakarta.validation.constraints.Min/Max` 无法解析。该问题非本次 v2 变更引入。
2. **XPath 单元测试被阻塞**：由于编译失败，`mvn test` 无法到达 test-compile 及 test 阶段。根据 `test_review_v2_r1.md` 的审查，测试代码本身结构正确。

## v2 变更验证结论
| v2 变更 | 验证方式 | 结果 |
|---------|---------|------|
| 父 POM `dependencyManagement` 中 5 个 Spring Boot starter 添加版本号 `3.2.5` | `mvn validate` 所有 11 模块 BUILD SUCCESS | ✅ 依赖解析正确 |
| `application/pom.xml` 中 patient/doctor/admin 添加 `<version>${project.version}</version>` | `mvn validate` application 模块 BUILD SUCCESS | ✅ 版本声明正确 |
| `ParentPomTest.java` XPath 谓词语法修复 | `test_review_v2_r1.md` 代码审查通过，但因预存编译阻塞无法执行 | ⚠️ 代码审查确认正确 |

## 测试执行日志


### Parent POM 验证 (mvn validate -N)

[INFO] Scanning for projects...
[INFO] 
[INFO] --------------------< com.aimedical:aimedical-sys >---------------------
[INFO] Building aimedical-sys 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  0.266 s
[INFO] Finished at: 2026-06-17T16:44:56+08:00
[INFO] ------------------------------------------------------------------------

### Maven 全量编译 (mvn compile -DskipTests)

[INFO] Scanning for projects...
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Build Order:
[INFO] 
[INFO] aimedical-sys                                                      [pom]
[INFO] common                                                             [jar]
[INFO] common-module-api                                                  [jar]
[INFO] common-module-impl                                                 [jar]
[INFO] ai-api                                                             [jar]
[INFO] ai-impl                                                            [jar]
[INFO] patient                                                            [jar]
[INFO] doctor                                                             [jar]
[INFO] admin                                                              [jar]
[INFO] application                                                        [jar]
[INFO] integration                                                        [jar]
[INFO] 
[INFO] --------------------< com.aimedical:aimedical-sys >---------------------
[INFO] Building aimedical-sys 0.0.1-SNAPSHOT                             [1/11]
[INFO]   from pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] ------------------------< com.aimedical:common >------------------------
[INFO] Building common 0.0.1-SNAPSHOT                                    [2/11]
[INFO]   from common\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common ---
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 11 source files with javac [debug release 17] to target\classes
[INFO] -------------------------------------------------------------
[ERROR] COMPILATION ERROR : 
[INFO] -------------------------------------------------------------
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[3,38] �����jakarta.validation.constraints������
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[4,38] �����jakarta.validation.constraints������
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[10,6] �Ҳ�������
  ����:   �� Min
  λ��: �� com.aimedical.common.result.PageQuery
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[13,6] �Ҳ�������
  ����:   �� Min
  λ��: �� com.aimedical.common.result.PageQuery
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[14,6] �Ҳ�������
  ����:   �� Max
  λ��: �� com.aimedical.common.result.PageQuery
[INFO] 5 errors 
[INFO] -------------------------------------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for aimedical-sys 0.0.1-SNAPSHOT:
[INFO] 
[INFO] aimedical-sys ...................................... SUCCESS [  0.002 s]
[INFO] common ............................................. FAILURE [  1.282 s]
[INFO] common-module-api .................................. SKIPPED
[INFO] common-module-impl ................................. SKIPPED
[INFO] ai-api ............................................. SKIPPED
[INFO] ai-impl ............................................ SKIPPED
[INFO] patient ............................................ SKIPPED
[INFO] doctor ............................................. SKIPPED
[INFO] admin .............................................. SKIPPED
[INFO] application ........................................ SKIPPED
[INFO] integration ........................................ SKIPPED
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.627 s
[INFO] Finished at: 2026-06-17T16:45:12+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile (default-compile) on project common: Compilation failure: Compilation failure: 
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[3,38] �����jakarta.validation.constraints������
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[4,38] �����jakarta.validation.constraints������
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[10,6] �Ҳ�������
[ERROR]   ����:   �� Min
[ERROR]   λ��: �� com.aimedical.common.result.PageQuery
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[13,6] �Ҳ�������
[ERROR]   ����:   �� Min
[ERROR]   λ��: �� com.aimedical.common.result.PageQuery
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[14,6] �Ҳ�������
[ERROR]   ����:   �� Max
[ERROR]   λ��: �� com.aimedical.common.result.PageQuery
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
[ERROR] 
[ERROR] After correcting the problems, you can resume the build with the command
[ERROR]   mvn <args> -rf :common

### Java XPath 单元测试 (mvn test -pl common -Dtest=ParentPomTest)

[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------------< com.aimedical:common >------------------------
[INFO] Building common 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common ---
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 11 source files with javac [debug release 17] to target\classes
[INFO] -------------------------------------------------------------
[ERROR] COMPILATION ERROR : 
[INFO] -------------------------------------------------------------
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[3,38] �����jakarta.validation.constraints������
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[4,38] �����jakarta.validation.constraints������
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[10,6] �Ҳ�������
  ����:   �� Min
  λ��: �� com.aimedical.common.result.PageQuery
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[13,6] �Ҳ�������
  ����:   �� Min
  λ��: �� com.aimedical.common.result.PageQuery
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[14,6] �Ҳ�������
  ����:   �� Max
  λ��: �� com.aimedical.common.result.PageQuery
[INFO] 5 errors 
[INFO] -------------------------------------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.641 s
[INFO] Finished at: 2026-06-17T16:45:30+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile (default-compile) on project common: Compilation failure: Compilation failure: 
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[3,38] �����jakarta.validation.constraints������
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[4,38] �����jakarta.validation.constraints������
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[10,6] �Ҳ�������
[ERROR]   ����:   �� Min
[ERROR]   λ��: �� com.aimedical.common.result.PageQuery
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[13,6] �Ҳ�������
[ERROR]   ����:   �� Min
[ERROR]   λ��: �� com.aimedical.common.result.PageQuery
[ERROR] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/main/java/com/aimedical/common/result/PageQuery.java:[14,6] �Ҳ�������
[ERROR]   ����:   �� Max
[ERROR]   λ��: �� com.aimedical.common.result.PageQuery
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException

### Maven 依赖解析验证 (mvn validate)

[INFO] Reactor Build Order:
[INFO] Building aimedical-sys 0.0.1-SNAPSHOT                             [1/11]
[INFO] Building common 0.0.1-SNAPSHOT                                    [2/11]
[INFO] Building common-module-api 0.0.1-SNAPSHOT                         [3/11]
[INFO] Building common-module-impl 0.0.1-SNAPSHOT                        [4/11]
[INFO] Building ai-api 0.0.1-SNAPSHOT                                    [5/11]
[INFO] Building ai-impl 0.0.1-SNAPSHOT                                   [6/11]
[INFO] Building patient 0.0.1-SNAPSHOT                                   [7/11]
[INFO] Building doctor 0.0.1-SNAPSHOT                                    [8/11]
[INFO] Building admin 0.0.1-SNAPSHOT                                     [9/11]
[INFO] Building application 0.0.1-SNAPSHOT                              [10/11]
[INFO] Building integration 0.0.1-SNAPSHOT                              [11/11]
[INFO] aimedical-sys ...................................... SUCCESS [  0.002 s]
[INFO] common ............................................. SUCCESS [  0.000 s]
[INFO] common-module-api .................................. SUCCESS [  0.001 s]
[INFO] common-module-impl ................................. SUCCESS [  0.000 s]
[INFO] ai-api ............................................. SUCCESS [  0.000 s]
[INFO] ai-impl ............................................ SUCCESS [  0.000 s]
[INFO] patient ............................................ SUCCESS [  0.000 s]
[INFO] doctor ............................................. SUCCESS [  0.000 s]
[INFO] admin .............................................. SUCCESS [  0.000 s]
[INFO] application ........................................ SUCCESS [  0.022 s]
[INFO] integration ........................................ SUCCESS [  0.019 s]
[INFO] BUILD SUCCESS
[INFO] Total time:  0.395 s
