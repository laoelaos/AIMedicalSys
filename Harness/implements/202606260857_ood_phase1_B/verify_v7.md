# 验证报告（v7）

## 结果
FAILED

## 统计
- 通过：230
- 失败：1
- 错误：4
- 跳过：0
- 总测试数：235

## 说明
- GlobalRateLimitFilterTest（v7新增）：9 用例全部通过 ✅
- UserRepositoryTest（已有）：9 用例中 1 失败、4 错误，为 H2 `BIT(1)` 语法兼容性预存问题，非 v7 代码引入

## 测试执行日志

[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------< com.aimedical:common-module-impl >------------------
[INFO] Building common-module-impl 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ common-module-impl ---
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\src\main\resources
[INFO] skip non existing resourceDirectory C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\src\main\resources
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ common-module-impl ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ common-module-impl ---
[INFO] Copying 0 resource from src\test\resources to target\test-classes
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ common-module-impl ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ common-module-impl ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklistTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.087 s -- in com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklistTest
[INFO] Running com.aimedical.modules.commonmodule.auth.login.LoginAttemptTrackerTest
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.665 s -- in com.aimedical.modules.commonmodule.auth.login.LoginAttemptTrackerTest
[INFO] Running com.aimedical.modules.commonmodule.auth.rateLimit.InMemoryRateLimitGuardTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 10.12 s -- in com.aimedical.modules.commonmodule.auth.rateLimit.InMemoryRateLimitGuardTest
[INFO] Running com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounterTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.574 s -- in com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounterTest
[INFO] Running com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilterTest
WARNING: A Java agent has been loaded dynamically (C:\Users\laoE\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.13\byte-buddy-agent-1.14.13.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.395 s -- in com.aimedical.modules.commonmodule.auth.security.GlobalRateLimitFilterTest
[INFO] Running com.aimedical.modules.commonmodule.CommonModulePlaceholderTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.CommonModulePlaceholderTest
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$ChangePasswordTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.077 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$ChangePasswordTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$UpdateMeTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$UpdateMeTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$MeTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$MeTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$RefreshTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.016 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$RefreshTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$LogoutTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$LogoutTests
[INFO] Running com.aimedical.modules.commonmodule.controller.AuthControllerTest$LoginTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest$LoginTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.154 s -- in com.aimedical.modules.commonmodule.controller.AuthControllerTest
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$DeleteMenuTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.037 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$DeleteMenuTests
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$UpdateMenuTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$UpdateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$CreateMenuTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$CreateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.controller.MenuControllerTest$GetMenuTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest$GetMenuTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.067 s -- in com.aimedical.modules.commonmodule.controller.MenuControllerTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.LoginRequestTest
12:54:00.598 [main] INFO org.hibernate.validator.internal.util.Version -- HV000001: Hibernate Validator 8.0.1.Final
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.379 s -- in com.aimedical.modules.commonmodule.dto.request.LoginRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.MenuCreateRequestTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.018 s -- in com.aimedical.modules.commonmodule.dto.request.MenuCreateRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequestTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.020 s -- in com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequestTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.015 s -- in com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.ProfileUpdateRequestTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.041 s -- in com.aimedical.modules.commonmodule.dto.request.ProfileUpdateRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequestTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequestTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.LoginResponseTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.dto.response.LoginResponseTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.MenuResponseTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.commonmodule.dto.response.MenuResponseTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponseTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponseTest
[INFO] Running com.aimedical.modules.commonmodule.dto.response.UserInfoResponseTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.dto.response.UserInfoResponseTest
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$InitTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.204 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$InitTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetExpirationTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetExpirationTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ExtractTokenTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ExtractTokenTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetRoleTests
12:54:01.214 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.010 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetRoleTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetUserIdTests
12:54:01.226 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GetUserIdTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenAndGetClaimsTests
12:54:01.229 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenAndGetClaimsTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenTests
12:54:01.236 [main] WARN com.aimedical.modules.commonmodule.jwt.JwtUtil -- JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 1
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ValidateTokenTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ParseTokenTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$ParseTokenTests
[INFO] Running com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GenerateTokenTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest$GenerateTokenTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.251 s -- in com.aimedical.modules.commonmodule.jwt.JwtUtilTest
[INFO] Running com.aimedical.modules.commonmodule.permission.PermissionFunctionTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.permission.PermissionFunctionTest
[INFO] Running com.aimedical.modules.commonmodule.permission.PostTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.004 s -- in com.aimedical.modules.commonmodule.permission.PostTest
[INFO] Running com.aimedical.modules.commonmodule.permission.RoleTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.003 s -- in com.aimedical.modules.commonmodule.permission.RoleTest
[INFO] Running com.aimedical.modules.commonmodule.permission.UserRepositoryTest

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

2026-06-26T12:54:01.852+08:00  INFO 6616 --- [           main] c.a.m.c.permission.UserRepositoryTest    : Starting UserRepositoryTest using Java 21.0.11 with PID 6616 (started by laoE in C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl)
2026-06-26T12:54:01.853+08:00  INFO 6616 --- [           main] c.a.m.c.permission.UserRepositoryTest    : No active profile set, falling back to 1 default profile: "default"
2026-06-26T12:54:02.199+08:00  INFO 6616 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-06-26T12:54:02.246+08:00  INFO 6616 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 41 ms. Found 4 JPA repository interfaces.
2026-06-26T12:54:02.284+08:00  INFO 6616 --- [           main] beddedDataSourceBeanFactoryPostProcessor : Replacing 'dataSource' DataSource bean with embedded version
2026-06-26T12:54:02.433+08:00  INFO 6616 --- [           main] o.s.j.d.e.EmbeddedDatabaseFactory        : Starting embedded database: url='jdbc:h2:mem:655dc72c-80a8-41b0-8dec-f5254521df12;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
2026-06-26T12:54:02.672+08:00  INFO 6616 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-06-26T12:54:02.728+08:00  INFO 6616 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.4.4.Final
2026-06-26T12:54:02.762+08:00  INFO 6616 --- [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2026-06-26T12:54:02.864+08:00  INFO 6616 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2026-06-26T12:54:02.916+08:00  WARN 6616 --- [           main] org.hibernate.orm.deprecation            : HHH90000025: H2Dialect does not need to be specified explicitly using 'hibernate.dialect' (remove the property setting and it will be selected by default)
2026-06-26T12:54:03.763+08:00  INFO 6616 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
Hibernate: 
    drop table if exists post_function cascade 
Hibernate: 
    drop table if exists sys_function cascade 
Hibernate: 
    drop table if exists sys_post cascade 
Hibernate: 
    drop table if exists sys_role cascade 
Hibernate: 
    drop table if exists sys_user cascade 
Hibernate: 
    drop table if exists user_post cascade 
Hibernate: 
    drop table if exists user_role cascade 
Hibernate: 
    create table post_function (
        function_id bigint not null,
        post_id bigint not null,
        primary key (function_id, post_id)
    )
Hibernate: 
    create table sys_function (
        deleted boolean not null,
        enabled boolean not null,
        sort_order integer not null,
        visible boolean not null,
        created_at timestamp(6),
        id bigint generated by default as identity,
        parent_id bigint,
        updated_at timestamp(6),
        type varchar(20),
        code varchar(255) not null unique,
        description varchar(255),
        icon varchar(255),
        name varchar(255) not null,
        path varchar(255),
        primary key (id)
    )
Hibernate: 
    create table sys_post (
        deleted boolean not null,
        enabled boolean not null,
        sort integer,
        created_at timestamp(6),
        id bigint generated by default as identity,
        role_id bigint,
        updated_at timestamp(6),
        code varchar(255) not null unique,
        description varchar(255),
        name varchar(255),
        primary key (id)
    )
Hibernate: 
    create table sys_role (
        deleted boolean not null,
        enabled boolean not null,
        sort integer not null,
        created_at timestamp(6),
        id bigint generated by default as identity,
        updated_at timestamp(6),
        code varchar(255) not null unique,
        description varchar(255),
        name varchar(255),
        primary key (id)
    )
Hibernate: 
    create table sys_user (
        deleted boolean not null,
        enabled boolean not null,
        password_change_required BIT(1) DEFAULT 0 not null,
        token_version integer not null,
        created_at timestamp(6),
        id bigint generated by default as identity,
        updated_at timestamp(6),
        user_type varchar(20) not null check (user_type in ('DOCTOR','PATIENT','ADMIN')),
        email varchar(255),
        nickname varchar(255) not null,
        password varchar(255) not null,
        phone varchar(255),
        username varchar(255) not null unique,
        primary key (id)
    )
2026-06-26T12:54:03.799+08:00  WARN 6616 --- [           main] o.h.t.s.i.ExceptionHandlerLoggedImpl     : GenerationTarget encountered exception accepting command : Error executing DDL "
    create table sys_user (
        deleted boolean not null,
        enabled boolean not null,
        password_change_required BIT(1) DEFAULT 0 not null,
        token_version integer not null,
        created_at timestamp(6),
        id bigint generated by default as identity,
        updated_at timestamp(6),
        user_type varchar(20) not null check (user_type in ('DOCTOR','PATIENT','ADMIN')),
        email varchar(255),
        nickname varchar(255) not null,
        password varchar(255) not null,
        phone varchar(255),
        username varchar(255) not null unique,
        primary key (id)
    )" via JDBC [Syntax error in SQL statement "\000d\000a    create table sys_user (\000d\000a        deleted boolean not null,\000d\000a        enabled boolean not null,\000d\000a        password_change_required BIT[*](1) DEFAULT 0 not null,\000d\000a        token_version integer not null,\000d\000a        created_at timestamp(6),\000d\000a        id bigint generated by default as identity,\000d\000a        updated_at timestamp(6),\000d\000a        user_type varchar(20) not null check (user_type in ('DOCTOR','PATIENT','ADMIN')),\000d\000a        email varchar(255),\000d\000a        nickname varchar(255) not null,\000d\000a        password varchar(255) not null,\000d\000a        phone varchar(255),\000d\000a        username varchar(255) not null unique,\000d\000a        primary key (id)\000d\000a    )"; expected "ARRAY, INVISIBLE, VISIBLE, NOT NULL, DEFAULT, GENERATED, ON UPDATE, NOT NULL, DEFAULT ON NULL, SEQUENCE, SELECTIVITY, COMMENT, CONSTRAINT, COMMENT, PRIMARY KEY, UNIQUE, NOT NULL, CHECK, REFERENCES, ,, )";]

org.hibernate.tool.schema.spi.CommandAcceptanceException: Error executing DDL "
    create table sys_user (
        deleted boolean not null,
        enabled boolean not null,
        password_change_required BIT(1) DEFAULT 0 not null,
        token_version integer not null,
        created_at timestamp(6),
        id bigint generated by default as identity,
        updated_at timestamp(6),
        user_type varchar(20) not null check (user_type in ('DOCTOR','PATIENT','ADMIN')),
        email varchar(255),
        nickname varchar(255) not null,
        password varchar(255) not null,
        phone varchar(255),
        username varchar(255) not null unique,
        primary key (id)
    )" via JDBC [Syntax error in SQL statement "\000d\000a    create table sys_user (\000d\000a        deleted boolean not null,\000d\000a        enabled boolean not null,\000d\000a        password_change_required BIT[*](1) DEFAULT 0 not null,\000d\000a        token_version integer not null,\000d\000a        created_at timestamp(6),\000d\000a        id bigint generated by default as identity,\000d\000a        updated_at timestamp(6),\000d\000a        user_type varchar(20) not null check (user_type in ('DOCTOR','PATIENT','ADMIN')),\000d\000a        email varchar(255),\000d\000a        nickname varchar(255) not null,\000d\000a        password varchar(255) not null,\000d\000a        phone varchar(255),\000d\000a        username varchar(255) not null unique,\000d\000a        primary key (id)\000d\000a    )"; expected "ARRAY, INVISIBLE, VISIBLE, NOT NULL, DEFAULT, GENERATED, ON UPDATE, NOT NULL, DEFAULT ON NULL, SEQUENCE, SELECTIVITY, COMMENT, CONSTRAINT, COMMENT, PRIMARY KEY, UNIQUE, NOT NULL, CHECK, REFERENCES, ,, )";]
	at org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase.accept(GenerationTargetToDatabase.java:94) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.Helper.applySqlString(Helper.java:233) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.Helper.applySqlStrings(Helper.java:217) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.createTables(SchemaCreatorImpl.java:420) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.createSequencesTablesConstraints(SchemaCreatorImpl.java:340) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.createFromMetadata(SchemaCreatorImpl.java:239) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.performCreation(SchemaCreatorImpl.java:172) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.doCreation(SchemaCreatorImpl.java:142) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.doCreation(SchemaCreatorImpl.java:118) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.performDatabaseAction(SchemaManagementToolCoordinator.java:256) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.lambda$process$5(SchemaManagementToolCoordinator.java:145) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at java.base/java.util.HashMap.forEach(HashMap.java:1429) ~[na:na]
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.process(SchemaManagementToolCoordinator.java:142) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.boot.internal.SessionFactoryObserverForSchemaExport.sessionFactoryCreated(SessionFactoryObserverForSchemaExport.java:37) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.internal.SessionFactoryObserverChain.sessionFactoryCreated(SessionFactoryObserverChain.java:35) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.internal.SessionFactoryImpl.<init>(SessionFactoryImpl.java:315) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.boot.internal.SessionFactoryBuilderImpl.build(SessionFactoryBuilderImpl.java:450) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.build(EntityManagerFactoryBuilderImpl.java:1507) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider.createContainerEntityManagerFactory(SpringHibernateJpaPersistenceProvider.java:75) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.createNativeEntityManagerFactory(LocalContainerEntityManagerFactoryBean.java:390) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:409) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:396) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:366) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1833) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1782) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.getBean(AbstractApplicationContext.java:1234) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:952) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:624) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:334) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.lambda$loadContext$3(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.withHook(SpringApplication.java:1454) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader$ContextLoaderHook.run(SpringBootContextLoader.java:553) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:108) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContextInternal(DefaultCacheAwareContextLoaderDelegate.java:225) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:152) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DependencyInjectionTestExecutionListener.injectDependencies(DependencyInjectionTestExecutionListener.java:142) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DependencyInjectionTestExecutionListener.prepareTestInstance(DependencyInjectionTestExecutionListener.java:98) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]
Caused by: org.h2.jdbc.JdbcSQLSyntaxErrorException: Syntax error in SQL statement "\000d\000a    create table sys_user (\000d\000a        deleted boolean not null,\000d\000a        enabled boolean not null,\000d\000a        password_change_required BIT[*](1) DEFAULT 0 not null,\000d\000a        token_version integer not null,\000d\000a        created_at timestamp(6),\000d\000a        id bigint generated by default as identity,\000d\000a        updated_at timestamp(6),\000d\000a        user_type varchar(20) not null check (user_type in ('DOCTOR','PATIENT','ADMIN')),\000d\000a        email varchar(255),\000d\000a        nickname varchar(255) not null,\000d\000a        password varchar(255) not null,\000d\000a        phone varchar(255),\000d\000a        username varchar(255) not null unique,\000d\000a        primary key (id)\000d\000a    )"; expected "ARRAY, INVISIBLE, VISIBLE, NOT NULL, DEFAULT, GENERATED, ON UPDATE, NOT NULL, DEFAULT ON NULL, SEQUENCE, SELECTIVITY, COMMENT, CONSTRAINT, COMMENT, PRIMARY KEY, UNIQUE, NOT NULL, CHECK, REFERENCES, ,, )"; SQL statement:

    create table sys_user (
        deleted boolean not null,
        enabled boolean not null,
        password_change_required BIT(1) DEFAULT 0 not null,
        token_version integer not null,
        created_at timestamp(6),
        id bigint generated by default as identity,
        updated_at timestamp(6),
        user_type varchar(20) not null check (user_type in ('DOCTOR','PATIENT','ADMIN')),
        email varchar(255),
        nickname varchar(255) not null,
        password varchar(255) not null,
        phone varchar(255),
        username varchar(255) not null unique,
        primary key (id)
    ) [42001-224]
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:514) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:489) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.message.DbException.getSyntaxError(DbException.java:261) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.ParserBase.getSyntaxError(ParserBase.java:750) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.ParserBase.read(ParserBase.java:357) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.ParserBase.readIfMore(ParserBase.java:625) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.Parser.parseCreateTable(Parser.java:8940) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.Parser.parseCreate(Parser.java:6404) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.Parser.parsePrepared(Parser.java:666) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.Parser.parse(Parser.java:592) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.Parser.parse(Parser.java:569) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.Parser.prepareCommand(Parser.java:483) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.engine.SessionLocal.prepareLocal(SessionLocal.java:639) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.engine.SessionLocal.prepareCommand(SessionLocal.java:559) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.jdbc.JdbcConnection.prepareCommand(JdbcConnection.java:1166) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.jdbc.JdbcStatement.executeInternal(JdbcStatement.java:245) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.jdbc.JdbcStatement.execute(JdbcStatement.java:231) ~[h2-2.2.224.jar:2.2.224]
	at org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase.accept(GenerationTargetToDatabase.java:80) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	... 118 common frames omitted

