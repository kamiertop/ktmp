package com.ktmp.ui.screen.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@Composable
fun RenameDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String) -> Unit
) {
    var newTitle by remember { mutableStateOf(currentTitle) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        newTitle = currentTitle
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                label = { Text("名称") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newTitle.trim()) },
                enabled = newTitle.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
