package com.yolomedia.data.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import com.yolomedia.data.model.ImageItem
import com.yolomedia.data.model.SafeFolder
import com.yolomedia.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SafeRepository(private val context: Context) {

    private val safeDir: File
        get() {
            val dir = File(context.filesDir, "safe_vault")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun getSafeFolders(): List<SafeFolder> {
        val folders = mutableListOf<SafeFolder>()
        val dir = safeDir
        if (dir.exists()) {
            dir.listFiles()?.filter { it.isDirectory }?.forEach { folder ->
                val videos = folder.listFiles()?.count { it.name.endsWith(".nomedia_video") } ?: 0
                val photos = folder.listFiles()?.count { it.name.endsWith(".nomedia_photo") } ?: 0
                folders.add(
                    SafeFolder(
                        name = folder.name,
                        path = folder.absolutePath,
                        videoCount = videos,
                        photoCount = photos
                    )
                )
            }
        }
        return folders
    }

    fun createFolder(name: String): Boolean {
        val folder = File(safeDir, name)
        return if (!folder.exists()) {
            folder.mkdirs()
        } else false
    }

    fun deleteFolder(name: String): Boolean {
        val folder = File(safeDir, name)
        return if (folder.exists()) {
            folder.deleteRecursively()
        } else false
    }

    fun renameFolder(oldName: String, newName: String): Boolean {
        val oldFolder = File(safeDir, oldName)
        val newFolder = File(safeDir, newName)
        return if (oldFolder.exists() && !newFolder.exists()) {
            oldFolder.renameTo(newFolder)
        } else false
    }

    suspend fun moveVideoToSafe(video: VideoItem, folderName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val targetFolder = File(safeDir, folderName)
                if (!targetFolder.exists()) targetFolder.mkdirs()

                val sourceFile = File(video.path)
                if (!sourceFile.exists()) return@withContext false

                val targetFile = File(targetFolder, "${sourceFile.nameWithoutExtension}_${video.id}.nomedia_video")
                
                // Store original extension in a metadata file
                val metaFile = File(targetFolder, "${sourceFile.nameWithoutExtension}_${video.id}.meta")
                metaFile.writeText("${sourceFile.extension}\n${video.title}\n${video.duration}\n${video.size}\nvideo")

                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()

                // Notify media scanner about the deletion
                MediaScannerConnection.scanFile(context, arrayOf(video.path), null, null)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    suspend fun moveImageToSafe(image: ImageItem, folderName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val targetFolder = File(safeDir, folderName)
                if (!targetFolder.exists()) targetFolder.mkdirs()

                val sourceFile = File(image.path)
                if (!sourceFile.exists()) return@withContext false

                val targetFile = File(targetFolder, "${sourceFile.nameWithoutExtension}_${image.id}.nomedia_photo")
                
                val metaFile = File(targetFolder, "${sourceFile.nameWithoutExtension}_${image.id}.meta")
                metaFile.writeText("${sourceFile.extension}\n${image.title}\n0\n${image.size}\nphoto")

                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()

                MediaScannerConnection.scanFile(context, arrayOf(image.path), null, null)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    suspend fun restoreFromSafe(safeFilePath: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val safeFile = File(safeFilePath)
                if (!safeFile.exists()) return@withContext false

                val metaFile = File(safeFilePath.replace(".nomedia_video", ".meta").replace(".nomedia_photo", ".meta"))
                val extension = if (metaFile.exists()) {
                    metaFile.readLines().firstOrNull() ?: "mp4"
                } else "mp4"

                val isVideo = safeFilePath.endsWith(".nomedia_video")
                val restoreDir = if (isVideo) {
                    File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES), "YoloMedia")
                } else {
                    File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), "YoloMedia")
                }
                if (!restoreDir.exists()) restoreDir.mkdirs()

                val baseName = safeFile.nameWithoutExtension.substringBeforeLast("_")
                val restoredFile = File(restoreDir, "$baseName.$extension")

                safeFile.copyTo(restoredFile, overwrite = true)
                safeFile.delete()
                metaFile.delete()

                MediaScannerConnection.scanFile(context, arrayOf(restoredFile.absolutePath), null, null)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    fun getSafeVideos(folderPath: String): List<SafeMediaItem> {
        val folder = File(folderPath)
        if (!folder.exists()) return emptyList()

        return folder.listFiles()
            ?.filter { it.name.endsWith(".nomedia_video") }
            ?.map { file ->
                val metaFile = File(file.absolutePath.replace(".nomedia_video", ".meta"))
                val meta = if (metaFile.exists()) metaFile.readLines() else emptyList()
                SafeMediaItem(
                    name = meta.getOrNull(1) ?: file.nameWithoutExtension,
                    path = file.absolutePath,
                    size = meta.getOrNull(3)?.toLongOrNull() ?: file.length(),
                    duration = meta.getOrNull(2)?.toLongOrNull() ?: 0,
                    isVideo = true,
                    uri = Uri.fromFile(file)
                )
            }
            ?.sortedByDescending { it.name }
            ?: emptyList()
    }

    fun getSafePhotos(folderPath: String): List<SafeMediaItem> {
        val folder = File(folderPath)
        if (!folder.exists()) return emptyList()

        return folder.listFiles()
            ?.filter { it.name.endsWith(".nomedia_photo") }
            ?.map { file ->
                val metaFile = File(file.absolutePath.replace(".nomedia_photo", ".meta"))
                val meta = if (metaFile.exists()) metaFile.readLines() else emptyList()
                SafeMediaItem(
                    name = meta.getOrNull(1) ?: file.nameWithoutExtension,
                    path = file.absolutePath,
                    size = meta.getOrNull(3)?.toLongOrNull() ?: file.length(),
                    duration = 0,
                    isVideo = false,
                    uri = Uri.fromFile(file)
                )
            }
            ?.sortedByDescending { it.name }
            ?: emptyList()
    }
}

data class SafeMediaItem(
    val name: String,
    val path: String,
    val size: Long,
    val duration: Long,
    val isVideo: Boolean,
    val uri: Uri
)