Hibernate: 
    create table user_post (
        post_id bigint not null,
        user_id bigint not null,
        primary key (post_id, user_id)
    )
Hibernate: 
    create table user_role (
        role_id bigint not null,
        user_id bigint not null,
        primary key (role_id, user_id)
    )
Hibernate: 
    alter table if exists post_function 
       add constraint FKh56snoidh814t7tmnsvgkyp6c 
       foreign key (function_id) 
       references sys_function
Hibernate: 
    alter table if exists post_function 
       add constraint FKbv50wilq40pjojsdm6sg6g2xg 
       foreign key (post_id) 
       references sys_post
Hibernate: 
    alter table if exists sys_function 
       add constraint FKmp2cmbi9l9c1c7618t2o0v2xb 
       foreign key (parent_id) 
       references sys_function
Hibernate: 
    alter table if exists sys_post 
       add constraint FKjfpb3no7elnlin0vwqbx940gu 
       foreign key (role_id) 
       references sys_role
Hibernate: 
    alter table if exists user_post 
       add constraint FK1qq5m5bsjagqw0s8m1cyb1rmj 
       foreign key (post_id) 
       references sys_post
Hibernate: 
    alter table if exists user_post 
       add constraint FKafwurpfqy3g4a4k0xnse3l8vy 
       foreign key (user_id) 
       references sys_user
