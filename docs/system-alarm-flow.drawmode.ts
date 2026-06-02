const d = new Diagram({ type: "flowchart" });

const externalStyle = { color: "external" };
const backendStyle = { color: "backend" };
const frontendStyle = { color: "frontend" };
const orchestrationStyle = { color: "orchestration" };
const usersStyle = { color: "users" };

const pulse = d.addText("Pulse 系统闹钟 — 创建与删除流程", { x: 580, y: 20, fontSize: 22, fontFamily: 2, strokeColor: "#7048e8" });
const smsreceiver = d.addBox("SmsReceiver\n收到匹配关键词的短信", { ...externalStyle, row: 0, col: 0, width: 280, height: 72 });
const alertserviceonstartcommandIsactiveFalse = d.addBox("AlertService.onStartCommand()\nisActive = false，首次触发", { ...backendStyle, row: 1, col: 0, width: 300, height: 72 });
const alertserviceAlertrecordRoomWakelock60sFullscreenintentStartactivityalarmactivity = d.addBox("AlertService 报警动作\n━━━━━━━━━━━━━━━━━\n① 保存 AlertRecord → Room\n② 获取 WakeLock (60s)\n③ 发布前台通知 (FullScreenIntent)\n④ 触发声音 + 振动 (循环)\n⑤ startActivity(AlarmActivity)", { ...backendStyle, row: 2, col: 0, width: 320, height: 156 });
const alarmactivityoncreateSetshowwhenlockedTurnscreenonDismissreceiverActionfinishactivitySetcontentalarmscreen = d.addBox("AlarmActivity.onCreate()\n━━━━━━━━━━━━━━━━━\nsetShowWhenLocked + turnScreenOn\n注册 dismissReceiver\n(监听 ACTION_FINISH_ACTIVITY)\n→ setContent(AlarmScreen)", { ...frontendStyle, row: 3, col: 0, width: 320, height: 132 });
const alarmscreen20sUi = d.addBox("AlarmScreen\n━━━━━━━━━━━━━━━━━\n20s UI 倒计时\n显示短信内容\n「确认」按钮", { ...frontendStyle, row: 4, col: 0, width: 260, height: 108 });
const node59 = d.addDiamond("倒计时\n归零?", { ...backendStyle, row: 5, col: 0, width: 140, height: 90 });
const setsystemalarmviaintentActionsetalarm15sPulseSkipuitrue = d.addBox("setSystemAlarmViaIntent()\n━━━━━━━━━━━━━━━━━\nACTION_SET_ALARM\n15s 后在系统时钟创建可见闹钟\n标签: \"Pulse 紧急短信告警\"\nskipUi=true (静默创建)", { ...orchestrationStyle, row: 6, col: 0, width: 340, height: 132 });
const stopservicealertserviceWakelock = d.addBox("stopService(AlertService)\n铃声/振动停止\nWakeLock 释放", { ...backendStyle, row: 7, col: 0, width: 260, height: 78 });
const appPulse6075s = d.addBox("系统时钟 App\n用户可见的一次性 Pulse 闹钟\n(约 60-75s 后触发)", { ...externalStyle, row: 8, col: 0, width: 300, height: 84 });
const alarmreceiveronreceiveStartforegroundservicealertserviceFromalarmclocktrue = d.addBox("AlarmReceiver.onReceive()\n系统闹钟兜底触发\nstartForegroundService(AlertService)\nfrom_alarm_clock=true", { ...externalStyle, row: 9, col: 0, width: 340, height: 96 });
const alertserviceIsactiveTrue = d.addBox("新短信到达\nAlertService 已活跃\n(isActive = true)", { ...externalStyle, row: 0, col: 1, width: 260, height: 84 });
const trydismisssystemalarmActiondismissalarmAlarmscreen = d.addBox("tryDismissSystemAlarm()\n━━━━━━━━━━━━━━━━━\nACTION_DISMISS_ALARM\n按标签搜索并撤销旧闹钟\n(为新 AlarmScreen 倒计时\n 归零创建新闹钟做准备)", { ...orchestrationStyle, row: 1, col: 1, width: 320, height: 132 });
const node592 = d.addBox("用户点击通知栏\n「确认」按钮", { ...usersStyle, row: 2, col: 1, width: 220, height: 72 });
const alertserviceActiondismissTrydismisssystemalarmBroadcastactionfinishactivityStopself = d.addBox("AlertService 收到\nACTION_DISMISS\n━━━━━━━━━━━━━━━━━\n① tryDismissSystemAlarm()\n② broadcast(ACTION_FINISH_ACTIVITY)\n③ stopSelf()", { ...backendStyle, row: 3, col: 1, width: 320, height: 132 });
const alarmactivityDismissreceiverActionfinishactivityFinish = d.addBox("AlarmActivity\ndismissReceiver 收到\nACTION_FINISH_ACTIVITY\n→ finish()", { ...frontendStyle, row: 4, col: 1, width: 280, height: 108 });
const alarmscreenDismisswithcancel = d.addBox("用户点击 AlarmScreen\n「确认」按钮\ndismissWithCancel()", { ...usersStyle, row: 4, col: 2, width: 260, height: 96 });
const trydismisspulsealarmActiondismissalarmAlarmsearchmodelabelSkipuitrue = d.addBox("tryDismissPulseAlarm()\n━━━━━━━━━━━━━━━━━\nACTION_DISMISS_ALARM\nALARM_SEARCH_MODE_LABEL\n按标签搜索并撤销\nskipUi=true", { ...orchestrationStyle, row: 5, col: 2, width: 300, height: 132 });
const stopservicealertserviceOndismissFinishOndestroyStopservice = d.addBox("stopService(AlertService)\n→ onDismiss()\n→ finish()\n→ onDestroy() 再次 stopService", { ...backendStyle, row: 6, col: 2, width: 300, height: 108 });
const alarmactivityAlertservice = d.addBox("闹钟已撤销 ✓\nAlarmActivity 关闭\nAlertService 销毁", { ...usersStyle, row: 7, col: 2, width: 260, height: 84 });

