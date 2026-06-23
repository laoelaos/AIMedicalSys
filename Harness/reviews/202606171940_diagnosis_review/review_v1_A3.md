# R3: 权限模块 (common-module-api + common-module-impl) 实现与 OOD 设计一致性审查

审查时间：2026-06-17

### 审查范围

- `common-module-api/src/main/java/.../api/UserType.java`
- `common-module-api/pom.xml`
- `common-module-impl/src/main/java/.../permission/User.java`
- `common-module-impl/src/main/java/.../permission/Role.java`
- `common-module-impl/src/main/java/.../permission/Post.java`
- `common-module-impl/src/main/java/.../permission/Function.java`
- `common-module-impl/src/main/java/.../permission/UserRepository.java`
- `common-module-impl/src/main/java/.../dict/.gitkeep`
- `common-module-impl/src/main/java/.../config/`（应存在目录）
- `common-module-impl/pom.xml`
- 各实体和 Repository 的测试文件

### 发现

#### [一般] config/ 目录缺失

- **位置**：`common-module-impl/src/main/java/com/aimedical/modules/commonmodule/config/`
- **描述**：OOD §2.3 包命名规范明确要求 common-module-impl 包含 `config/` 子包（业务级配置目录），且审查范围第 10 项明确将该目录列为审查对象。该目录当前不存在。
- **建议**：创建 `com.aimedical.modules.commonmodule.config` 包目录，并放置 `.gitkeep` 占位文件以保持目录结构完整性。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 0 |
| 一般 | 1 |
| 轻微 | 0 |

### 总评

权限模块实体和 Repository 骨架代码质量良好，与 OOD §3.3 JPA 关系映射约定高度一致：

- 所有实体位于正确包路径 `com.aimedical.modules.commonmodule.permission`（§2.3）
- JPA 映射完全遵循 §3.3 约定：FetchType.LAZY 统一使用、无 cascade 声明、JoinTable 命名 `user_role`/`user_post`/`post_function` 正确、owning side 和 mappedBy 方向正确
- User 实体正确跨子模块引用 common-module-api 的 UserType 枚举（`import com.aimedical.modules.commonmodule.api.UserType`）
- common-module-api/pom.xml 仅依赖 common，无业务实现依赖（§2.2）
- common-module-impl/pom.xml 依赖 common-module-api，符合依赖方向（§2.2）
- 测试覆盖全面：所有 4 个实体、UserRepository、UserType 均有独立的测试类
- common-module-impl 仅含实体和 Repository 骨架，无门面接口/跨模块 Service（Phase 0 约束）

唯一缺陷是 `config/` 目录未创建，建议补充。
