# 再审议判定报告（v8）

## 判定结果

RETRY

## 判定理由

组件B诊断报告共识别10个问题，其中严重等级4个（P1事实错误、P2逻辑矛盾、P3深度不足、P4完整性缺口），一般等级4个（P5事实错误、P6深度不足、P7完整性缺口、P8完整性缺口），轻微等级2个（P9概念歧义、P10边界条件遗漏）。质询报告确认全部维度通过（LOCATED），组件B内部循环以实际1轮提前终止（最大12轮）。根据判定标准，审查报告包含严重或一般等级问题，故判定为 RETRY。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：DeadLetterCompensationService 补偿路径缺少 departmentName，RegistrationEvent 不含 departmentName 字段导致 selectDepartment 调用必然失败
- **所在位置**：§2.2 line 320；§1.3 line 144；§3.1 line 408
- **严重程度**：严重
- **改进建议**：二选一：(a) RegistrationEvent 补充 departmentName 字段；(b) TriageService.selectDepartment 增加仅基于 departmentId 的重载版本

- **问题描述**：AI 输入校验错误码（_AI_INPUT_INVALID 类）已定义但无生产者，全文中无任何触发位置或校验规则定义
- **所在位置**：§5.1 lines 1261-1268；§2.3 lines 330-357
- **严重程度**：严重
- **改进建议**：二选一：(a) AiService 方法契约中补充输入校验职责归属；(b) 标注为"Phase 4 预留"并从当前错误码表移除

- **问题描述**：核心业务 Service 接口（TriageService、PrescriptionAuditService、MedicalRecordService、PrescriptionAssistService）仅以自然语言描述，缺少显式方法签名
- **所在位置**：§3.1 lines 382-412；§3.2 lines 480-486；§3.3 lines 596-606；§3.4 lines 620-633
- **严重程度**：严重
- **改进建议**：为四个业务 Service 接口补充显式方法签名，参考 §2.3 AiService 格式

- **问题描述**：patientId 降级查询"最近分诊记录"缺少排序选择标准，triageTime 相同时无法确定"最近"
- **所在位置**：§1.1a line 22；§2.2 line 304；§3.1 lines 474-476；§2.1 line 172
- **严重程度**：严重
- **改进建议**：(a) 补充 findTopByPatientIdOrderByTriageTimeDesc 查询方法定义；(b) 显式说明排序依据；(c) patientId 补充索引说明

- **问题描述**：RegistrationEvent 缺少 departmentName 导致正常消费路径下 TriageRecord.finalDepartmentName 无法赋值
- **所在位置**：§1.3 line 144；§2.2 lines 302-304
- **严重程度**：一般
- **改进建议**：RegistrationEvent 补充 departmentName 字段，由 registration 模块在发布前通过 departmentId 查询填充

- **问题描述**：@Retryable 未定义触发重试的异常类型，不可治愈异常（如 IllegalArgumentException）也会触发重试
- **所在位置**：§2.2 line 306
- **严重程度**：一般
- **改进建议**：补充异常过滤器，仅对 DataAccessException、TimeoutException 等可治愈异常重试

- **问题描述**：TriageRecord.sessionId 缺少唯一约束定义，并发下 delete+insert 模式可能破坏"一对一"语义
- **所在位置**：§3.1 lines 474-476
- **严重程度**：一般
- **改进建议**：增加 @Column(unique = true) 或 @Table uniqueConstraints，明确 update 模式为推荐实现

- **问题描述**：多处独立使用 ScheduledExecutorService 管理定时任务，缺乏统一线程池管理和优雅关闭机制
- **所在位置**：§3.4 line 659；§3.4 line 687；§6.1 line 1301；§2.2 line 320
- **严重程度**：一般
- **改进建议**：引入统一 ScheduledTaskRegistry 或 Spring @Scheduled，由框架统一接管线程池生命周期
