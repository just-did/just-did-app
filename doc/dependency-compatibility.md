# 依赖兼容性问题记录

## 环境

| 组件 | 版本 |
|------|------|
| Gradle | 9.3.1 |
| AGP | 9.1.1 |
| Kotlin | 2.0.21 |
| KSP | 2.0.21-1.0.28 |
| Hilt | 2.60 |
| Room | 2.7.0 |

---

## 问题一：Hilt 插件报 `Android BaseExtension not found`

**现象**：

```
FAILURE: Build failed with an exception.
> Failed to apply plugin 'com.google.dagger.hilt.android'.
   > Android BaseExtension not found.
```

**原因**：AGP 9.x 移除了已废弃的 `BaseExtension` 内部 API。Hilt 2.55 ~ 2.57.2 的 Gradle 插件依赖该 API 检测 Android 项目，在 AGP 9.x 上直接崩溃。

**解决**：升级 Hilt 到 **2.60+**。此版本适配了 AGP 9.x 的新 API。

| Hilt 版本 | AGP 9.x | 说明 |
|-----------|---------|------|
| 2.55 ~ 2.57.2 | 不支持 | `BaseExtension not found` |
| 2.59 | 有 bug | `ComponentTreeDeps` 注解缺失，跳过 |
| **2.60+** | **支持** | 推荐版本 |

---

## 问题二：KSP 与 AGP 内置 Kotlin 冲突

**现象**：

```
Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin.
Kotlin source set 'debug' contains: [.../generated/ksp/...]
```

**原因**：AGP 9.x 内建了 Kotlin 编译支持（`built-in Kotlin`），不再允许 `kotlin.sourceSets` DSL 被外部插件修改。KSP 生成源代码时需要向 source set 添加路径，与内建 Kotlin 管理机制冲突。

**解决**：在 `gradle.properties` 中添加：

```properties
android.disallowKotlinSourceSets=false
```

> 此配置为实验性选项，当前默认值为 `true`。未来 AGP 版本可能移除此选项，届时需要关注 KSP 或 AGP 的更新方案。

---

## 问题三：Room Schema 导出警告

**现象**：

```
w: [ksp] Schema export directory was not provided to the annotation processor
so Room cannot export the schema.
```

**原因**：Room 默认尝试导出数据库 schema 到编译产物中，但未指定导出目录。

**解决**：在 `@Database` 注解中添加 `exportSchema = false`：

```kotlin
@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase()
```

> 生产环境中建议启用 schema 导出，配合 Room Gradle 插件或手动指定 `room.schemaLocation`，用于数据库迁移验证。

---

## 最终可用版本组合

```toml
# gradle/libs.versions.toml
[versions]
agp = "9.1.1"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"
hilt = "2.60"
room = "2.7.0"
```

```properties
# gradle.properties
android.disallowKotlinSourceSets=false
```
