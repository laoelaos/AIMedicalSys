# 计划审查报告（v14 r2）

## 审查结果
REJECTED

## 发现

### **[一般]** plan.md R14 NEW 节的方法描述与 task_v14.md 已修订方案存在严重不一致

- **问题**：plan.md R14 NEW 写着"扩展 @EntityGraph attributePaths 增加 'posts.functions'"和"将 findById 替换为 findWithDetailsById"，暗示修改现有 `findWithDetailsById` 方法的 EntityGraph。同时声称"与 T9/T8/T11 无交叉依赖"。
- **为什么是问题**：这些描述均与 task_v14.md 的已修订方案冲突。task_v14.md 正确采用了方案 A——新增专用方法 `findWithDetailsForMenuById`（带有扩展 EntityGraph），保持现有 `findWithDetailsById` 不变，以避免影响 `JwtAuthenticationFilter.java:85` 调用者。"无交叉依赖"声明在修订中已被纠正。若实施者仅阅读 plan.md 而忽略 task_v14.md，将修改错误的（现有）方法，破坏认证流程。
- **期望的修正方向**：将 plan.md R14 NEW 节更新为与 task_v14.md 一致：
  1. 任务描述改为"(1) UserRepository.java 新增 findWithDetailsForMenuById 方法，@EntityGraph(attributePaths = {"roles", "posts", "posts.functions"})；(2) MenuServiceImpl.java:44 findById → findWithDetailsForMenuById"
  2. 选择理由中删除"无交叉依赖"声明，补充交叉依赖说明（findWithDetailsById 被 JwtAuthenticationFilter 调用）

## 修改要求

更新 plan.md R14 NEW 节使其与 task_v14.md 修订版一致，消除两种冲突描述。
