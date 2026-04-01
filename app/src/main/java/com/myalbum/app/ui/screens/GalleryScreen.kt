package com.myalbum.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.app.Application
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.myalbum.app.data.MediaItem
import com.myalbum.app.data.MediaStoreHelper
import com.myalbum.app.ui.theme.AppColors
import androidx.navigation.NavController
import com.myalbum.app.viewmodel.GalleryViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModel.factory(LocalContext.current.applicationContext as Application)
    ),
    onMediaClick: (Int) -> Unit
) {
    val mediaItems by viewModel.mediaItems.collectAsState()
    val mediaType by viewModel.mediaType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showSearch by remember { mutableStateOf(false) }
    var showMediaTypeMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (showSearch) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onSearch = {},
                    onActiveChange = { if (!it) { showSearch = false; viewModel.setSearchQuery("") } },
                    active = false,
                    onCloseClick = { showSearch = false; viewModel.setSearchQuery("") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    placeholder = { Text("Tìm kiếm ảnh/video...") }
                ) {}
            } else {
                TopAppBar(
                    title = {
                        Text(
                            "Thư viện",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    },
                    actions = {
                        if (isSelectionMode) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Hủy chọn")
                            }
                        } else {
                            IconButton(onClick = { showSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                            }
                            Box {
                                IconButton(onClick = { showMediaTypeMenu = true }) {
                                    Icon(Icons.Default.FilterList, contentDescription = "Bộ lọc")
                                }
                                MediaTypeDropdownMenu(
                                    expanded = showMediaTypeMenu,
                                    onDismiss = { showMediaTypeMenu = false },
                                    currentType = mediaType,
                                    onTypeSelected = {
                                        viewModel.setMediaType(it)
                                        showMediaTypeMenu = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (mediaItems.isEmpty()) {
            EmptyStateView(
                modifier = Modifier.padding(paddingValues),
                message = "Không tìm thấy ảnh/video nào",
                icon = Icons.Outlined.PhotoLibrary
            )
        } else {
            Column(modifier = Modifier.padding(paddingValues)) {
                // Filter chips
                FilterChipsRow(
                    currentType = mediaType,
                    onTypeSelected = { viewModel.setMediaType(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Selection info
                if (isSelectionMode) {
                    SelectionInfoBar(
                        selectedCount = selectedItems.size,
                        totalCount = mediaItems.size,
                        onSelectAll = { viewModel.selectAll(mediaItems) },
                        onClear = { viewModel.clearSelection() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Media grid
                MediaGrid(
                    items = mediaItems,
                    selectedItems = selectedItems,
                    isSelectionMode = isSelectionMode,
                    onMediaClick = { index, _ -> onMediaClick(index) },
                    onMediaLongClick = { _, item -> viewModel.toggleSelection(item.id) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun FilterChipsRow(
    currentType: MediaStoreHelper.MediaType,
    onTypeSelected: (MediaStoreHelper.MediaType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChipItem(
            label = "Tất cả",
            selected = currentType == MediaStoreHelper.MediaType.ALL,
            onClick = { onTypeSelected(MediaStoreHelper.MediaType.ALL) }
        )
        FilterChipItem(
            label = "Ảnh",
            selected = currentType == MediaStoreHelper.MediaType.PHOTOS,
            onClick = { onTypeSelected(MediaStoreHelper.MediaType.PHOTOS) },
            icon = Icons.Outlined.Image
        )
        FilterChipItem(
            label = "Video",
            selected = currentType == MediaStoreHelper.MediaType.VIDEOS,
            onClick = { onTypeSelected(MediaStoreHelper.MediaType.VIDEOS) },
            icon = Icons.Outlined.Videocam
        )
    }
}

@Composable
fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun MediaTypeDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    currentType: MediaStoreHelper.MediaType,
    onTypeSelected: (MediaStoreHelper.MediaType) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Mới nhất") },
            onClick = { onTypeSelected(currentType) },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) }
        )
        Divider()
        DropdownMenuItem(
            text = { Text("Ảnh") },
            onClick = { onTypeSelected(MediaStoreHelper.MediaType.PHOTOS) },
            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Video") },
            onClick = { onTypeSelected(MediaStoreHelper.MediaType.VIDEOS) },
            leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null) }
        )
    }
}

@Composable
fun SelectionInfoBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Đã chọn $selectedCount / $totalCount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSelectAll) {
                    Text("Chọn tất cả", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onClear) {
                    Text("Bỏ chọn", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGrid(
    items: List<MediaItem>,
    selectedItems: Set<Long>,
    isSelectionMode: Boolean,
    onMediaClick: (Int, MediaItem) -> Unit,
    onMediaLongClick: (Int, MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    spanCount: Int = 3
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(spanCount),
        modifier = modifier,
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            val index = items.indexOf(item)
            MediaGridItem(
                item = item,
                isSelected = selectedItems.contains(item.id),
                isSelectionMode = isSelectionMode,
                onClick = { onMediaClick(index, item) },
                onLongClick = { onMediaLongClick(index, item) }
            )
        }
    }
}

@Composable
fun MediaGridItem(
    item: MediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium
        )

        // Video indicator
        if (item.isVideo) {
            VideoOverlay(item)
        }

        // Selection overlay
        AnimatedVisibility(
            visible = isSelected || isSelectionMode,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isSelected) AppColors.SelectionOverlay
                        else Color.Transparent
                    )
            )

            if (isSelectionMode) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp),
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    },
                    border = if (!isSelected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    } else null
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.padding(2.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoOverlay(item: MediaItem) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, AppColors.VideoOverlay)
                )
            )
    ) {
        // Play icon
        Icon(
            Icons.Default.PlayCircle,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp),
            tint = Color.White.copy(alpha = 0.9f)
        )

        // Duration badge
        if (item.duration > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = AppColors.VideoOverlay
            ) {
                Text(
                    item.formattedDuration,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        // Video badge
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp),
            shape = RoundedCornerShape(4.dp),
            color = AppColors.VideoBadge
        ) {
            Icon(
                Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.padding(3.dp).size(12.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
fun EmptyStateView(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = AppColors.EmptyState
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun GalleryScreenWithNavigation(
    navController: NavController,
    onItemsLoaded: (List<MediaItem>) -> Unit,
    viewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModel.factory(LocalContext.current.applicationContext as Application)
    )
) {
    val mediaItems by viewModel.mediaItems.collectAsState()

    LaunchedEffect(mediaItems) {
        if (mediaItems.isNotEmpty()) {
            onItemsLoaded(mediaItems)
        }
    }

    GalleryScreen(
        viewModel = viewModel,
        onMediaClick = { index ->
            navController.navigate("viewer/gallery/$index")
        }
    )
}
