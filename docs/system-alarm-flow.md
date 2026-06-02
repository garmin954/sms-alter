# Pulse 系统闹钟流程图

## 路径 B：系统时钟闹钟兜底（完整流程）

```mermaid
flowchart TD
    %% ===== 阶段1：短信到达，启动报警服务 =====
    subgraph TRIGGER["📨 短信触发"]
        SMS["Incoming SMS 命中关键词"] --> Svc["AlertService.onStartCommand()"]
    end

    %% 判断是否为首次报警：首次直接启动Activity，非首次先删除旧系统闹钟
    Svc --> CheckActive{"isActive?"}
    CheckActive -->|"false（首次）"| Launch["启动 AlarmActivity<br/>铃声 + 振动 + WakeLock"]
    CheckActive -->|"true（新短信）"| DelOld["🗑️ tryDismissSystemAlarm()<br/>删除旧系统时钟闹钟"]

    Launch --> Screen["AlarmScreen 渲染<br/>⏱️ 20s UI 倒计时开始"]

    %% ===== 阶段2：20秒倒计时，用户有四种操作路径 =====
    subgraph COUNTDOWN["⏱️ 倒计时阶段（20 秒内）"]
        Screen --> UserWait["用户看到全屏报警<br/>⚠️ 闪烁 + 脉冲呼吸动画"]

        UserWait --> Choice{"用户操作？"}
        Choice -->|"✅ 点击确认按钮"| Dismiss1["AlarmScreen.dismissWithCancel()"]
        Choice -->|"🔔 通知栏关闭按钮"| Dismiss2["AlertService 收到<br/>ACTION_DISMISS"]
        Choice -->|"📨 新短信到达"| Dismiss3["AlertService.isActive=true<br/>分支"]
        Choice -->|"⏰ 无操作"| Timeout["倒计时归零 remainingSeconds==0"]
    end

    %% ===== 阶段3：闹钟删除（三种入口统一走 ACTION_DISMISS_ALARM） =====
    subgraph DISMISS["🗑️ 闹钟删除（三个入口，同一机制）"]
        Dismiss1 --> DismissAct1["tryDismissPulseAlarm()"]
        Dismiss2 --> DismissAct2["tryDismissSystemAlarm()"]
        Dismiss3 --> DismissAct3["tryDismissSystemAlarm()"]

        DismissAct1 --> DismissIntent["Intent(ACTION_DISMISS_ALARM)<br/>EXTRA_ALARM_SEARCH_MODE = LABEL<br/>EXTRA_MESSAGE = PULSE_ALARM_LABEL<br/>EXTRA_SKIP_UI = true"]
        DismissAct2 --> DismissIntent
        DismissAct3 --> DismissIntent

        DismissIntent --> ClockDelete["系统时钟 App 删除<br/>匹配标签的闹钟"]
        ClockDelete --> LogDel["LogStore: '已尝试撤销 Pulse 系统闹钟'"]
    end

    %% 三种结束路径：确认→停止服务并关闭 / 通知栏关闭→停止自身并通知Activity / 新短信→等待新闹钟
    Dismiss1 --> StopSvc1["stopService(AlertService)<br/>→ onDismiss() → finish()"]
    Dismiss2 --> StopSvc2["stopSelf()<br/>→ 广播 ACTION_FINISH_ACTIVITY<br/>→ AlarmActivity finish()"]
    Dismiss3 --> WaitNew["等待新 AlertScreen<br/>倒计时归零后创建新闹钟"]

    %% ===== 阶段4：倒计时归零后的兜底——创建系统时钟闹钟 =====
    subgraph FALLBACK["🆘 兜底：系统时钟闹钟创建"]
        Timeout --> CreateAlarm["setSystemAlarmViaIntent()"]

        CreateAlarm --> CalcTime["Calendar 计算触发时间<br/>set(SECOND, 0) → add(MINUTE, 1)<br/>闹钟在下一整分触发（最长 60s）"]

        CalcTime --> AlarmIntent["Intent(ACTION_SET_ALARM)<br/>EXTRA_HOUR / EXTRA_MINUTES<br/>EXTRA_MESSAGE = PULSE_ALARM_LABEL<br/>EXTRA_VIBRATE = true<br/>EXTRA_SKIP_UI = true<br/>EXTRA_DAYS = []  // 一次性"]

        AlarmIntent --> ClockCreate["系统时钟 App 创建<br/>用户可见的一次性闹钟"]

        ClockCreate --> LogCreate["LogStore: '系统闹钟已创建：HH:mm，标签：Pulse 紧急短信告警'"]
    end

    %% 倒计时归零后停止报警服务（闹钟已创建，由系统时钟接管）
    Timeout --> StopSvc3["stopService(AlertService)"]

    %% ===== 阶段5：系统时钟闹钟独立触发（不依赖Pulse应用） =====
    subgraph CLOCK_FIRE["⏰ 系统时钟闹钟触发（分钟边界）"]
        ClockCreate -.->|"分钟后触发"| ClockRing["系统时钟 App 播放<br/>标准闹钟铃声 🔔<br/>（完全独立于 Pulse 应用）"]
        ClockRing -.->|"用户可手动关闭"| UserHandle["用户在时钟 App<br/>或通知栏处理"]
    end

    %% ===== 图例：贯穿全流程的关键标识符 =====
    subgraph LEGEND["🔑 关键标识符"]
        Label["PULSE_ALARM_LABEL<br/>= 'Pulse 紧急短信告警'<br/>贯穿创建和删除，<br/>确保只操作 Pulse 自己的闹钟"]
    end

    %% 以上各子图背景色已移除，仅保留边框默认样式
```

