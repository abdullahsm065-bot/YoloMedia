package com.yolomedia.data.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class VideoItem(
    val id: Long,
    val title: String,
    val path: String,
    val uri: String,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val width: Int = 0,
    val height: Int = 0,
    val folderName: String = "",
    val folderPath: String = "",
    val mimeType: String = ""
) : Parcelable {
    val name: String get() = title

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(title)
        parcel.writeString(path)
        parcel.writeString(uri)
        parcel.writeLong(duration)
        parcel.writeLong(size)
        parcel.writeLong(dateAdded)
        parcel.writeLong(dateModified)
        parcel.writeInt(width)
        parcel.writeInt(height)
        parcel.writeString(folderName)
        parcel.writeString(folderPath)
        parcel.writeString(mimeType)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<VideoItem> {
        override fun createFromParcel(parcel: Parcel): VideoItem = VideoItem(parcel)
        override fun newArray(size: Int): Array<VideoItem?> = arrayOfNulls(size)
    }
}

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
    val thumbnailUri: String?,
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
