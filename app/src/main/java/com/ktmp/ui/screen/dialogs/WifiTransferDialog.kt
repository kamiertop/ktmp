package com.ktmp.ui.screen.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ktmp.transfer.WifiTransferManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiTransferDialog(
    transferManager: WifiTransferManager,
    onDismiss: () -> Unit
) {
    val state by transferManager.state.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.padding(24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("WiFi 传歌", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    if (state.isRunning) transferManager.stop()
                    onDismiss()
                }) { Icon(Icons.Default.Close, "关闭") }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "电脑和手机连同一个 WiFi，\n浏览器打开下方地址即可拖文件上传",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // URL display
            if (state.isRunning && state.url.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ktmp", state.url))
                            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.url,
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ContentCopy, "复制",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.uploadedCount > 0) {
                    Text(
                        "✓ 已上传 ${state.uploadedCount} 个文件",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (state.clientConnected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text(
                            "已连接",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4CAF50)
                        )
                    }
                } else {
                    Text(
                        state.status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Start / Stop button
            Button(
                onClick = {
                    if (state.isRunning) transferManager.stop()
                    else transferManager.start()
                },
                colors = if (state.isRunning)
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else ButtonDefaults.buttonColors(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    if (state.isRunning) "停止服务器" else "启动服务器",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
