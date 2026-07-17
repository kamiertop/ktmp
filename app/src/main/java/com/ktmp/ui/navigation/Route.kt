package com.ktmp.ui.navigation

sealed class Route(val route: String) {
    data object Library : Route("library")
    data object NowPlaying : Route("now_playing")
    data object VideoPlayer : Route("video_player")
    data object PlaylistDetail : Route("playlist_detail/{playlistId}") {
        fun create(playlistId: Long) = "playlist_detail/$playlistId"
    }
    data object Settings : Route("settings")
}
