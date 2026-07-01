# Project Quickstart

## 1. 文档目标

本文件用于帮助新人或协作者完成本地环境准备、启动当前仓库中的前后端工程，并按照当前阶段验证最小可运行链路。

本文件不是只服务于某一个阶段的一次性说明，而是应随着 `Docs/03_roadmap.md` 中各阶段能力演进持续更新。

使用原则如下：

- 启动命令优先保持稳定，尽量适用于多个阶段
- 阶段差异通过 "当前 phase 关注点 / 验证项" 补充说明
- 若启动方式、默认 profile、端口、数据库或验证接口发生变化，应优先更新本文档

## 2. 前置条件检查

开始前请确认本机已安装以下环境：

| 运行时 | 最低版本 | 用途 |
|------|---------|------|
| JDK | 17+ | 运行 Spring Boot 3.2.5 |
| Node.js | 18+ 或 20 LTS | 运行 Vue 3 + Vite |
| npm | 9+ | 管理 frontend workspaces |
| Docker（可选） | 任意较新版本 | 仅 CI 中 OpenAPI diff 使用 `openapitools/openapi-diff`；本地开发非必须 |

建议检查命令：

```powershell
java -version
node -v
npm -v
```

数据库（按使用 profile 选择其一）：

- `dev` profile：使用 H2 内存数据库，无需任何外部依赖，开箱即用
- `phase1` 或 `prod` profile：使用 MySQL，需要本地或远程可用的 MySQL 实例，并在环境变量中配置连接信息

## 3. 仓库结构说明

本仓库采用 "项目根 + 源码子目录" 布局：

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
- 子模块 JaCoCo 覆盖率门禁由 CI 强制，本地可直接 `mvn verify` 验证

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

默认配置：

- 激活的 `spring.profiles.active`：`phase1,dev`（见 `application.yml`）
- `dev` profile：自动启用 H2 内存数据库 + `spring-devtools`（热重启，触发文件 `.trigger-restart`）+ H2 控制台
  - H2 控制台：`http://localhost:8080/h2-console`
  - JDBC URL：`jdbc:h2:mem:aimedical`，用户名：`sa`，密码：**空**（见 `application-dev.yml`）
- 默认监听：`http://localhost:8080`

切换至 MySQL（先准备好 MySQL 实例）：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/aimedical?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="<your-password>"
$env:JWT_SECRET="<your-jwt-secret-min-256bits>"
mvn -f application/pom.xml spring-boot:run -Dspring-boot.run.profiles=phase1
```

> 若后续阶段引入了 Redis、对象存储、其他中间件或外部服务依赖，应在本节同步补充启动前置条件。

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

## 5. 按阶段验证

### 5.1 通用验证原则

完成启动后，请不要只验证 "进程能跑起来"，还应结合当前阶段的目标检查最小业务闭环是否可用。

建议按以下顺序验证：

1. 先验证后端健康检查或最小可达接口
2. 再验证前端页面是否可访问
3. 最后验证当前 phase 新增的核心链路

当前阶段范围与目标，以 `Docs/03_roadmap.md` 为准。

### 5.2 后端可运行验证

#### 5.2.1 业务 ping（保留用于骨架与最小冒烟）

```powershell
curl http://localhost:8080/api/ping
```

预期返回类似：

```json
{"code":"SUCCESS","data":"pong"}
```

说明：

- 若返回结构中包含 `message`、`success` 等附加字段，只要能成功返回健康检查结果且符合统一响应契约即可
- 若后续阶段将该接口下线，应同步更新本文档

#### 5.2.2 Spring Boot 健康检查（推荐作为本地首选健康检查）

```powershell
curl http://localhost:8080/actuator/health
```

预期返回：

```json
{"status":"UP"}
```

说明：

- `actuator` 端点默认在所有 profile 下至少暴露 `health` / `info`
- `dev` profile 下额外暴露 `metrics`
- 若本地 H2 初始化或 JPA 实体映射异常，`status` 会变为 `DOWN`，需回头检查日志

#### 5.2.3 OpenAPI 契约与 Swagger UI

在浏览器或终端验证 API 文档已生成：

- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- Swagger UI：`http://localhost:8080/swagger-ui.html`

> CI 中 OpenAPI 契约导出与 baseline diff 的输入即为 `/v3/api-docs`；保持接口契约不发生未授权 breaking change 是 PR 评审的基本约束。

### 5.3 前端可访问验证

浏览器访问对应端口：

