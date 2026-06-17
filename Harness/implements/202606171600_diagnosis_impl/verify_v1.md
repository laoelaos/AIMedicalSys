# 验证报告（v1）

## 结果
FAILED

## 统计
- Maven 编译：0/10 模块通过（构建无法读取 10 个子模块 POM）
- 父 POM 验证：1/1 通过（`mvn validate -N` BUILD SUCCESS）
- Java XPath 单元测试：12/13 通过，1 失败（测试用例 XPath 表达式 bug，非代码问题）
- 总通过：2/4 项

## 失败原因摘要
1. **`dependencyManagement` 中 5 个 Spring Boot starter 无 `<version>`**：starter-web、starter-data-jpa、starter-security、starter-validation、starter-test 在 `<dependencyManagement>` 中声明但无版本号，Maven 在 `dependencyManagement` 匹配到条目后停止向 parent BOM 查找，导致版本不可解析。涉及所有 10 个子模块。
2. **application/pom.xml 依赖 patient/doctor/admin 但无版本**：父 POM `dependencyManagement` 已删除这 3 个业务模块条目，且 application 未提供 `<version>`。涉及 application 模块的 3 个依赖错误。
3. **ParentPomTest.dependencyManagementShouldContainCoreInternalModules 测试失败**：XPath 表达式 `and` 关键字位于谓词括号外（`[groupId='com.aimedical'] and artifactId='common'`），导致布尔上下文错误。此测试 bug 不影响代码正确性——`dependencyManagement` 实际包含全部 6 个核心内部模块。

## 测试执行日志


### Maven 编译验证 (mvn compile -pl common -am) 

[INFO] Scanning for projects...
[ERROR] [ERROR] Some problems were encountered while processing the POMs:
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 17, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ line 22, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 27, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 17, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ line 21, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 25, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 17, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 25, column 21
[ERROR] 'dependencies.dependency.version' for com.aimedical:patient:jar is missing. @ line 34, column 21
[ERROR] 'dependencies.dependency.version' for com.aimedical:doctor:jar is missing. @ line 38, column 21
[ERROR] 'dependencies.dependency.version' for com.aimedical:admin:jar is missing. @ line 42, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 48, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ line 52, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-security:jar is missing. @ line 56, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 79, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 25, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ line 29, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-validation:jar is missing. @ line 33, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 37, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 25, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ line 29, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-validation:jar is missing. @ line 33, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 37, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 25, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ line 29, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-validation:jar is missing. @ line 33, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 37, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 22, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 34, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-security:jar is missing. @ line 39, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ line 44, column 21
 @ 
[ERROR] The build could not read 10 projects -> [Help 1]
[ERROR]   
[ERROR]   The project com.aimedical:common:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\pom.xml) has 3 errors
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 17, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ line 22, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 27, column 21
[ERROR]   
[ERROR]   The project com.aimedical:common-module-api:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-api\pom.xml) has 1 error
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 17, column 21
[ERROR]   
[ERROR]   The project com.aimedical:common-module-impl:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\common-module-impl\pom.xml) has 2 errors
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ line 21, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 25, column 21
[ERROR]   
[ERROR]   The project com.aimedical:ai-api:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-api\pom.xml) has 1 error
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 17, column 21
[ERROR]   
[ERROR]   The project com.aimedical:ai-impl:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-impl\pom.xml) has 1 error
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 25, column 21
[ERROR]   
[ERROR]   The project com.aimedical:application:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\application\pom.xml) has 7 errors
[ERROR]     'dependencies.dependency.version' for com.aimedical:patient:jar is missing. @ line 34, column 21
[ERROR]     'dependencies.dependency.version' for com.aimedical:doctor:jar is missing. @ line 38, column 21
[ERROR]     'dependencies.dependency.version' for com.aimedical:admin:jar is missing. @ line 42, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 48, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ line 52, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-security:jar is missing. @ line 56, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 79, column 21
[ERROR]   
[ERROR]   The project com.aimedical:patient:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\patient\pom.xml) has 4 errors
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 25, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ line 29, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-validation:jar is missing. @ line 33, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 37, column 21
[ERROR]   
[ERROR]   The project com.aimedical:doctor:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\doctor\pom.xml) has 4 errors
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 25, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ line 29, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-validation:jar is missing. @ line 33, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 37, column 21
[ERROR]   
[ERROR]   The project com.aimedical:admin:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\admin\pom.xml) has 4 errors
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 25, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ line 29, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-validation:jar is missing. @ line 33, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 37, column 21
[ERROR]   
[ERROR]   The project com.aimedical:integration:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\integration\pom.xml) has 4 errors
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 22, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 34, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-security:jar is missing. @ line 39, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ line 44, column 21
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/ProjectBuildingException

### Java XPath 单元测试结果

=== POM XPath Test Results ===
Tests found: 13
Tests succeeded: 12
Tests failed: 1
Tests skipped: 0
Time: 1781684924099ms