2026-06-26T12:54:03.820+08:00  WARN 6616 --- [           main] o.h.t.s.i.ExceptionHandlerLoggedImpl     : GenerationTarget encountered exception accepting command : Error executing DDL "
    alter table if exists user_post 
       add constraint FKafwurpfqy3g4a4k0xnse3l8vy 
       foreign key (user_id) 
       references sys_user" via JDBC [Table "SYS_USER" not found;]

org.hibernate.tool.schema.spi.CommandAcceptanceException: Error executing DDL "
    alter table if exists user_post 
       add constraint FKafwurpfqy3g4a4k0xnse3l8vy 
       foreign key (user_id) 
       references sys_user" via JDBC [Table "SYS_USER" not found;]
	at org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase.accept(GenerationTargetToDatabase.java:94) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.Helper.applySqlString(Helper.java:233) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.Helper.applySqlStrings(Helper.java:217) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.createForeignKeys(SchemaCreatorImpl.java:303) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.createFromMetadata(SchemaCreatorImpl.java:250) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.performCreation(SchemaCreatorImpl.java:172) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.doCreation(SchemaCreatorImpl.java:142) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.doCreation(SchemaCreatorImpl.java:118) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.performDatabaseAction(SchemaManagementToolCoordinator.java:256) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.lambda$process$5(SchemaManagementToolCoordinator.java:145) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at java.base/java.util.HashMap.forEach(HashMap.java:1429) ~[na:na]
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.process(SchemaManagementToolCoordinator.java:142) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.boot.internal.SessionFactoryObserverForSchemaExport.sessionFactoryCreated(SessionFactoryObserverForSchemaExport.java:37) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.internal.SessionFactoryObserverChain.sessionFactoryCreated(SessionFactoryObserverChain.java:35) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.internal.SessionFactoryImpl.<init>(SessionFactoryImpl.java:315) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.boot.internal.SessionFactoryBuilderImpl.build(SessionFactoryBuilderImpl.java:450) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.build(EntityManagerFactoryBuilderImpl.java:1507) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider.createContainerEntityManagerFactory(SpringHibernateJpaPersistenceProvider.java:75) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.createNativeEntityManagerFactory(LocalContainerEntityManagerFactoryBean.java:390) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:409) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:396) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:366) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1833) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1782) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.getBean(AbstractApplicationContext.java:1234) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:952) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:624) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:334) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.lambda$loadContext$3(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.withHook(SpringApplication.java:1454) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader$ContextLoaderHook.run(SpringBootContextLoader.java:553) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:108) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContextInternal(DefaultCacheAwareContextLoaderDelegate.java:225) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:152) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DependencyInjectionTestExecutionListener.injectDependencies(DependencyInjectionTestExecutionListener.java:142) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DependencyInjectionTestExecutionListener.prepareTestInstance(DependencyInjectionTestExecutionListener.java:98) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]
Caused by: org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "SYS_USER" not found; SQL statement:

    alter table if exists user_post 
       add constraint FKafwurpfqy3g4a4k0xnse3l8vy 
       foreign key (user_id) 
       references sy [42102-224]
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:514) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:489) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.message.DbException.get(DbException.java:223) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.message.DbException.get(DbException.java:199) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.ddl.AlterTableAddConstraint.tryUpdate(AlterTableAddConstraint.java:204) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.ddl.AlterTableAddConstraint.update(AlterTableAddConstraint.java:74) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.ddl.AlterTable.update(AlterTable.java:46) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.CommandContainer.update(CommandContainer.java:169) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.Command.executeUpdate(Command.java:256) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.jdbc.JdbcStatement.executeInternal(JdbcStatement.java:262) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.jdbc.JdbcStatement.execute(JdbcStatement.java:231) ~[h2-2.2.224.jar:2.2.224]
	at org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase.accept(GenerationTargetToDatabase.java:80) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	... 117 common frames omitted

Hibernate: 
    alter table if exists user_role 
       add constraint FKdec2ggmqwgdhhb59jw7o488wx 
       foreign key (role_id) 
       references sys_role
Hibernate: 
    alter table if exists user_role 
       add constraint FKsrs64lo4ci4xyu3da9clbiv8r 
       foreign key (user_id) 
       references sys_user
2026-06-26T12:54:03.824+08:00  WARN 6616 --- [           main] o.h.t.s.i.ExceptionHandlerLoggedImpl     : GenerationTarget encountered exception accepting command : Error executing DDL "
    alter table if exists user_role 
       add constraint FKsrs64lo4ci4xyu3da9clbiv8r 
       foreign key (user_id) 
       references sys_user" via JDBC [Table "SYS_USER" not found;]

