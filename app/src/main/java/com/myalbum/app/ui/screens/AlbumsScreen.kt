package com.myalbum.app.ui.screens

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.myalbum.app.data.AlbumInfo
import com.myalbum.app.data.MediaItem
import com.myalbum.app.viewmodel.AlbumViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    onAlbumClick: (String, String) -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.factory(app))
    val albums by viewModel.albums.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Album",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (albums.isEmpty()) {
            EmptyStateView(
                modifier = Modifier.padding(paddingValues),
                message = "Không có album nào",
                icon = Icons.Outlined.PhotoAlbum
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(albums, key = { it.bucketId }) { album ->
                    AlbumCard(
                        album = album,
                        onClick = {
                            onAlbumClick(album.bucketId, album.name)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumCard(
    album: AlbumInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                if (album.coverUri != null) {
                    AsyncImage(
                        model = album.coverUri,
                        contentDescription = album.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.Medium
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "${album.count}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    album.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${album.count} mục",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumMediaScreenWithNavigation(
    bucketId: String,
    bucketName: String,
    navController: NavController,
    onItemsLoaded: (List<MediaItem>) -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.factory(app))
    val mediaItems by viewModel.albumMedia.collectAsState()

    LaunchedEffect(bucketId) {
        viewModel.selectAlbum(bucketId)
    }

    LaunchedEffect(mediaItems) {
        if (mediaItems.isNotEmpty()) {
            onItemsLoaded(mediaItems)
        }
    }

    AlbumMediaScreen(
        bucketName = bucketName,
        mediaItems = mediaItems,
        onMediaClick = { index ->
            navController.navigate("viewer/album/$index")
        },
        onBackClick = { navController.navigateUp() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumMediaScreen(
    bucketName: String,
    mediaItems: List<MediaItem>,
    onMediaClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            bucketName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${mediaItems.size} mục",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (mediaItems.isEmpty()) {
            EmptyStateView(
                modifier = Modifier.padding(paddingValues),
                message = "Album trống",
                icon = Icons.Outlined.PhotoAlbum
            )
        } else {
            MediaGrid(
                items = mediaItems,
                selectedItems = emptySet(),
                isSelectionMode = false,
                onMediaClick = { index, _ -> onMediaClick(index) },
                onMediaLongClick = { _, _ -> },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreenWithNavigation(
    navController: NavController,
    onItemsLoaded: (List<MediaItem>) -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.factory(app))
    val favorites by viewModel.favorites.collectAsState()

    LaunchedEffect(favorites) {
        if (favorites.isNotEmpty()) {
            onItemsLoaded(favorites)
        }
    }

    FavoritesScreen(
        favorites = favorites,
        onMediaClick = { index ->
            navController.navigate("viewer/favorites/$index")
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favorites: List<MediaItem>,
    onMediaClick: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Yêu thích",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        if (favorites.isNotEmpty()) {
                            Text(
                                "${favorites.size} mục",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (favorites.isEmpty()) {
            EmptyStateView(
                modifier = Modifier.padding(paddingValues),
                message = "Chưa có ảnh yêu thích\nChạm giữ vào ảnh để thêm",
                icon = Icons.Outlined.FavoriteBorder
            )
        } else {
            MediaGrid(
                items = favorites,
                selectedItems = emptySet(),
                isSelectionMode = false,
                onMediaClick = { index, _ -> onMediaClick(index) },
                onMediaLongClick = { _, _ -> },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
