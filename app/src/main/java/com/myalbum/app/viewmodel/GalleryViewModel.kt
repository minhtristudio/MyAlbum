package com.myalbum.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myalbum.app.data.MediaItem
import com.myalbum.app.data.MediaStoreHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GalleryViewModel(application) as T
            }
        }
    }
    private val mediaStoreHelper = MediaStoreHelper(application)

    private val _mediaType = MutableStateFlow(MediaStoreHelper.MediaType.ALL)
    val mediaType: StateFlow<MediaStoreHelper.MediaType> = _mediaType

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _selectedItems = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItems: StateFlow<Set<Long>> = _selectedItems
    val isSelectionMode: StateFlow<Boolean> = _selectedItems.map { it.isNotEmpty() }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val mediaItems: StateFlow<List<MediaItem>> = combine(
        _mediaType,
        _searchQuery
    ) { type, query ->
        Pair(type, query)
    }.flatMapLatest { (type, query) ->
        mediaStoreHelper.getAllMedia(
            mediaType = type,
            query = query.ifBlank { null }
        )
    }.stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )

    fun setMediaType(type: MediaStoreHelper.MediaType) {
        _mediaType.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelection(id: Long) {
        _selectedItems.update { current ->
            if (current.contains(id)) current - id
            else current + id
        }
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
    }

    fun selectAll(items: List<MediaItem>) {
        _selectedItems.value = items.map { it.id }.toSet()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Force re-collection by toggling a flow
            _mediaType.value = _mediaType.value
            _isRefreshing.value = false
        }
    }
}
