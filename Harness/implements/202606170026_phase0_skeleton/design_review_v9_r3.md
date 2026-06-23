# 设计审查报告（v9 r3）

## 审查结果
REJECTED

## 发现

### **[一般]** `@SpringBootTest` 占位测试设计在模块结构上不可行

**问题**：「占位测试契约」章节规定每个模块的测试类应使用 `@SpringBootTest` 加载完整 Spring 上下文，并通过 `TestRestTemplate`/`MockMvc` 验证端点可达。但在当前多模块 Maven 结构中：

- 业务模块（patient/doctor/admin）各自没有独立的 `@SpringBootApplication` 或 `@SpringBootConfiguration` 类
- `@SpringBootTest` 默认从测试包向上扫描寻找 `@SpringBootConfiguration`，在 `com.aimedical.modules.patient` 包层次中不存在这样的配置类，测试将无法启动
- `application` 模块仅有 `pom.xml`，无 `Application.java` 主类，无法作为 `@SpringBootTest` 引用的引导类
- 三个模块的实际实现均为空 JUnit5 测试（无 `@SpringBootTest` 注解），与设计描述不一致

**为什么是问题**：设计描述的测试方案在现有模块结构下不可执行，属于结构性缺陷。若严格按设计实现将导致编译通过后测试运行失败，或产生模块间的循环依赖。

**修正方向**（二选一）：
1. **调整测试范围为 Phase 0 占位**：将测试设计改为纯 POJO 占位测试（与实际实现一致），删除 `@SpringBootTest` / `TestRestTemplate` / `MockMvc` 的要求，留待 Phase 1+ 在 application 或 integration 模块中补充集成测试
2. **补充测试基础设施设计**：说明每个业务模块如何设置测试专用的 `@SpringBootConfiguration`（如 `@SpringBootTest(classes = {PatientController.class, PatientServiceImpl.class})`），或要求 application 模块提供 `Application.java` 主类并说明模块间测试依赖关系

### **[轻微]** 获取占位结果的职责描述不一致

**问题**：「核心抽象/PatientController」写道 Controller 返回 `Result.success("placeholder")`，而「关键行为契约/Controller → Service 协作」显示值由 ServiceImpl 生产、Controller 透传。两者不矛盾但可更精确。

**修正方向**：对齐两处描述，统一为 "Controller 委托 Service 占位方法返回 `Result.success("...placeholder")`"。