- 患者端：`http://localhost:5173`
- 医生端：`http://localhost:5174`
- 管理员端：`http://localhost:5175`

预期结果：

- 页面可打开，并自动跳转或渲染登录页
- 端口冲突时按 Vite 提示顺延

### 5.4 当前 phase 核心验证建议

请按 roadmap 中 "本阶段做完后新增什么能力" 与 "验收标准" 执行验证。

建议口径（随阶段演化逐步替换最低验证项）：

- Phase 0：验证骨架启动、占位首页、`/api/ping` 健康检查（**已完成**，本节后续不再以该口径为唯一标准）
- **Phase 1（当前主线）**：验证统一认证、动态菜单、权限矩阵、健康档案基础版 CRUD
  - 三大终端均能完成「登录 → 看到与自己角色匹配的菜单 → 进入 Dashboard」全链路冒烟通过
  - 患者端：注册 → 登录 → 进入 `/profile` → 编辑并保存个人资料 → 进入 `/health-record` 录入基础字段
  - 医生端 / 管理员端：登录后看到按角色下发的菜单
- Phase 2 及以后：在基础启动成功后，验证该阶段首次落地的业务或 AI 能力链路

如果当前仓库状态已经进入更高阶段，应以更高阶段的验证项替换仅针对骨架的检查，不能再把 "看到占位页" 当作唯一验收标准。

#### 5.4.1 默认账密

种子数据由 `db/data.sql` 提供，密码统一为 `password123`。

| 角色 | 用户名 | 密码 | 对应终端 |
|------|--------|------|---------|
| 系统管理员 | admin | admin123 | 管理员端 (`:5175`) |
| 医生 | doctor01 | admin123 | 医生端 (`:5174`) |
| 患者 | 13900000003 | admin123 | 患者端 (`:5173`) |

#### 5.4.2 Phase 1 验证示例

种子账号与初始数据见 `AIMedical/backend/application/src/main/resources/db/data.sql`。如果该文件当前未提供测试账号，则执行：

```powershell
mvn -f application/pom.xml spring-boot:run
```

后通过 `http://localhost:8080/h2-console` 自行核对 `users` / `roles` / `role_menus` 等表中的种子数据。

最低验证步骤：

1. 浏览器打开 `http://localhost:5173`
2. 使用患者种子账号登录
3. 确认菜单中包含 `个人中心`、`健康档案`
4. 在健康档案页录入基础字段并保存，重新加载页面验证数据已落库
5. 切换至医生端 `http://localhost:5174`、管理员端 `http://localhost:5175`，重复登录冒烟

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
- 激活的 profile 是否符合当前阶段要求（默认 `phase1,dev`）
- `application.yml` 及环境配置是否与当前阶段要求一致
- 当前阶段新增的数据库、中间件或外部服务依赖是否已准备完成
- 启动日志中是否存在 profile、数据源或 Bean 装配错误
- `Application` 启动时会校验 `phase0` 必须与 `dev` 同时激活；非 `phase0` profile 不受此约束

### 6.3 健康检查接口失败

检查项：

- 后端进程是否已成功启动
- 区分两种接口：`/api/ping`（业务 ping，始终可用）和 `/actuator/health`（Spring Boot Actuator，可能因组件健康状态返回 `DOWN`）
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
- 跨域调用后端时检查 `vite.config.ts` 中的 proxy 配置与后端 CORS 配置

### 6.6 CI 门禁相关失败

- 后端 `jacoco:check` 不通过：本地先 `mvn verify`，按需调整被覆盖率的代码或测试
- 后端子模块 JaCoCo guard 失败：确认对应子模块的 `pom.xml` 同时设置了 `<jacoco.skip>false</jacoco.skip>` 和 `<jacoco.skip.check>false</jacoco.skip>`
- OpenAPI 契约 diff 命中 breaking change：核对 `Docs/contracts/openapi-baseline.json`，必要时同步更新 baseline

## 7. 文档维护要求

当以下信息发生变化时，应同步更新本文档：

- 默认启动命令
- 默认 profile 或配置文件约定
- 后端健康检查接口
- 前端端口或 workspace 脚本
- 当前阶段的最低验证口径
- 数据库、Redis、对象存储等新增中间件依赖

若某一阶段新增了必须依赖的中间件、第三方服务或本地配置步骤，也应直接写入本文档，而不是仅散落在阶段设计文档中。

## 8. 参考文档

- `Docs/03_roadmap.md`
- `Docs/04_ood_phase0.md`
- `Docs/02_tech.md`
- `CONTRIBUTING.md`
