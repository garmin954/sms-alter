# SMS Alert — Claude 项目上下文

> 面向 Claude Code 的 Android 项目速查手册。阅读后应能快速定位模块、理解数据流、遵循现有编码规范。

---

## 1. 项目概览

**SMS Alert** 是一款 Android 短信紧急告警应用。监听 incoming SMS，匹配用户预设关键词后触发全屏报警（声音 + 振动 + 锁屏唤醒 + 系统闹钟兜底）。

| 项 | 值 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Hilt DI |
| 构建 | Gradle (Groovy DSL) |
| minSdk | 26 (Android 8.0) |
| targetSdk / compileSdk | 35 (Android 14) |
| Kotlin | 1.9.24 |
| AGP | 8.4.0 |

---

## 2. 目录结构

```
app/src/main/java/com/example/smsalert/
├── SmsAlertApp.kt              # Application 入口，@HiltAndroidApp
├── MainActivity.kt             # 主 Activity：Compose NavHost + 权限引导
├── AlarmActivity.kt            # 全屏报警 Activity（锁屏唤醒、FLAG_KEEP_SCREEN_ON）
├── LogActivity.kt              # 旧版日志查看 Activity（XML Layout，保留兼容）
├── SmsReceiver.kt              # SMS 广播接收器：解析短信 → 关键词匹配 → 启动 AlertService
├── SmsReceiverDedup.kt         # 3 秒去重逻辑
├── MonitorService.kt           # 监控前台服务：常驻通知，显示监听时长
├── AlertService.kt             # 报警前台服务：铃声、振动、WakeLock、启动 AlarmActivity
├── AlarmReceiver.kt            # 系统闹钟 BroadcastReceiver，AlarmManager 触发作为报警兜底
├── KeywordStore.kt             # SharedPreferences 关键词存储（JSON 数组格式）
├── LogStore.kt                 # 内存日志存储（CopyOnWriteArrayList，上限 500 条）
├── data/
│   ├── AppDatabase.kt          # Room 数据库定义
│   ├── AppPreferences.kt       # DataStore 偏好（监听开关状态）
│   ├── entity/AlertRecord.kt   # 报警记录实体
│   └── dao/AlertDao.kt         # Room DAO
├── di/
│   └── DatabaseModule.kt       # Hilt 提供 Database / Dao
├── ui/
│   ├── theme/
│   │   ├── Color.kt            # 颜色定义（Light / Dark / Alarm 三Palette）
│   │   ├── Theme.kt            # SmsAlertTheme + AppColors CompositionLocal
│   │   └── Type.kt             # Typography
│   ├── screens/
│   │   ├── DashboardScreen.kt  # 首页：监听球 + 关键词管理 + 测试按钮
│   │   ├── HistoryScreen.kt    # 历史页：报警记录（Room）+ 运行日志（LogStore）
│   │   ├── SettingsScreen.kt   # 设置页：权限状态列表
│   │   └── AlarmScreen.kt      # 全屏报警 Compose UI（20s 倒计时 + 系统闹钟兜底）
│   └── components/
│       ├── ListeningOrb.kt     # 监听状态球（呼吸动画 + 水波纹）
│       ├── KeywordCard.kt      # 关键词输入 + Chip 列表
│       ├── StatusCard.kt       # 权限状态卡片（含 checkAllPermissions / checkEssentialPermissions）
│       └── BottomNavBar.kt     # 底部导航栏
└── viewmodel/
    ├── DashboardViewModel.kt   # 监听开关、关键词 CRUD、测试报警
    ├── HistoryViewModel.kt     # 日志 / 报警记录查询、统计
    └── SettingsViewModel.kt    # 权限状态刷新
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
    ├── KeywordStore.match() — 关键词匹配
    ├── SmsReceiverDedup.isDuplicate() — 3s 去重
    └── startForegroundService(AlertService)
            │
            ▼
        AlertService.onStartCommand()
            ├── 保存 AlertRecord 到 Room
            ├── acquireWakeLock() — 唤醒屏幕（60s 超时）
            ├── triggerAlarm() — 循环振动 + 循环铃声
            ├── startForeground() — 高优先级通知
            └── startActivity(AlarmActivity) — 全屏弹窗
                    │
                    ▼
                AlarmScreen
                    ├── 10s UI 倒计时
                    ├── 用户确认 → 停止 AlertService → 取消兜底闹钟 → dismiss
                    └── 倒计时归零 → 停止 AlertService
                          ├── ACTION_SET_ALARM 创建系统时钟可见闹钟（15s 后）
                          └── AlarmManager.setExact 设置兜底闹钟（15s 后）
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
    ├── 检查必需权限（checkEssentialPermissions）
    ├── AppPreferences.setIsListening() — DataStore 持久化
    ├── SmsReceiver.setEnabled() — 动态启用/禁用组件
    └── MonitorService.start() / stop() — 前台服务
```

