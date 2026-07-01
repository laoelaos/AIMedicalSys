# 实现报告（v5）

## 概述

在 common-module-api 的 `store` 包下新增三个 Store 接口（SessionStore、SuggestionStore、DraftContextStore）及一个 ConcurrentHashMapStore 实现类，提供线程安全的内存存储抽象层。

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | common-module-api/.../store/SessionStore.java | 泛型键值存储接口 |
| 新建 | common-module-api/.../store/SuggestionStore.java | AI 建议结果存储接口，扩展 SessionStore，增加 compute 方法 |
| 新建 | common-module-api/.../store/DraftContextStore.java | 处方草稿上下文存储接口，扩展 SessionStore |
| 新建 | common-module-api/.../store/impl/ConcurrentHashMapStore.java | 同时实现 SuggestionStore 和 DraftContextStore 的线程安全内存存储 |

## 编译验证

通过 (`mvn compile -f common-module-api/pom.xml`，零错误零警告)

## 设计偏差说明

无偏差
