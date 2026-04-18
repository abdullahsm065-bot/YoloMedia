package com.yolomedia.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.yolomedia.data.model.ImageItem
import com.yolomedia.data.model.MediaFolder
import com.yolomedia.data.model.SortOrder
import com.yolomedia.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(private val context: Context) {

    suspend fun getAllVideos(sortOrder: SortOrder = SortOrder.DATE_DESC): List<VideoItem> =
        withContext(Dispatchers.IO) {
            val videos = mutableListOf<VideoItem>()
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.BUCKET_ID
            )

            val orderBy = when (sortOrder) {
                SortOrder.NAME_ASC -> "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
                SortOrder.NAME_DESC -> "${MediaStore.Video.Media.DISPLAY_NAME} DESC"
                SortOrder.DATE_ASC -> "${MediaStore.Video.Media.DATE_ADDED} ASC"
                SortOrder.DATE_DESC -> "${MediaStore.Video.Media.DATE_ADDED} DESC"
                SortOrder.SIZE_ASC -> "${MediaStore.Video.Media.SIZE} ASC"
                SortOrder.SIZE_DESC -> "${MediaStore.Video.Media.SIZE} DESC"
            }

            context.contentResolver.query(collection, projection, null, null, orderBy)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val path = cursor.getString(dataCol) ?: continue
                    val folderPath = File(path).parent ?: continue
                    val folderName = cursor.getString(bucketCol) ?: File(folderPath).name

                    videos.add(
                        VideoItem(
                            id = id,
                            title = cursor.getString(nameCol) ?: "Unknown",
                            path = path,
                            uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString(),
                            duration = cursor.getLong(durationCol),
                            size = cursor.getLong(sizeCol),
                            dateAdded = cursor.getLong(dateAddedCol),
                            dateModified = cursor.getLong(dateModifiedCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            folderName = folderName,
                            folderPath = folderPath,
                            mimeType = cursor.getString(mimeCol) ?: "video/*"
                        )
                    )
                }
            }
            videos
        }

    suspend fun getVideoFolders(sortOrder: SortOrder = SortOrder.DATE_DESC): List<MediaFolder> =
        withContext(Dispatchers.IO) {
            val videos = getAllVideos(sortOrder)
            videos.groupBy { it.folderPath }.map { (path, items) ->
                MediaFolder(
                    name = items.first().folderName,
                    path = path,
                    thumbnailUri = items.firstOrNull()?.uri,
                    mediaCount = items.size,
                    totalSize = items.sumOf { it.size }
                )
            }.sortedByDescending { it.mediaCount }
        }

    suspend fun getVideosInFolder(folderPath: String, sortOrder: SortOrder = SortOrder.DATE_DESC): List<VideoItem> =
        withContext(Dispatchers.IO) {
            getAllVideos(sortOrder).filter { it.folderPath == folderPath }
        }

    suspend fun getAllImages(sortOrder: SortOrder = SortOrder.DATE_DESC): List<ImageItem> =
        withContext(Dispatchers.IO) {
            val images = mutableListOf<ImageItem>()
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_ID
            )

            val orderBy = when (sortOrder) {
                SortOrder.NAME_ASC -> "${MediaStore.Images.Media.DISPLAY_NAME} ASC"
                SortOrder.NAME_DESC -> "${MediaStore.Images.Media.DISPLAY_NAME} DESC"
                SortOrder.DATE_ASC -> "${MediaStore.Images.Media.DATE_ADDED} ASC"
                SortOrder.DATE_DESC -> "${MediaStore.Images.Media.DATE_ADDED} DESC"
                SortOrder.SIZE_ASC -> "${MediaStore.Images.Media.SIZE} ASC"
                SortOrder.SIZE_DESC -> "${MediaStore.Images.Media.SIZE} DESC"
            }

            context.contentResolver.query(collection, projection, null, null, orderBy)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val path = cursor.getString(dataCol) ?: continue
                    val folderPath = File(path).parent ?: continue
                    val folderName = cursor.getString(bucketCol) ?: File(folderPath).name

                    images.add(
                        ImageItem(
                            id = id,
                            title = cursor.getString(nameCol) ?: "Unknown",
                            path = path,
                            uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                            size = cursor.getLong(sizeCol),
                            dateAdded = cursor.getLong(dateAddedCol),
                            dateModified = cursor.getLong(dateModifiedCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            folderName = folderName,
                            folderPath = folderPath,
                            mimeType = cursor.getString(mimeCol) ?: "image/*"
                        )
                    )
                }
            }
            images
        }

    suspend fun getImageFolders(sortOrder: SortOrder = SortOrder.DATE_DESC): List<MediaFolder> =
        withContext(Dispatchers.IO) {
            val images = getAllImages(sortOrder)
            images.groupBy { it.folderPath }.map { (path, items) ->
                MediaFolder(
                    name = items.first().folderName,
                    path = path,
                    thumbnailUri = items.firstOrNull()?.uri?.toString(),
                    mediaCount = items.size,
                    totalSize = items.sumOf { it.size }
                )
            }.sortedByDescending { it.mediaCount }
        }
}
