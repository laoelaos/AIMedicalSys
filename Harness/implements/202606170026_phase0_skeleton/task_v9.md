# 任务指令（v9）

## 动作
NEW

## 任务描述
创建三个业务模块骨架（patient/doctor/admin），每个模块包含以下占位文件（共 3 个模块 × 8 个占位文件 = 24 个源文件 + 3 个 POM + 3 个测试占位），并更新父 POM：

### 各模块目录结构
```
backend/patient/
├── pom.xml
└── src/
    ├── main/java/com/aimedical/modules/patient/
    │   ├── api/PatientController.java      — @RestController 占位，@RequestMapping("/api/patient")
    │   ├── service/PatientService.java     — Service 接口
    │   ├── service/impl/PatientServiceImpl.java — @Service 占位实现
    │   ├── repository/PatientRepository.java    — extends JpaRepository 骨架
    │   ├── entity/PatientEntity.java            — extends BaseEntity 占位实体
    │   ├── dto/PatientDto.java                  — 占位 DTO record/class
    │   └── converter/PatientConverter.java     — 占位 Converter 接口/类
    └── test/java/com/aimedical/modules/patient/
        └── PatientPlaceholderTest.java    — 占位测试类

backend/doctor/  — 同上结构，包名 com.aimedical.modules.doctor
backend/admin/   — 同上结构，包名 com.aimedical.modules.admin
```

### 父 POM 变更
- modules 列表增加：`<module>patient</module>`, `<module>doctor</module>`, `<module>admin</module>`
- dependencyManagement 增加 `patient`, `doctor`, `admin` 内部模块依赖声明
- 在 `<ignoredUnusedDeclaredDependencies>` 中增加 ai-api 和 common-module-api 豁免（业务模块声明但暂未引用）

## 选择理由
业务模块是后端依赖树的下一层，application 模块需要它们存在才能正确引用；三模块采用相同的占位模板，适合合并为一个任务。

## 任务上下文
- OOD §2.1: 业务模块位于 backend/modules/{patient|doctor|admin}（但 Phase 0 实际结构按现有惯例放置在 backend/{patient|doctor|admin} 根级）
- OOD §2.3: 包命名 com.aimedical.modules.{patient|doctor|admin}，下分 api/service/repository/entity/dto/converter 六个子包
- OOD §2.4: 各模块依赖 common、common-module-api、ai-api，模块间互不依赖
- OOD §4.2: 占位 Controller 不注入 AiService，仅返回 Result.success("placeholder")

## 已有代码上下文
- 后端父 POM 当前包含 6 个模块：common, common-module-api, common-module-impl, ai-api, ai-impl, application
- 已有模板参考：common-module-impl 的实体和 Repository 写法，ai-api 的接口定义风格
- common 模块提供 BaseEntity (id, createdAt, updatedAt, deleted)、Result<T>、PageQuery、PageResponse 等基类
- spring-boot-starter-web、spring-boot-starter-data-jpa、spring-boot-starter-validation 已在父 POM dependencyManagement 中声明版本
- 需要在 `<ignoredUnusedDeclaredDependencies>` 中添加这三个模块的 ai-api 和 common-module-api 豁免

## 各模块 POM 依赖
每个业务模块 POM 需声明（父 POM 已管理版本）：
- com.aimedical:common (compile)
- com.aimedical:common-module-api (compile)
- com.aimedical:ai-api (compile)
- spring-boot-starter-web (compile)
- spring-boot-starter-data-jpa (compile)
- spring-boot-starter-validation (compile)
- spring-boot-starter-test (test)
