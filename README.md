# 刚刚做了什么 · Just Did（手机端）

「刚刚做了什么」日报系统的**手机端**，是配套电脑端的随身记录工具。随时把「刚刚做了什么」记下来（暂存），扫码连接电脑端后一键推送；也可按日期从电脑端拉取日报浏览。电脑端是整个系统的**唯一权威数据源**，手机端负责记录与查阅：电脑端启动服务后，手机扫码即可接入。

## 功能特性

- **随手暂存**：按天分文件记录，随时打开随时记，上滑增量加载不卡顿
- **扫码连接**：扫描电脑端二维码获取服务地址，周期健康检查，连接状态实时指示
- **一键推送**：暂存记录打包为 ZIP 批数据推送电脑端，携带批 ID 幂等提交，失败可丢弃重试
- **日报拉取**：日历多选日期，批量拉取日报文件与索引；日历状态圆点（已同步/待同步/无数据）一目了然
- **首页展示**：暂存与已拉取日报按时间线合并展示
- **管理页**：缓存占用展示、按日期/按时间清理、连接配置管理、日报来源饼图统计

## 架构

```
┌──────────────────────────────────────────────┐
│              手机端（记录端）                  │
│                                              │
│  ┌──────────┐   ┌──────────────────┐        │
│  │   首页    │   │     管理页        │        │
│  │ (记录+展示)│   │ (拉取+清理+配置)  │        │
│  └────┬─────┘   └────────┬─────────┘        │
│       │                  │                   │
│       └─────────┬────────┘                   │
│                 ▼                            │
│  ┌───────────────────────────┐              │
│  │         本地存储            │              │
│  │  staging/staging-YYYYMMDD.txt            │
│  │  data/YYYY/MM/DD.txt       │              │
│  │  Room 索引 + DataStore     │              │
│  └─────────────┬─────────────┘              │
│                │                             │
│  ┌─────────────┴──────────────┐              │
│  │         HTTP 客户端         │              │
│  │  GET  /health             │──► 电脑端     │
│  │  POST /sync/submit        │──► 电脑端     │
│  │  POST /sync/fetch         │◄── 电脑端     │
│  │  POST /sync/fetch-index   │◄── 电脑端     │
│  └───────────────────────────┘              │
└──────────────────────────────────────────────┘
```

代码按三层组织，依赖方向为 `ui → domain ← data`，Hilt 负责把 Data 实现绑定到 Domain 接口：

| 层 | 目录 | 职责 |
|------|------|------|
| UI | `app/src/main/java/com/zhouyp/justdid/ui/` | Compose 界面、ViewModel、导航 |
| Domain | `.../domain/` | 业务模型与 Repository 接口（零 Android 依赖） |
| Data | `.../data/` | Room/文件存储、OkHttp 请求、Repository 实现 |

## 技术栈

| 组件 | 用途 |
|------|------|
| Kotlin 2.0 | 主语言 |
| Jetpack Compose + Material3 | UI |
| Hilt | 依赖注入 |
| Room + DataStore | 日报索引与连接配置 |
| OkHttp / Gson | 网络请求（电脑端地址动态获取） |
| ZXing（journeyapps 移植版） | 二维码扫描 |

## 构建

### 前置条件

- Windows / macOS / Linux（本项目在 Windows 上开发验证）
- JDK 17
- Android SDK（compileSdk 37 / minSdk 36 / targetSdk 36）

### 编译

```bash
./gradlew assembleDebug
```

调试包输出在 `app/build/outputs/apk/debug/app-debug.apk`，`adb install -r` 即可安装。

## 打包发布

release 包需要签名。首次生成密钥库：

```bash
keytool -genkeypair -v -keystore keystore/release.jks -alias justdid \
  -keyalg RSA -keysize 2048 -validity 10000
```

然后在 `app/` 下创建 `keystore.properties`（已加入 .gitignore）：

```
storeFile=../keystore/release.jks
storePassword=<你的密码>
keyAlias=justdid
keyPassword=<你的密码>
```

一键打包：

```bash
./build-apk.sh
```

产物输出到 `dist/`（debug + release 两个 APK）。未配置 `keystore.properties` 时 release 为未签名 APK。

## HTTP API

4 个 REST 接口均通过局域网访问（电脑端主界面显示二维码，手机端扫码获取地址），协议与电脑端一一对应：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/health` | GET | 存活探测，连接后每 60s 调一次，无状态副作用 |
| `/sync/submit` | POST | 推送暂存 ZIP 批数据，`X-Batch-ID` 头幂等，两阶段合并入电脑端日报文件 |
| `/sync/fetch` | POST | 按日期列表拉取日报内容 ZIP |
| `/sync/fetch-index` | POST | 按日期列表拉取索引条目（仅路径与大小，不含内容） |

实现位置：`ConnectionRepositoryImpl`（health）、`PushRepositoryImpl`（submit）、`DailyReportRepositoryImpl`（fetch / fetch-index）。

## 数据存储

```
{应用私有目录}/
  staging/
    staging-YYYYMMDD.txt   # 暂存记录（按天一个文件）
  data/
    {YYYY}/{MM}/{DD}.txt   # 从电脑端拉取的日报内容
  {批ID}/                  # 推送中的批数据（暂存目录重命名而来，完成后删除）
  Room 数据库               # 日报索引（日期、路径、大小、状态）
  DataStore                # 电脑端地址等配置
```

- 暂存文件格式与电脑端日报一致：`HH:MM` 开头的时间块，块内多条记录直接换行，块间空行分隔，记录按时间递增排序
- Room 仅存索引：**日历状态只查索引，点击具体日期才读文件**，内容与索引分离
- **隐私声明：所有数据只保存在你自己的手机与电脑上**，应用不包含任何数据上传逻辑

## 许可证

本项目以 [Apache License 2.0](LICENSE) 开源，版权归 zhouyp001 所有。应用图标为作者自制。

本项目使用以下第三方组件，其版权归各自所有者：

| 组件 | 许可证 |
|------|--------|
| Kotlin | Apache-2.0 |
| Jetpack Compose / AndroidX（含 Room、Navigation、DataStore） | Apache-2.0 |
| Hilt / Dagger | Apache-2.0 |
| OkHttp / Retrofit / Gson | Apache-2.0 |
| ZXing（journeyapps 移植版） | Apache-2.0 |

## 致谢

- 电脑端项目：[just-did-desktop](https://github.com/zhouyp001/just-did-desktop)
