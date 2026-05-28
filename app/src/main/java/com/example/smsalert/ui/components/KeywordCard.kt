package com.example.smsalert.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsalert.ui.theme.*

@Composable
fun KeywordCard(
    keywords: List<String>,
    onAddKeyword: (String) -> Unit,
    onRemoveKeyword: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(24.dp),
    ) {
        Text(
            text = "报警关键词设置",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DarkBlue,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Input row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        "输入关键词...",
                        fontSize = 14.sp,
                        color = TextGray,
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBackground,
                    unfocusedContainerColor = InputBackground,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val kw = inputText.trim()
                        if (kw.isNotEmpty()) {
                            onAddKeyword(kw)
                            inputText = ""
                        }
                    }
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    color = DarkBlue,
                ),
                singleLine = true,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val kw = inputText.trim()
                    if (kw.isNotEmpty()) {
                        onAddKeyword(kw)
                        inputText = ""
                    }
                },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "添加",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chips
        if (keywords.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                var rowItems = mutableListOf<List<String>>()
                var currentRow = mutableListOf<String>()
                for (kw in keywords) {
                    currentRow.add(kw)
                    if (currentRow.size >= 3) {
                        rowItems.add(currentRow.toList())
                        currentRow.clear()
                    }
                }
                if (currentRow.isNotEmpty()) rowItems.add(currentRow.toList())

                for (row in rowItems) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (kw in row) {
                            KeywordChip(
                                keyword = kw,
                                onRemove = { onRemoveKeyword(kw) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeywordChip(
    keyword: String,
    onRemove: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ChipBackground)
            .padding(start = 16.dp, end = 12.dp),
    ) {
        Text(
            text = keyword,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = DarkBlue,
        )
        Spacer(modifier = Modifier.width(6.dp))
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(18.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "删除",
                tint = DarkBlue,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
