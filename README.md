# 有数

<p align="center">
  <img src="./logo.png" alt="有数 Banner" width="35%" />
</p>

<p align="center">
  <a href="#"><img src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white" alt="Android" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
  <a href="#"><img src="https://img.shields.io/badge/minSdk-26-FF8A00" alt="minSdk 26" /></a>
  <a href="https://github.com/gorkys/youshu/actions/workflows/android-ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/gorkys/youshu/android-ci.yml?branch=master&label=ci" alt="CI Status" /></a>
  <a href="./LICENSE"><img src="https://img.shields.io/badge/license-MIT-22c55e" alt="License MIT" /></a>
</p>


> 心中有数，遇事不怵。

有数是一个面向家庭物品管理的 Android App，用来记录家中物品的位置、数量、有效期与状态，降低遗忘和浪费成本。项目重点围绕「拍照快速录入」「到期提醒」「分类与库房管理」「自然语言搜索入口」展开，并采用沉浸式页面与毛玻璃悬浮导航作为核心视觉语言。

---

## 功能特点

- 📸 拍照快速录入：从拍照到保存尽量压缩操作路径，适合高频、碎片化记录。
- 🧠 AI 模型管理入口：预置模型别名、Provider、Endpoint、API Key 的管理表单，便于后续接入真实 AI 服务。
- 🔎 搜索中心与库房：支持统一查看全部物品、已用完、待评价、已评价等状态。
- 🗂 分类与位置双维度管理：可按物品分类或按存放位置浏览，并支持新增/删除。
- ⏰ 到期提醒：基于 WorkManager 的本地到期通知，带重复提醒去重。
- ♻️ 回收站：删除后进入回收站，30 天内可恢复，超期自动清理。
- ⭐ 使用后评价：物品标记为已用完后支持星级评价，沉淀后续购买参考。
- ✨ 沉浸式 UI：橙色渐变品牌体系、大圆角卡片、毛玻璃悬浮导航、移动端沉浸式顶部布局。

---

## 预览

### 品牌展示

<p align="center">
  <img src="./宣传图.png" alt="有数品牌展示" width="100%" />
</p>

### 当前页面能力

- 首页：搜索入口、到期提醒、全部物品、最近添加、分类筛选。
- 库房：按状态查看全部物品、已用完、待评价、已评价。
- 分类：按分类 / 按位置切换，并查看对应物品列表。
- 详情：左右滑动浏览、编辑、移入回收站、标记已用完、评分。
- 我的：统计面板、AI 模型管理、到期提醒、回收站、设置等入口。

---

## 技术栈

- Kotlin
- Jetpack Compose
- Navigation Compose
- Room
- Hilt
- WorkManager
- CameraX
- Coil

---

## 项目结构

```text
.
├─ app/
│  ├─ src/main/java/com/youshu/app
│  │  ├─ data/
│  │  │  ├─ local/          # Room 实体、DAO、数据库与迁移
│  │  │  └─ repository/     # 数据仓库层
│  │  ├─ di/                # Hilt 依赖注入
│  │  ├─ ui/
│  │  │  ├─ components/     # 通用 UI 组件
│  │  │  ├─ navigation/     # 路由与导航
│  │  │  ├─ screen/         # 页面实现
│  │  │  └─ viewmodel/      # 状态与交互逻辑
│  │  ├─ util/              # 通知、日期、图片等工具
│  │  ├─ MainActivity.kt
│  │  └─ YouShuApplication.kt
│  └─ src/main/res/         # 图标、主题、字符串、图片资源
├─ gradle/
├─ you-shu-prd.md
├─ 功能与样式.md
└─ README.md
```

架构采用 MVVM：

```text
UI -> ViewModel -> Repository -> Room DB
```

---

## 安装方法

### 环境要求

- Android Studio 最新稳定版
- JDK 17
- Android SDK 35

### 克隆项目

