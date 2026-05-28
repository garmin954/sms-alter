package com.example.smsalert.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsalert.LogStore
import com.example.smsalert.ui.theme.*

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
) {
    var logs by remember { mutableStateOf(LogStore.entries.toList()) }
    var logCount by remember { mutableStateOf(LogStore.entries.size) }

    LaunchedEffect(Unit) {
        LogStore.events.collect {
            logs = LogStore.entries.toList()
            logCount = LogStore.entries.size
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Background)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = "日志",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBlue,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$logCount 条",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBlue.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "清空",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DangerRed,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        LogStore.clear()
                        logs = emptyList()
                        logCount = 0
                    }
                    .padding(4.dp),
            )
        }

        // Log list
        if (logs.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                Text(
                    text = "暂无日志",
                    fontSize = 14.sp,
                    color = TextGray,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                logs.forEachIndexed { index, entry ->
                    val isAlert = entry.contains("[E]") || entry.contains("触发") || entry.contains("匹配")
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    ) {
                        Icon(
                            imageVector = if (isAlert) Icons.Default.Campaign else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isAlert) AlertRed else TextGray,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = entry,
                            fontSize = 13.sp,
                            fontWeight = if (isAlert) FontWeight.Medium else FontWeight.Normal,
                            color = if (isAlert) DarkBlue else TextGray,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (index < logs.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(BorderGray),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
