# 设计审查报告（v1 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** BaseEntity 同时使用了两种机制管理 `createdAt`/`updatedAt`：类型签名中声明了 `@CreatedDate`、`@LastModifiedDate` 和 `@EntityListeners(AuditingEntityListener.class)`（JPA 审计监听器自动赋值），而公开接口中又声明了 `@PrePersist`/`@PreUpdate` 生命周期回调方法手动设置这两个字段。这两种机制是重复且相互矛盾的。AuditingEntityListener 已在 persist/update 时自动注入时间戳，再通过 `@PrePersist`/`@PreUpdate` 覆盖同一字段是无意义的冗余，且会造成实现者困惑（到底应该实现哪个？）。必须选择其中一种机制并删除另一种。

## 修改要求

1. **BaseEntity 审计机制二择一**：删除 `@PrePersist`/`@PreUpdate` 方法声明（保留 `@CreatedDate`/`@LastModifiedDate` + `AuditingEntityListener` 的注解式审计），或者删除 `@CreatedDate`/`@LastModifiedDate`/`AuditingEntityListener`（保留 `@PrePersist`/`@PreUpdate` 回调式审计）。推荐删除 `@PrePersist`/`@PreUpdate`，因为注解式审计更符合 Spring 惯用做法且更简洁。
