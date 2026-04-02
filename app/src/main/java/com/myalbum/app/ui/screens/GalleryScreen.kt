package com.myalbum.app.ui.screens

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.myalbum.app.data.MediaItem
import com.myalbum.app.data.MediaStoreHelper
import com.myalbum.app.ui.theme.AppColors
import com.myalbum.app.viewmodel.GalleryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun groupMediaByDate(items: List<MediaItem>): Map<String, List<MediaItem>> {
    if (items.isEmpty()) return emptyMap()

    val calendar = Calendar.getInstance()
    val today = calendar.clone() as Calendar
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    val yesterday = today.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_MONTH, -1)

    val weekStart = today.clone() as Calendar
    weekStart.add(Calendar.DAY_OF_MONTH, -weekStart.get(Calendar.DAY_OF_WEEK) + Calendar.SUNDAY)
    if (weekStart.after(today)) weekStart.add(Calendar.DAY_OF_MONTH, -7)

    val monthStart = today.clone() as Calendar
    monthStart.set(Calendar.DAY_OF_MONTH, 1)

    val groups = linkedMapOf<String, MutableList<MediaItem>>()

    for (item in items) {
        val itemDate = Calendar.getInstance()
        itemDate.timeInMillis = item.dateAdded * 1000L
        itemDate.set(Calendar.HOUR_OF_DAY, 0)
        itemDate.set(Calendar.MINUTE, 0)
        itemDate.set(Calendar.SECOND, 0)
        itemDate.set(Calendar.MILLISECOND, 0)

        val header = when {
            itemDate.timeInMillis >= today.timeInMillis -> "Hom nay"
            itemDate.timeInMillis >= yesterday.timeInMillis -> "Hom qua"
            itemDate.timeInMillis >= weekStart.timeInMillis -> "Tuan nay"
            itemDate.timeInMillis >= monthStart.timeInMillis -> "Thang nay"
            else -> {
                val sdf = SimpleDateFormat("MMMM yyyy", Locale("vi", "VN"))
                sdf.format(Date(item.dateAdded * 1000L))
            }
        }

        groups.getOrPut(header) { mutableListOf() }.add(item)
    }

    return groups
}

