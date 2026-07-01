# AIMedicalSys

`AIMedicalSys`（智慧云脑诊疗平台）是一个面向医疗业务场景的前后端一体化工程仓库。当前阶段已跨过 `Phase 0` 骨架期，进入 `Phase 1` 实施与 `Phase 2~3` 并行演进状态。

当前仓库已经包含：

- Spring Boot 3.2.5 多模块后端，按业务域拆分子模块（患者 / 医生 / 管理员 / 挂号 / 处方 / 病历 / 医嘱 / 会诊 / AI / common）
- Vue 3 + Vite + TypeScript 多 workspace 前端，包含患者端、医生端、管理员端以及 `shared` / `ui-core` 共享包
- 统一认证与权限骨架（JWT、用户/角色/岗位/菜单）
- 最小业务闭环：患者注册登录、健康档案基础版（CRUD）、医生/管理员登录与动态菜单
- AI 能力接口契约、Mock 与超时配置；`/api/ping` 与 `/actuator/health` 健康检查端点
- CI、覆盖率门禁、OpenAPI 契约 diff、本地快速启动文档

## 项目目标

按 `Docs/03_roadmap.md` 的阶段划分，本仓库的总目标是构建面向六大业务域、三大终端、13 项 AI 能力的完整诊疗平台。当前阶段的工作重点：

- 持续完善 `Phase 1` 基础设施与认证权限
- 并行推进 `Phase 2` 患者端主流程与 `Phase 3` 医生端诊疗闭环
- 为后续 AI 接入、Phase 4~6 留好演进路径

如果你是第一次接触本仓库，建议按以下顺序阅读：

1. `Docs/QUICKSTART.md`
2. `Docs/02_tech.md` 与 `Docs/03_roadmap.md`
3. `CONTRIBUTING.md` 与 `Docs/04_ood_phase0.md`

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2.5
- Maven 多模块构建（含 JaCoCo 覆盖率门禁）
- Spring Data JPA + Hibernate
- MySQL（默认）与 H2 内存数据库（`dev` profile 自动启用）
- springdoc-openapi（`/v3/api-docs`、`/swagger-ui.html`）
- Spring Security + JWT（jjwt，`access-token` / `refresh-token`）
- Element Plus / Ant Design Vue（前端 UI 组件库基线见 `Docs/02_tech.md`）

### 前端

- Node.js 18+，推荐 20 LTS
- npm workspaces
- Vue 3（Composition API）
- Vite 5
- TypeScript 5
- Vue Router 4 / Pinia
- Element Plus

### 工程与协作

- GitHub Actions CI（含单测、覆盖率、OpenAPI diff、Spring Boot 启动冒烟）
- Apache License 2.0

## 仓库结构

```text
AIMedicalSys/
├── AIMedical/
│   ├── backend/                # Spring Boot 多模块后端
│   │   ├── common/             # 通用响应 / 异常 / 工具
│   │   ├── modules/
│   │   │   ├── ai/             # AI 能力接口契约 + Mock
│   │   │   ├── common-module/  # 跨模块共享基类、权限模型
│   │   │   ├── patient/        # 患者域（含注册登录、健康档案基础版）
│   │   │   ├── doctor/         # 医生域
│   │   │   ├── admin/          # 管理员域
│   │   │   ├── registration/   # 挂号中心
│   │   │   ├── prescription/   # 处方与处方审核
│   │   │   ├── medical-record/ # 病历中心
│   │   │   ├── medical-order/  # 医嘱
│   │   │   └── consultation/   # 会诊 / 病情咨询
│   │   ├── application/        # 应用启动模块 + /api/ping
│   │   └── integration/        # 集成测试 + OpenAPI 契约导出
│   └── frontend/               # Vue 多 workspace 前端
│       ├── apps/
│       │   ├── patient/        # @aimedical/app-patient
│       │   ├── doctor/         # @aimedical/app-doctor
│       │   └── admin/          # @aimedical/app-admin
│       └── packages/
│           ├── shared/         # 跨端共享类型 / API 封装
│           └── ui-core/        # 跨端 UI 组件与样式基线
├── Docs/                       # 设计、路线图、契约、快速启动等文档
├── .github/                    # CI 配置与 PR 模板
├── CONTRIBUTING.md             # 协作规范
└── README.md
```

## 当前已落地能力

- 后端可通过 Maven 完整构建（`mvn verify` 含 JaCoCo 覆盖率门禁）
- 后端应用可本地启动，默认监听 `http://localhost:8080`
  - 提供 `GET /api/ping`、`GET /actuator/health` 健康检查
  - OpenAPI 契约：`http://localhost:8080/v3/api-docs`，Swagger UI：`/swagger-ui.html`
- 统一认证：登录、Token 刷新、当前用户信息；JWT 在请求头 `Authorization: Bearer <token>`
- 权限模型：`用户 / 角色 / 岗位 / 功能 / 菜单`，菜单按角色动态下发
- 患者端：注册、登录、个人中心、健康档案基础字段 + 5 类结构化子字段 CRUD
- 医生端 / 管理员端：登录、动态菜单、Dashboard
- 前端支持三端独立或同时启动；三端均能完成"登录 → 菜单 → 业务页"最小闭环
- CI 会执行前端构建 + 单测 + 覆盖率、后端构建 + 单测 + JaCoCo + 依赖分析 + OpenAPI 契约 diff + Spring Boot 启动冒烟

