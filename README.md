# JustDid

「手机记、电脑收」的日报同步 Android 应用。手机端随时记录每日工作内容（暂存），扫码连接电脑端后一键推送；也能按日期从电脑端拉取日报，在首页以日历视图浏览。

## 功能特性

- **暂存记录**：随时记录工作内容，按天分文件存储
- **扫码连接**：二维码扫描连接电脑端，连接状态实时指示
- **暂存推送**：暂存记录打包为 zip 批量推送至电脑端，支持失败丢弃与重试
- **日报拉取**：按日期多选拉取日报文件与索引，日历状态圆点一目了然
- **首页展示**：按天分组展示日报，上滑流畅加载更多
- **管理页**：缓存占用展示、日报清理、配置管理、日报来源饼图统计

## 技术栈与架构

Kotlin 2.0 + Jetpack Compose (Material3) + Hilt + Room + DataStore + OkHttp/Retrofit + ZXing

三层分层、单向依赖：

```
UI (Compose + ViewModel)
    ↓ 依赖
Domain (接口 + 数据模型，零 Android 依赖)
    ↑ 实现
Data (Remote / Local / Repository 实现)
```

## 构建

要求：JDK 17、Android SDK（compileSdk 37）

```bash
./gradlew assembleDebug
```

调试包输出在 `app/build/outputs/apk/debug/app-debug.apk`。

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

## 与电脑端配合

本应用为手机端，需配合电脑端服务使用（电脑端地址通过二维码传递）。协议端点：

| 端点 | 用途 | 实现位置 |
|------|------|----------|
| `GET /ping` | 健康检查 | `data/remote/ApiService.kt` |
| `POST /sync/submit` | 推送暂存批数据（zip） | `data/repository/PushRepositoryImpl.kt` |
| `POST /sync/fetch` | 拉取日报文件 | `data/repository/DailyReportRepositoryImpl.kt` |
| `POST /sync/fetch-index` | 拉取日报索引 | `data/repository/DailyReportRepositoryImpl.kt` |

## License

[Apache License 2.0](LICENSE)
