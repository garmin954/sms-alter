package com.example.pulse.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pulse.R
import com.example.pulse.ui.theme.*
import com.example.pulse.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val logs by viewModel.logs.collectAsState()
    val logCount by viewModel.logCount.collectAsState()
    val alertRecords by viewModel.alertRecords.collectAsState()
    val alertCountToday by viewModel.alertCountToday.collectAsState()

    val timeFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val colors = LocalAppColors.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.history_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.darkBlue,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.log_count_format, logCount),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textGray,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.darkBlue.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = {
                    val text = (alertRecords.take(100) + logs).joinToString("\n")
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Pulse 日志", text))
                    Toast.makeText(context, "已复制 ${text.lines().size} 条记录到剪贴板", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制日志",
                    tint = colors.textGray,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.clear_button),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.dangerRed,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.clearLogs() }
                    .padding(4.dp),
            )
        }

        // Alert summary
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = null,
                tint = colors.alertRed,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "今日警报: $alertCountToday 条",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.darkBlue,
                modifier = Modifier.weight(1f),
            )
            if (alertRecords.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.clear_button),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.dangerRed,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.clearAlerts() }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }

        // Alert records from Room
        if (alertRecords.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
                    .padding(horizontal = 24.dp),
            ) {
                alertRecords.forEachIndexed { index, record ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = if (record.source == "test") colors.textGray else colors.alertRed,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.message,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.darkBlue,
                                maxLines = 3,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = timeFormat.format(Date(record.timestamp)),
                                fontSize = 11.sp,
                                color = colors.textGray,
                            )
                        }
                    }
                    if (index < alertRecords.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.borderGray),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // No alerts — show log list
            if (logs.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.no_logs),
                        fontSize = 14.sp,
                        color = colors.textGray,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .weight(1f)
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
                                tint = if (isAlert) colors.alertRed else colors.textGray,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = entry,
                                fontSize = 13.sp,
                                fontWeight = if (isAlert) FontWeight.Medium else FontWeight.Normal,
                                color = if (isAlert) colors.darkBlue else colors.textGray,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (index < logs.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.borderGray),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}
