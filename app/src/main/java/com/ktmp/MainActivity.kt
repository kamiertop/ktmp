package com.ktmp

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.media3.session.SessionToken
import com.ktmp.playback.MusicService
import com.ktmp.playback.PlayerController
import com.ktmp.transfer.WifiTransferManager
import com.ktmp.ui.MainScreen
import com.ktmp.ui.theme.KtmpTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playerController: PlayerController

    @Inject
    lateinit var wifiTransferManager: WifiTransferManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect PlayerController to the MusicService
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        playerController.connect(sessionToken)

        setContent {
            KtmpTheme {
                MainScreen(
                    playerController = playerController,
                    wifiTransferManager = wifiTransferManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerController.release()
        wifiTransferManager.stop()
    }
}
