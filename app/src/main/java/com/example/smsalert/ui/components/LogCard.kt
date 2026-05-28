package com.example.smsalert.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsalert.ui.theme.*

@Composable
fun LogCard(
    logs: List<LogItem>,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(24.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "最近日志",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkBlue,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "查看全部 >",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryBlue,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onViewAll() }
                    .padding(4.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Text(
                text = "暂无日志",
                fontSize = 14.sp,
                color = TextGray,
            )
        } else {
            logs.take(3).forEachIndexed { index, log ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BorderGray),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LogRow(log = log)
                if (index < logs.take(3).lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun LogRow(log: LogItem) {
    Row(
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = if (log.isAlert) Icons.Default.Campaign else Icons.Default.Info,
            contentDescription = null,
            tint = if (log.isAlert) AlertRed else TextGray,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DarkBlue,
            )
            if (log.subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = log.subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextGray,
                )
            }
        }
    }
}

data class LogItem(
    val title: String,
    val subtitle: String? = null,
    val isAlert: Boolean = false,
)
