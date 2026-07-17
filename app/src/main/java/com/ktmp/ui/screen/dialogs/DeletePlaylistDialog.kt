package com.ktmp.ui.screen.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onDeleteOnly: () -> Unit,
    onDeleteAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除合集") },
        text = {
            Column {
                Text("「$playlistName」将被删除。请选择：")

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDeleteOnly)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "仅删除合集（内容移至默认合集）",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDeleteAll)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "删除合集及所有内容",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
