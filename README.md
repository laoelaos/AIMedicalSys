# AIMedicalSys

`AIMedicalSys` 是一个面向医疗业务场景的前后端一体化工程仓库。目前仓库处于 `Phase 0` 骨架阶段，重点是搭建可运行、可协作、可演进的基础工程，而不是交付完整业务功能。

当前仓库已经包含：

- Spring Boot 3 多模块后端骨架
- Vue 3 + Vite 多端前端骨架
- 患者端、医生端、管理员端三个前端 workspace
- `/api/ping` 最小可用后端健康检查接口
- CI、协作规范和本地快速启动文档

## 项目目标

Phase 0 的目标是先把工程底座搭好，支持多人并行推进后续业务开发：

- 统一后端模块边界与依赖方向
- 统一前端 workspace 组织方式
- 提供最小可运行的启动链路
- 固化仓库级协作规范与验证入口

如果你是第一次接触本仓库，可以先看：

- `Docs/QUICKSTART.md`
- `CONTRIBUTING.md`

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2.5
- Maven 多模块构建
- H2 内存数据库
- springdoc-openapi

### 前端

- Node.js 18+，推荐 20 LTS
- npm workspaces
- Vue 3
- Vite 5
- TypeScript 5

### 工程与协作

- GitHub Actions CI
- Apache License 2.0

## 仓库结构

```text
AIMedicalSys/
├── AIMedical/
│   ├── backend/                # Spring Boot 多模块后端
│   │   ├── common/
│   │   ├── modules/
│   │   │   ├── ai/
│   │   │   ├── common-module/
│   │   │   ├── patient/
│   │   │   ├── doctor/
│   │   │   └── admin/
│   │   ├── application/        # 应用启动模块
│   │   └── integration/        # 集成测试模块
│   └── frontend/               # Vue 多 workspace 前端
│       ├── apps/
│       │   ├── patient/
│       │   ├── doctor/
│       │   └── admin/
│       └── packages/
│           ├── shared/
│           └── ui-core/
├── Docs/                       # 设计、路线图、快速启动等文档
├── .github/workflows/          # CI 配置
├── CONTRIBUTING.md             # 协作规范
└── README.md
```

## 当前已落地能力

- 后端可通过 Maven 完整构建
- 后端应用可本地启动，默认监听 `http://localhost:8080`
- 提供健康检查接口 `GET /api/ping`
- 前端支持患者端、医生端、管理员端独立或同时启动
- CI 会分别执行前端构建和后端校验

当前阶段仍以骨架为主，不应将 README 理解为完整产品说明。

## 环境要求

开始前请确认本机已安装：

- JDK 17+
- Node.js 18+，推荐 20 LTS
- npm 9+

建议先执行：

```powershell
java -version
node -v
npm -v
```

## 快速开始

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
- Profile：`phase0,dev`
- 数据库：H2 内存数据库

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
```

预期返回：

```json
{"code":"SUCCESS","data":"pong"}
```

### 前端验证

浏览器访问：

- 患者端：`http://localhost:5173`
- 医生端：`http://localhost:5174`
- 管理员端：`http://localhost:5175`

预期结果：

- 页面可以正常打开
- 页面显示系统名称“智慧云脑诊疗平台”
- 页面显示对应终端的占位内容

## 常用命令

### 后端

```powershell
mvn install -DskipTests
mvn -f application/pom.xml spring-boot:run
mvn verify
mvn dependency:analyze
```

### 前端

```powershell
npm ci
npm run dev
npm run dev:patient
npm run dev:doctor
npm run dev:admin
npm run build:all
```

## CI 校验

仓库当前 CI 位于 `.github/workflows/ci.yml`，包含两类检查：

- 前端：`npm ci` + `npm run build:all`
- 后端：`mvn -B verify` + `mvn -B dependency:analyze`

CI 会在 `main`、`develop` 分支的 `push` 和 `pull_request` 上触发。

## 协作规范

请在提交前阅读 `CONTRIBUTING.md`。当前已固化的最小协作规则包括：

- 使用主题明确的功能分支
- 使用统一的 commit message 格式
- 通过 Pull Request 合入改动
- 提交前完成最小本地验证

如果变更涉及 Phase 0 骨架边界，请同时对照 `Docs/04_ood_phase0.md` 检查。

## 相关文档

- `Docs/QUICKSTART.md`：本地启动和环境检查
- `CONTRIBUTING.md`：分支、提交、PR、Review 规范
- `Docs/04_ood_phase0.md`：Phase 0 OOD 冻结边界
- `Docs/03_roadmap.md`：阶段路线图
- `Docs/02_tech.md`：技术基线说明

## 许可证

本项目使用 `Apache License 2.0`，详见 `LICENSE`。
