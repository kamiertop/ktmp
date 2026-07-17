package com.ktmp.ui.screen.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView

@Composable
fun VideoPlayerScreen(
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val metadata by viewModel.currentMetadata.collectAsState()
    val currentIndex by viewModel.currentMediaItemIndex.collectAsState()
    val context = LocalContext.current
    val player = viewModel.playerController.player
    var showControls by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { showControls = !showControls }
    ) {
        // Video surface — key forces rebuild on track change to fix frozen frame after background
        if (player != null) {
            key(currentIndex) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).also { pv ->
                            pv.player = player
                            pv.useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Back button and title — add status bar padding since MainScreen
        // removes the top window insets from the scaffold padding
        if (showControls) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 8.dp, top = 4.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }

                Text(
                    text = metadata?.title?.toString() ?: "",
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
