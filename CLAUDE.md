# Pulse — Claude 项目上下文

> 面向 Claude Code 的 Android 项目速查手册。阅读后应能快速定位模块、理解数据流、遵循现有编码规范。

---

## 1. 项目概览

**Pulse** 是一款 Android 短信紧急告警应用。监听 incoming SMS，匹配用户预设关键词后触发全屏报警（声音 + 振动 + 锁屏唤醒 + 系统闹钟兜底）。支持通过 GitHub Releases 检查应用更新。

| 项 | 值 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Hilt DI |
| 构建 | Gradle (Kotlin DSL + Version Catalog) |
| minSdk | 26 (Android 8.0) |
| targetSdk / compileSdk | 35 (Android 14) |
| Kotlin | 1.9.24 |
| AGP | 8.4.0 |
| versionCode / versionName | 3 / 1.0.3 |

---

## 2. 目录结构

```
app/src/main/java/com/example/pulse/
├── SmsAlertApp.kt              # Application 入口，@HiltAndroidApp
├── MainActivity.kt             # 主 Activity：Compose NavHost + 权限引导
├── AlarmActivity.kt            # 全屏报警 Activity（锁屏唤醒 + KeyguardManager.requestDismissKeyguard() + FLAG_KEEP_SCREEN_ON）
├── LogActivity.kt              # 旧版日志查看 Activity（XML Layout，保留兼容）
├── SmsReceiver.kt              # SMS 广播接收器：解析短信 → 关键词匹配 → 启动 AlertService
├── SmsReceiverDedup.kt         # 3 秒去重逻辑（object 单例）
├── BootAndKeepAliveReceiver.kt # 开机/保活广播接收器：@AndroidEntryPoint，BOOT_COMPLETED + 周期保活
├── KeepAliveScheduler.kt       # 保活调度器：每 15min AlarmManager.setExactAndAllowWhileIdle
├── MonitorService.kt           # 监控前台服务：常驻通知 + Chronometer 计时 + 保活调度
├── AlertService.kt             # 报警前台服务：铃声、振动、WakeLock、启动 AlarmActivity，支持通知栏关闭
├── AlarmReceiver.kt            # 系统闹钟 BroadcastReceiver，AlarmManager 触发作为报警兜底
├── KeywordStore.kt             # DataStore 关键词存储（内存缓存 + DataStore 持久化）
├── LogStore.kt                 # 内存 + 文件日志存储（CopyOnWriteArrayList，上限 500 条，持久化到 filesDir）
├── data/
│   ├── AppDatabase.kt          # Room 数据库定义
│   ├── AppPreferences.kt       # DataStore 偏好（监听开关 + 更新检查时间 + monitor 计时 + 旧版迁移）
│   ├── UpdateChecker.kt        # GitHub Releases API 更新检查（OkHttp + 限流检测）
│   ├── entity/AlertRecord.kt   # 报警记录实体
│   └── dao/AlertDao.kt         # Room DAO
├── di/
│   └── DatabaseModule.kt       # Hilt 提供 Database / Dao
├── ui/
│   ├── theme/
│   │   ├── Color.kt            # 颜色定义（Light / Dark / Alarm 三Palette）
│   │   ├── Theme.kt            # PulseTheme + AppColors CompositionLocal
│   │   └── Type.kt             # Typography
│   ├── screens/
│   │   ├── DashboardScreen.kt  # 首页：监听球 + 关键词管理 + 测试按钮
│   │   ├── HistoryScreen.kt    # 历史页：报警记录（Room）+ 运行日志（LogStore）
│   │   ├── SettingsScreen.kt   # 设置页：权限状态 + 版本号 + 检查更新按钮 + 更新对话框 + 主题切换
│   │   └── AlarmScreen.kt      # 全屏报警 Compose UI（20s 倒计时 + 系统闹钟兜底）
│   └── components/
│       ├── ListeningOrb.kt     # 监听状态球（呼吸动画 + 水波纹）
│       ├── KeywordCard.kt      # 关键词输入 + Chip 列表
│       ├── StatusCard.kt       # 权限状态卡片（含 checkAllPermissions / checkEssentialPermissions）
│       └── BottomNavBar.kt     # 底部导航栏
└── viewmodel/
    ├── DashboardViewModel.kt   # 监听开关、关键词 CRUD、测试报警
    ├── HistoryViewModel.kt     # 日志 / 报警记录查询、统计
    └── SettingsViewModel.kt    # 权限状态刷新 + 更新检查逻辑 + UpdateUiState
```