org.hibernate.tool.schema.spi.CommandAcceptanceException: Error executing DDL "
    alter table if exists user_role 
       add constraint FKsrs64lo4ci4xyu3da9clbiv8r 
       foreign key (user_id) 
       references sys_user" via JDBC [Table "SYS_USER" not found;]
	at org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase.accept(GenerationTargetToDatabase.java:94) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.Helper.applySqlString(Helper.java:233) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.Helper.applySqlStrings(Helper.java:217) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.createForeignKeys(SchemaCreatorImpl.java:303) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.createFromMetadata(SchemaCreatorImpl.java:250) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.performCreation(SchemaCreatorImpl.java:172) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.doCreation(SchemaCreatorImpl.java:142) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.internal.SchemaCreatorImpl.doCreation(SchemaCreatorImpl.java:118) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.performDatabaseAction(SchemaManagementToolCoordinator.java:256) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.lambda$process$5(SchemaManagementToolCoordinator.java:145) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at java.base/java.util.HashMap.forEach(HashMap.java:1429) ~[na:na]
	at org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.process(SchemaManagementToolCoordinator.java:142) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.boot.internal.SessionFactoryObserverForSchemaExport.sessionFactoryCreated(SessionFactoryObserverForSchemaExport.java:37) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.internal.SessionFactoryObserverChain.sessionFactoryCreated(SessionFactoryObserverChain.java:35) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.internal.SessionFactoryImpl.<init>(SessionFactoryImpl.java:315) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.boot.internal.SessionFactoryBuilderImpl.build(SessionFactoryBuilderImpl.java:450) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.build(EntityManagerFactoryBuilderImpl.java:1507) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	at org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider.createContainerEntityManagerFactory(SpringHibernateJpaPersistenceProvider.java:75) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.createNativeEntityManagerFactory(LocalContainerEntityManagerFactoryBean.java:390) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:409) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:396) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:366) ~[spring-orm-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1833) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1782) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:326) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:324) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.getBean(AbstractApplicationContext.java:1234) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:952) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:624) ~[spring-context-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:334) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.lambda$loadContext$3(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46) ~[spring-core-6.1.6.jar:6.1.6]
	at org.springframework.boot.SpringApplication.withHook(SpringApplication.java:1454) ~[spring-boot-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader$ContextLoaderHook.run(SpringBootContextLoader.java:553) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:137) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.boot.test.context.SpringBootContextLoader.loadContext(SpringBootContextLoader.java:108) ~[spring-boot-test-3.2.5.jar:3.2.5]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContextInternal(DefaultCacheAwareContextLoaderDelegate.java:225) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.loadContext(DefaultCacheAwareContextLoaderDelegate.java:152) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DefaultTestContext.getApplicationContext(DefaultTestContext.java:130) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DependencyInjectionTestExecutionListener.injectDependencies(DependencyInjectionTestExecutionListener.java:142) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.support.DependencyInjectionTestExecutionListener.prepareTestInstance(DependencyInjectionTestExecutionListener.java:98) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.TestContextManager.prepareTestInstance(TestContextManager.java:260) ~[spring-test-6.1.6.jar:6.1.6]
	at org.springframework.test.context.junit.jupiter.SpringExtension.postProcessTestInstance(SpringExtension.java:163) ~[spring-test-6.1.6.jar:6.1.6]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$10(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.executeAndMaskThrowable(ClassBasedTestDescriptor.java:383) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeTestInstancePostProcessors$11(ClassBasedTestDescriptor.java:378) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:179) ~[na:na]
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.StreamSpliterators$WrappingSpliterator.forEachRemaining(StreamSpliterators.java:310) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:735) ~[na:na]
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:762) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.invokeTestInstancePostProcessors(ClassBasedTestDescriptor.java:377) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$instantiateAndPostProcessTestInstance$6(ClassBasedTestDescriptor.java:290) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.instantiateAndPostProcessTestInstance(ClassBasedTestDescriptor.java:289) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$4(ClassBasedTestDescriptor.java:279) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at java.base/java.util.Optional.orElseGet(Optional.java:364) ~[na:na]
	at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$testInstancesProvider$5(ClassBasedTestDescriptor.java:278) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.execution.TestInstancesProvider.getTestInstances(TestInstancesProvider.java:31) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$prepare$0(TestMethodTestDescriptor.java:106) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:105) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.prepare(TestMethodTestDescriptor.java:69) ~[junit-jupiter-engine-5.10.2.jar:5.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$prepare$2(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.prepare(NodeTestTask.java:123) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:90) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596) ~[na:na]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54) ~[junit-platform-engine-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47) ~[junit-platform-launcher-1.10.2.jar:1.10.2]
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122) ~[surefire-junit-platform-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507) ~[surefire-booter-3.1.2.jar:3.1.2]
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495) ~[surefire-booter-3.1.2.jar:3.1.2]
Caused by: org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "SYS_USER" not found; SQL statement:

    alter table if exists user_role 
       add constraint FKsrs64lo4ci4xyu3da9clbiv8r 
       foreign key (user_id) 
       references sy [42102-224]
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:514) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:489) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.message.DbException.get(DbException.java:223) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.message.DbException.get(DbException.java:199) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.ddl.AlterTableAddConstraint.tryUpdate(AlterTableAddConstraint.java:204) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.ddl.AlterTableAddConstraint.update(AlterTableAddConstraint.java:74) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.ddl.AlterTable.update(AlterTable.java:46) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.CommandContainer.update(CommandContainer.java:169) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.command.Command.executeUpdate(Command.java:256) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.jdbc.JdbcStatement.executeInternal(JdbcStatement.java:262) ~[h2-2.2.224.jar:2.2.224]
	at org.h2.jdbc.JdbcStatement.execute(JdbcStatement.java:231) ~[h2-2.2.224.jar:2.2.224]
	at org.hibernate.tool.schema.internal.exec.GenerationTargetToDatabase.accept(GenerationTargetToDatabase.java:80) ~[hibernate-core-6.4.4.Final.jar:6.4.4.Final]
	... 117 common frames omitted

2026-06-26T12:54:03.828+08:00  INFO 6616 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-06-26T12:54:04.253+08:00  INFO 6616 --- [           main] c.a.m.c.permission.UserRepositoryTest    : Started UserRepositoryTest in 2.796 seconds (process running for 17.636)
Hibernate: 
    select
        u1_0.id,
        u1_0.created_at,
        u1_0.deleted,
        u1_0.email,
        u1_0.enabled,
        u1_0.nickname,
        u1_0.password,
        u1_0.password_change_required,
        u1_0.phone,
        u1_0.token_version,
        u1_0.updated_at,
        u1_0.user_type,
        u1_0.username 
    from
        sys_user u1_0 
    where
        u1_0.username=?
2026-06-26T12:54:04.446+08:00  WARN 6616 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 42102, SQLState: 42S02
2026-06-26T12:54:04.446+08:00 ERROR 6616 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : Table "SYS_USER" not found; SQL statement:
select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=? [42102-224]
Hibernate: 
    insert 
    into
        sys_user
        (created_at, deleted, email, enabled, nickname, password, password_change_required, phone, token_version, updated_at, user_type, username, id) 
    values
        (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, default)
2026-06-26T12:54:04.532+08:00  WARN 6616 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 42102, SQLState: 42S02
2026-06-26T12:54:04.532+08:00 ERROR 6616 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : Table "SYS_USER" not found; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [42102-224]
Hibernate: 
    insert 
    into
        sys_user
        (created_at, deleted, email, enabled, nickname, password, password_change_required, phone, token_version, updated_at, user_type, username, id) 
    values
        (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, default)
2026-06-26T12:54:04.538+08:00  WARN 6616 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 42102, SQLState: 42S02
2026-06-26T12:54:04.538+08:00 ERROR 6616 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : Table "SYS_USER" not found; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [42102-224]
Hibernate: 
    insert 
    into
        sys_user
        (created_at, deleted, email, enabled, nickname, password, password_change_required, phone, token_version, updated_at, user_type, username, id) 
    values
        (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, default)
2026-06-26T12:54:04.552+08:00  WARN 6616 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 42102, SQLState: 42S02
2026-06-26T12:54:04.552+08:00 ERROR 6616 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : Table "SYS_USER" not found; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [42102-224]
[ERROR] Tests run: 9, Failures: 1, Errors: 4, Skipped: 0, Time elapsed: 3.313 s <<< FAILURE! -- in com.aimedical.modules.commonmodule.permission.UserRepositoryTest
[ERROR] com.aimedical.modules.commonmodule.permission.UserRepositoryTest.shouldFindByUsernameReturnEmptyWhenNotFound -- Time elapsed: 0.201 s <<< ERROR!
org.springframework.dao.InvalidDataAccessResourceUsageException: 
could not prepare statement [Table "SYS_USER" not found; SQL statement:
select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=? [42102-224]] [select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=?]; SQL [select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=?]
	at org.springframework.orm.jpa.vendor.HibernateJpaDialect.convertHibernateAccessException(HibernateJpaDialect.java:277)
	at org.springframework.orm.jpa.vendor.HibernateJpaDialect.translateExceptionIfPossible(HibernateJpaDialect.java:241)
	at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.translateExceptionIfPossible(AbstractEntityManagerFactoryBean.java:550)
	at org.springframework.dao.support.ChainedPersistenceExceptionTranslator.translateExceptionIfPossible(ChainedPersistenceExceptionTranslator.java:61)
	at org.springframework.dao.support.DataAccessUtils.translateIfNecessary(DataAccessUtils.java:335)
	at org.springframework.dao.support.PersistenceExceptionTranslationInterceptor.invoke(PersistenceExceptionTranslationInterceptor.java:152)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.data.jpa.repository.support.CrudMethodMetadataPostProcessor$CrudMethodMetadataPopulatingMethodInterceptor.invoke(CrudMethodMetadataPostProcessor.java:135)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.aop.interceptor.ExposeInvocationInterceptor.invoke(ExposeInvocationInterceptor.java:97)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.aop.framework.JdkDynamicAopProxy.invoke(JdkDynamicAopProxy.java:223)
	at jdk.proxy2/jdk.proxy2.$Proxy172.findByUsername(Unknown Source)
	at com.aimedical.modules.commonmodule.permission.UserRepositoryTest.shouldFindByUsernameReturnEmptyWhenNotFound(UserRepositoryTest.java:104)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
