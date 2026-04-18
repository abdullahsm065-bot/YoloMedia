package com.yolomedia.data.model

import android.net.Uri

data class VideoItem(
    val id: Long,
    val title: String,
    val path: String,
    val uri: Uri,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val width: Int = 0,
    val height: Int = 0,
    val folderName: String = "",
    val folderPath: String = "",
    val mimeType: String = ""
)

data class ImageItem(
    val id: Long,
    val title: String,
    val path: String,
    val uri: Uri,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val width: Int = 0,
    val height: Int = 0,
    val folderName: String = "",
    val folderPath: String = "",
    val mimeType: String = ""
)

data class MediaFolder(
    val name: String,
    val path: String,
    val thumbnailUri: Uri?,
    val mediaCount: Int,
    val totalSize: Long = 0
)

data class SafeFolder(
    val name: String,
    val path: String,
    val videoCount: Int = 0,
    val photoCount: Int = 0
)

enum class SortOrder {
    NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, SIZE_ASC, SIZE_DESC
}

enum class ViewMode {
    GRID, LIST
}

enum class BottomBarStyle {
    FIXED, FLOATING, COMPACT
}

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}