---

## 3. 核心数据流

### 3.1 报警触发链路

```
Incoming SMS
    │
    ▼
SmsReceiver.onReceive()
    ├── 解析短信内容 (Telephony.Sms.Intents.getMessagesFromIntent)
    ├── 获取关键词列表
    │     ├── 优先：KeywordStore.getInstance()?.getKeywords()（内存缓存同步读取）
    │     └── 兜底：loadKeywordsFallback() — 从 SharedPreferences 读取（非空默认值）
    ├── 关键词匹配 — matchKeywords(body, keywords)（大小写不敏感）
    ├── SmsReceiverDedup.isDuplicate() — 3s 去重（内容匹配 + 时间戳，基于 DataStore）
    └── startForegroundService(AlertService)
            │
            ▼
        AlertService.onStartCommand()
            ├── 保存 AlertRecord 到 Room
            ├── acquireWakeLock() — PARTIAL_WAKE_LOCK|ACQUIRE_CAUSES_WAKEUP|ON_AFTER_RELEASE（60s 超时）
            ├── triggerAlarm() — 循环振动 + 循环铃声
            ├── startForeground() — 高优先级通知（含 FullScreenIntent + 关闭按钮）
            └── startActivity(AlarmActivity) — 全屏弹窗
                    │
                    ▼
                AlarmScreen
                    ├── 20s UI 倒计时
                    ├── 用户确认 → 尝试撤销系统闹钟(ACTION_DISMISS_ALARM) → 停止 AlertService → dismiss
                    │   (setExact + ACTION_DISMISS_ALARM 双重取消)
                    ├── 通知栏关闭 → AlertService 接收 ACTION_DISMISS → 尝试撤销系统闹钟 → 发送 ACTION_FINISH_ACTIVITY → 关闭 Activity
                    └── 倒计时归零 → 停止 AlertService
                          ├── ACTION_SET_ALARM 创建系统时钟可见闹钟（15s 后）
                          └── AlarmManager.setExact(RTC_WAKEUP) 设置精确兜底闹钟（15s 后）
                                │
                                ▼
                          AlarmReceiver.onReceive()
                                ├── 启动 AlertService（带 from_alarm_clock=true）
                                └── AlertService 透传标志 → AlarmActivity 重新激活 AlarmScreen
```

### 3.2 监听控制链路

```
用户点击 ListeningOrb
    │
    ▼
DashboardViewModel.toggleListening()
    ├── 检查必需权限（checkEssentialPermissions：# SMS + 通知 + 悬浮窗 + 电池优化，不含 SCHEDULE_EXACT_ALARM）
    ├── AppPreferences.setIsListening() — DataStore 持久化
    ├── SmsReceiver.setEnabled() — 动态启用/禁用组件
    └── MonitorService.start() / stop() — 前台服务
            │
            ▼
        MonitorService.onStartCommand()
            ├── 发布前台通知（含 Chronometer 计时）
            └── KeepAliveScheduler.schedule() — 调度 15min 保活闹钟
```

### 3.3 更新检查链路

```
用户点击"检查更新"
    │
    ▼
SettingsViewModel.checkForUpdates()
    ├── 设置 UpdateUiState.Checking
    ├── UpdateChecker.check(currentVersion)
    │     └── GET https://api.github.com/repos/garmin954/sms-alter/releases
    │         ├── 取第一个非 draft release
    │         ├── 比较 tag_name（去 v 前缀，语义化版本号逐段比较）
    │         └── 返回 UpdateResult（UpdateAvailable / UpToDate / Error）
    └── 更新 UpdateUiState → SettingsScreen 显示 Snackbar 或弹出版本更新对话框
```

### 3.4 保活与进程复活链路

```
Android BOOT_COMPLETED
    │ 或 KeepAliveScheduler 15min 闹钟触发
    ▼
BootAndKeepAliveReceiver.onReceive()
    ├── runBlocking 读取 AppPreferences.isListening（默认 true）
    ├── 若未监听 → 跳过
    ├── 若 MonitorService 未运行 → MonitorService.start()
    └── KeepAliveScheduler.schedule() — 重新调度 15min 后
            │
            ▼
        15min 后 AlarmManager 再次触发
            │
            ▼
        BootAndKeepAliveReceiver.onReceive()
            └── 循环……

开机/保活时若 MonitorService 刚被启动，由它的 onStartCommand() 负责调度下一次保活；
若 MonitorService 已在运行，由 BootAndKeepAliveReceiver 直接调度。两者互斥，无冗余。
```

