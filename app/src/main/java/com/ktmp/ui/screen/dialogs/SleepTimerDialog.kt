package com.ktmp.ui.screen.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SleepTimerDialog(
    activeDurationMs: Long?,
    onDismiss: () -> Unit,
    onSet: (durationMs: Long) -> Unit,
    onCancel: () -> Unit
) {
    // Preset options (in milliseconds)
    val presets = listOf(
        15 * 60 * 1000L to "15 分钟",
        30 * 60 * 1000L to "30 分钟",
        45 * 60 * 1000L to "45 分钟",
        60 * 60 * 1000L to "1 小时",
        120 * 60 * 1000L to "2 小时"
    )

    var selectedPreset by remember { mutableLongStateOf(-1L) }
    var customMinutes by remember { mutableStateOf("") }
    var isCustomSelected by remember { mutableStateOf(false) }

    val selectedDurationMs: Long? = when {
        isCustomSelected -> customMinutes.toLongOrNull()?.times(60_000L)?.takeIf { it > 0 }
        selectedPreset > 0 -> selectedPreset
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("定时关闭") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                presets.forEach { (durationMs, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isCustomSelected && selectedPreset == durationMs,
                            onClick = {
                                isCustomSelected = false
                                selectedPreset = durationMs
                            }
                        )
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Custom time option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isCustomSelected,
                        onClick = {
                            isCustomSelected = true
                            selectedPreset = -1L
                        }
                    )
                    Text("自定义", modifier = Modifier.padding(start = 8.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = {
                            isCustomSelected = true
                            selectedPreset = -1L
                            customMinutes = it.filter { c -> c.isDigit() }
                        },
                        modifier = Modifier.width(96.dp),
                        placeholder = { Text("分钟") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                if (activeDurationMs != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("取消定时", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedDurationMs?.let { onSet(it) } },
                enabled = selectedDurationMs != null
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
