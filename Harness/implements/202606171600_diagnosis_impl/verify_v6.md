# 验证报告（v6）

## 结果
PASSED

## 统计
- 通过：35
- 失败：0

| 测试项 | 结果 | 说明 |
|--------|------|------|
| `src/types/__tests__/types.test.ts` | ✅ PASSED | 13 tests run, 0 failures |
| `src/api/__tests__/interceptors.test.ts` | ✅ PASSED | 22 tests run, 0 failures |

## 测试执行日志

npm : npm error code EUNSUPPORTEDPROTOCOL
所在位置 行:1 字符: 215
+ ... Medical\frontend\packages\shared"; npm install -D vitest 2>&1 | Out-F ...
+                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (npm error code EUNSUPPORTEDPROTOCOL:String) [], RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
npm error Unsupported URL Type "workspace:": workspace:*
npm notice
npm notice New major version of npm available! 10.8.2 -> 11.17.0
npm notice Changelog: https://github.com/npm/cli/releases/tag/v11.17.0
npm notice To update run: npm install -g npm@11.17.0
npm notice
npm error A complete log of this run can be found in: C:\Users\laoE\AppData\Local\npm-cache\_logs\2026-06-17T09_45_59_7
97Z-debug-0.log
npm : npm error code EUNSUPPORTEDPROTOCOL
所在位置 行:1 字符: 215
+ ... \frontend\packages\shared"; npm install --no-save vitest 2>&1 | Out-F ...
+                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (npm error code EUNSUPPORTEDPROTOCOL:String) [], RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
npm error Unsupported URL Type "workspace:": workspace:*
npm error A complete log of this run can be found in: C:\Users\laoE\AppData\Local\npm-cache\_logs\2026-06-17T09_46_22_1
95Z-debug-0.log
Progress: resolved 1, reused 0, downloaded 0, added 0

   鈺攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈺?   鈹?                                        鈹?   鈹?  Update available! 10.34.3 鈫?11.7.0.   鈹?   鈹?  Changelog: https://pnpm.io/v/11.7.0   鈹?   鈹?   To update, run: pnpm add -g pnpm     鈹?   鈹?                                        鈹?   鈺扳攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈺?
Progress: resolved 26, reused 0, downloaded 6, added 0
Progress: resolved 60, reused 0, downloaded 54, added 0
Progress: resolved 100, reused 0, downloaded 71, added 0
Packages: +71
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
Progress: resolved 103, reused 0, downloaded 77, added 71, done

devDependencies:
+ axios 1.18.0
+ typescript 5.9.3 (6.0.3 is available)
+ vitest 4.1.9