---

## 4. 关键模块详解

### 4.1 SmsReceiver

- `exported="true"`，`priority="1000"` 确保高优先级接收。
- 通过 `PackageManager.setComponentEnabledSetting` 动态启用/禁用，避免未开启监听时占用资源。
- 去重窗口：**3 秒**，基于内容完全匹配 + 时间戳。去重数据存储在专用 DataStore（sms_dedup_prefs），通过 unBlocking 同步读写，保持存储方案统一。
- **双轨关键词机制**：
  - 优先路径：`KeywordStore.getInstance()?.getKeywords()`（Hilt `@Singleton`，内存缓存）。
  - 兜底路径：当 `getInstance()` 返回 null（Hilt 懒加载尚未完成），回退到 `loadKeywordsFallback()`，使用 `sms_alert_prefs` SharedPreferences 中的关键词，若为空则使用内置的 **非空默认值**：`listOf("ALERT", "紧急", "交警", "服务器宕机")`。
  - 注意：KeywordStore 本身的 `DEFAULT_KEYWORDS` 是空列表，两者默认值不同。

### 4.2 AlertService

- 前台服务类型：`specialUse`，子类型 `urgent_sms_alert`。
- 振动模式：长数组循环 `VibrationEffect.createWaveform(pattern, 0)`。
- 铃声：`RingtoneManager.TYPE_ALARM`，`isLooping = true`。
- WakeLock：`PARTIAL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP | ON_AFTER_RELEASE`，60 秒超时，`setReferenceCounted(false)`。
  - 注意：虽然注释说"屏幕唤醒由 AlarmActivity 处理，Service 只需保持 CPU"，但实际代码仍使用了 `ACQUIRE_CAUSES_WAKEUP`，这意味着 Service 层面也会唤醒屏幕，与注释矛盾。建议二选一。
- **通知栏关闭按钮**：发送 `ACTION_DISMISS` Intent → AlertService 收到后调用 `tryDismissSystemAlarm()` 尝试撤销系统闹钟 → 广播 `ACTION_FINISH_ACTIVITY` 通知 AlarmActivity 关闭。
- 通知已 `setFullScreenIntent(pendingIntent, true)`，确保锁屏时也能全屏弹出。
- 服务销毁时自动停止铃声、取消振动、释放 WakeLock、取消协程作用域。

### 4.3 AlarmScreen 倒计时与兜底

- 常量：`COUNTDOWN_SECONDS = 20`（UI 倒计时），`FALLBACK_DELAY_SECONDS = 15L`（倒计时归零后兜底闹钟延迟秒数）。总兜底时间 = 20 + 15 = 35s。
- **双重兜底机制**：倒计时归零时同时设置两种闹钟——
  - `ACTION_SET_ALARM`：在系统时钟 App 创建用户可见的一次性闹钟。
  - `AlarmManager.setExact(RTC_WAKEUP)`：精确唤醒，由 `AlarmReceiver` 接收。
  两者均在倒计时归零后 15s（即 AlarmScreen 打开后 35s）触发。
- **`from_alarm_clock` 标志**：AlarmReceiver 启动 AlertService 时置为 true，AlertService 透传至 AlarmActivity，确保兜底触发时 UI 正确重置。
- **用户点击确认**：停止 AlertService → 尝试撤销系统闹钟(ACTION_DISMISS_ALARM) → 调用 `onDismiss()` 关闭 Activity。
  - ⚠ **已知问题**：`AlarmManager.setExact` 注册的兜底闹钟（AlarmReceiver）在该路径上未被显式 cancel。用户确认后 15s 仍可能被 AlarmReceiver 再次唤醒。
