# 工程审计报告 — SMS Alert

> 审计日期: 2026-05-28
> 审计范围: 全项目 (Kotlin + Jetpack Compose + Android)

---

## 1. 严重问题（Critical）

### C1. `LogStore.onNewEntry` 存在多观察者覆盖的静默丢失问题

**文件**: [app/src/main/java/com/example/smsalert/LogStore.kt:18](app/src/main/java/com/example/smsalert/LogStore.kt#L18)

`var onNewEntry: (() -> Unit)? = null` 只能存储**一个**观察者。当 `HistoryScreen` 和 `LogActivity` 同时存在时，后注册的会覆盖前者。两个 Compose screen 可能同时进入 composition，谁覆盖谁完全不确定。

**后果**: 被覆盖的那个页面日志永远不会更新，且没有任何警告或崩溃，属于静默 bug。

**修复**: 改为 `CopyOnWriteArrayList<() -> Unit>` 管理回调，或用 `MutableSharedFlow`。

---

### C2. `MonitorService.checkNewSms()` 直接查询 `content://sms/` 是非官方 API

**文件**: [app/src/main/java/com/example/smsalert/MonitorService.kt:158-163](app/src/main/java/com/example/smsalert/MonitorService.kt#L158-L163)

直接 query `content://sms/inbox` 在 Android 4.4+ 已不推荐，Android 11+ 对非默认短信应用的 SMS Provider 访问进一步收紧。ContentObserver 监听 `content://sms/` 同理。

**后果**: 如果目标是 Google Play，此代码必被拒审。

**修复**: 核心短信检测完全依赖 `SmsReceiver`（BroadcastReceiver 监听 `SMS_RECEIVED`），去掉 ContentObserver + 轮询作为双通道检测，或只保留作为非主流市场的 fallback。

---

### C3. `WakeLock` 无超时释放，可能彻底耗尽电池

**文件**: [app/src/main/java/com/example/smsalert/AlertService.kt:107-120](app/src/main/java/com/example/smsalert/AlertService.kt#L107-L120)

`wakeLock.acquire()` 使用 `SCREEN_BRIGHT_WAKE_LOCK + ACQUIRE_CAUSES_WAKEUP` 且无超时设置。如果用户没有手动关闭 AlarmActivity（比如通知被划掉但 Service 没 stop），WakeLock 会一直持有直到电池耗尽。

**修复**: 使用 `wakeLock.acquire(60_000)` 设置 60 秒超时，或使用 `AlarmManager` 闹钟 + 前台通知来替代 WakeLock 方案。

---

### C4. `HistoryScreen` 的 DisposableEffect 泄露观察者

**文件**: [app/src/main/java/com/example/smsalert/ui/screens/HistoryScreen.kt:31-38](app/src/main/java/com/example/smsalert/ui/screens/HistoryScreen.kt#L31-L38)

`onDispose { }` 是空块。它覆盖了已有的回调但从未清除。当 HistoryScreen 离开 composition 时，`LogStore.onNewEntry` 仍然指向这个已被 dispose 的 lambda，而这个 lambda 捕获的 `logs` 和 `logCount` 的 MutableState 引用已经不再有效。

**后果**: 内存泄漏 + 可能因访问失效的 compose state 导致 crash。

**修复**: 在 `onDispose` 中把 `LogStore.onNewEntry` 设为 null 或移除此特定观察者（配合 C1 的修复）。

---

### C5. 混用 XML View 和 Jetpack Compose — 两套 UI 范式、两套主题

项目同时存在 XML View 体系（`activity_main.xml`、`LogActivity`、`AlarmActivity`）和 Compose 体系（DashboardScreen、SettingsScreen、HistoryScreen）。

`activity_main.xml` 是一个完整的旧版 XML 全功能页面，但 `MainActivity` 的 `setContent` 里只放了 Compose UI，这个 XML 文件已经是死代码。`AlarmActivity` 用 XML 硬编码颜色（`#FF5252`, `#D32F2F` 等），与 Compose 的 `Color.kt` 色板分离。

**后果**: 未来改主题要改两处。代码 review 时让人困惑哪个是真正的 UI。

**修复**: 删除 `activity_main.xml`。`AlarmActivity` 用 Compose 重写，或至少统一颜色到 `colors.xml`。`LogActivity` 可保留作为独立页面或迁移到 Compose。

---

### C6. `applicationId` 与 `namespace` 不匹配

**文件**: [app/build.gradle:8-14](app/build.gradle#L8-L14)

`namespace 'com.example.smsalert'` 但 `applicationId "com.example.smsalert2"`。这是上线前临时改名忘记统一清理的痕迹。

**后果**: 如果未来需要使用 `BuildConfig.APPLICATION_ID`，行为会和预期不一致。

**修复**: 统一 `namespace` 和 `applicationId`。

---

## 2. 中级问题（Major）

### M1. `onResume` 中不必要的 `permissionRefreshKey++`

**文件**: [app/src/main/java/com/example/smsalert/MainActivity.kt:40-43](app/src/main/java/com/example/smsalert/MainActivity.kt#L40-L43)

权限状态在 99% 的 `onResume` 调用中没有任何变化，但每次 resume 都触发 `SettingsScreen` 的 `remember(permissionsRefreshKey)` 重新计算，重新查询 7 项权限（含 `Settings.canDrawOverlays()` 这种跨进程系统调用）。

**修复**: 只在权限请求回调中 `permissionRefreshKey++`，去掉 `onResume` 中的无条件递增。

---

### M2. 用 `"##"` 做关键词分隔符是脆弱的序列化方案

**文件**: [app/src/main/java/com/example/smsalert/KeywordStore.kt:18](app/src/main/java/com/example/smsalert/KeywordStore.kt#L18)

如果用户输入的关键词本身就包含 `##`，存储和解析都会出错。

**修复**: 使用 `SharedPreferences` 的 `putStringSet` 或 JSON 数组 `["kw1","kw2"]`。

---

### M3. `SmsReceiver` 用静态变量去重 — 多进程/进程被杀后失效

**文件**: [app/src/main/java/com/example/smsalert/SmsReceiver.kt:14-16](app/src/main/java/com/example/smsalert/SmsReceiver.kt#L14-L16)

`private var lastBody: String = ""` 和 `private var lastTime: Long = 0` 是 JVM 静态变量。进程被杀死后去重状态丢失。厂商双进程保活场景下互不可见。

**修复**: 用 `SharedPreferences` 持久化最后一条短信 ID 和时间。

---

### M4. `ContentObserver` 在 MainLooper 上运行

**文件**: [app/src/main/java/com/example/smsalert/MonitorService.kt:56](app/src/main/java/com/example/smsalert/MonitorService.kt#L56)

`Handler(Looper.getMainLooper())` 意味着 ContentObserver 的 `onChange` 回调在主线程执行。ContentProvider query 是 IO 操作，不应在主线程。

**修复**: 使用 `HandlerThread` 或 `Dispatchers.IO` 协程。

---

<!-- M5 removed: VibrationEffect.createWaveform with repeat=0 DOES loop (confirmed against API docs). No issue. -->

### M6. Ringtone 没有 fallback 和错误处理

**文件**: [app/src/main/java/com/example/smsalert/AlertService.kt:99-104](app/src/main/java/com/example/smsalert/AlertService.kt#L99-L104)

如果用户手机没有设置闹钟铃声（`RingtoneManager.getDefaultUri(TYPE_ALARM)` 返回 null），`RingtoneManager.getRingtone(this, null)` 的行为在不同设备上不一致。

**修复**: 添加 null 检查 + fallback 到通知铃声或默认铃声。

---

### M7. 不存在 ViewModel 层

整个项目完全没有 ViewModel。`DashboardScreen` 直接操作 SharedPreferences、KeywordStore、MonitorService、SmsReceiver。业务逻辑和 UI 完全耦合在 Composable 函数中。

**修复**: 引入 ViewModel + StateFlow 做状态持有者，屏幕旋转后状态不丢失。

---

### M8. `ListeningOrb` 中的无波动画仍被创建和运行

**文件**: [app/src/main/java/com/example/smsalert/ui/components/ListeningOrb.kt:50-88](app/src/main/java/com/example/smsalert/ui/components/ListeningOrb.kt#L50-L88)

`ripple1Alpha/Scale`、`ripple2Alpha/Scale`、`ripple3Alpha/Scale` 这 6 个 `rememberInfiniteTransition` 变量定义了完整的 ripple 动画参数，但实际渲染时使用的是下面重新计算的 `r1a/r1s/r2a/r2s/r3a/r3s`。这 6 个 infinite transition 一直运行但从未被使用。

**后果**: 纯浪费 GPU 资源，增加电池消耗。

**修复**: 删除未使用的动画变量，只保留 `rippleAnimProgress`。

---

### M9. `proguard-rules.pro` 是空壳

**文件**: [app/proguard-rules.pro](app/proguard-rules.pro)

Release 构建 `minifyEnabled false`，proguard 文件几乎是空的注释。

**修复**: 至少添加 Compose、Material3 等常用库的 keep 规则。Release 应开启 `minifyEnabled true`。

---

### M10. 没有单元测试，没有 UI 测试

整个项目零测试。关键词匹配、权限检查逻辑、去重逻辑都是纯函数，非常容易测试。对于一个处理短信 + 警报的生产应用来说不可接受。

---

## 3. 可优化项（Minor）

- `MainActivity.openAutoStartSettings()` 定义了但从未被调用，`openPermissionSetting` 的 `"autostart"` 分支指向的是 `openAppSettings()` 而不是 `openAutoStartSettings()`。
- `LogStore.add()` 用 `@Synchronized` 但操作的是 `CopyOnWriteArrayList`（本身就是线程安全的），锁是多余的。
- `AlarmActivity.onDestroy()` 调 `stopService()` 假设 Service 还在，但如果 Service 被系统杀死，这行无害但逻辑不严谨。
- `ListeningOrb` 中 `Spacer(52.dp)` 硬编码居中偏移，不同屏幕尺寸表现不一致。
- `DashboardScreen` 底部 `Spacer(100.dp)` 是为了让导航栏不遮挡内容，应该用 `navigationBarsPadding()`。
- UI 中的中文硬编码字符串不可国际化，应全部提取到 `strings.xml`。
- `KeywordCard` 的 chip 换行逻辑用纯 Kotlin 计算而非 Compose 的 `FlowRow`。
- `LogStore` 的 `timeFormat` 是 `SimpleDateFormat`，在 synchronized 块内使用暂时安全，但推荐迁移到 `java.time` API。

---

## 4. 架构升级建议

**当前架构问题**:
- 无分层：UI → 直接调用 → SharedPreferences / Service / ContentProvider
- 无状态管理：状态散落在 `remember{}`、静态变量、SharedPreferences 中
- 无依赖注入
- Service 和 UI 之间无正式通信通道

**目标架构**:

```
UI (Compose)
  → ViewModel (StateFlow)
    → UseCase / Repository
      → DataSource (SharedPreferences → DataStore → Room DB)
      → ServiceManager (封装 Service 启停)
```

**具体升级**:
1. 引入 `ViewModel` + `StateFlow` 做状态持有者
2. 用 `Room` 替代 `SharedPreferences` 存关键词（支持模糊搜索）
3. 用 `DataStore` 替代 `SharedPreferences` 存简单 KV
4. Service 与 UI 通信用 `StateFlow` 或 `callbackFlow`，而不是静态变量
5. 引入 Hilt 做依赖注入
6. Compose Navigation 替代手动 `when(selectedRoute)` 路由

---

## 5. 稳定性升级建议

| # | 问题 | 修复 |
|---|------|------|
| 1 | 去重逻辑用静态变量，进程重启丢失 | 用 SharedPreferences 持久化最后一条短信 ID 和时间 |
| 2 | WakeLock 无超时兜底 | 设置 60 秒最大持有时间 + AlarmManager 闹钟做二次唤醒 |
| 3 | Service 崩溃自恢复需确保幂等 | `onCreate` 里的 ContentObserver 注册需检查是否已注册 |
| 4 | ContentObserver 安全 unregister | 如果注册失败（`onCreate` 中 try-catch 吞掉了），`onDestroy` 调用 unregister 会抛 `IllegalArgumentException` |
| 5 | Android 14+ 前台 Service 类型 | 已使用 `specialUse`，需确保 Google Play 审核时能说明用例 |
| 6 | 通知渠道被用户手动关闭 | 运行时检测通知渠道是否启用，在 UI 中提示用户 |

---

## 6. 性能升级建议

| # | 问题 | 修复 |
|---|------|------|
| 1 | `ListeningOrb` 中 6 个未使用的 infinite transition | 删除，减少 GPU 开销 |
| 2 | `LogStore.toList()` 每次回调都拷贝 500 条字符串 | 用 diff 增量更新，或降低回调频率（debounce） |
| 3 | `checkAllPermissions` 查询 `Settings.canDrawOverlays()` 是跨进程 IPC | 缓存结果，仅在权限回调时刷新 |
| 4 | `mutableStateOf` 每次从 SharedPreferences 反序列化关键词 | 在 ViewModel 中缓存 |
| 5 | Release 构建 `minifyEnabled false` | 开启 R8/ProGuard 混淆、压缩、优化 |
| 6 | `MonitorService` 每 8 秒轮询 ContentProvider | 去掉轮询，完全依赖 BroadcastReceiver |

---

## 7. UI 专业化建议

| # | 问题 | 修复 |
|---|------|------|
| 1 | 三套颜色体系并存（Compose Color.kt / colors.xml / XML layout 硬编码） | 统一到 Compose Color.kt 或 colors.xml，只保留一套 |
| 2 | AlarmActivity 用 emoji ⚠ 作为警告图标 | 用 VectorDrawable 替代，保证各版本渲染一致 |
| 3 | BottomNavBar 88dp 高度过大 | 改为 64dp + `navigationBarsPadding()` |
| 4 | ListeningOrb 内层 Canvas 和外层 ripple 点击热区重叠且不一致 | 统一到最外层 Box 的 clickable |
| 5 | "AI MONITORING" 标签误导用户 | 项目未使用任何 AI/ML 技术，改为 "MONITORING" 或 "ACTIVE" |
| 6 | 测试警报按钮用硬编码 `Color.White` 做 containerColor | 使用 theme 的 surface 色，确保暗色主题适配 |
| 7 | `Spacer(100.dp)` 硬编码底部间距 | 改用 `navigationBarsPadding()` |
| 8 | 未适配暗色主题 | 添加 darkColorScheme |
| 9 | 未适配横屏 / 平板 | 添加 `WindowSizeClass` 自适应布局 |

---

## 8. 重构路线图

### 第一轮: 低风险修复（1-2 天）

- [x] 删除 `activity_main.xml`（旧的 XML 版首页），确认只用 Compose
- [x] `AlarmActivity` 改用 Compose + 统一颜色系统
- [x] `LogStore` 的回调机制改为 `MutableSharedFlow`
- [x] 修复 `ListeningOrb` 的无效动画 + 多重 clickable 问题
- [x] ~~修复 `VibrationEffect` 循环振动参数~~（确认 `repeat=0` 即循环，代码正确，不做改动）
- [x] WakeLock 加 60 秒超时
- [x] 修复 `HistoryScreen` 的观察者泄漏（已随 SharedFlow 重构解决）
- [x] `KeywordStore` 改用 JSON 序列化（含 **##** 旧格式自动迁移）
- [x] 统一 `namespace` 和 `applicationId`

### 第二轮: 中等重构（3-5 天）

- [ ] 引入 ViewModel 层: `DashboardViewModel`, `SettingsViewModel`, `HistoryViewModel`
- [ ] 短信检测完全依赖 `SmsReceiver`，去掉 ContentObserver + 轮询
- [ ] 去重逻辑持久化到 SharedPreferences
- [ ] 统一 UI 颜色体系，删除 `colors.xml` 中 Compose 不用的颜色
- [ ] 提取所有中文字符串到 `strings.xml`
- [ ] 补充单元测试（关键词匹配、去重、权限检查）
- [ ] Compose Navigation 替代手动 `when(selectedRoute)` 路由

### 第三轮: 架构升级（1-2 周）

- [ ] 引入 Hilt 依赖注入
- [ ] `SharedPreferences` → `DataStore`
- [ ] 引入 Room 存储历史警报记录（可搜索、可统计）
- [ ] CI/CD pipeline（GitHub Actions）
- [ ] Release 开启 R8 混淆 + 完善 keep 规则
- [ ] Google Play 合规审核准备（隐私政策、特殊权限声明、Data Safety 表单）
- [ ] 适配横屏 / 平板（WindowSizeClass）
- [ ] 适配暗色主题

---

## 总结

这是一个功能正确的 MVP，核心逻辑（短信检测 → 匹配关键词 → 触发警报）能跑通。但工程化水平距"生产级"有明显差距：

- **无 ViewModel / 无状态管理**: 配置变更（屏幕旋转）可能丢状态
- **无测试**: 零覆盖
- **多套 UI 体系并存**: XML 和 Compose 混用，维护成本翻倍
- **WakeLock 无超时**: 生产环境风险高
- **观察者泄漏**: HistoryScreen 退出后仍持有引用
- **双通道短信检测中有一条依赖非官方 API**: Google Play 审核风险

如果这是个人小工具，足够使用；如果要上架应用商店或长期多人协作维护，需要完成上述三轮重构。