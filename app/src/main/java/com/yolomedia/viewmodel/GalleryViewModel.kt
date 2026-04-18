package com.yolomedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yolomedia.data.model.ImageItem
import com.yolomedia.data.model.SortOrder
import com.yolomedia.data.preferences.AppPreferences
import com.yolomedia.data.repository.MediaRepository
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val preferences = AppPreferences(application)

    private val _images = MutableLiveData<List<ImageItem>>()
    val images: LiveData<List<ImageItem>> = _images

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadImages() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val imageList = repository.getAllImages(preferences.sortOrder)
                _images.value = imageList
            } catch (e: Exception) {
                _images.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        preferences.sortOrder = sortOrder
        loadImages()
    }

    fun deleteImage(image: ImageItem) {
        viewModelScope.launch {
            try {
                getApplication<Application>().contentResolver.delete(image.uri, null, null)
                loadImages()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
