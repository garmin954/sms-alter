# 项目核心上下文: SmsAlertApp

## 1. 项目概述
`SmsAlertApp` 是一个 Android 紧急短信警报应用程序。它的核心功能是实时监听系统收到的短信，一旦短信内容中包含预设的紧急关键词（如 "ALERT"、"紧急"、"验证码"、"服务器宕机"），应用将触发强烈的警报，包括亮屏、震动、播放报警铃声，并弹出全屏的红色警报页面。

## 2. 核心架构与技术栈
* **开发语言**: Kotlin (JVM Target 17)
* **最低支持版本 (minSdk)**: 26 (Android 8.0)
* **目标支持版本 (targetSdk)**: 35 (Android 15)
* **核心组件**:
  * `MainActivity.kt`: 应用主入口，负责申请必要的动态权限（接收短信、读取短信、震动、通知）以及引导用户加入电池优化白名单。
  * `SmsReceiver.kt`: 继承自 `BroadcastReceiver`，用于静态监听系统 `Telephony.SMS_RECEIVED` 广播。
  * `AlertService.kt`: 前台服务 (`ForegroundService`)，类型为 `mediaPlayback`，负责在后台播放报警铃声、控制震动、以及维持前台通知。
  * `AlarmActivity.kt`: 警报全屏活动页面，配置为在锁屏上显示 (`showOnLockScreen`) 并强制亮屏 (`turnScreenOn`)。
  * `KeywordStore.kt`: 存储匹配关键字（如 "ALERT", "紧急", "验证码", "服务器宕机"）。
* **UI 框架**: XML Layout + ViewBinding (Classic Views API)

## 3. 保活与稳定性注意事项
在国产定制系统（如 MIUI/HyperOS, EMUI, ColorOS, OriginOS）上，为确保短信到达时应用能被唤醒，用户必须手动开启以下权限：
1. 允许应用自启动 / 关联启动
2. 允许后台无限制运行 / 锁屏清理白名单
3. 电池优化设置为“无限制”
4. 允许显示锁屏通知 / 悬浮通知 / 后台弹出界面

## 4. 目录结构
```text
SmsAlertApp/
├── contexts/
│   └── context.md             # 本上下文文件
├── app/
│   ├── src/main/
│   │   ├── java/com/example/smsalert/
│   │   │   ├── MainActivity.kt
│   │   │   ├── SmsReceiver.kt
│   │   │   ├── AlertService.kt
│   │   │   ├── AlarmActivity.kt
│   │   │   └── KeywordStore.kt
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_alarm.xml
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── drawable/
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
└── gradle.properties
```
