## 代码实现

### 1
启动审议式实现流程，完成：
@Docs\05_ood_phase1_B.md 
项目：@AIMedical，其他文档（需求、路线、技术栈、其他ood）在: @Docs 
在启动 Plan Agent 时候需要提示其需要在 plan.md 开头添加详细的实施路线表格，之后每次进展打勾确认

启动流程时不要读取文件，直接启动流程
### 2
启动审议式实现流程，完成：
修复 @Docs\Diagnosis\impl\04_phase1B_report.md 里面提到的问题
项目：@AIMedical，OOD文档：@Docs\05_ood_phase1_B.md
在启动 Plan Agent 时候需要提示其需要在 plan.md 开头添加详细的实施路线表格，之后每次进展打勾确认

启动流程时不要读取文件，直接启动流程

## 代码审议

### 1
启动审议式代码审查流程，从当前分支到develop分支：
依据 Docs\05_ood_phase1_B.md 该OOD设计。
自行决定需要启动多少轮，每次并行启动3个agent，每个agent执行一轮，直到完成。
### 2
将所有的 review_v{*}.md 文件中的严重和一般问题整理到 todo.md
你需要保留题目、位置、描述，忽略其中的建议。测试相关的问题需要单列

## 问题定位

启动再审议框架，执行问题定位流程，不要进入initial_artifact模式，定位：

@Harness\reviews\202606270204_fix_phase1B_code_review\todo.md
其中的问题，是真实存在还是误报？
问题的原因是OOD文档存在着矛盾、偏差、不完善或是错误？或者是实现编码问题？
OOD文档：@Docs\05_ood_phase1_B.md
该OOD设计所实现的项目：@AIMedical
你只需要输出问题定位的结果以及修改的流程和建议，修改流程确保每次任务不多不少。

在启动流程的过程中，不要尝试阅读文档，直接启动流程。