```bash
git clone <your-repo-url>
cd you-shu
```

### 同步与构建

```bash
./gradlew assembleDebug
```

Windows 可使用：

```powershell
.\gradlew.bat assembleDebug
```

如果你的依赖已经缓存完成，也可以离线构建：

```powershell
.\gradlew.bat assembleDebug --offline
```

---

## 快速开始

30 秒内跑起来的最短路径：

```powershell
git clone <your-repo-url>
cd you-shu
.\gradlew.bat assembleDebug
```

然后使用 Android Studio 运行 `app` 模块，或将生成的 APK 安装到设备：

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

首次启动建议重点体验以下路径：

1. 首页 -> 搜索中心 / 全部物品 / 即将过期
2. 底部中间拍照按钮 -> 快速录入 -> 保存
3. 我的 -> AI 模型管理 / 回收站 / 到期提醒

---

## 配置 / API 参考

本项目当前以本地功能为主，暂无真正对外开放的公网 API。和配置最相关的能力如下：

| 配置项 | 位置 | 说明 |
| --- | --- | --- |
| `applicationId` | `app/build.gradle.kts` | 当前为 `com.youshu.app` |
| `minSdk` / `targetSdk` | `app/build.gradle.kts` | 当前为 `26 / 35` |
| AI 模型别名 | 我的 -> AI 模型管理 | 用于区分不同模型配置 |
| Provider | 我的 -> AI 模型管理 | 例如 `OpenAI Compatible`、`Ollama` |
| Endpoint | 我的 -> AI 模型管理 | 模型服务地址，例如 `https://api.example.com/v1` |
| API Key | 我的 -> AI 模型管理 | 当前仅作为配置录入，尚未接入真实推理调用 |
| 通知权限 | Android 13+ | 到期提醒依赖 `POST_NOTIFICATIONS` |
| 相机权限 | 首次拍照时申请 | 拍照录入依赖 CameraX |
| 本地数据库 | Room | 当前已包含评分字段与回收站软删除迁移 |

### 数据状态说明

- `在用`：正常物品。
- `已用完`：已消耗，可进入评价流程。
- `已丢弃`：业务状态，区别于删除。
- `回收站`：删除后进入软删除区，30 天内可恢复。

### 到期提醒说明

- 使用 WorkManager 周期检查。
- 针对同一物品同一日期 / 状态做了提醒去重。
- 依赖系统通知权限，未授权时不会发送通知。

---

## 开发说明

### UI / 交互方向

- 顶部页面强调沉浸式布局，与系统状态栏衔接。
- 底部导航采用毛玻璃悬浮样式，中间拍照按钮作为主操作入口。
- 列表卡片、标签、弹窗、搜索框统一使用大圆角与暖橙色体系。

### 最近完成的关键能力

- 回收站与 30 天恢复
- 库房状态筛选与详情联动
- 详情页评价流程
- Android 12+ Splash 链路收口为纯背景启动页
- 图标资源链按 adaptive icon 规范拆分

---

## Contributing

欢迎继续完善这个项目，建议按下面的方式参与：

1. Fork 仓库并新建分支。
2. 保持最小必要改动，避免无关重构。
3. 提交前至少完成一次本地构建验证：

```bash
./gradlew assembleDebug
```

4. Commit message 建议使用：

```text
feat: ...
fix: ...
refactor: ...
docs: ...
test: ...
chore: ...
```

5. 如果变更涉及 UI，请附截图或录屏。
6. 如果变更涉及数据库、通知、图标或导航，请在 PR 描述中明确风险边界。

---

## Roadmap

- [ ] 接入真实 AI 识别与自然语言搜索
- [ ] 增加更完整的设置中心
- [x] 补充回收站批量恢复 / 永久删除能力
- [ ] 增加 Demo GIF 或录屏视频
- [x] 补充自动化测试与 CI

---

## License

[MIT](./LICENSE)
