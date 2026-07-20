package com.ktmp.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ktmp.domain.model.PlaybackState
import com.ktmp.playback.PlayerController
import com.ktmp.transfer.WifiTransferManager
import com.ktmp.ui.components.MiniPlayer
import com.ktmp.ui.navigation.KtmpNavHost
import com.ktmp.ui.navigation.Route
import com.ktmp.ui.screen.library.LibraryViewModel

@Composable
fun MainScreen(
    playerController: PlayerController,
    wifiTransferManager: WifiTransferManager,
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    var selectedTab by remember { mutableIntStateOf(0) }

    // MiniPlayer 状态
    val metadata by playerController.currentMetadata.collectAsState()
    val miniPlaybackState by playerController.playbackState.collectAsState()
    val miniPositionMs by playerController.positionMs.collectAsState()
    val miniDurationMs by playerController.durationMs.collectAsState()
    val miniPlayerVisible = miniPlaybackState != PlaybackState.IDLE
        && currentRoute != Route.NowPlaying.route
        && currentRoute != Route.VideoPlayer.route

    // 双击/双滑返回退出
    val context = LocalContext.current
    var lastBackTime by remember { mutableLongStateOf(0L) }
    val atRoot = currentRoute == Route.Library.route
    BackHandler(enabled = atRoot) {
        val now = System.currentTimeMillis()
        if (now - lastBackTime < 2000) {
            (context as? Activity)?.finish()
        } else {
            lastBackTime = now
            Toast.makeText(context, "再滑一次退出", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        navController.navigate(Route.Library.route) {
                            popUpTo(Route.Library.route) { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "媒体库") },
                    label = { Text("媒体库") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        navController.navigate(Route.NowPlaying.route)
                    },
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = "播放") },
                    label = { Text("播放") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        navController.navigate(Route.Settings.route)
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                    label = { Text("设置") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            Box(modifier = Modifier.weight(1f)) {
                KtmpNavHost(
                    navController = navController,
                    playerController = playerController,
                    wifiTransferManager = wifiTransferManager,
                    libraryViewModel = libraryViewModel
                )
            }
            MiniPlayer(
                isVisible = miniPlayerVisible,
                title = metadata?.title?.toString() ?: "未选择曲目",
                artist = metadata?.artist?.toString(),
                albumArtUri = metadata?.artworkUri?.toString(),
                playbackState = miniPlaybackState,
                positionMs = miniPositionMs,
                durationMs = miniDurationMs,
                onPlayPauseClick = {
                    if (miniPlaybackState == PlaybackState.PLAYING) playerController.pause()
                    else playerController.play()
                },
                onSkipPreviousClick = { playerController.skipToPrevious() },
                onSkipNextClick = { playerController.skipToNext() },
                onMiniPlayerClick = {
                    selectedTab = 1
                    navController.navigate(Route.NowPlaying.route)
                }
            )
        }
    }
}
