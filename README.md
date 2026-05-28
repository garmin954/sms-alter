# SMS Alert

Android 短信紧急告警应用 — 监听 incoming SMS，匹配预设关键词后触发全屏报警（声音 + 振动 + 锁屏唤醒）。

## 功能

- **关键词监控** — 自定义关键词列表，收到匹配短信时立即触发报警
- **全屏报警** — 锁屏唤醒、循环铃声、持续振动，确保不会错过紧急通知
- **前台服务** — 使用 Foreground Service 常驻后台，保证监控不中断
- **监听开关** — 一键开启/关闭短信监听
- **报警历史** — 记录所有报警事件，方便回溯
- **模拟测试** — 内置测试按钮，无需真实短信即可验证报警流程

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 构建 | Gradle (Groovy DSL) |
| 最低 SDK | Android 8.0 (API 26) |
| 目标 SDK | Android 14 (API 35) |

## 项目结构

```
app/src/main/java/com/example/smsalert/
├── AlertService.kt          # 报警前台服务（铃声/振动/WakeLock）
├── MonitorService.kt        # 短信监听前台服务
├── SmsReceiver.kt           # 短信广播接收器
├── KeywordStore.kt          # 关键词持久化存储
├── LogStore.kt              # 报警日志存储
├── MainActivity.kt          # 主 Activity
├── AlarmActivity.kt         # 全屏报警 Activity
├── LogActivity.kt           # 日志查看 Activity
└── ui/
    ├── theme/               # 主题定义（颜色/字体）
    ├── screens/
    │   ├── DashboardScreen.kt   # 首页仪表盘
    │   ├── HistoryScreen.kt     # 报警历史
    │   └── SettingsScreen.kt    # 设置页
    └── components/
        ├── ListeningOrb.kt      # 监听状态球
        ├── KeywordCard.kt       # 关键词管理卡片
        ├── LogCard.kt           # 日志条目卡片
        ├── StatusCard.kt        # 状态卡片
        └── BottomNavBar.kt      # 底部导航栏
```

## 构建与运行

```bash
# 调试构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

在 Android Studio 中直接打开项目目录，Sync Gradle 后即可运行。

## 权限

应用需要以下权限（首次启动时引导用户授权）：

- `RECEIVE_SMS` — 读取短信
- `NOTIFICATION` — 发送通知
- `FOREGROUND_SERVICE` — 前台服务
- `WAKE_LOCK` — 唤醒屏幕
- `VIBRATE` — 振动控制
