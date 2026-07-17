package com.ktmp.data.scanner

import android.net.Uri

interface MediaScanner {
    suspend fun scanAll(): Int
    suspend fun addFiles(uris: List<Uri>): List<Long>
    suspend fun scanDirectory(treeUri: Uri): List<Uri>
    fun hasRequiredPermissions(): Boolean
    fun getRequiredPermissions(): List<String>
}
