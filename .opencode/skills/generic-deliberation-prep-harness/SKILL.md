---
name: generic-deliberation-prep-harness
description: "审议式流程的前置准备。任何审议式流程执行前必须先调用本技能。"
---

## 调用参数

- **`instruction`**（必填）— 用户指令原文，逐字记录

## 执行流程

### 1. 生成时间戳

执行 `date +%Y%m%d%H%M` 获取时间戳 `{ts}` 。后续审议式流程如果需要时间戳，直接复用该时间戳。

### 2. 写入指令

将 `instruction` 原文写入 `Harness/instructions/{ts}.md`（如目录不存在则创建）

### 3. 后续工作

主Agent应该严格按照用户指令原文，继续加载对应的审议式流程。