## 环境要求

开始前请确认本机已安装：

- JDK 17+
- Node.js 18+，推荐 20 LTS
- npm 9+
- 可选：Docker（CI 中 OpenAPI diff 使用 `openapitools/openapi-diff`）

建议先执行：

```powershell
java -version
node -v
npm -v
```

## 快速开始

完整命令与按阶段验证说明见 `Docs/QUICKSTART.md`，下面是默认入口。

### 1. 构建后端

在 `AIMedical/backend` 目录执行：

```powershell
mvn install -DskipTests
```

这会按 Maven reactor 顺序构建并安装多模块产物到本地仓库。

### 2. 安装前端依赖

在 `AIMedical/frontend` 目录执行：

```powershell
npm ci
```

### 3. 启动后端

在 `AIMedical/backend` 目录执行：

```powershell
mvn -f application/pom.xml spring-boot:run
```

默认配置：

- 地址：`http://localhost:8080`
- Profile：`phase1,dev`（`dev` 自动启用 H2 内存数据库与 `spring-devtools`；如需切换至 MySQL，请使用 `phase1` 或 `prod` profile 并配置 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` / `JWT_SECRET`）
- H2 控制台（仅 dev）：`http://localhost:8080/h2-console`，JDBC URL 见 `application-dev.yml`

### 4. 启动前端

在 `AIMedical/frontend` 目录执行：

同时启动三端：

```powershell
npm run dev
```

分别启动单个终端：

```powershell
npm run dev:patient
npm run dev:doctor
npm run dev:admin
```

## 启动后验证

### 后端验证

```powershell
curl http://localhost:8080/api/ping
curl http://localhost:8080/actuator/health
```

`/api/ping` 预期返回：

```json
{"code":"SUCCESS","data":"pong"}
```

`/actuator/health` 预期 `"status":"UP"`。

### 前端验证

浏览器访问：

- 患者端：`http://localhost:5173`
- 医生端：`http://localhost:5174`
- 管理员端：`http://localhost:5175`

最低验证口径（参见 `Docs/QUICKSTART.md` §5.4）：

- 三端可打开登录页
- 已注入的种子账号可登录（账号见各模块初始化 SQL 或 `db/data.sql`）
- 患者端登录后能进入 `/profile` 与 `/health-record`
- 医生端 / 管理员端登录后能进入 Dashboard，菜单按角色下发

## 常用命令

### 后端

```powershell
mvn install -DskipTests
mvn -f application/pom.xml spring-boot:run
mvn verify
mvn dependency:analyze
```

切换至 MySQL（profile 不含 `dev`，需先准备好 `DB_URL/DB_USERNAME/DB_PASSWORD`）：

```powershell
mvn -f application/pom.xml spring-boot:run -Dspring-boot.run.profiles=phase1
```

### 前端

```powershell
npm ci
npm run dev
npm run dev:patient
npm run dev:doctor
npm run dev:admin
npm run build:all
npm run test            # 前端单元测试
npm run test:coverage   # 前端覆盖率
```

## CI 校验

仓库当前 CI 位于 `.github/workflows/ci.yml`，关键门禁如下：

- 前端：`npm ci` + `npm run test:coverage` + `npm run build:all` + 覆盖率报告归档
- 后端：
  - JaCoCo 子模块 guard（确保有源码的子模块都显式启用覆盖率）
  - `mvn -B verify`（含 `jacoco:check`，line ≥ 50% / branch ≥ 40%）
  - `mvn -B dependency:analyze`
  - Spring Boot 启动冒烟（等待 `/actuator/health` 返回 `UP`）并导出 `/v3/api-docs`
  - 与 `Docs/contracts/openapi-baseline.json` 做 OpenAPI breaking-change diff
  - 覆盖率报告与当前 OpenAPI 契约归档

CI 会在 `main`、`develop` 分支的 `push` 与 `pull_request` 上触发。

## 协作规范

请在提交前阅读 `CONTRIBUTING.md`。当前已固化的最小协作规则包括：

- 使用主题明确的功能分支
- 使用统一的 commit message 格式
- 通过 Pull Request 合入改动（PR 模板见 `.github/pull_request_template.md`）
- 提交前完成最小本地验证

设计相关变更请同时对照 `Docs/03_roadmap.md` 与 `Docs/04_ood_phase0.md`，避免越界引入后续阶段实现细节。

## 相关文档

- `Docs/QUICKSTART.md`：本地启动与环境检查
- `CONTRIBUTING.md`：分支、提交、PR、Review 规范
- `Docs/02_tech.md`：技术基线说明
- `Docs/03_roadmap.md`：阶段路线图
- `Docs/04_ood_phase0.md`：Phase 0 OOD 冻结边界
- `Docs/contracts/openapi-baseline.json`：OpenAPI 契约基线

## 许可证

本项目使用 `Apache License 2.0`，详见 `LICENSE`。
