# 再审议判定报告（v11）

## 判定结果

RETRY

## 判定理由

诊断报告（v3修订版）共识别7个问题，其中问题5（callerRole提取不一致）、问题6（PatientInfo类型未定义）、问题7（TimeoutException死代码）严重程度为"重要"，属于事实错误/关键遗漏类严重问题；问题2/3/4/8/9为一般问题。质询报告结论为LOCATED，确认了诊断报告的审查结论（实际轮次3 < 最大轮次12，提前终止且审查被确认）。根据判定标准，审查报告包含严重及以上等级问题，需重新运行组件A。

## 需要解决的问题（仅 RETRY 时存在）

- **问题描述**：`AiOrchestrator.handle()` catch块中`callerRole`提取逻辑与`AbstractCapabilityExecutor.extractCallerRole()`不一致，导致`AiCallRecord.callerRole`字段值格式不统一
- **所在位置**：§4.1（行2687-2689）vs §3.1 `extractCallerRole()`定义（行1202-1225）
- **严重程度**：严重
- **改进建议**：将`extractCallerRole()`抽取为`RequestContextUtils`中的公用静态方法

- **问题描述**：`PatientInfo`类型未定义且`PrescriptionLocalRuleFallback`引用了不存在的字段路径
- **所在位置**：§3.11.2（行2548）DTO扩展字段表；§3.7（行2180-2181）最小安全规则表
- **严重程度**：严重
- **改进建议**：补充`PatientInfo`类的完整字段表，修正§3.7中`request.patientAge`和`request.pregnancyStatus`的引用路径

- **问题描述**：`doExecuteInternal()`中`catch (TimeoutException)`因`.join()`包装成为死代码，超时被错误归类为基础设施异常
- **所在位置**：§4.1 `doExecuteInternal()`，行2864和行2915
- **严重程度**：严重
- **改进建议**：将`.join()`替换为`.get()`并在`catch (ExecutionException)`中拆解原始异常，或改用`Future.get(timeout, unit)`

- **问题描述**：`ExperimentGroup`类图节点缺失（图-文不一致）
- **所在位置**：§2.3 类图
- **严重程度**：一般
- **改进建议**：在§2.3类图中新增`ExperimentGroup`类节点及与`Experiment`的关联线

- **问题描述**：`AiCallLogStats`类图节点缺失（图-文不一致）
- **所在位置**：§2.3 类图
- **严重程度**：一般
- **改进建议**：在§2.3类图中新增`AiCallLogStats`类节点

- **问题描述**：`StructuredOutputParser.parse()`独立超时未体现于§4.1伪代码
- **所在位置**：§3.2（文本描述）vs §4.1（伪代码行2901）
- **严重程度**：一般
- **改进建议**：在§4.1行2901处补充超时包裹逻辑

- **问题描述**：`PrescriptionAssistRequest`与`PrescriptionCheckRequest`的患者数据建模方式不一致
- **所在位置**：§3.11.2（行2548）vs §3.11.4（行2576）
- **严重程度**：一般
- **改进建议**：统一患者数据建模方式，两个DTO均使用`PatientInfo`内嵌对象或明确说明差异是有意为之

- **问题描述**：`AbstractCapabilityExecutor`构造器中`ObjectMapper`的来源未定义
- **所在位置**：§3.1构造器（行1266-1284）和`execute()`伪代码（行2714）
- **严重程度**：一般
- **改进建议**：在构造器中新增`ObjectMapper`参数，或在注释中说明获取方式
