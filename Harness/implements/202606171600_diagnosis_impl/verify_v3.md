# 验证报告（v3）

## 结果
PASSED

## 统计
- 通过：4
- 失败：0

| 测试项 | 结果 | 说明 |
|--------|------|------|
| 父 POM 验证 (`mvn validate -N`) | ✅ PASSED | 父 POM `aimedical-sys` BUILD SUCCESS |
| 全模块编译 (`mvn compile -DskipTests`) | ✅ PASSED | 11/11 模块全部 SUCCESS，v3 移除 `PageQuery.java` 注解后编译阻塞消除 |
| ParentPomTest (`mvn test -pl common -Dtest=ParentPomTest`) | ✅ PASSED | 5 tests run, 0 failures — 预存编译阻塞已消除，XPath 测试正常执行 |
| PageQueryTest (`mvn test -pl common -Dtest=PageQueryTest`) | ✅ PASSED | 6 tests run, 0 failures — POJO 基本行为测试全部通过 |

## 测试执行日志
[INFO] Scanning for projects...
[INFO] 
[INFO] --------------------< com.aimedical:aimedical-sys >---------------------
[INFO] Building aimedical-sys 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  0.268 s
[INFO] Finished at: 2026-06-17T16:57:13+08:00
[INFO] ------------------------------------------------------------------------


### Maven ȫ������ (mvn compile -DskipTests)
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
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] ------------------< com.aimedical:common-module-api >-------------------
[INFO] Building common-module-api 0.0.1-SNAPSHOT                         [3/11]
[INFO]   from common-module-api\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common-module-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-api\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-api\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common-module-api ---
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 1 source file with javac [debug release 17] to target\classes
[INFO] 
[INFO] ------------------< com.aimedical:common-module-impl >------------------
[INFO] Building common-module-impl 0.0.1-SNAPSHOT                        [4/11]
[INFO]   from common-module-impl\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common-module-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-impl\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-impl\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common-module-impl ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 5 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] ------------------------< com.aimedical:ai-api >------------------------
[INFO] Building ai-api 0.0.1-SNAPSHOT                                    [5/11]
[INFO]   from ai-api\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ ai-api ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-api\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-api\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ ai-api ---
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 31 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] -----------------------< com.aimedical:ai-impl >------------------------
[INFO] Building ai-impl 0.0.1-SNAPSHOT                                   [6/11]
[INFO]   from ai-impl\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ ai-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-impl\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-impl\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ ai-impl ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 3 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] -----------------------< com.aimedical:patient >------------------------
[INFO] Building patient 0.0.1-SNAPSHOT                                   [7/11]
[INFO]   from patient\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ patient ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\patient\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\patient\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ patient ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 7 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] ------------------------< com.aimedical:doctor >------------------------
[INFO] Building doctor 0.0.1-SNAPSHOT                                    [8/11]
[INFO]   from doctor\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ doctor ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\doctor\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\doctor\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ doctor ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 7 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] ------------------------< com.aimedical:admin >-------------------------
[INFO] Building admin 0.0.1-SNAPSHOT                                     [9/11]
[INFO]   from admin\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ admin ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\admin\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\admin\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ admin ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 7 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] ---------------------< com.aimedical:application >----------------------
[INFO] Building application 0.0.1-SNAPSHOT                              [10/11]
[INFO]   from application\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ application ---
[INFO] Copying 3 resources from src\main\resources to target\classes
[INFO] Copying 0 resource from src\main\resources to target\classes
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ application ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 3 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] ---------------------< com.aimedical:integration >----------------------
[INFO] Building integration 0.0.1-SNAPSHOT                              [11/11]
[INFO]   from integration\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ integration ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\integration\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\integration\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ integration ---
[INFO] No sources to compile
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for aimedical-sys 0.0.1-SNAPSHOT:
[INFO] 
[INFO] aimedical-sys ...................................... SUCCESS [  0.002 s]
[INFO] common ............................................. SUCCESS [  0.624 s]
[INFO] common-module-api .................................. SUCCESS [  0.484 s]
[INFO] common-module-impl ................................. SUCCESS [  0.335 s]
[INFO] ai-api ............................................. SUCCESS [  0.199 s]
[INFO] ai-impl ............................................ SUCCESS [  0.830 s]
[INFO] patient ............................................ SUCCESS [  0.512 s]
[INFO] doctor ............................................. SUCCESS [  0.432 s]
[INFO] admin .............................................. SUCCESS [  0.392 s]
[INFO] application ........................................ SUCCESS [  0.678 s]
[INFO] integration ........................................ SUCCESS [  0.033 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.868 s
[INFO] Finished at: 2026-06-17T16:57:23+08:00
[INFO] ------------------------------------------------------------------------


### ParentPomTest (mvn test -pl common -Dtest=ParentPomTest)
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
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ common ---
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 15 source files with javac [debug release 17] to target\test-classes
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ common ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.common.pom.ParentPomTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.096 s -- in com.aimedical.common.pom.ParentPomTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  3.541 s
[INFO] Finished at: 2026-06-17T16:57:32+08:00
[INFO] ------------------------------------------------------------------------


### PageQueryTest (mvn test -pl common -Dtest=PageQueryTest)
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
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ common ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ common ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.common.result.PageQueryTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.042 s -- in com.aimedical.common.result.PageQueryTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  2.110 s
[INFO] Finished at: 2026-06-17T16:57:40+08:00
[INFO] ------------------------------------------------------------------------

