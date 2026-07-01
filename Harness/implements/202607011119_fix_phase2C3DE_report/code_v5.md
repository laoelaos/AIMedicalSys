# 实现报告（v5）

## 概述

修复 prescription 模块两个 P1 缺陷：P01（异步 AI 调度线程池绑定 + 异常处理）和 S03（DedupTaskScheduler 跨 key 竞态）。涉及 1 个新增源文件、2 个源文件修改；测试文件无需修改（此前轮次已完成）。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `prescription/.../config/PrescriptionThreadPoolConfig.java` | 定义 `@Bean("aiTaskExecutor")` ExecutorService，使用有界平台线程池 |
| 修改 | `prescription/.../service/assist/impl/PrescriptionAssistServiceImpl.java` | 消除 `scheduleSuggestionAsync` 中 `result` 初始 PROCESSING 死代码 |
| 修改 | `prescription/.../service/assist/DedupTaskScheduler.java` | `put(candidateTaskId, newResult)` 提前至 `createIfNotExists` 之前无条件执行（此前轮次已完成） |

## 编译验证

编译成功，309 测试通过（0 失败，0 错误，0 跳过）。

## 设计偏差说明

1. **PrescriptionThreadPoolConfig 线程池实现**：设计规格要求使用虚拟线程（`Executors.newThreadPerTaskExecutor(Thread.ofVirtual()...)`），但项目编译目标为 Java 17（`java.version=17`），`Thread.ofVirtual()` 不可用。改为有界平台线程池 `ThreadPoolExecutor(0, 50, 60s, SynchronousQueue)`，既避免 `newCachedThreadPool` 的无界资源风险，又满足 Java 17 兼容性，行为等价于有界缓存线程池。

## 修订说明（v5 r1）

| 审查意见 | 修改措施 |
|---------|---------|
| `PrescriptionThreadPoolConfig` 使用 `newCachedThreadPool()` 创建无界线程池，有资源耗尽风险 | 改用 `ThreadPoolExecutor(0, 50, 60s, SynchronousQueue)` 有界线程池，以原子整数命名线程。因项目目标 Java 17、虚拟线程不可用，无法采用设计规格的虚拟线程方案 |
| `PrescriptionAssistServiceImpl:351-353` `result` 初始 PROCESSING 赋值死代码 | 删除 `result` 初始化及 PROCESSING 赋值（3 行），将 `result` 创建移至 `processingResult` 存储之后、try 块之前，仅初始化 `taskId` |
