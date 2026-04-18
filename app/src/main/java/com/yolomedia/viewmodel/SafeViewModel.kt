package com.yolomedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yolomedia.data.model.ImageItem
import com.yolomedia.data.model.SafeFolder
import com.yolomedia.data.model.VideoItem
import com.yolomedia.data.preferences.AppPreferences
import com.yolomedia.data.repository.SafeMediaItem
import com.yolomedia.data.repository.SafeRepository
import kotlinx.coroutines.launch

class SafeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SafeRepository(application)
    val preferences = AppPreferences(application)

    private val _isAuthenticated = MutableLiveData(false)
    val isAuthenticated: LiveData<Boolean> = _isAuthenticated

    private val _folders = MutableLiveData<List<SafeFolder>>()
    val folders: LiveData<List<SafeFolder>> = _folders

    private val _safeVideos = MutableLiveData<List<SafeMediaItem>>()
    val safeVideos: LiveData<List<SafeMediaItem>> = _safeVideos

    private val _safePhotos = MutableLiveData<List<SafeMediaItem>>()
    val safePhotos: LiveData<List<SafeMediaItem>> = _safePhotos

    private val _currentFolder = MutableLiveData<SafeFolder?>(null)
    val currentFolder: LiveData<SafeFolder?> = _currentFolder

    private val _operationResult = MutableLiveData<OperationResult?>()
    val operationResult: LiveData<OperationResult?> = _operationResult

    fun authenticate(pin: String): Boolean {
        val stored = preferences.safePin
        return if (stored == pin) {
            _isAuthenticated.value = true
            true
        } else false
    }

    fun authenticateWithBiometric() {
        _isAuthenticated.value = true
    }

    fun setupPin(pin: String) {
        preferences.safePin = pin
        preferences.isSafeSetup = true
        _isAuthenticated.value = true
    }

    fun lockSafe() {
        _isAuthenticated.value = false
        _currentFolder.value = null
    }

    fun loadFolders() {
        _folders.value = repository.getSafeFolders()
    }

    fun createFolder(name: String): Boolean {
        val result = repository.createFolder(name)
        if (result) loadFolders()
        return result
    }

    fun deleteFolder(name: String): Boolean {
        val result = repository.deleteFolder(name)
        if (result) loadFolders()
        return result
    }

    fun renameFolder(oldName: String, newName: String): Boolean {
        val result = repository.renameFolder(oldName, newName)
        if (result) loadFolders()
        return result
    }

    fun openFolder(folder: SafeFolder) {
        _currentFolder.value = folder
        loadFolderContents(folder.path)
    }

    fun goBack() {
        _currentFolder.value = null
    }

    private fun loadFolderContents(folderPath: String) {
        _safeVideos.value = repository.getSafeVideos(folderPath)
        _safePhotos.value = repository.getSafePhotos(folderPath)
    }

    fun moveVideoToSafe(video: VideoItem, folderName: String) {
        viewModelScope.launch {
            val result = repository.moveVideoToSafe(video, folderName)
            _operationResult.value = if (result) {
                OperationResult.Success("Video moved to Safe successfully")
            } else {
                OperationResult.Error("Failed to move video to Safe")
            }
            loadFolders()
        }
    }

    fun moveImageToSafe(image: ImageItem, folderName: String) {
        viewModelScope.launch {
            val result = repository.moveImageToSafe(image, folderName)
            _operationResult.value = if (result) {
                OperationResult.Success("Photo moved to Safe successfully")
            } else {
                OperationResult.Error("Failed to move photo to Safe")
            }
            loadFolders()
        }
    }

    fun restoreMedia(safeFilePath: String) {
        viewModelScope.launch {
            val result = repository.restoreFromSafe(safeFilePath)
            _operationResult.value = if (result) {
                OperationResult.Success("Media restored successfully")
            } else {
                OperationResult.Error("Failed to restore media")
            }
            _currentFolder.value?.let { loadFolderContents(it.path) }
            loadFolders()
        }
    }

    fun clearOperationResult() {
        _operationResult.value = null
    }

    fun getSafeFolderNames(): List<String> {
        return repository.getSafeFolders().map { it.name }
    }

    sealed class OperationResult {
        data class Success(val message: String) : OperationResult()
        data class Error(val message: String) : OperationResult()
    }
}