- **用户未操作**：20s 倒计时归零 → 停止 AlertService → 15s 后双重闹钟触发 → AlarmReceiver 重启 AlertService → AlarmActivity 再次弹出（循环直到用户确认）。
- ⚠ **已知问题**：`setSystemAlarmViaIntent()` 中 Calendar 的分钟舍入逻辑有误——加 15 秒后检查 `get(SECOND) > 0` 然后加 1 分钟，由于 15 秒很少恰好跨整分，闹钟实际被设置在约 60-75 秒后而非预期的 15 秒。建议先 `set(SECOND, 0)` 再 `add(MINUTE, 1)`。

### 4.4 MonitorService

- 前台服务类型：`specialUse`，子类型 `sms_monitoring`。
- **计时机制**：使用 `SystemClock.elapsedRealtime()` 记录启动时间戳，通过 `AppPreferences`（DataStore）持久化 `monitor_start_time` 以在进程重启后恢复（兼容旧版 SharedPreferences `monitor_prefs`）。注意：设备重启后 `elapsedRealtime` 归零，旧值将被抛弃。
- **通知计时**：使用 `setUsesChronometer(true)` + `setWhen(whenTime)` 让系统自动管理计时器显示（非自行每秒更新）。
- `START_STICKY` 确保系统尽量重启服务。
- 静态 `isRunning()` / `getElapsedMs()` 供外部查询运行状态和已运行时长。
- `start()` / `stop()` 静态方法封装 `startForegroundService` / `stopService` 调用，含异常保护。
- **保活集成**：`onStartCommand()` 中调用 `KeepAliveScheduler.schedule(this)`，`onDestroy()` 中调用 `KeepAliveScheduler.cancel(this)`。

### 4.5 KeywordStore

- 架构：Hilt `@Singleton`，通过 `companion object` 维护实例引用供非 Hilt 组件（如 SmsReceiver）访问。
- 兼容旧版 `##` 分隔符格式，读取时自动迁移。
- 内存缓存：`@Volatile cachedKeywords` 实现同步读取，`match()` 和 `getKeywords()` 无需挂起。
- **初始化**：在 `init` 中使用 `runBlocking` 同步加载 DataStore（优先）或旧 SharedPreferences（首次迁移），确保 `SmsAlertApp.onCreate()` 调用 `getKeywords()` 时完整数据已就绪，消除 SmsReceiver 读到过期缓存的竞态条件。
- 旧版兼容：首次启动时自动从旧 SharedPreferences `sms_alert_prefs.keywords` 迁移到 DataStore。
- 限制：最多 50 条，单条最长 50 字符。
- 默认关键词：**空列表**（首次安装无预设关键词）。
- ⚠ 注意：SmsReceiver 中有独立的 `DEFAULT_KEYWORDS = listOf("ALERT", "紧急", "交警", "服务器宕机")` 作为 Hilt 未就绪时的兜底，两者默认值不同。

### 4.6 LogStore

- 线程安全：`CopyOnWriteArrayList<String>`，上限 500 条，超出时移除末尾。
- **文件持久化**：日志同时写入 `filesDir/pulse_events.log`（上限 200KB，超出时截断保留末尾 500 行）。进程重启后通过 `init()` 从文件恢复上一进程日志。
- 提供 `SharedFlow<Unit>` 事件流，供 UI 订阅刷新。
- 日志格式：`[HH:mm:ss] [D/I/W/E] message`。
- `init(Context)` 须在 `Application.onCreate()` 中最先调用，否则文件日志不生效。

### 4.7 UpdateChecker

- 位于 `data/UpdateChecker.kt`，`object` 单例。
- 调用 GitHub Releases API（`garmin954/sms-alter`），过滤 draft。
- **网络层**：使用 **OkHttp**（`OkHttpClient`），连接/读/写超时各 10s，替代原始 `HttpURLConnection`。
- **GitHub API 限流检测**：HTTP 403 + body 含 "rate limit" 时返回 `Error("GitHub API 限流，请稍后再试")`，避免无提示卡死。
- `isVersionNewer()` 按语义化版本号逐段比较（`x.y.z`）。
- 返回 sealed class `UpdateResult`：`UpdateAvailable`（含 versionName / htmlUrl / changelog）、`UpToDate`、`Error`。

### 4.8 AppPreferences

- DataStore 存储，key：`is_listening`（Boolean）、`last_update_check`（Long）、`monitor_start_time`（Long）。
- 旧版迁移：`is_listening` 从旧 `sms_alert_prefs` 迁移（默认 `true`）；`monitor_start_time` 从旧 `monitor_prefs` 迁移。
- 通过 Hilt `@Singleton` 注入。

