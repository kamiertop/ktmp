package com.ktmp.ui.screen.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ktmp.data.repository.PlaylistRepository
import com.ktmp.domain.model.Playlist

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onAdd: (playlistId: Long) -> Unit,
    onCreateNew: () -> Unit
) {
    var selected by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加到合集") },
        text = {
            Column {
                if (playlists.isEmpty()) {
                    Text(
                        "还没有合集",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    playlists.forEach { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = playlist.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected == playlist.id,
                                onClick = { selected = playlist.id }
                            )
                            Text(
                                text = playlist.name,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onCreateNew) {
                    Text("+ 新建合集")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let { onAdd(it) } },
                enabled = selected != null
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
