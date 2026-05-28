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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smsalert.R
import com.example.smsalert.ui.theme.*
import com.example.smsalert.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val logs by viewModel.logs.collectAsState()
    val logCount by viewModel.logCount.collectAsState()

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
                text = stringResource(R.string.history_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBlue,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.log_count_format, logCount),
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
                text = stringResource(R.string.clear_button),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DangerRed,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.clearLogs() }
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
                    text = stringResource(R.string.no_logs),
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
