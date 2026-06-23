# Project Quickstart

## 1. 文档目标

本文件用于帮助新人或协作者完成本地环境准备、启动当前仓库中的前后端工程，并按照当前阶段验证最小可运行链路。

本文件不是只服务于 `Phase 0` 的一次性说明，而是应随着 `Docs/03_roadmap.md` 中各阶段能力演进持续更新。

使用原则如下：

- 启动命令优先保持稳定，尽量适用于多个阶段
- 阶段差异通过“当前 phase 关注点 / 验证项”补充说明
- 若启动方式、默认 profile、端口或验证接口发生变化，应优先更新本文档

## 2. 前置条件检查

开始前请确认本机已安装以下环境：

| 运行时 | 最低版本 | 用途 |
|------|---------|------|
| JDK | 17+ | 运行 Spring Boot 3.2.5 |
| Node.js | 18+ 或 20 LTS | 运行 Vue 3 + Vite |
| npm | 9+ | 管理 frontend workspaces |

建议检查命令：

```powershell
java -version
node -v
npm -v
```

## 3. 仓库结构说明

本仓库采用“项目根 + 源码子目录”布局：

- 仓库根：`AIMedicalSys/`
- 后端源码根：`AIMedicalSys/AIMedical/backend/`
- 前端源码根：`AIMedicalSys/AIMedical/frontend/`
- 设计文档目录：`AIMedicalSys/Docs/`

后续所有命令都基于该目录结构执行。

## 4. 标准启动流程

### 4.1 克隆仓库

```powershell
git clone <repo-url>
```

将 `<repo-url>` 替换为实际仓库地址。

### 4.2 构建后端

进入后端目录：

```powershell
cd AIMedicalSys\AIMedical\backend
```

执行基础构建：

```powershell
mvn install -DskipTests
```

说明：

- 该命令会按 Maven reactor 顺序构建并安装多模块产物到本地仓库
- `-DskipTests` 适合首次环境准备；进入功能开发或联调后，应按阶段要求补跑测试

### 4.3 安装前端依赖

进入前端目录：

```powershell
cd ..\frontend
```

安装 workspaces 依赖：

```powershell
npm ci
```

### 4.4 启动后端

请保持一个终端窗口停留在 `AIMedicalSys\AIMedical\backend`，执行：

```powershell
mvn -f application/pom.xml spring-boot:run
```

说明：

- 默认启动方式以 `application` 模块为准
- 当前启用的 `spring.profiles.active`、数据库类型、外部依赖和本地联调前提，应以 `application.yml` 及其对应环境配置为准
- 若 roadmap 对某阶段引入了新的基础设施依赖，应在本文档同步补充启动前置条件
- 若本地未改默认端口，后端通常监听 `http://localhost:8080`

### 4.5 启动前端应用

再打开一个终端，进入前端目录：

```powershell
cd AIMedicalSys\AIMedical\frontend
```

优先使用根目录已提供的 workspace 启动脚本。

同时启动三端：

```powershell
npm run dev
```

按需启动任一端：

患者端：

```powershell
npm run dev:patient
```

医生端：

```powershell
npm run dev:doctor
```

管理员端：

```powershell
npm run dev:admin
```

若需直接使用 workspace 命令，也可执行：

患者端：

```powershell
npm run dev --workspace @aimedical/app-patient
```

医生端：

```powershell
npm run dev --workspace @aimedical/app-doctor
```

管理员端：

```powershell
npm run dev --workspace @aimedical/app-admin
```

若对应 workspace 脚本尚未补齐，可进入对应 `apps/*` 目录后执行 `npm run dev`。

## 5. 按阶段验证

### 5.1 通用验证原则

完成启动后，请不要只验证“进程能跑起来”，还应结合当前阶段的目标检查最小业务闭环是否可用。

建议按以下顺序验证：

- 先验证后端健康检查或最小可达接口
- 再验证前端页面是否可访问
- 最后验证当前 phase 新增的核心链路

当前阶段范围与目标，以 `Docs/03_roadmap.md` 为准。

### 5.2 基础可运行验证

在任意终端执行：

```powershell
curl http://localhost:8080/api/ping
```

若当前实现仍保留该接口，预期返回类似：

```json
{"code":"SUCCESS","data":"pong"}
```

说明：

- 若返回结构中包含 `message`、`success` 等附加字段，只要能成功返回健康检查结果且符合统一响应契约即可
- 若后续阶段将健康检查入口改为 `/actuator/health` 或其他标准端点，应同步更新本文档

### 5.3 前端可访问验证

浏览器访问对应端口：

- 患者端：`http://localhost:5173`
- 医生端：`http://localhost:5174`
- 管理员端：`http://localhost:5175`

预期结果：

- 页面可打开
- 页面已加载当前终端对应应用
- 页面内容与当前阶段实现一致，不要求永远停留在“占位页”状态

### 5.4 当前 phase 核心验证建议

请按 roadmap 中“本阶段做完后新增什么能力”与“验收标准”执行验证。

建议口径：

- `Phase 0`：验证骨架启动、占位首页、基础健康检查
- `Phase 1`：验证登录、权限、菜单与基础资料链路
- `Phase 2` 及以后：在基础启动成功后，验证该阶段首次落地的业务或 AI 能力链路

如果当前仓库状态已经进入更高阶段，应以更高阶段的验证项替换仅针对骨架的检查，不要继续把“看到占位页”当作唯一验收标准。

## 6. 常见问题排查

### 6.1 `mvn install -DskipTests` 失败

检查项：

- 是否安装 JDK 17+
- `JAVA_HOME` 是否指向正确 JDK
- 是否在 `AIMedicalSys\AIMedical\backend` 目录执行命令
- 本地 Maven 是否能正常下载依赖

### 6.2 后端启动失败

检查项：

- 端口 `8080` 是否被占用
- `application.yml` 及环境配置是否与当前阶段要求一致
- 当前阶段新增的数据库、中间件或外部服务依赖是否已准备完成
- 启动日志中是否存在 profile、数据源或 Bean 装配错误

### 6.3 健康检查接口失败

检查项：

- 后端进程是否已成功启动
- 当前阶段实际暴露的健康检查接口是否仍为 `/api/ping`
- 本地防火墙或端口占用是否影响访问

### 6.4 `npm ci` 失败

检查项：

- Node.js 和 npm 版本是否满足要求
- 是否在 `AIMedicalSys\AIMedical\frontend` 目录执行
- 网络环境是否能正常下载 npm 依赖

### 6.5 前端页面打不开

检查项：

- 对应 workspace 是否存在 `dev` 脚本
- Vite dev server 是否成功启动
- 端口 `5173`、`5174`、`5175` 是否被占用
- 当前阶段是否引入了额外的环境变量或后端联调前提

## 7. 文档维护要求

当以下信息发生变化时，应同步更新本文档：

- 默认启动命令
- 默认 profile 或配置文件约定
- 后端健康检查接口
- 前端端口或 workspace 脚本
- 当前阶段的最低验证口径

若某一阶段新增了必须依赖的中间件、第三方服务或本地配置步骤，也应直接写入本文档，而不是仅散落在阶段设计文档中。

## 8. 参考文档

- `Docs/03_roadmap.md`
- `Docs/04_ood_phase0.md`
- `Docs/02_tech.md`
- `CONTRIBUTING.md`
