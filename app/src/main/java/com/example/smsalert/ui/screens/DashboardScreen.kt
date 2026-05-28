package com.example.smsalert.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsalert.AlertService
import com.example.smsalert.KeywordStore
import com.example.smsalert.MonitorService
import com.example.smsalert.SmsReceiver
import com.example.smsalert.ui.components.KeywordCard
import com.example.smsalert.ui.components.ListeningOrb
import com.example.smsalert.ui.components.checkEssentialPermissions
import com.example.smsalert.ui.theme.*

private const val PREFS_NAME = "sms_alert_prefs"
private const val KEY_LISTENING = "is_listening"

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    var isListening by remember {
        mutableStateOf(
            prefs.getBoolean(KEY_LISTENING, MonitorService.isRunning())
        )
    }
    var keywords by remember { mutableStateOf(KeywordStore.getKeywords(context)) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        keywords = KeywordStore.getKeywords(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Listening Orb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ListeningOrb(
                isListening = isListening,
                onClick = {
                    val nextState = !isListening
                    if (nextState) {
                        if (!checkEssentialPermissions(context)) {
                            showPermissionDialog = true
                            return@ListeningOrb
                        }
                    }
                    isListening = nextState
                    prefs.edit().putBoolean(KEY_LISTENING, isListening).apply()
                    if (isListening) {
                        MonitorService.start(context)
                    } else {
                        MonitorService.stop(context)
                    }
                    SmsReceiver.setEnabled(context, isListening)
                },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Keyword Card
        KeywordCard(
            keywords = keywords,
            onAddKeyword = { kw ->
                KeywordStore.addKeyword(context, kw)
                keywords = KeywordStore.getKeywords(context)
            },
            onRemoveKeyword = { kw ->
                KeywordStore.removeKeyword(context, kw)
                keywords = KeywordStore.getKeywords(context)
            },
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Test alarm button
        OutlinedButton(
            onClick = {
                val intent = Intent(context, AlertService::class.java).apply {
                    putExtra("msg", "模拟报警测试：检测到紧急关键词")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DangerRed),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = DangerRed,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = null,
                tint = DangerRed,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "触发报警模拟测试",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DangerRed,
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text("权限未授予", fontWeight = FontWeight.Bold, color = DarkBlue)
            },
            text = {
                Text("请前往「设置」页面检查并开启所需权限", color = TextGray)
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("知道了", color = PrimaryBlue)
                }
            },
        )
    }
}