const smsAlertserviceAlarmactivityAlarmscreen = d.addGroup("创建流程: SMS → AlertService → AlarmActivity → AlarmScreen → 系统闹钟兜底", [smsreceiver, alertserviceonstartcommandIsactiveFalse, alertserviceAlertrecordRoomWakelock60sFullscreenintentStartactivityalarmactivity, alarmactivityoncreateSetshowwhenlockedTurnscreenonDismissreceiverActionfinishactivitySetcontentalarmscreen, alarmscreen20sUi, node59, setsystemalarmviaintentActionsetalarm15sPulseSkipuitrue, stopservicealertserviceWakelock, appPulse6075s, alarmreceiveronreceiveStartforegroundservicealertserviceFromalarmclocktrue], { strokeColor: "#3498db", opacity: 30 });
const a = d.addGroup("删除路径 A: 新短信到达时清理旧闹钟", [alertserviceIsactiveTrue, trydismisssystemalarmActiondismissalarmAlarmscreen], { strokeColor: "#e67e22", opacity: 30 });
const b = d.addGroup("删除路径 B: 通知栏关闭按钮", [node592, alertserviceActiondismissTrydismisssystemalarmBroadcastactionfinishactivityStopself, alarmactivityDismissreceiverActionfinishactivityFinish], { strokeColor: "#e74c3c", opacity: 30 });
const c = d.addGroup("删除路径 C: 用户点击确认按钮", [alarmscreenDismisswithcancel, trydismisspulsealarmActiondismissalarmAlarmsearchmodelabelSkipuitrue, stopservicealertserviceOndismissFinishOndestroyStopservice, alarmactivityAlertservice], { strokeColor: "#2ecc71", opacity: 30 });

d.connect(smsreceiver, alertserviceonstartcommandIsactiveFalse, "startForegroundService");
d.connect(alertserviceonstartcommandIsactiveFalse, alertserviceAlertrecordRoomWakelock60sFullscreenintentStartactivityalarmactivity);
d.connect(alertserviceAlertrecordRoomWakelock60sFullscreenintentStartactivityalarmactivity, alarmactivityoncreateSetshowwhenlockedTurnscreenonDismissreceiverActionfinishactivitySetcontentalarmscreen, "startActivity");
d.connect(alarmactivityoncreateSetshowwhenlockedTurnscreenonDismissreceiverActionfinishactivitySetcontentalarmscreen, alarmscreen20sUi, "setContent");
d.connect(alarmscreen20sUi, node59);
d.connect(node59, setsystemalarmviaintentActionsetalarm15sPulseSkipuitrue, "是\n(remainingSeconds=0)", { strokeColor: "#e74c3c" });
d.connect(node59, alarmscreen20sUi, "否\n(继续计时)", { strokeColor: "#3498db", endArrowhead: null });
d.connect(setsystemalarmviaintentActionsetalarm15sPulseSkipuitrue, stopservicealertserviceWakelock, "创建后立即停止");
d.connect(stopservicealertserviceWakelock, appPulse6075s, "~15s 后系统闹钟响");
d.connect(appPulse6075s, alarmreceiveronreceiveStartforegroundservicealertserviceFromalarmclocktrue, "AlarmManager 触发", { strokeColor: "#e67e22" });
d.connect(alarmreceiveronreceiveStartforegroundservicealertserviceFromalarmclocktrue, alertserviceonstartcommandIsactiveFalse, "重新拉起 AlertService\n(兜底循环)", { strokeColor: "#e67e22" });
d.connect(alertserviceonstartcommandIsactiveFalse, alertserviceIsactiveTrue, "再次 onStartCommand", { strokeColor: "#e67e22" });
d.connect(alertserviceIsactiveTrue, trydismisssystemalarmActiondismissalarmAlarmscreen);
d.connect(alertserviceAlertrecordRoomWakelock60sFullscreenintentStartactivityalarmactivity, node592, "通知栏操作", { strokeColor: "#e74c3c" });
d.connect(node592, alertserviceActiondismissTrydismisssystemalarmBroadcastactionfinishactivityStopself, "ACTION_DISMISS");
d.connect(alertserviceActiondismissTrydismisssystemalarmBroadcastactionfinishactivityStopself, alarmactivityDismissreceiverActionfinishactivityFinish, "广播 ACTION_FINISH_ACTIVITY");
d.connect(alarmscreen20sUi, alarmscreenDismisswithcancel, "用户操作", { strokeColor: "#2ecc71" });
d.connect(alarmscreenDismisswithcancel, trydismisspulsealarmActiondismissalarmAlarmsearchmodelabelSkipuitrue);
d.connect(trydismisspulsealarmActiondismissalarmAlarmsearchmodelabelSkipuitrue, stopservicealertserviceOndismissFinishOndestroyStopservice);
d.connect(stopservicealertserviceOndismissFinishOndestroyStopservice, alarmactivityAlertservice);
d.connect(alarmactivityDismissreceiverActionfinishactivityFinish, alarmactivityAlertservice, undefined, { strokeColor: "#e74c3c" });

return d.render({ path: "d:/andorid/stip/docs/system-alarm-flow" });