---

## 5. 编码规范

### 5.1 命名

| 类型 | 规范 | 示例 |
|---|---|---|
| 类 | PascalCase | `AlertService`, `DashboardViewModel` |
| 函数 / 变量 | camelCase | `toggleListening()`, `isListening` |
| 常量 | UPPER_SNAKE_CASE | `COUNTDOWN_SECONDS`, `CHANNEL_ID` |
| Compose 函数 | PascalCase | `ListeningOrb()`, `AlarmScreen()` |
| 资源字符串 | snake_case | `R.string.test_alarm_button` |

### 5.2 Compose 规范

- 使用 `LocalAppColors.current` 获取主题色，**不要**直接硬编码 `Color(...)`。
- Screen 级 Composable 接收 `modifier: Modifier = Modifier` 参数并优先应用。
- ViewModel 通过 `hiltViewModel()` 获取，不要在 Composable 中直接操作 Context 做 IO。
- 动画使用 `rememberInfiniteTransition` + `animateFloat`，指定 `label` 参数。

### 5.3 协程与生命周期

- ViewModel 中使用 `viewModelScope`。
- Service 中自建 `CoroutineScope(Dispatchers.IO)`，在 `onDestroy` 中 `cancel()`。
- Flow 收集使用 `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)`。

### 5.4 权限

- 运行时权限统一在 `MainActivity.requestRuntimePermissions()` 中申请。
- 权限检查辅助函数位于 `StatusCard.kt`：
  - `checkEssentialPermissions()` — 监听开启前检查（SMS + 通知 + 悬浮窗 + 电池优化）。
    - ⚠ **不含** `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`，这意味着兜底 `AlarmManager.setExact` 可能在 Android 12+ 上静默降级为 `setWindow`，无任何提示。
  - `checkAllPermissions()` — 设置页展示完整权限清单（含厂商手动权限 + 精确闹钟权限）。
- `com.android.alarm.permission.SET_ALARM` 声明在 Manifest 中，用于 `ACTION_SET_ALARM` 在系统时钟 App 中创建可见闹钟。

---

## 6. 构建与 CI

### 6.1 依赖管理

- 使用 **Version Catalog**（`gradle/libs.versions.toml`）集中管理所有依赖版本。
- 构建脚本为 **Kotlin DSL**（`.kts`），类型安全。
- 添加新依赖时在 `gradle/libs.versions.toml` 中声明版本和库，在 `app/build.gradle.kts` 中通过 `libs.*` 访问器引用。

### 6.2 本地构建

```bash
# 调试 APK
./gradlew :app:assembleDebug

# 发布 APK（R8 混淆 + 资源压缩）
./gradlew :app:assembleRelease

# Lint + 单元测试
./gradlew :app:lintDebug :app:testDebugUnitTest
```

### 6.3 CI (GitHub Actions)

- 触发：`push` / `pull_request` 到 `main`。
- Build 工作流：Lint → Unit Tests → Debug APK → Release APK → 上传产物。
- Release 工作流：tag push 时自动构建签名 Release APK 并创建 GitHub Release。
- JDK 17 + Gradle 缓存。

### 6.4 签名

- Release 构建从 `local.properties` 或环境变量读取签名配置（`RELEASE_KEYSTORE_FILE` / `RELEASE_KEYSTORE_PASSWORD` / `RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD`）。
- 未配置签名时跳过签名步骤。

---

## 7. 常见修改场景

### 7.1 修改关键词默认项

关键词存在两个独立的默认值位置，修改时需同时考虑：
- `KeywordStore.kt` 中 `DEFAULT_KEYWORDS`（当前为空列表，控制 UI 看到的初始值）。
- `SmsReceiver.kt` 中 `DEFAULT_KEYWORDS`（当前为 `listOf("ALERT", "紧急", "交警", "服务器宕机")`，控制 Hilt 未就绪时的兜底匹配值）。
- 建议统一为一个数据源。

### 7.2 调整报警倒计时或兜底闹钟延迟

- UI 倒计时：修改 `AlarmScreen.kt` 中 `COUNTDOWN_SECONDS` 常量（默认 20s）。
- 兜底闹钟延迟：修改 `AlarmScreen.kt` 中 `FALLBACK_DELAY_SECONDS` 常量（默认 15s，从倒计时归零起算）。

