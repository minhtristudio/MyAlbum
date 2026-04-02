package com.myalbum.app.ui.screens

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.myalbum.app.data.AlbumInfo
import com.myalbum.app.data.MediaItem
import com.myalbum.app.viewmodel.AlbumViewModel

enum class AlbumSortOrder(val displayName: String) {
    NAME("Ten"),
    COUNT("So luong"),
    RECENT("Gan day")
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    onAlbumClick: (String, String) -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.factory(app))
    val albums by viewModel.albums.collectAsState()

    var sortExpanded by remember { mutableStateOf(false) }
    var currentSort by remember { mutableStateOf(AlbumSortOrder.COUNT) }

    val sortedAlbums = remember(albums, currentSort) {
        when (currentSort) {
            AlbumSortOrder.NAME -> albums.sortedBy { it.name.lowercase() }
            AlbumSortOrder.COUNT -> albums.sortedByDescending { it.count }
            AlbumSortOrder.RECENT -> albums
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Album",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { sortExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Sap xep"
                            )
                        }
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false }
                        ) {
                            AlbumSortOrder.values().forEach { sortOrder ->
                                DropdownMenuItem(
                                    text = { Text(sortOrder.displayName) },
                                    onClick = {
                                        currentSort = sortOrder
                                        sortExpanded = false
                                    },
                                    leadingIcon = if (sortOrder == currentSort) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { paddingValues ->
        if (sortedAlbums.isEmpty()) {
            EmptyStateView(
                modifier = Modifier.padding(paddingValues),
                message = "Khong co album nao",
                icon = Icons.Outlined.PhotoAlbum
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedAlbums, key = { it.bucketId }) { album ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumCard(
    album: AlbumInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
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
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }

                // Bottom gradient
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.45f)
                                )
                            )
                        )
                )

                // Count badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Text(
                        "${album.count}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    album.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${album.count} muc",
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
    onItemsLoaded: (List<MediaItem>) -> Unit,
    gridSize: Int = 3
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
        onBackClick = { navController.navigateUp() },
        gridSize = gridSize
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumMediaScreen(
    bucketName: String,
    mediaItems: List<MediaItem>,
    onMediaClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    gridSize: Int = 3
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            bucketName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                        if (mediaItems.isNotEmpty()) {
                            Text(
                                "${mediaItems.size} muc",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lai")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { paddingValues ->
        if (mediaItems.isEmpty()) {
            EmptyStateView(
                modifier = Modifier.padding(paddingValues),
                message = "Album trong",
                icon = Icons.Outlined.PhotoAlbum
            )
        } else {
            MediaGrid(
                items = mediaItems,
                selectedItems = emptySet(),
                isSelectionMode = false,
                onMediaClick = { index, _ -> onMediaClick(index) },
                onMediaLongClick = { _, _ -> },
                modifier = Modifier.padding(paddingValues),
                spanCount = gridSize
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreenWithNavigation(
    navController: NavController,
    onItemsLoaded: (List<MediaItem>) -> Unit,
    gridSize: Int = 3
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
        },
        gridSize = gridSize
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favorites: List<MediaItem>,
    onMediaClick: (Int) -> Unit,
    gridSize: Int = 3
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Yeu thich",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (favorites.isNotEmpty()) {
                            Text(
                                "${favorites.size} muc",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { paddingValues ->
        if (favorites.isEmpty()) {
            EmptyStateView(
                modifier = Modifier.padding(paddingValues),
                message = "Chua co anh yeu thich\nCham giu vao anh de them",
                icon = Icons.Outlined.FavoriteBorder
            )
        } else {
            MediaGrid(
                items = favorites,
                selectedItems = emptySet(),
                isSelectionMode = false,
                onMediaClick = { index, _ -> onMediaClick(index) },
                onMediaLongClick = { _, _ -> },
                modifier = Modifier.padding(paddingValues),
                spanCount = gridSize
            )
        }
    }
}
