package com.ktmp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ktmp.domain.model.MediaItem
import com.ktmp.domain.model.toMedia3Item
import com.ktmp.playback.PlayerController
import com.ktmp.ui.screen.dialogs.CreatePlaylistDialog
import com.ktmp.ui.screen.dialogs.SleepTimerDialog
import com.ktmp.ui.screen.dialogs.WifiTransferDialog
import com.ktmp.transfer.WifiTransferManager
import com.ktmp.ui.screen.library.LibraryScreen
import com.ktmp.ui.screen.library.LibraryViewModel
import com.ktmp.ui.screen.nowplaying.NowPlayingScreen
import com.ktmp.ui.screen.nowplaying.NowPlayingViewModel
import com.ktmp.ui.screen.playlistdetail.PlaylistDetailScreen
import com.ktmp.ui.screen.settings.SettingsScreen
import com.ktmp.ui.screen.video.VideoPlayerScreen

@Composable
fun KtmpNavHost(
    navController: NavHostController,
    playerController: PlayerController,
    wifiTransferManager: WifiTransferManager,
    libraryViewModel: LibraryViewModel
) {
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showWifiTransferDialog by remember { mutableStateOf(false) }
    var targetPlaylistId by remember { mutableLongStateOf(-1L) }

    val nowPlayingViewModel: NowPlayingViewModel = viewModel()

    // 文件选择器（用于合集详情页添加曲目）
    val playlistFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty() && targetPlaylistId > 0) {
            libraryViewModel.addFilesToPlaylist(uris, targetPlaylistId)
            targetPlaylistId = -1L
        }
    }
    val sleepTimerRemaining by nowPlayingViewModel.sleepTimerRemaining.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Route.Library.route
    ) {
        composable(Route.Library.route) {
            LibraryScreen(
                playerController = playerController,
                onMediaClick = { item, _ ->
                    playerController.playMedia(listOf(item.toMedia3Item()), 0)
                    navController.navigate(Route.NowPlaying.route)
                },
                onArtistClick = { artist -> },
                onPlaylistClick = { playlistId ->
                    navController.navigate(Route.PlaylistDetail.create(playlistId))
                },
                onPlayPlaylist = { playlistId ->
                    libraryViewModel.playPlaylist(playlistId) { items ->
                        val mediaItems = items.map { it.toMedia3Item() }
                        playerController.playMedia(mediaItems, 0)
                        navController.navigate(Route.NowPlaying.route)
                    }
                },
                onCreatePlaylist = { showCreatePlaylistDialog = true },
                viewModel = libraryViewModel
            )
        }

        composable(
            Route.NowPlaying.route,
            popEnterTransition = { fadeIn(tween(100)) },
            popExitTransition = { fadeOut(tween(150)) }
        ) {
            NowPlayingScreen(
                onBack = { navController.popBackStack() },
                onSleepTimerClick = { showSleepTimerDialog = true }
            )
        }

        composable(
            Route.VideoPlayer.route,
            popEnterTransition = { fadeIn(tween(100)) },
            popExitTransition = { fadeOut(tween(150)) }
        ) {
            VideoPlayerScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Route.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
            popEnterTransition = { fadeIn(tween(100)) },
            popExitTransition = { fadeOut(tween(150)) }
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            PlaylistDetailScreen(
                playlistId = playlistId,
                playerController = playerController,
                onBack = { navController.popBackStack() },
                onMediaClick = { item, _ ->
                    playerController.playMedia(listOf(item.toMedia3Item()), 0)
                    navController.navigate(Route.NowPlaying.route)
                },
                onAddToPlaylist = {
                    targetPlaylistId = playlistId
                    playlistFilePicker.launch(arrayOf("audio/*", "video/*"))
                }
            )
        }

        composable(
            Route.Settings.route,
            popEnterTransition = { fadeIn(tween(100)) },
            popExitTransition = { fadeOut(tween(150)) }
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onSleepTimerClick = { showSleepTimerDialog = true },
                onWifiTransferClick = { showWifiTransferDialog = true }
            )
        }
    }

    // 对话框
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name, description ->
                libraryViewModel.createPlaylistAndAddPending(name, description)
                showCreatePlaylistDialog = false
            }
        )
    }

    if (showWifiTransferDialog) {
        WifiTransferDialog(
            transferManager = wifiTransferManager,
            onDismiss = { showWifiTransferDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            activeDurationMs = sleepTimerRemaining,
            onDismiss = { showSleepTimerDialog = false },
            onSet = { durationMs ->
                nowPlayingViewModel.sleepTimerManager.start(
                    durationMs = durationMs,
                    scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main),
                    onExpired = { nowPlayingViewModel.pause() }
                )
                showSleepTimerDialog = false
            },
            onCancel = {
                nowPlayingViewModel.sleepTimerManager.cancel()
                showSleepTimerDialog = false
            }
        )
    }
}