Caused by: org.hibernate.exception.SQLGrammarException: could not prepare statement [Table "SYS_USER" not found; SQL statement:
select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=? [42102-224]] [select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=?]
	at org.hibernate.exception.internal.SQLExceptionTypeDelegate.convert(SQLExceptionTypeDelegate.java:66)
	at org.hibernate.exception.internal.StandardSQLExceptionConverter.convert(StandardSQLExceptionConverter.java:58)
	at org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(SqlExceptionHelper.java:108)
	at org.hibernate.engine.jdbc.internal.StatementPreparerImpl$StatementPreparationTemplate.prepareStatement(StatementPreparerImpl.java:194)
	at org.hibernate.engine.jdbc.internal.StatementPreparerImpl.prepareQueryStatement(StatementPreparerImpl.java:155)
	at org.hibernate.sql.exec.spi.JdbcSelectExecutor.lambda$list$0(JdbcSelectExecutor.java:85)
	at org.hibernate.sql.results.jdbc.internal.DeferredResultSetAccess.executeQuery(DeferredResultSetAccess.java:231)
	at org.hibernate.sql.results.jdbc.internal.DeferredResultSetAccess.getResultSet(DeferredResultSetAccess.java:167)
	at org.hibernate.sql.results.jdbc.internal.JdbcValuesResultSetImpl.advanceNext(JdbcValuesResultSetImpl.java:218)
	at org.hibernate.sql.results.jdbc.internal.JdbcValuesResultSetImpl.processNext(JdbcValuesResultSetImpl.java:98)
	at org.hibernate.sql.results.jdbc.internal.AbstractJdbcValues.next(AbstractJdbcValues.java:19)
	at org.hibernate.sql.results.internal.RowProcessingStateStandardImpl.next(RowProcessingStateStandardImpl.java:66)
	at org.hibernate.sql.results.spi.ListResultsConsumer.consume(ListResultsConsumer.java:202)
	at org.hibernate.sql.results.spi.ListResultsConsumer.consume(ListResultsConsumer.java:33)
	at org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl.doExecuteQuery(JdbcSelectExecutorStandardImpl.java:209)
	at org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl.executeQuery(JdbcSelectExecutorStandardImpl.java:83)
	at org.hibernate.sql.exec.spi.JdbcSelectExecutor.list(JdbcSelectExecutor.java:76)
	at org.hibernate.sql.exec.spi.JdbcSelectExecutor.list(JdbcSelectExecutor.java:65)
	at org.hibernate.query.sqm.internal.ConcreteSqmSelectQueryPlan.lambda$new$2(ConcreteSqmSelectQueryPlan.java:137)
	at org.hibernate.query.sqm.internal.ConcreteSqmSelectQueryPlan.withCacheableSqmInterpretation(ConcreteSqmSelectQueryPlan.java:362)
	at org.hibernate.query.sqm.internal.ConcreteSqmSelectQueryPlan.performList(ConcreteSqmSelectQueryPlan.java:303)
	at org.hibernate.query.sqm.internal.QuerySqmImpl.doList(QuerySqmImpl.java:509)
	at org.hibernate.query.spi.AbstractSelectionQuery.list(AbstractSelectionQuery.java:427)
	at org.hibernate.query.spi.AbstractSelectionQuery.getSingleResult(AbstractSelectionQuery.java:564)
	at org.springframework.data.jpa.repository.query.JpaQueryExecution$SingleEntityExecution.doExecute(JpaQueryExecution.java:223)
	at org.springframework.data.jpa.repository.query.JpaQueryExecution.execute(JpaQueryExecution.java:92)
	at org.springframework.data.jpa.repository.query.AbstractJpaQuery.doExecute(AbstractJpaQuery.java:149)
	at org.springframework.data.jpa.repository.query.AbstractJpaQuery.execute(AbstractJpaQuery.java:137)
	at org.springframework.data.repository.core.support.RepositoryMethodInvoker.doInvoke(RepositoryMethodInvoker.java:170)
	at org.springframework.data.repository.core.support.RepositoryMethodInvoker.invoke(RepositoryMethodInvoker.java:158)
	at org.springframework.data.repository.core.support.QueryExecutorMethodInterceptor.doInvoke(QueryExecutorMethodInterceptor.java:164)
	at org.springframework.data.repository.core.support.QueryExecutorMethodInterceptor.invoke(QueryExecutorMethodInterceptor.java:143)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor.invoke(DefaultMethodInvokingMethodInterceptor.java:70)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.transaction.interceptor.TransactionInterceptor$1.proceedWithInvocation(TransactionInterceptor.java:123)
	at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:392)
	at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:119)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.dao.support.PersistenceExceptionTranslationInterceptor.invoke(PersistenceExceptionTranslationInterceptor.java:137)
	... 11 more
Caused by: org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "SYS_USER" not found; SQL statement:
select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=? [42102-224]
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:514)
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:489)
	at org.h2.message.DbException.get(DbException.java:223)
	at org.h2.message.DbException.get(DbException.java:199)
	at org.h2.command.Parser.getTableOrViewNotFoundDbException(Parser.java:8064)
	at org.h2.command.Parser.getTableOrViewNotFoundDbException(Parser.java:8035)
	at org.h2.command.Parser.readTableOrView(Parser.java:8024)
	at org.h2.command.Parser.readTablePrimary(Parser.java:1788)
	at org.h2.command.Parser.readTableReference(Parser.java:2268)
	at org.h2.command.Parser.parseSelectFromPart(Parser.java:2718)
	at org.h2.command.Parser.parseSelect(Parser.java:2824)
	at org.h2.command.Parser.parseQueryPrimary(Parser.java:2708)
	at org.h2.command.Parser.parseQueryTerm(Parser.java:2564)
	at org.h2.command.Parser.parseQueryExpressionBody(Parser.java:2543)
	at org.h2.command.Parser.parseQueryExpressionBodyAndEndOfQuery(Parser.java:2536)
	at org.h2.command.Parser.parseQueryExpression(Parser.java:2529)
	at org.h2.command.Parser.parseQuery(Parser.java:2498)
	at org.h2.command.Parser.parsePrepared(Parser.java:627)
	at org.h2.command.Parser.parse(Parser.java:592)
	at org.h2.command.Parser.parse(Parser.java:564)
	at org.h2.command.Parser.prepareCommand(Parser.java:483)
	at org.h2.engine.SessionLocal.prepareLocal(SessionLocal.java:639)
	at org.h2.engine.SessionLocal.prepareCommand(SessionLocal.java:559)
	at org.h2.jdbc.JdbcConnection.prepareCommand(JdbcConnection.java:1166)
	at org.h2.jdbc.JdbcPreparedStatement.<init>(JdbcPreparedStatement.java:93)
	at org.h2.jdbc.JdbcConnection.prepareStatement(JdbcConnection.java:316)
	at org.hibernate.engine.jdbc.internal.StatementPreparerImpl$5.doPrepare(StatementPreparerImpl.java:153)
	at org.hibernate.engine.jdbc.internal.StatementPreparerImpl$StatementPreparationTemplate.prepareStatement(StatementPreparerImpl.java:183)
	... 47 more

