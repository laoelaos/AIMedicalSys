# 测试报告（v15）

## 概述
为 medical-record 模块（包D-AI2 病历生成）编写单元测试，覆盖枚举、DTO、JPA 实体、Converter、Template 配置管理、MissingField 检测、Service/Impl、Controller。

## 测试文件清单

| 文件路径 | 被测类型 | 用例数 | 覆盖维度 |
|---------|---------|--------|---------|
| src/test/java/.../enums/MedicalRecordFieldTest.java | MedicalRecordField | 4 | 常量个数、valueOf、name、异常 |
| src/test/java/.../enums/MedicalRecordErrorCodeTest.java | MedicalRecordErrorCode | 4 | ErrorCode接口、code/message、唯一性 |
| src/test/java/.../dto/RecordGenerateRequestTest.java | RecordGenerateRequest | 2 | 默认值、getter/setter |
| src/test/java/.../dto/RecordGenerateResponseTest.java | RecordGenerateResponse | 2 | 默认值、getter/setter |
| src/test/java/.../dto/FieldMissingHintTest.java | FieldMissingHint | 2 | 默认值、getter/setter |
| src/test/java/.../entity/MedicalRecordTest.java | MedicalRecord | 4 | 默认值、getter/setter、@PrePersist、@PreUpdate |
| src/test/java/.../entity/DeptTemplateConfigTest.java | DeptTemplateConfig | 4 | 默认值、getter/setter、@PrePersist、@PreUpdate |
| src/test/java/.../converter/MedicalRecordContentConverterTest.java | MedicalRecordContentConverter | 9 | null→null、null→emptyMap、序列化、反序列化、无效JSON、未知枚举、混合已知/未知key、roundtrip |
| src/test/java/.../converter/MedicalRecordConverterTest.java | MedicalRecordConverter | 5 | toFieldsMap全部字段、null保留、toAiRequest、构建响应、超时错误码 |
| src/test/java/.../template/DepartmentTemplateConfigTest.java | DepartmentTemplateConfig | 1 | 全参构造/getter |
| src/test/java/.../template/DatabaseTemplateConfigManagerTest.java | DatabaseTemplateConfigManager | 10 | 未知科室→DEFAULT、DB加载、解析异常降级、无效枚举降级、null字段、缺失子对象、缓存、刷新、DEFAULT模板占位符 |
| src/test/java/.../detector/MissingFieldDetectorImplTest.java | MissingFieldDetectorImpl | 10 | 全部已填→空、null→提示、空字符串→提示、空白字符串→提示、多字段缺失、全部缺失、占位符解析、自定义文案、默认文案、全字段中文名 |
| src/test/java/.../service/impl/MedicalRecordServiceImplTest.java | MedicalRecordServiceImpl | 10 | encounterId null/空、VisitFacade超时降级、异常降级、AI超时→degraded、正常流程、持久化、乐观锁异常、缺失字段检测、detector hints传递、fallback标志 |
| src/test/java/.../api/MedicalRecordControllerTest.java | MedicalRecordController | 3 | stream=true→fail（verify Service未调用）、success→success、fail→fail |
| **合计** | **14 个测试文件** | **70 个用例** | 正常路径、边界条件、错误路径、状态交互 |

## 设计偏差
无偏差。测试严格按详细设计 v15 行为契约编写。根据 test_review_v15_r1 审查意见修订完成，所有 5 项问题均已修复。

## 测试方法
- DTO/Entity/枚举：直接构造 + getter/setter 基本验证
- Converter：构造真实实例，传入构造好的输入验证输出
- Detector：构造真实实例，验证差集算法和占位符解析
- Template Manager：匿名 Stub 类模拟 Repository（与 consultation 模块风格一致）
- Service：匿名 Stub 类模拟 6 个依赖（VisitFacade、TemplateConfigManager、AiService、MissingFieldDetector、MedicalRecordConverter、MedicalRecordRepository）
- Controller：匿名 Stub 类模拟 Service

## 修订说明（v15 R1）
| 审查意见 | 修改措施 |
|---------|---------|
| `shouldSetVisitIdFallbackWhenEncounterIdFallbackUsed` 中 `fallback=true` 使 Stub 正常返回而非抛异常，服务端不会进入降级路径 | 移除 `StubVisitFacade.fallback`；测试改为 `throwException=true` 触发 catch 降级路径 |
| `shouldReturnDegradedWhenAiTimesOut` 使用 `new CompletableFuture<>()` 导致 12s 阻塞 | 改为 `CompletableFuture.supplyAsync(() -> { throw ... })`，立即抛出 ExecutionException |
| `shouldUseFallbackWhenVisitFacadeTimesOut` 中 `Thread.sleep(3000)` 不必要延长测试时间 | 移除 sleep，timeout 路径直接 `throw new RuntimeException()` |
| stream=true 测试未验证 Service 未被调用（可能假阴性） | `StubService` 新增 `generateCalled` 标志并 assertFalse 验证 |
| 未知枚举反序列化仅测试全部未知场景，未覆盖混合有效/无效 key | 新增 `convertToEntityAttributeShouldHandleMixedKnownAndUnknownKeys` 测试 |
