# 详细设计（v1）

## 概述

为 `TriageServiceImpl.findDoctorsForDepartments()` 中的 `doctorFacade.findAvailableDoctorsByDepartment()` 调用注入超时控制，配置键 `consultation.doctor-facade.timeout`（默认 2s）。超时时捕获异常、记录 WARN 日志（含耗时/异常类型/科室 ID），跳过该科室（医生列表不追加），不阻断分诊主流程。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/consultation/src/main/java/com/aimedical/modules/consultation/service/impl/TriageServiceImpl.java` | 修改 | 注入 `doctorFacadeTimeout` 配置；在 `findDoctorsForDepartments` 中用 `CompletableFuture.supplyAsync(() -> ...).get(timeout, SECONDS)` 包裹调用 |

## 类型定义

### `TriageServiceImpl`（已有类，修改）
**形态**：class
**包路径**：`com.aimedical.modules.consultation.service.impl`
**职责**：分诊服务实现；为 DoctorFacade 调用增加超时保护

**新增字段**：
```
private final long doctorFacadeTimeout;
```

**修改的构造方式**：
```
public TriageServiceImpl(
    AiService aiService,
    TriageRuleEngine triageRuleEngine,
    DepartmentFallbackProvider fallbackProvider,
    DoctorFacade doctorFacade,
    DialogueSessionManager sessionManager,
    TriageRecordRepository triageRecordRepository,
    TriageConverter triageConverter,
    ObjectMapper objectMapper,
    PlatformTransactionManager transactionManager,
    @Value("${ai.timeout.triage:8}") long aiTimeout,
    @Value("${consultation.doctor-facade.timeout:2}") long doctorFacadeTimeout  // 新增参数
)
```

新增赋值：`this.doctorFacadeTimeout = doctorFacadeTimeout;`

**修改的方法**：`findDoctorsForDepartments(List<RecommendedDepartment> departments) -> List<RecommendedDoctor>`

**修改详情（方法第 213-236 行）**：
- 保持现有 `for (RecommendedDepartment dept : departments)` 循环、`start = System.currentTimeMillis()`、`catch (Exception e)`、`WARN 日志` 结构不变
- 将第 221 行直接调用 `doctorFacade.findAvailableDoctorsByDepartment(dept.getDepartmentId())` 替换为：
  ```
  CompletableFuture<List<AvailableDoctor>> future = CompletableFuture.supplyAsync(
      () -> doctorFacade.findAvailableDoctorsByDepartment(dept.getDepartmentId()));
  List<AvailableDoctor> available = future.get(doctorFacadeTimeout, TimeUnit.SECONDS);
  ```
- `supplyAsync` 使用 `ForkJoinPool.commonPool()`，未经定线程池传入。理由：
  1. `MedicalRecordServiceImpl:132-134` 已使用完全相同的模式处理同类跨模块 facade 调用（`visitFacade.findVisitIdByEncounterId` + `supplyAsync().get(timeout)`），此为项目既成模式
  2. 循环体按科室顺序依次执行（每次迭代只有一次 supplyAsync），单请求最多占用 1 个 commonPool 线程
  3. `doctorFacadeTimeout` 秒硬超时保证线程不会无限阻塞
  4. commonPool 并行度 = `Runtime.availableProcessors() - 1`（通常 ≥7），足以应对预期并发量
  5. 若未来实测发现 commonPool 争用，可提取为专用 ExecutorService 参数，目前不引入额外复杂度
- 现有 `catch (Exception e)` 捕获 `TimeoutException`、`ExecutionException`、`InterruptedException`，不需新增 catch 类型；其中 `InterruptedException` 在 catch 块中判断并恢复中断标志：`if (e instanceof InterruptedException) Thread.currentThread().interrupt();`
- 不需要新 import（`CompletableFuture`、`TimeUnit` 已在第 38/40 行导入）

**超时科室的语义**：当某科室的 DoctorFacade 调用超时或失败时，该科室的医生不被追加到 `result` 列表（等同于该科室无可用医生），其他科室已成功获取的医生不受影响。这不等于将 `TriageResponse.doctors` 整体置为空列表——后者会清空其他科室的结果，不符合"跳过故障科室、不影响正常科室"的降级意图。

## 错误处理

采用异常捕获 + 跳过降级模式：
- `TimeoutException`：被现有 `catch (Exception e)` 捕获，WARN 日志已输出 `elapsedMs`（含超时耗时）、`e.getClass().getName()`（含 `TimeoutException`）、`dept.getDepartmentId()`
- `ExecutionException`：被同一 catch 捕获，WARN 日志同上述格式
- `InterruptedException`：被同一 catch 捕获，并在 catch 中执行 `if (e instanceof InterruptedException) Thread.currentThread().interrupt();` 恢复中断标志，与同文件第 125-127 行 `aiTimeout` 的中断处理策略一致
- 异常科室不追加到 `result` 列表，等同于该科室无医生返回

## 行为契约

- **前置条件**：无变化（`departments` 为 null/empty 时直接返回空列表）
- **后置条件**：返回值始终 ≤5 个元素，按 `availableSlotCount` 降序排列
- **超时行为**：任一科室的 DoctorFacade 调用超过 `doctorFacadeTimeout` 秒时，该科室医生不参与推荐，其他科室不受影响
- **配置生效**：`application.yml:27-29` 的 `consultation.doctor-facade.timeout: 2` 通过 `@Value` 注入生效；可通过环境变量或配置中心覆盖

## 依赖关系

- **已有依赖**：`DoctorFacade`（`com.aimedical.modules.commonmodule.doctor`）、`CompletableFuture`（JDK）、`TimeUnit`（JDK）
- **新增依赖**：无
- **暴露接口**：无变化（`TriageService` 接口不变）
- **配置依赖**：`consultation.doctor-facade.timeout`（默认值 2，单位秒），已在 `application.yml:27-29` 定义

## 修订说明（v1 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [一般] supplyAsync 未指定线程池，存在 commonPool 线程耗尽风险 | 在设计文档中补充定量论证：该模式已存在于 `MedicalRecordServiceImpl` 同类场景；循环体顺序执行，单请求最多 1 个 commonPool 线程；硬超时兜底；并行度足够。不在本轮引入专用 ExecutorService，并注明未来可按需提取。 |
| [一般] InterruptedException 被 catch(Exception) 捕获后未恢复中断标志，与 aiTimeout 处理不一致 | 在 catch 块中补充 `if (e instanceof InterruptedException) Thread.currentThread().interrupt();`，与第 125-127 行保持一致。 |
| [轻微] "置为空列表" vs "跳过该科室"语义差异 | 在设计文档中明确此语义选择：超时时仅跳过故障科室（不追加到 result），而非清空整个 result 列表。与任务预期"跳过该科室（医生列表不追加）"一致。 |