---

## 4. 关键模块详解

### 4.1 SmsReceiver

- `exported="true"`，`priority="1000"` 确保高优先级接收。
- 通过 `PackageManager.setComponentEnabledSetting` 动态启用/禁用，避免未开启监听时占用资源。
- 去重窗口：**3 秒**，基于内容完全匹配 + 时间戳。

### 4.2 AlertService

- 前台服务类型：`specialUse`，子类型 `urgent_sms_alert`。
- 振动模式：长数组循环 `VibrationEffect.createWaveform(pattern, 0)`。
- 铃声：`RingtoneManager.TYPE_ALARM`，`isLooping = true`。
- WakeLock：`SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP | ON_AFTER_RELEASE`，60 秒超时。
- 服务销毁时自动停止铃声、取消振动、释放 WakeLock、取消协程作用域。

### 4.3 AlarmScreen 倒计时与兜底

- 常量：`COUNTDOWN_SECONDS = 10`（UI 倒计时），`FALLBACK_DELAY_SECONDS = 15L`（倒计时归零后兜底闹钟延迟秒数）。总兜底时间 = 10 + 15 = 25s。
- **双重兜底机制**：倒计时归零时同时设置两种闹钟——
  - `ACTION_SET_ALARM`：在系统时钟 App 创建用户可见的一次性闹钟。
  - `AlarmManager.setExact(RTC_WAKEUP)`：精确唤醒，由 `AlarmReceiver` 接收。
  两者均在倒计时归零后 15s（即 AlarmScreen 打开后 25s）触发。
- **`from_alarm_clock` 标志**：AlarmReceiver 启动 AlertService 时置为 true，AlertService 透传至 AlarmActivity，确保兜底触发时 UI 正确重置。
- **用户点击确认**：停止 AlertService → 取消 AlarmManager 闹钟 → 尝试撤销系统时钟闹钟 → 调用 `onDismiss()` 关闭 Activity。
- **用户未操作**：10s 倒计时归零 → 停止 AlertService → 15s 后双重闹钟触发 → AlarmReceiver 重启 AlertService → AlarmActivity 再次弹出（循环直到用户确认）。

### 4.4 MonitorService

- 前台服务类型：`specialUse`，子类型 `sms_monitoring`。
- 每秒更新通知内容，显示监听累计时长 `HH:MM:SS`。
- `START_STICKY` 确保系统尽量重启服务。

### 4.5 KeywordStore

- 存储格式：SharedPreferences 中以 **JSON 数组**保存。
- 兼容旧版 `##` 分隔符格式，读取时自动迁移。
- 限制：最多 50 条，单条最长 50 字符。
- 默认关键词：`["ALERT", "紧急", "交警", "服务器宕机"]`。

### 4.6 LogStore

- 线程安全：`CopyOnWriteArrayList<String>`。
- 上限 500 条，超出时移除末尾。
- 提供 `SharedFlow<Unit>` 事件流，供 UI 订阅刷新。
- 日志格式：`[HH:mm:ss] [D/I/W/E] message`。

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
  - `checkAllPermissions()` — 设置页展示完整权限清单（含厂商手动权限）。

---

## 6. 构建与 CI

### 6.1 本地构建

```bash
# 调试 APK
./gradlew :app:assembleDebug

# 发布 APK（R8 混淆）
./gradlew :app:assembleRelease

# Lint + 单元测试
./gradlew :app:lintDebug :app:testDebugUnitTest
```

### 6.2 CI (GitHub Actions)

- 触发：`push` / `pull_request` 到 `main`。
- 步骤：Lint → Unit Tests → Debug APK → Release APK → 上传产物。
- JDK 17 + Gradle 缓存。

---

## 7. 常见修改场景

### 7.1 添加新关键词默认项

修改 `KeywordStore.kt` 中 `DEFAULT_KEYWORDS` 列表。

### 7.2 调整报警倒计时或兜底闹钟延迟

- UI 倒计时：修改 `AlarmScreen.kt` 中 `COUNTDOWN_SECONDS` 常量（默认 10s）。
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

---

## 8. 注意事项

- **AlarmScreen 的倒计时与系统闹钟是兜底机制**：确保即使应用被系统限制后台，闹钟仍能唤起。
- **厂商权限**：自启动、后台弹出界面、锁屏显示等无法通过标准 API 检测，设置页中标记为"需手动设置"并引导用户跳转。
- **SmsReceiver 动态启用**：首次安装后默认启用，但用户关闭监听后会禁用组件，重新开启时恢复。
- **LogActivity 为旧版 XML 实现**：新功能优先使用 Compose（HistoryScreen），LogActivity 保留用于调试兼容。
- **ProGuard**：Release 构建启用 R8，Room 实体和 Hilt 模块已受注解保护，无需额外规则。
