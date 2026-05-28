package com.example.smsalert.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smsalert.R
import com.example.smsalert.ui.components.KeywordCard
import com.example.smsalert.ui.components.ListeningOrb
import com.example.smsalert.ui.theme.*
import com.example.smsalert.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(),
) {
    val isListening by viewModel.isListening.collectAsState()
    val keywords by viewModel.keywords.collectAsState()
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ListeningOrb(
                isListening = isListening,
                onClick = viewModel::toggleListening,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        KeywordCard(
            keywords = keywords,
            onAddKeyword = viewModel::addKeyword,
            onRemoveKeyword = viewModel::removeKeyword,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = viewModel::testAlarm,
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
                text = stringResource(R.string.test_alarm_button),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DangerRed,
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPermissionDialog,
            title = {
                Text(stringResource(R.string.permission_not_granted_title), fontWeight = FontWeight.Bold, color = DarkBlue)
            },
            text = {
                Text(stringResource(R.string.permission_not_granted_message), color = TextGray)
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissPermissionDialog) {
                    Text(stringResource(R.string.dismiss_button), color = PrimaryBlue)
                }
            },
        )
    }
}
