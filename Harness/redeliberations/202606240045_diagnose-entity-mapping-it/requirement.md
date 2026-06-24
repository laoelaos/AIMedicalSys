# 问题定位需求

定位以下问题并给出修复方案：

1. **EntityMappingIT 缺少集成测试**：完全缺少 Phase 1 包A 核心实体 User/Role/Post 的集成测试
2. **password 无 NOT NULL 约束**：password 字段在实体或 DDL 中缺少 NOT NULL 约束
3. **DDL 与 BaseEntity 的 deleted 列 NOT NULL 不一致**：DDL 中 deleted 列的 NOT NULL 约束与 BaseEntity 定义不一致
4. **enabled/visible 布尔字段跨实体缺少默认值**：不同实体中的 enabled/visible 布尔字段缺少默认值