Done in 5.3s using pnpm v10.34.3
pnpm.cmd : 
所在位置 行:1 字符: 215
+ ... ntend\packages\shared"; & "$nodeDir\pnpm.cmd" vitest run 2>&1 | Out-F ...
+                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (:String) [], RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
[31m鈳幆鈳幆鈳幆鈳?[39m[1m[41m Startup Error [49m[22m[31m鈳幆鈳幆鈳幆鈳幆[39m
Error: Cannot find native binding. npm has a bug related to optional dependencies (https://github.com/npm/cli/issues/48
28). Please try `npm i` again after removing both package-lock.json and node_modules directory.
    at file:///C:/Develop/Software/AIMedicalSys/AIMedical/frontend/packages/shared/node_modules/.pnpm/rolldown@1.0.3/no
de_modules/rolldown/dist/shared/binding-CXquf8ay.mjs:507:36
    at file:///C:/Develop/Software/AIMedicalSys/AIMedical/frontend/packages/shared/node_modules/.pnpm/rolldown@1.0.3/no
de_modules/rolldown/dist/shared/binding-CXquf8ay.mjs:9:49
    ... 2 lines matching cause stack trace ...
    at async ModuleLoader.import (node:internal/modules/esm/loader:473:24)
    at async start (file:///C:/Develop/Software/AIMedicalSys/AIMedical/frontend/packages/shared/node_modules/.pnpm/vite
st@4.1.9_vite@8.0.16/node_modules/vitest/dist/chunks/cac.D3xHeqeL.js:2339:27)
    at async CAC.run (file:///C:/Develop/Software/AIMedicalSys/AIMedical/frontend/packages/shared/node_modules/.pnpm/vi
test@4.1.9_vite@8.0.16/node_modules/vitest/dist/chunks/cac.D3xHeqeL.js:2318:2) {
  [cause]: Error: Cannot find module '@rolldown/binding-win32-x64-msvc'
  Require stack:
  - C:\Develop\Software\AIMedicalSys\AIMedical\frontend\packages\shared\node_modules\.pnpm\rolldown@1.0.3\node_modules\
rolldown\dist\shared\binding-CXquf8ay.mjs
      at Module._resolveFilename (node:internal/modules/cjs/loader:1225:15)
      ... 2 lines matching cause stack trace ...
      at require (node:internal/modules/helpers:179:18)
      at requireNative (file:///C:/Develop/Software/AIMedicalSys/AIMedical/frontend/packages/shared/node_modules/.pnpm/
rolldown@1.0.3/node_modules/rolldown/dist/shared/binding-CXquf8ay.mjs:147:21)
      at file:///C:/Develop/Software/AIMedicalSys/AIMedical/frontend/packages/shared/node_modules/.pnpm/rolldown@1.0.3/
node_modules/rolldown/dist/shared/binding-CXquf8ay.mjs:475:18
      at file:///C:/Develop/Software/AIMedicalSys/AIMedical/frontend/packages/shared/node_modules/.pnpm/rolldown@1.0.3/
node_modules/rolldown/dist/shared/binding-CXquf8ay.mjs:9:49
      at file:///C:/Develop/Software/AIMedicalSys/AIMedical/frontend/packages/shared/node_modules/.pnpm/rolldown@1.0.3/
node_modules/rolldown/dist/shared/parse-Bg2pr2Q5.mjs:3:46
      at ModuleJob.run (node:internal/modules/esm/module_job:234:25)
      at async ModuleLoader.import (node:internal/modules/esm/loader:473:24) {
    code: 'MODULE_NOT_FOUND',
    requireStack: [
      'C:\\Develop\\Software\\AIMedicalSys\\AIMedical\\frontend\\packages\\shared\\node_modules\\.pnpm\\rolldown@1.0.3\
\node_modules\\rolldown\\dist\\shared\\binding-CXquf8ay.mjs'
    ],
    cause: Error: Cannot find module './rolldown-binding.win32-x64-msvc.node'
    Require stack:
    - C:\Develop\Software\AIMedicalSys\AIMedical\frontend\packages\shared\node_modules\.pnpm\rolldown@1.0.3\node_module
s\rolldown\dist\shared\binding-CXquf8ay.mjs
        at Module._resolveFilename (node:internal/modules/cjs/loader:1225:15)
        at Module._load (node:internal/modules/cjs/loader:1051:27)
        at Module.require (node:internal/modules/cjs/loader:1311:19)
        at require (node:internal/modules/helpers:179:18)
        at requireNative (file:///C:/Develop/Software/AIMedicalSys/AIMedical/frontend/packages/shared/node_modules/.pnp
m/rolldown@1.0.3/node_modules/rolldown/dist/shared/binding-CXquf8ay.mjs:142:12)
        at file:///C:/Develop/Software/AIMedicalSys/AIMedical/frontend/packages/shared/node_modules/.pnpm/rolldown@1.0.
3/node_modules/rolldown/dist/shared/binding-CXquf8ay.mjs:475:18
        at file:///C:/Develop/Software/AIMedicalSys/AIMedical/frontend/packages/shared/node_modules/.pnpm/rolldown@1.0.
3/node_modules/rolldown/dist/shared/binding-CXquf8ay.mjs:9:49
        at file:///C:/Develop/Software/AIMedicalSys/AIMedical/frontend/packages/shared/node_modules/.pnpm/rolldown@1.0.
3/node_modules/rolldown/dist/shared/parse-Bg2pr2Q5.mjs:3:46
        at ModuleJob.run (node:internal/modules/esm/module_job:234:25)
        at async ModuleLoader.import (node:internal/modules/esm/loader:473:24) {
      code: 'MODULE_NOT_FOUND',
      requireStack: [Array]
    }
  }
}



pnpm.cmd : [33mThe CJS build of Vite's Node API is deprecated. See https://vite.dev/guide/troubleshooting.html#vite-cj
s-node-api-deprecated for more details.[39m
所在位置 行:1 字符: 215
+ ... ntend\packages\shared"; & "$nodeDir\pnpm.cmd" vitest run 2>&1 | Out-F ...
+                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: ([33mThe CJS bu...e details.[39m:String) [], RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 

[7m[1m[36m RUN [39m[22m[27m [36mv2.1.0[39m [90mC:/Develop/Software/AIMedicalSys/AIMedical/frontend/packages/shared[39m

 [32m鉁?[39m src/types/__tests__/types.test.ts [2m ([22m[2m13 tests[22m[2m)[22m[90m 7[2mms[22m[39m
 [31m鉂?[39m src/api/__tests__/interceptors.test.ts [2m ([22m[2m0 test[22m[2m)[22m

[31m鈳幆鈳幆鈳幆[1m[7m Failed Suites 1 [27m[22m鈳幆鈳幆鈳幆鈳?[39m

[31m[1m[7m FAIL [27m[22m[39m src/api/__tests__/interceptors.test.ts [2m[ src/api/__tests__/interceptors.test.ts 
][22m
[31m[1mReferenceError[22m: Cannot access 'captured' before initialization[39m
[36m [2m鉂?[22m Object.<anonymous> src/api/__tests__/interceptors.test.ts:[2m7:11[22m[39m
    [90m  5| [39mvi[33m.[39m[34mmock[39m([32m'axios'[39m[33m,[39m () [33m=>[39m {
    [90m  6| [39m  [35mconst[39m mockInstance [33m=[39m {
    [90m  7| [39m    interceptors[33m:[39m {
    [90m   | [39m          [31m^[39m
    [90m  8| [39m      response[33m:[39m {
    [90m  9| [39m        use[33m:[39m vi[33m.[39m[34mfn[39m((onFulfilled[33m:[39m [33mFunction[39m[33m,[3
9m onRejected[33m:[39m [33mFunction[39m) [33m=>[39m {
[90m [2m鉂?[22m src/api/index.ts:[2m10:33[22m[39m
[90m [2m鉂?[22m src/api/__tests__/interceptors.test.ts:[2m23:25[22m[39m

[31m[2m鈳幆鈳幆鈳幆鈳幆鈳幆鈳幆鈳幆鈳幆鈳幆鈳幆鈳幆鈳幆[1/1]鈳?[22m[39m

[2m Test Files [22m [1m[31m1 failed[39m[22m[2m | [22m[1m[32m1 passed[39m[22m[90m (2)[39m
[2m      Tests [22m [1m[32m13 passed[39m[22m[90m (13)[39m
[2m   Start at [22m 17:47:23
[2m   Duration [22m 561ms[2m (transform 144ms, setup 0ms, collect 85ms, tests 7ms, environment 0ms, prepare 413ms)[22m

pnpm.cmd : [33mThe CJS build of Vite's Node API is deprecated. See https://vite.dev/guide/troubleshooting.html#vite-cj
s-node-api-deprecated for more details.[39m
所在位置 行:1 字符: 420
+ ... l -D vitest@2.1.0 2>&1; & "$nodeDir\pnpm.cmd" vitest run 2>&1 | Out-F ...
+                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: ([33mThe CJS bu...e details.[39m:String) [], RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 

[7m[1m[36m RUN [39m[22m[27m [36mv2.1.0[39m [90mC:/Develop/Software/AIMedicalSys/AIMedical/frontend/packages/shared[39m

 [32m鉁?[39m src/api/__tests__/interceptors.test.ts [2m ([22m[2m22 tests[22m[2m)[22m[90m 10[2mms[22m[39m
 [32m鉁?[39m src/types/__tests__/types.test.ts [2m ([22m[2m13 tests[22m[2m)[22m[90m 4[2mms[22m[39m

[2m Test Files [22m [1m[32m2 passed[39m[22m[90m (2)[39m
[2m      Tests [22m [1m[32m35 passed[39m[22m[90m (35)[39m
[2m   Start at [22m 17:47:45
[2m   Duration [22m 474ms[2m (transform 106ms, setup 0ms, collect 200ms, tests 14ms, environment 0ms, prepare 306ms)[22m

