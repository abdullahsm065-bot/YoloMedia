package com.yolomedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yolomedia.data.model.MediaFolder
import com.yolomedia.data.model.SortOrder
import com.yolomedia.data.model.VideoItem
import com.yolomedia.data.preferences.AppPreferences
import com.yolomedia.data.repository.MediaRepository
import kotlinx.coroutines.launch

class VideosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val preferences = AppPreferences(application)

    private val _folders = MutableLiveData<List<MediaFolder>>()
    val folders: LiveData<List<MediaFolder>> = _folders

    private val _videos = MutableLiveData<List<VideoItem>>()
    val videos: LiveData<List<VideoItem>> = _videos

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _currentFolder = MutableLiveData<String?>(null)
    val currentFolder: LiveData<String?> = _currentFolder

    private val _currentFolderName = MutableLiveData<String?>(null)
    val currentFolderName: LiveData<String?> = _currentFolderName

    fun loadFolders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val folderList = repository.getVideoFolders(preferences.sortOrder)
                _folders.value = folderList
            } catch (e: Exception) {
                _folders.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun loadVideosInFolder(folderPath: String, folderName: String) {
        _currentFolder.value = folderPath
        _currentFolderName.value = folderName
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val videoList = repository.getVideosInFolder(folderPath, preferences.sortOrder)
                _videos.value = videoList
            } catch (e: Exception) {
                _videos.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun goBackToFolders() {
        _currentFolder.value = null
        _currentFolderName.value = null
        loadFolders()
    }

    fun setSortOrder(sortOrder: SortOrder) {
        preferences.sortOrder = sortOrder
        if (_currentFolder.value != null) {
            loadVideosInFolder(_currentFolder.value!!, _currentFolderName.value ?: "")
        } else {
            loadFolders()
        }
    }

    fun deleteVideo(video: VideoItem) {
        viewModelScope.launch {
            try {
                val uri = android.net.Uri.parse(video.uri)
                getApplication<Application>().contentResolver.delete(uri, null, null)
                _currentFolder.value?.let { folder ->
                    loadVideosInFolder(folder, _currentFolderName.value ?: "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