[ERROR] com.aimedical.modules.commonmodule.permission.UserRepositoryTest.shouldPersistWithValidPassword -- Time elapsed: 0.054 s <<< ERROR!
org.hibernate.exception.SQLGrammarException: 
could not prepare statement [Table "SYS_USER" not found; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [42102-224]] [insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)]
	at org.hibernate.exception.internal.SQLExceptionTypeDelegate.convert(SQLExceptionTypeDelegate.java:66)
	at org.hibernate.exception.internal.StandardSQLExceptionConverter.convert(StandardSQLExceptionConverter.java:58)
	at org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(SqlExceptionHelper.java:108)
	at org.hibernate.engine.jdbc.internal.MutationStatementPreparerImpl$StatementPreparationTemplate.prepareStatement(MutationStatementPreparerImpl.java:117)
	at org.hibernate.engine.jdbc.internal.MutationStatementPreparerImpl.prepareStatement(MutationStatementPreparerImpl.java:63)
	at org.hibernate.id.insert.GetGeneratedKeysDelegate.prepareStatement(GetGeneratedKeysDelegate.java:86)
	at org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.lambda$identityPreparation$1(ModelMutationHelper.java:132)
	at org.hibernate.engine.jdbc.mutation.internal.PreparedStatementDetailsStandard.resolveStatement(PreparedStatementDetailsStandard.java:87)
	at org.hibernate.id.insert.GetGeneratedKeysDelegate.performInsert(GetGeneratedKeysDelegate.java:104)
	at org.hibernate.engine.jdbc.mutation.internal.MutationExecutorPostInsertSingleTable.execute(MutationExecutorPostInsertSingleTable.java:100)
	at org.hibernate.persister.entity.mutation.InsertCoordinator.doStaticInserts(InsertCoordinator.java:175)
	at org.hibernate.persister.entity.mutation.InsertCoordinator.coordinateInsert(InsertCoordinator.java:113)
	at org.hibernate.persister.entity.AbstractEntityPersister.insert(AbstractEntityPersister.java:2868)
	at org.hibernate.action.internal.EntityIdentityInsertAction.execute(EntityIdentityInsertAction.java:81)
	at org.hibernate.engine.spi.ActionQueue.execute(ActionQueue.java:670)
	at org.hibernate.engine.spi.ActionQueue.addResolvedEntityInsertAction(ActionQueue.java:291)
	at org.hibernate.engine.spi.ActionQueue.addInsertAction(ActionQueue.java:272)
	at org.hibernate.engine.spi.ActionQueue.addAction(ActionQueue.java:322)
	at org.hibernate.event.internal.AbstractSaveEventListener.addInsertAction(AbstractSaveEventListener.java:386)
	at org.hibernate.event.internal.AbstractSaveEventListener.performSaveOrReplicate(AbstractSaveEventListener.java:300)
	at org.hibernate.event.internal.AbstractSaveEventListener.performSave(AbstractSaveEventListener.java:219)
	at org.hibernate.event.internal.AbstractSaveEventListener.saveWithGeneratedId(AbstractSaveEventListener.java:134)
	at org.hibernate.event.internal.DefaultPersistEventListener.entityIsTransient(DefaultPersistEventListener.java:175)
	at org.hibernate.event.internal.DefaultPersistEventListener.persist(DefaultPersistEventListener.java:93)
	at org.hibernate.event.internal.DefaultPersistEventListener.onPersist(DefaultPersistEventListener.java:77)
	at org.hibernate.event.internal.DefaultPersistEventListener.onPersist(DefaultPersistEventListener.java:54)
	at org.hibernate.event.service.internal.EventListenerGroupImpl.fireEventOnEachListener(EventListenerGroupImpl.java:127)
	at org.hibernate.internal.SessionImpl.firePersist(SessionImpl.java:754)
	at org.hibernate.internal.SessionImpl.persist(SessionImpl.java:738)
	at org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager.persist(TestEntityManager.java:92)
	at org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager.persistAndFlush(TestEntityManager.java:130)
	at com.aimedical.modules.commonmodule.permission.UserRepositoryTest.shouldPersistWithValidPassword(UserRepositoryTest.java:69)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
Caused by: org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "SYS_USER" not found; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [42102-224]
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:514)
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:489)
	at org.h2.message.DbException.get(DbException.java:223)
	at org.h2.message.DbException.get(DbException.java:199)
	at org.h2.command.Parser.getTableOrViewNotFoundDbException(Parser.java:8064)
	at org.h2.command.Parser.getTableOrViewNotFoundDbException(Parser.java:8035)
	at org.h2.command.Parser.readTableOrView(Parser.java:8024)
	at org.h2.command.Parser.readTableOrView(Parser.java:7990)
	at org.h2.command.Parser.parseInsert(Parser.java:1540)
	at org.h2.command.Parser.parsePrepared(Parser.java:719)
	at org.h2.command.Parser.parse(Parser.java:592)
	at org.h2.command.Parser.parse(Parser.java:564)
	at org.h2.command.Parser.prepareCommand(Parser.java:483)
	at org.h2.engine.SessionLocal.prepareLocal(SessionLocal.java:639)
	at org.h2.engine.SessionLocal.prepareCommand(SessionLocal.java:559)
	at org.h2.jdbc.JdbcConnection.prepareCommand(JdbcConnection.java:1166)
	at org.h2.jdbc.JdbcPreparedStatement.<init>(JdbcPreparedStatement.java:93)
	at org.h2.jdbc.JdbcConnection.prepareStatement(JdbcConnection.java:1094)
	at org.hibernate.engine.jdbc.internal.MutationStatementPreparerImpl$2.doPrepare(MutationStatementPreparerImpl.java:61)
	at org.hibernate.engine.jdbc.internal.MutationStatementPreparerImpl$StatementPreparationTemplate.prepareStatement(MutationStatementPreparerImpl.java:106)
	... 31 more

[ERROR] com.aimedical.modules.commonmodule.permission.UserRepositoryTest.shouldRejectNullPassword -- Time elapsed: 0.008 s <<< FAILURE!
org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <org.hibernate.exception.ConstraintViolationException> but was: <org.hibernate.exception.SQLGrammarException>
	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
	at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
	at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
	at org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
	at com.aimedical.modules.commonmodule.permission.UserRepositoryTest.shouldRejectNullPassword(UserRepositoryTest.java:59)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
