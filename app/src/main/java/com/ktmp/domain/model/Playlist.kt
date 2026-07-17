package com.ktmp.domain.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val coverUri: String? = null,
    val category: String = "默认",
    val createdAt: Long,
    val updatedAt: Long
)