## 路径 A：AlarmManager 保活（独立机制，与报警无关）

```mermaid
flowchart TD
    %% ===== 触发源：开机广播 / 15分钟定时 / MonitorService启动 =====
    subgraph TRIGGERS["🔄 触发源"]
        Boot["BOOT_COMPLETED 广播"] --> Receiver
        Timer["AlarmManager 15min 定时"] --> Receiver
        MS["MonitorService.onStartCommand()"] --> Schedule
    end

    Receiver["BootAndKeepAliveReceiver<br/>@AndroidEntryPoint<br/>@Inject AppPreferences"]

    %% 先检查监听开关，未开启则跳过
    Receiver --> Check{"appPreferences<br/>.isListening?"}

    Check -->|"false"| Skip["🛑 跳过，不做任何操作"]
    Check -->|"true"| MSCheck{"MonitorService<br/>.isRunning()?"}

    %% 服务未运行则启动，已在运行则直接续期保活定时器
    MSCheck -->|"false"| StartMS["MonitorService.start()<br/>→ onStartCommand()"]
    MSCheck -->|"true"| Renew["直接续期保活定时器"]

    StartMS --> Schedule["KeepAliveScheduler.schedule()"]
    Renew --> Schedule

    %% 通过 AlarmManager.setExactAndAllowWhileIdle 设置15分钟精确唤醒
    Schedule --> AM["AlarmManager<br/>.setExactAndAllowWhileIdle()<br/>ELAPSED_REALTIME_WAKEUP<br/>+ 15 分钟"]

    AM -.->|"15min 后"| Receiver

    %% ===== 取消路径：MonitorService销毁时取消保活闹钟 =====
    subgraph CANCEL["🛑 取消"]
        MSStop["MonitorService.onDestroy()"] --> CancelSch["KeepAliveScheduler.cancel()"]
        CancelSch --> CancelAM["AlarmManager.cancel()"]
    end

    %% 以上各子图背景色已移除，仅保留边框默认样式
```

## 对比总结

| 维度 | 路径 A：AlarmManager 保活 | 路径 B：系统时钟闹钟兜底 |
|---|---|---|
| **API** | `AlarmManager.setExactAndAllowWhileIdle` | `AlarmClock.ACTION_SET_ALARM` |
| **接收方** | `BootAndKeepAliveReceiver`（应用内） | 系统时钟 App（应用外） |
| **目的** | 保持 MonitorService 进程存活 | 确保用户最终收到告警 |
| **精度** | 毫秒级（精确唤醒） | 分钟级（分钟边界触发） |
| **用户可见** | 不可见 | 系统时钟 App 中可见 |
| **间隔** | 每 15 分钟重复 | 一次性（`EXTRA_DAYS=[]`） |
| **唯一标识** | 请求码 `9001` | 标签 `PULSE_ALARM_LABEL` |

## 系统闹钟创建逻辑（已修复）

[AlarmScreen.kt](app/src/main/java/com/example/pulse/ui/screens/AlarmScreen.kt)

```kotlin
val calendar = Calendar.getInstance().apply {
    set(Calendar.SECOND, 0)
    add(Calendar.MINUTE, 1)
}
```

去掉了原来的 `+15s` 缓冲和 `if (SECOND > 0)` 条件分支，直接取当前时间的下一整分。`ACTION_SET_ALARM` 只支持分钟精度，最坏情况延迟 60 秒。