Caused by: org.hibernate.exception.SQLGrammarException: could not prepare statement [Table "SYS_USER" not found; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [42102-224]] [insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)]
	at org.hibernate.exception.internal.SQLExceptionTypeDelegate.convert(SQLExceptionTypeDelegate.java:66)
	at org.hibernate.exception.internal.StandardSQLExceptionConverter.convert(StandardSQLExceptionConverter.java:58)
	at org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(SqlExceptionHelper.java:108)
	at org.hibernate.engine.jdbc.internal.MutationStatementPreparerImpl$StatementPreparationTemplate.prepareStatement(MutationStatementPreparerImpl.java:117)
	at org.hibernate.engine.jdbc.internal.MutationStatementPreparerImpl.prepareStatement(MutationStatementPreparerImpl.java:63)
	at org.hibernate.id.insert.GetGeneratedKeysDelegate.prepareStatement(GetGeneratedKeysDelegate.java:86)
	at org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.lambda$identityPreparation$1(ModelMutationHelper.java:132)
	at org.hibernate.engine.jdbc.mutation.internal.PreparedStatementDetailsStandard.resolveStatement(PreparedStatementDetailsStandard.java:87)
	at org.hibernate.id.insert.GetGeneratedKeysDelegate.performInsert(GetGeneratedKeysDelegate.java:104)
	at org.hibernate.engine.jdbc.mutation.internal.MutationExecutorPostInsertSingleTable.execute(MutationExecutorPostInsertSingleTable.java:100)
	at org.hibernate.persister.entity.mutation.InsertCoordinator.doStaticInserts(InsertCoordinator.java:175)
	at org.hibernate.persister.entity.mutation.InsertCoordinator.coordinateInsert(InsertCoordinator.java:113)
	at org.hibernate.persister.entity.AbstractEntityPersister.insert(AbstractEntityPersister.java:2868)
	at org.hibernate.action.internal.EntityIdentityInsertAction.execute(EntityIdentityInsertAction.java:81)
	at org.hibernate.engine.spi.ActionQueue.execute(ActionQueue.java:670)
	at org.hibernate.engine.spi.ActionQueue.addResolvedEntityInsertAction(ActionQueue.java:291)
	at org.hibernate.engine.spi.ActionQueue.addInsertAction(ActionQueue.java:272)
	at org.hibernate.engine.spi.ActionQueue.addAction(ActionQueue.java:322)
	at org.hibernate.event.internal.AbstractSaveEventListener.addInsertAction(AbstractSaveEventListener.java:386)
	at org.hibernate.event.internal.AbstractSaveEventListener.performSaveOrReplicate(AbstractSaveEventListener.java:300)
	at org.hibernate.event.internal.AbstractSaveEventListener.performSave(AbstractSaveEventListener.java:219)
	at org.hibernate.event.internal.AbstractSaveEventListener.saveWithGeneratedId(AbstractSaveEventListener.java:134)
	at org.hibernate.event.internal.DefaultPersistEventListener.entityIsTransient(DefaultPersistEventListener.java:175)
	at org.hibernate.event.internal.DefaultPersistEventListener.persist(DefaultPersistEventListener.java:93)
	at org.hibernate.event.internal.DefaultPersistEventListener.onPersist(DefaultPersistEventListener.java:77)
	at org.hibernate.event.internal.DefaultPersistEventListener.onPersist(DefaultPersistEventListener.java:54)
	at org.hibernate.event.service.internal.EventListenerGroupImpl.fireEventOnEachListener(EventListenerGroupImpl.java:127)
	at org.hibernate.internal.SessionImpl.firePersist(SessionImpl.java:754)
	at org.hibernate.internal.SessionImpl.persist(SessionImpl.java:738)
	at org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager.persist(TestEntityManager.java:92)
	at org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager.persistAndFlush(TestEntityManager.java:130)
	at com.aimedical.modules.commonmodule.permission.UserRepositoryTest.lambda$shouldRejectNullPassword$0(UserRepositoryTest.java:59)
	at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
	... 6 more
Caused by: org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "SYS_USER" not found; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [42102-224]
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:514)
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:489)
	at org.h2.message.DbException.get(DbException.java:223)
	at org.h2.message.DbException.get(DbException.java:199)
	at org.h2.command.Parser.getTableOrViewNotFoundDbException(Parser.java:8064)
	at org.h2.command.Parser.getTableOrViewNotFoundDbException(Parser.java:8035)
	at org.h2.command.Parser.readTableOrView(Parser.java:8024)
	at org.h2.command.Parser.readTableOrView(Parser.java:7990)
	at org.h2.command.Parser.parseInsert(Parser.java:1540)
	at org.h2.command.Parser.parsePrepared(Parser.java:719)
	at org.h2.command.Parser.parse(Parser.java:592)
	at org.h2.command.Parser.parse(Parser.java:564)
	at org.h2.command.Parser.prepareCommand(Parser.java:483)
	at org.h2.engine.SessionLocal.prepareLocal(SessionLocal.java:639)
	at org.h2.engine.SessionLocal.prepareCommand(SessionLocal.java:559)
	at org.h2.jdbc.JdbcConnection.prepareCommand(JdbcConnection.java:1166)
	at org.h2.jdbc.JdbcPreparedStatement.<init>(JdbcPreparedStatement.java:93)
	at org.h2.jdbc.JdbcConnection.prepareStatement(JdbcConnection.java:1094)
	at org.hibernate.engine.jdbc.internal.MutationStatementPreparerImpl$2.doPrepare(MutationStatementPreparerImpl.java:61)
	at org.hibernate.engine.jdbc.internal.MutationStatementPreparerImpl$StatementPreparationTemplate.prepareStatement(MutationStatementPreparerImpl.java:106)
	... 35 more

[ERROR] com.aimedical.modules.commonmodule.permission.UserRepositoryTest.shouldFindByUsernameReturnUserWhenExists -- Time elapsed: 0.004 s <<< ERROR!
org.hibernate.exception.SQLGrammarException: 
could not prepare statement [Table "SYS_USER" not found; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [42102-224]] [insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)]
	at org.hibernate.exception.internal.SQLExceptionTypeDelegate.convert(SQLExceptionTypeDelegate.java:66)
	at org.hibernate.exception.internal.StandardSQLExceptionConverter.convert(StandardSQLExceptionConverter.java:58)
	at org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(SqlExceptionHelper.java:108)
	at org.hibernate.engine.jdbc.internal.MutationStatementPreparerImpl$StatementPreparationTemplate.prepareStatement(MutationStatementPreparerImpl.java:117)
	at org.hibernate.engine.jdbc.internal.MutationStatementPreparerImpl.prepareStatement(MutationStatementPreparerImpl.java:63)
	at org.hibernate.id.insert.GetGeneratedKeysDelegate.prepareStatement(GetGeneratedKeysDelegate.java:86)
	at org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.lambda$identityPreparation$1(ModelMutationHelper.java:132)
	at org.hibernate.engine.jdbc.mutation.internal.PreparedStatementDetailsStandard.resolveStatement(PreparedStatementDetailsStandard.java:87)
	at org.hibernate.id.insert.GetGeneratedKeysDelegate.performInsert(GetGeneratedKeysDelegate.java:104)
	at org.hibernate.engine.jdbc.mutation.internal.MutationExecutorPostInsertSingleTable.execute(MutationExecutorPostInsertSingleTable.java:100)
	at org.hibernate.persister.entity.mutation.InsertCoordinator.doStaticInserts(InsertCoordinator.java:175)
	at org.hibernate.persister.entity.mutation.InsertCoordinator.coordinateInsert(InsertCoordinator.java:113)
	at org.hibernate.persister.entity.AbstractEntityPersister.insert(AbstractEntityPersister.java:2868)
	at org.hibernate.action.internal.EntityIdentityInsertAction.execute(EntityIdentityInsertAction.java:81)
	at org.hibernate.engine.spi.ActionQueue.execute(ActionQueue.java:670)
	at org.hibernate.engine.spi.ActionQueue.addResolvedEntityInsertAction(ActionQueue.java:291)
	at org.hibernate.engine.spi.ActionQueue.addInsertAction(ActionQueue.java:272)
	at org.hibernate.engine.spi.ActionQueue.addAction(ActionQueue.java:322)
	at org.hibernate.event.internal.AbstractSaveEventListener.addInsertAction(AbstractSaveEventListener.java:386)
	at org.hibernate.event.internal.AbstractSaveEventListener.performSaveOrReplicate(AbstractSaveEventListener.java:300)
	at org.hibernate.event.internal.AbstractSaveEventListener.performSave(AbstractSaveEventListener.java:219)
	at org.hibernate.event.internal.AbstractSaveEventListener.saveWithGeneratedId(AbstractSaveEventListener.java:134)
	at org.hibernate.event.internal.DefaultPersistEventListener.entityIsTransient(DefaultPersistEventListener.java:175)
	at org.hibernate.event.internal.DefaultPersistEventListener.persist(DefaultPersistEventListener.java:93)
	at org.hibernate.event.internal.DefaultPersistEventListener.onPersist(DefaultPersistEventListener.java:77)
	at org.hibernate.event.internal.DefaultPersistEventListener.onPersist(DefaultPersistEventListener.java:54)
	at org.hibernate.event.service.internal.EventListenerGroupImpl.fireEventOnEachListener(EventListenerGroupImpl.java:127)
	at org.hibernate.internal.SessionImpl.firePersist(SessionImpl.java:754)
	at org.hibernate.internal.SessionImpl.persist(SessionImpl.java:738)
	at org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager.persist(TestEntityManager.java:92)
	at org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager.persistAndFlush(TestEntityManager.java:130)
	at com.aimedical.modules.commonmodule.permission.UserRepositoryTest.shouldFindByUsernameReturnUserWhenExists(UserRepositoryTest.java:95)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