fun formatNumber(num: Int): String {
    return String.format("%,d", num)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModel.factory(LocalContext.current.applicationContext as Application)
    ),
    onMediaClick: (Int) -> Unit,
    gridSize: Int = 3
) {
    val mediaItems by viewModel.mediaItems.collectAsState()
    val mediaType by viewModel.mediaType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showSearch by remember { mutableStateOf(false) }
    var showMediaTypeMenu by remember { mutableStateOf(false) }

    val photoCount = mediaItems.count { !it.isVideo }
    val videoCount = mediaItems.count { it.isVideo }
    val groupedMedia = remember(mediaItems) { groupMediaByDate(mediaItems) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                        .windowInsetsPadding(WindowInsets.statusBars),
                    placeholder = { Text("Tim kiem anh/video...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                showSearch = false
                                viewModel.setSearchQuery("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Dong")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp)
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            "Thu vien",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        if (isSelectionMode) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Huy chon")
                            }
                        } else {
                            IconButton(onClick = { showSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Tim kiem")
                            }
                            Box {
                                IconButton(onClick = { showMediaTypeMenu = true }) {
                                    Icon(Icons.Default.FilterList, contentDescription = "Bo loc")
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
                    ),
                    windowInsets = WindowInsets.statusBars
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (mediaItems.isEmpty()) {
            EmptyStateView(
                modifier = Modifier.padding(paddingValues),
                message = "Khong tim thay anh/video nao",
                icon = Icons.Outlined.PhotoLibrary
            )
        } else {
            Column(modifier = Modifier.padding(paddingValues)) {
                // Filter chips
                FilterChipsLazyRow(
                    currentType = mediaType,
                    onTypeSelected = { viewModel.setMediaType(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                // Stats bar
                StatsBar(
                    photoCount = photoCount,
                    videoCount = videoCount,
                    albumCount = groupedMedia.size
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
                GroupedMediaGrid(
                    groupedMedia = groupedMedia,
                    allItems = mediaItems,
                    selectedItems = selectedItems,
                    isSelectionMode = isSelectionMode,
                    onMediaClick = { index, _ -> onMediaClick(index) },
                    onMediaLongClick = { _, item -> viewModel.toggleSelection(item.id) },
                    modifier = Modifier.fillMaxSize(),
                    spanCount = gridSize
                )
            }
        }
    }
}

@Composable
fun FilterChipsLazyRow(
    currentType: MediaStoreHelper.MediaType,
    onTypeSelected: (MediaStoreHelper.MediaType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
    ) {
        item {
            FilterChipItem(
                label = "Tat ca",
                selected = currentType == MediaStoreHelper.MediaType.ALL,
                onClick = { onTypeSelected(MediaStoreHelper.MediaType.ALL) }
            )
        }
        item {
            FilterChipItem(
                label = "Anh",
                selected = currentType == MediaStoreHelper.MediaType.PHOTOS,
                onClick = { onTypeSelected(MediaStoreHelper.MediaType.PHOTOS) },
                icon = Icons.Outlined.Image
            )
        }
        item {
            FilterChipItem(
                label = "Video",
                selected = currentType == MediaStoreHelper.MediaType.VIDEOS,
                onClick = { onTypeSelected(MediaStoreHelper.MediaType.VIDEOS) },
                icon = Icons.Outlined.Videocam
            )
        }
    }
}

@Composable
fun StatsBar(
    photoCount: Int,
    videoCount: Int,
    albumCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatsItem(
                icon = Icons.Outlined.Image,
                count = photoCount,
                label = "anh"
            )
            StatsDivider()
            StatsItem(
                icon = Icons.Outlined.Videocam,
                count = videoCount,
                label = "video"
            )
            StatsDivider()
            StatsItem(
                icon = Icons.Outlined.Folder,
                count = albumCount,
                label = "nhom"
            )
        }
    }
}

@Composable
fun StatsItem(icon: ImageVector, count: Int, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "${formatNumber(count)} $label",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatsDivider() {
    Box(
        modifier = Modifier
            .height(20.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
            text = { Text("Moi nhat") },
            onClick = { onTypeSelected(currentType) },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) }
        )
        Divider()
        DropdownMenuItem(
            text = { Text("Anh") },
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
                "Da chon $selectedCount / $totalCount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSelectAll) {
                    Text("Chon tat ca", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onClear) {
                    Text("Bo chon", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupedMediaGrid(
    groupedMedia: Map<String, List<MediaItem>>,
    allItems: List<MediaItem>,
    selectedItems: Set<Long>,
    isSelectionMode: Boolean,
    onMediaClick: (Int, MediaItem) -> Unit,
    onMediaLongClick: (Int, MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    spanCount: Int = 3
) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(spanCount),
        modifier = modifier,
        state = gridState,
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        groupedMedia.forEach { (dateHeader, items) ->
            item(span = { GridItemSpan(spanCount) }) {
                DateHeaderItem(title = dateHeader, count = items.size)
            }
            items(
                items = items,
                key = { it.id }
            ) { item ->
                val globalIndex = allItems.indexOf(item)
                MediaGridItem(
                    item = item,
                    isSelected = selectedItems.contains(item.id),
                    isSelectionMode = isSelectionMode,
                    onClick = { onMediaClick(globalIndex, item) },
                    onLongClick = { onMediaLongClick(globalIndex, item) }
                )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DateHeaderItem(title: String, count: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        color = Color.Transparent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "(${formatNumber(count)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
        SubcomposeAsyncImage(
            model = item.uri,
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AppColors.ShimmerStart,
                                    AppColors.ShimmerEnd,
                                    AppColors.ShimmerStart
                                )
                            )
                        )
                )
            }
        )

        if (item.isVideo) {
            VideoOverlay(item)
        }

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
                            tint = MaterialTheme.colorScheme.onPrimary
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
        Icon(
            Icons.Default.PlayCircle,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp),
            tint = Color.White.copy(alpha = 0.9f)
        )

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
                modifier = Modifier
                    .padding(3.dp)
                    .size(12.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
fun EmptyStateView(
    message: String,
    icon: ImageVector,
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
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun GalleryScreenWithNavigation(
    navController: NavController,
    onItemsLoaded: (List<MediaItem>) -> Unit,
    gridSize: Int = 3,
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
        },
        gridSize = gridSize
    )
}