=== FAILURES ===
FAIL: dependencyManagementShouldContainCoreInternalModules()
org.opentest4j.AssertionFailedError: expected: <true> but was: <false>
	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
	at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
	at org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
	at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
	at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:31)
	at org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:183)
	at com.aimedical.common.pom.ParentPomTest.dependencyManagementShouldContainCoreInternalModules(ParentPomTest.java:64)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

### Maven 父 POM 验证 (mvn validate -N)

[INFO] Scanning for projects...
[INFO] 
[INFO] --------------------< com.aimedical:aimedical-sys >---------------------
[INFO] Building aimedical-sys 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  0.283 s
[INFO] Finished at: 2026-06-17T16:28:49+08:00
[INFO] ------------------------------------------------------------------------

### Maven 全量验证 (mvn validate 2>&1 | Select-String -Pattern 'ERROR|BUILD')


[INFO] Scanning for projects...
[ERROR] [ERROR] Some problems were encountered while processing the POMs:
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 1
7, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ l
ine 22, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 
27, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 
17, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ l
ine 21, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 
25, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 
17, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 
25, column 21
[ERROR] 'dependencies.dependency.version' for com.aimedical:patient:jar is missing. @ line 34, column 21
[ERROR] 'dependencies.dependency.version' for com.aimedical:doctor:jar is missing. @ line 38, column 21
[ERROR] 'dependencies.dependency.version' for com.aimedical:admin:jar is missing. @ line 42, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 4
8, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ l
ine 52, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-security:jar is missing. @ l
ine 56, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 
79, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 2
5, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ l
ine 29, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-validation:jar is missing. @
 line 33, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 
37, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 2
5, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ l
ine 29, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-validation:jar is missing. @
 line 33, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 
37, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 2
5, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ l
ine 29, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-validation:jar is missing. @
 line 33, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 
37, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ line 
22, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ line 3
4, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-security:jar is missing. @ l
ine 39, column 21
[ERROR] 'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing. @ l
ine 44, column 21
 @ 
[ERROR] The build could not read 10 projects -> [Help 1]
[ERROR]   
[ERROR]   The project com.aimedical:common:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\common\po
m.xml) has 3 errors
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ li
ne 17, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing.
 @ line 22, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ l
ine 27, column 21
[ERROR]   
[ERROR]   The project com.aimedical:common-module-api:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backen
d\common-module-api\pom.xml) has 1 error
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ l
ine 17, column 21
[ERROR]   
[ERROR]   The project com.aimedical:common-module-impl:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backe
nd\common-module-impl\pom.xml) has 2 errors
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing.
 @ line 21, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ l
ine 25, column 21
[ERROR]   
[ERROR]   The project com.aimedical:ai-api:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-api\po
m.xml) has 1 error
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ l
ine 17, column 21
[ERROR]   
[ERROR]   The project com.aimedical:ai-impl:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\ai-impl\
pom.xml) has 1 error
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ l
ine 25, column 21
[ERROR]   
[ERROR]   The project com.aimedical:application:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\appl
ication\pom.xml) has 7 errors
[ERROR]     'dependencies.dependency.version' for com.aimedical:patient:jar is missing. @ line 34, column 21
[ERROR]     'dependencies.dependency.version' for com.aimedical:doctor:jar is missing. @ line 38, column 21
[ERROR]     'dependencies.dependency.version' for com.aimedical:admin:jar is missing. @ line 42, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ li
ne 48, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing.
 @ line 52, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-security:jar is missing.
 @ line 56, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ l
ine 79, column 21
[ERROR]   
[ERROR]   The project com.aimedical:patient:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\patient\
pom.xml) has 4 errors
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ li
ne 25, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing.
 @ line 29, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-validation:jar is missin
g. @ line 33, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ l
ine 37, column 21
[ERROR]   
[ERROR]   The project com.aimedical:doctor:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\doctor\po
m.xml) has 4 errors
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ li
ne 25, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing.
 @ line 29, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-validation:jar is missin
g. @ line 33, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ l
ine 37, column 21
[ERROR]   
[ERROR]   The project com.aimedical:admin:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\admin\pom.
xml) has 4 errors
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ li
ne 25, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing.
 @ line 29, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-validation:jar is missin
g. @ line 33, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ l
ine 37, column 21
[ERROR]   
[ERROR]   The project com.aimedical:integration:0.0.1-SNAPSHOT (C:\Develop\Software\AIMedicalSys\AIMedical\backend\inte
gration\pom.xml) has 4 errors
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-test:jar is missing. @ l
ine 22, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-web:jar is missing. @ li
ne 34, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-security:jar is missing.
 @ line 39, column 21
[ERROR]     'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-data-jpa:jar is missing.
 @ line 44, column 21
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/ProjectBuildingException