Caused by: org.h2.jdbc.JdbcSQLSyntaxErrorException: Table "SYS_USER" not found; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [42102-224]
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:514)
	at org.h2.message.DbException.getJdbcSQLException(DbException.java:489)
	at org.h2.message.DbException.get(DbException.java:223)
	at org.h2.message.DbException.get(DbException.java:199)
	at org.h2.command.Parser.getTableOrViewNotFoundDbException(Parser.java:8064)
	at org.h2.command.Parser.getTableOrViewNotFoundDbException(Parser.java:8035)
	at org.h2.command.Parser.readTableOrView(Parser.java:8024)
	at org.h2.command.Parser.readTableOrView(Parser.java:7990)
	at org.h2.command.Parser.parseInsert(Parser.java:1540)
	at org.h2.command.Parser.parsePrepared(Parser.java:719)
	at org.h2.command.Parser.parse(Parser.java:592)
	at org.h2.command.Parser.parse(Parser.java:564)
	at org.h2.command.Parser.prepareCommand(Parser.java:483)
	at org.h2.engine.SessionLocal.prepareLocal(SessionLocal.java:639)
	at org.h2.engine.SessionLocal.prepareCommand(SessionLocal.java:559)
	at org.h2.jdbc.JdbcConnection.prepareCommand(JdbcConnection.java:1166)
	at org.h2.jdbc.JdbcPreparedStatement.<init>(JdbcPreparedStatement.java:93)
	at org.h2.jdbc.JdbcConnection.prepareStatement(JdbcConnection.java:1094)
	at org.hibernate.engine.jdbc.internal.MutationStatementPreparerImpl$2.doPrepare(MutationStatementPreparerImpl.java:61)
	at org.hibernate.engine.jdbc.internal.MutationStatementPreparerImpl$StatementPreparationTemplate.prepareStatement(MutationStatementPreparerImpl.java:106)
	... 31 more

[ERROR] com.aimedical.modules.commonmodule.permission.UserRepositoryTest.shouldHaveNotNullConstraintOnPasswordColumn -- Time elapsed: 0.026 s <<< ERROR!
org.springframework.dao.EmptyResultDataAccessException: Incorrect result size: expected 1, actual 0
	at org.springframework.dao.support.DataAccessUtils.nullableSingleResult(DataAccessUtils.java:190)
	at org.springframework.jdbc.core.JdbcTemplate.queryForObject(JdbcTemplate.java:520)
	at org.springframework.jdbc.core.JdbcTemplate.queryForObject(JdbcTemplate.java:526)
	at com.aimedical.modules.commonmodule.permission.UserRepositoryTest.shouldHaveNotNullConstraintOnPasswordColumn(UserRepositoryTest.java:78)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[INFO] Running com.aimedical.modules.commonmodule.permission.UserTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.aimedical.modules.commonmodule.permission.UserTest
[INFO] Running com.aimedical.modules.commonmodule.service.AuthServiceTest
[INFO] Running com.aimedical.modules.commonmodule.service.AuthServiceTest$UpdateProfileTests
2026-06-26T12:54:04.752+08:00  INFO 6616 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��������ϸ��³ɹ����û���: testuser
2026-06-26T12:54:04.809+08:00  WARN 6616 --- [           main] c.a.modules.commonmodule.jwt.JwtUtil     : JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 0
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.217 s -- in com.aimedical.modules.commonmodule.service.AuthServiceTest$UpdateProfileTests
[INFO] Running com.aimedical.modules.commonmodule.service.AuthServiceTest$GetCurrentUserTests
2026-06-26T12:54:04.870+08:00  WARN 6616 --- [           main] c.a.modules.commonmodule.jwt.JwtUtil     : JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 0
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.179 s -- in com.aimedical.modules.commonmodule.service.AuthServiceTest$GetCurrentUserTests
[INFO] Running com.aimedical.modules.commonmodule.service.AuthServiceTest$RefreshTokenTests
2026-06-26T12:54:05.047+08:00  WARN 6616 --- [           main] c.a.modules.commonmodule.jwt.JwtUtil     : JWT���Ƹ�ʽ����: Invalid compact JWT string: Compact JWSs must contain exactly 2 period characters, and compact JWEs must contain exactly 4.  Found: 0
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.176 s -- in com.aimedical.modules.commonmodule.service.AuthServiceTest$RefreshTokenTests
[INFO] Running com.aimedical.modules.commonmodule.service.AuthServiceTest$LogoutTests
2026-06-26T12:54:05.222+08:00  INFO 6616 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û��ǳ�
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.057 s -- in com.aimedical.modules.commonmodule.service.AuthServiceTest$LogoutTests
[INFO] Running com.aimedical.modules.commonmodule.service.AuthServiceTest$LoginTests
2026-06-26T12:54:05.283+08:00  WARN 6616 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : ��¼ʧ�ܣ��û��ѱ����ã��û���: testuser
2026-06-26T12:54:05.397+08:00  INFO 6616 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : �û���¼�ɹ����û���: testuser
2026-06-26T12:54:05.453+08:00  WARN 6616 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : ��¼ʧ�ܣ��û������ڣ��û���: nonexistent
2026-06-26T12:54:05.567+08:00  WARN 6616 --- [           main] c.a.m.c.service.impl.AuthServiceImpl     : ��¼ʧ�ܣ���������û���: testuser
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.344 s -- in com.aimedical.modules.commonmodule.service.AuthServiceTest$LoginTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.976 s -- in com.aimedical.modules.commonmodule.service.AuthServiceTest
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetMenuByIdTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.049 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetMenuByIdTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$DeleteMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$DeleteMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$UpdateMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$UpdateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$CreateMenuTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.008 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$CreateMenuTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetAllMenusTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetAllMenusTests
[INFO] Running com.aimedical.modules.commonmodule.service.MenuServiceTest$GetUserMenuTreeTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest$GetUserMenuTreeTests
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.079 s -- in com.aimedical.modules.commonmodule.service.MenuServiceTest
[INFO] 
[INFO] Results:
[INFO] 
[ERROR] Failures: 
[ERROR]   UserRepositoryTest.shouldRejectNullPassword:59 Unexpected exception type thrown, expected: <org.hibernate.exception.ConstraintViolationException> but was: <org.hibernate.exception.SQLGrammarException>
[ERROR] Errors: 
[ERROR]   UserRepositoryTest.shouldFindByUsernameReturnEmptyWhenNotFound:104 ? InvalidDataAccessResourceUsage could not prepare statement [Table "SYS_USER" not found; SQL statement:
select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=? [42102-224]] [select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=?]; SQL [select u1_0.id,u1_0.created_at,u1_0.deleted,u1_0.email,u1_0.enabled,u1_0.nickname,u1_0.password,u1_0.password_change_required,u1_0.phone,u1_0.token_version,u1_0.updated_at,u1_0.user_type,u1_0.username from sys_user u1_0 where u1_0.username=?]
[ERROR]   UserRepositoryTest.shouldFindByUsernameReturnUserWhenExists:95 ? SQLGrammar could not prepare statement [Table "SYS_USER" not found; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [42102-224]] [insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)]
[ERROR]   UserRepositoryTest.shouldHaveNotNullConstraintOnPasswordColumn:78 ? EmptyResultDataAccess Incorrect result size: expected 1, actual 0
[ERROR]   UserRepositoryTest.shouldPersistWithValidPassword:69 ? SQLGrammar could not prepare statement [Table "SYS_USER" not found; SQL statement:
insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default) [42102-224]] [insert into sys_user (created_at,deleted,email,enabled,nickname,password,password_change_required,phone,token_version,updated_at,user_type,username,id) values (?,?,?,?,?,?,?,?,?,?,?,?,default)]
[INFO] 
[ERROR] Tests run: 235, Failures: 1, Errors: 4, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  20.899 s
[INFO] Finished at: 2026-06-26T12:54:05+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.1.2:test (default-test) on project common-module-impl: There are test failures.
[ERROR] 
[ERROR] Please refer to C:\Develop\Software\AIMedicalSys\AIMedical\backend\modules\common-module\common-module-impl\target\surefire-reports for the individual test results.
[ERROR] Please refer to dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
