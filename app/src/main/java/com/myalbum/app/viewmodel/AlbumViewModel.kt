package com.myalbum.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myalbum.app.data.AlbumInfo
import com.myalbum.app.data.MediaItem
import com.myalbum.app.data.MediaStoreHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AlbumViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AlbumViewModel(application) as T
            }
        }
    }
    private val mediaStoreHelper = MediaStoreHelper(application)

    private val _selectedAlbum = MutableStateFlow<String?>(null)
    val selectedAlbum: StateFlow<String?> = _selectedAlbum

    val albums: StateFlow<List<AlbumInfo>> = mediaStoreHelper.getAlbums()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val albumMedia: StateFlow<List<MediaItem>> = _selectedAlbum
        .flatMapLatest { bucketId ->
            if (bucketId != null) {
                mediaStoreHelper.getMediaByAlbum(bucketId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favorites: StateFlow<List<MediaItem>> = mediaStoreHelper.getFavorites()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectAlbum(bucketId: String?) {
        _selectedAlbum.value = bucketId
    }
}
