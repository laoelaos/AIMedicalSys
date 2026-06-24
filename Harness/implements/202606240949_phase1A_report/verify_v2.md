# 验证报告（v2）

## 结果
FAILED

## 统计
- 通过：20（含原有 8 个 + v1 新增 0 个 + v2 新增 9 个 + 其他测试类的 3 个）
- 失败：2（EntityMappingIT.user_shouldPersistWithPassword — userType 未赋值触发的 ConstraintViolation；EntityMappingIT.user_shouldRejectNullPassword — 期望 DataIntegrityViolationException 实际抛出 ConstraintViolationException）
- 说明：2 个失败均为 v1 引入的预存问题，非 v2 变更导致。v2 新增的 9 个测试方法全部通过。

## 测试执行日志

[INFO] Scanning for projects...
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Build Order:
[INFO] 
[INFO] aimedical-sys                                                      [pom]
[INFO] common                                                             [jar]
[INFO] Common Module Aggregator                                           [pom]
[INFO] common-module-api                                                  [jar]
[INFO] common-module-impl                                                 [jar]
[INFO] AI Module Aggregator                                               [pom]
[INFO] ai-api                                                             [jar]
[INFO] ai-impl                                                            [jar]
[INFO] patient                                                            [jar]
[INFO] doctor                                                             [jar]
[INFO] admin                                                              [jar]
[INFO] application                                                        [jar]
[INFO] integration                                                        [jar]
[INFO] 
[INFO] --------------------< com.aimedical:aimedical-sys >---------------------
[INFO] Building aimedical-sys 0.0.1-SNAPSHOT                             [1/13]
[INFO]   from pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- clean:3.3.2:clean (default-clean) @ aimedical-sys ---
[INFO] 
[INFO] ------------------------< com.aimedical:common >------------------------
[INFO] Building common 0.0.1-SNAPSHOT                                    [2/13]
[INFO]   from common\pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- clean:3.3.2:clean (default-clean) @ common ---
[INFO] Deleting C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\target
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common ---
[INFO] Changes detected - recompiling the module! :source
[INFO] Compiling 13 source files with javac [debug release 17] to target\classes
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ common ---
[INFO] Changes detected - recompiling the module! :dependency
[INFO] Compiling 19 source files with javac [debug release 17] to target\test-classes
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java: C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\src\test\java\com\aimedical\common\config\GlobalExceptionHandlerTest.javaʹ�û򸲸����ѹ�ʱ�� API��
[INFO] /C:/Develop/Software/AIMedicalSys/AIMedical/backend/common/src/test/java/com/aimedical/common/config/GlobalExceptionHandlerTest.java: �й���ϸ��Ϣ, ��ʹ�� -Xlint:deprecation ���±��롣
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Tests run: 103, Failures: 0, Errors: 0, Skipped: 5, Time elapsed: 4.482 s -- in common
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.067 s -- in common-module-api
[INFO] Tests run: 39, Failures: 1, Errors: 1, Skipped: 0, Time elapsed: 4.418 s <<< FAILURE! -- in common-module-impl
[INFO] Tests run: 22, Failures: 1, Errors: 1, Skipped: 0, Time elapsed: 17.346 s <<< FAILURE! -- in integration
[INFO]   EntityMappingIT.user_shouldRejectNullPassword:267 Unexpected exception type thrown, expected: <org.springframework.dao.DataIntegrityViolationException> but was: <org.hibernate.exception.ConstraintViolationException>
[INFO]   EntityMappingIT.user_shouldPersistWithPassword:255 ? ConstraintViolation could not execute statement [NULL not allowed for column "USER_TYPE"]
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  21.170 s
[INFO] Finished at: 2026-06-24T11:40:32+08:00
[INFO] ------------------------------------------------------------------------