### 7.3 修改报警铃声

修改 `AlertService.triggerAlarm()` 中 `RingtoneManager.getDefaultUri(...)` 的 type，或替换为自定义 Uri。

### 7.4 新增 Room 实体 / 迁移

1. 在 `data/entity/` 下新增实体。
2. 更新 `AppDatabase.kt` 的 `entities` 数组和 `version`。
3. 如需迁移，添加 `Migration` 并在 `DatabaseModule` 的 `Room.databaseBuilder` 中 `.addMigrations(...)`。

### 7.5 新增权限

1. `AndroidManifest.xml` 添加 `<uses-permission>`。
2. `StatusCard.kt` 的 `checkAllPermissions()` / `checkEssentialPermissions()` 中增加检查逻辑。
3. `MainActivity.openPermissionSetting()` 中增加跳转分支。
4. `strings.xml` 添加对应文案。

### 7.6 修改更新检查源

修改 `UpdateChecker.kt` 中的 `GITHUB_API_URL` 常量。

### 7.7 修改保活间隔

修改 `KeepAliveScheduler.kt` 中的 `INTERVAL_MS` 常量（默认 15 分钟）。

---

## 8. 注意事项

- **AlarmScreen 的倒计时与系统闹钟是兜底机制**：确保即使应用被系统限制后台，闹钟仍能唤起。总兜底时间 = COUNTDOWN_SECONDS + FALLBACK_DELAY_SECONDS = 35s。
- **已知问题 — AlarmManager 兜底闹钟未在用户确认时取消**：用户点击确认后未调用 `alarmManager.cancel()` 取消 AlarmReceiver 的 PendingIntent，15s 后仍可能被唤醒。
- **已知问题 — System Alarm Calendar 舍入**：`setSystemAlarmViaIntent()` 中 Calendar 在加 15s 后的分钟舍入逻辑有误，导致系统时钟闹钟设置在约 60-75s 后而非 15s。
- **MonitorService 计时持久化**：使用 `SystemClock.elapsedRealtime()` + DataStore（`AppPreferences.monitor_start_time`）持久化启动时间，进程重启后可恢复计时（兼容旧版 `monitor_prefs`）。设备重启后 `elapsedRealtime` 归零，旧值被抛弃。
- **保活机制**：`KeepAliveScheduler` 每 15min 通过 `AlarmManager.setExactAndAllowWhileIdle` 触发 `BootAndKeepAliveReceiver`，若监听开启且 MonitorService 未运行则自动重启并调度下次保活；若已在运行则直接续期保活定时器。此机制与 MonitorService 绑定，关闭监听后不再调度。
- **厂商权限**：自启动、后台弹出界面、锁屏显示等无法通过标准 API 检测，设置页中标记为"需手动设置"并引导用户跳转。
- **SmsReceiver 动态启用**：首次安装后默认启用，但用户关闭监听后会禁用组件，重新开启时恢复。
- **LogActivity 为旧版 XML 实现**：新功能优先使用 Compose（HistoryScreen），LogActivity 保留用于调试兼容。
- **ProGuard**：Release 构建启用 R8 + `shrinkResources`，Room 实体和 Hilt 模块已受注解保护，无需额外规则。
- **AppPreferences 兼容迁移**：`is_listening` 读取时自动从旧 SharedPreferences (`sms_alert_prefs`) 迁移，旧版数据不会丢失。
- **KeywordStore 初始化时序**：`init` 中使用 `runBlocking` 同步加载 DataStore，确保 `SmsAlertApp.onCreate()` 调用 `getKeywords()` 时完整关键词列表已就绪，消除此前同步读旧 SharedPreferences + 异步读 DataStore 的竞态条件。
- **精确闹钟权限**：`checkEssentialPermissions()` 未检查 `SCHEDULE_EXACT_ALARM`，Android 12+ 上若用户拒绝，兜底 `AlarmManager.setExact` 静默降级为 `setWindow`，延迟可能从 15s 变为数分钟。
- **BootAndKeepAliveReceiver 使用 Hilt**：该 Receiver 声明在 Manifest 中（`@AndroidEntryPoint`），依赖 Hilt 在 Application 启动时完成初始化。若进程刚启动即收到 BOOT_COMPLETED，存在极低概率的注入未就绪风险。
