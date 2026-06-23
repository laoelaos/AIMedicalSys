# 计划审查报告（v13 r1）

## 审查结果
REJECTED

## 发现

- **[严重]** Root `package.json` 的 `dev` 脚本使用 `concurrently` 命令，但未在任意 `package.json`（root 或子包）中声明 `concurrently` 依赖。执行 `npm run dev` 将报错 `command not found: concurrently`，直接阻断验收标准 #2（三端同时启动）。

- **[严重]** `apps/*/vite.config.ts` 引入 `@vitejs/plugin-vue` 插件，但该插件未在任何 `package.json` 的 `devDependencies` 中声明。`npm install` 后 Vite 启动时将因缺少插件而失败。

- **[一般]** 依赖关系图声明 `@aimedical/app-*` 依赖 `vue-router`，但 `apps/patient/package.json` 的职责描述仅列出 `vue + vite`，未提及 `vue-router`。当前占位代码虽未使用路由，但依赖定义不完整，后续开发可能遗漏。

- **[轻微]** `apps/doctor` 和 `apps/admin` 的文件清单以"同 patient 结构"概括，未逐项列出。虽节省篇幅，但可能因解读差异导致实现与预期不符。

## 修改要求

1. **concurrently 依赖缺失**：在 root `package.json` 的 `devDependencies` 中添加 `"concurrently": "^8.2.2"`。
2. **@vitejs/plugin-vue 依赖缺失**：在各 `apps/*/package.json` 或 root `devDependencies` 中添加 `"@vitejs/plugin-vue": "^5.0.0"`。
3. **vue-router 依赖补充**：在各 `apps/*/package.json` 的 `dependencies` 中添加 `"vue-router": "^4.3.0"`，使其与依赖关系图一致。
