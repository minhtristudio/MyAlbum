package com.myalbum.app.ui.screens

import android.app.Application
import android.net.Uri
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.myalbum.app.data.MediaItem
import com.myalbum.app.data.MediaStoreHelper
import com.myalbum.app.data.VideoThumbnailUtil
import com.myalbum.app.ui.theme.AppColors
import com.myalbum.app.viewmodel.GalleryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

fun formatNumber(num: Int): String = String.format("%,d", num)

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
    var showSortMenu by remember { mutableStateOf(false) }

    val photoCount = mediaItems.count { !it.isVideo }
    val videoCount = mediaItems.count { it.isVideo }
    val totalSize = mediaItems.sumOf { it.size }
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
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            "Thu vien",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
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
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.Default.Sort, contentDescription = "Sap xep")
                                }
                                SortDropdownMenu(
                                    expanded = showSortMenu,
                                    onDismiss = { showSortMenu = false },
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
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (mediaItems.isEmpty() && !showSearch) {
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Stats bar
                StatsBar(
                    photoCount = photoCount,
                    videoCount = videoCount,
                    albumCount = groupedMedia.size,
                    totalSize = totalSize
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
    totalSize: Long = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
            if (totalSize > 0) {
                StatsDivider()
                StatsSizeItem(totalSize)
            }
        }
    }
}

@Composable
fun StatsItem(icon: ImageVector, count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            formatNumber(count),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatsSizeItem(totalSize: Long) {
    val sizeStr = when {
        totalSize < 1024 * 1024 * 1024 -> String.format("%.1f MB", totalSize / (1024.0 * 1024))
        else -> String.format("%.1f GB", totalSize / (1024.0 * 1024 * 1024))
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            sizeStr,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "dung luong",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatsDivider() {
    Box(
        modifier = Modifier
            .height(24.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun SortDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Moi nhat") },
            onClick = { onDismiss() },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) }
        )
        androidx.compose.material3.Divider()
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Cu nhat") },
            onClick = { onDismiss() },
            leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) }
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Ten A-Z") },
            onClick = { onDismiss() },
            leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null) }
        )
    }
}

@Composable
fun MediaTypeDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    currentType: MediaStoreHelper.MediaType,
    onTypeSelected: (MediaStoreHelper.MediaType) -> Unit
) {
    androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Moi nhat") },
            onClick = { onTypeSelected(currentType) },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) }
        )
        androidx.compose.material3.Divider()
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Anh") },
            onClick = { onTypeSelected(MediaStoreHelper.MediaType.PHOTOS) },
            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
        )
        androidx.compose.material3.DropdownMenuItem(
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
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Da chon $selectedCount / $totalCount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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

    // Pre-compute index map for O(1) lookup instead of O(n) indexOf
    val indexMap = remember(allItems) {
        allItems.withIndex().associateBy { it.value.id }.mapValues { it.value.index }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(spanCount),
        modifier = modifier,
        state = gridState,
        contentPadding = PaddingValues(4.dp),
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
                val globalIndex = indexMap[item.id] ?: allItems.indexOf(item)
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
        contentPadding = PaddingValues(4.dp),
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
            .padding(vertical = 6.dp, horizontal = 8.dp),
        color = Color.Transparent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "${formatNumber(count)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
            .aspectRatio(item.gridAspectRatio)
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current

        // For videos: generate thumbnail via produceState, then load via Coil
        val videoThumbnailUri by produceState<Uri?>(null, item.uri) {
            if (item.isVideo) {
                value = withContext(Dispatchers.IO) {
                    VideoThumbnailUtil.getOrCreateThumbnail(context, item.uri)
                }
            }
        }

        // Image/Video thumbnail
        val imageModel = if (item.isVideo && videoThumbnailUri != null) {
            videoThumbnailUri as Any
        } else if (item.isVideo) {
            null
        } else {
            item.uri
        }

        if (imageModel != null) {
            SubcomposeAsyncImage(
                model = imageModel,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                },
                error = {
                    // Safe fallback - just show placeholder, never retry
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (item.isVideo) Icons.Default.Videocam else Icons.Outlined.Image,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            )
        } else {
            // Loading video thumbnail or video thumbnail failed
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        // Video overlay with play icon, duration, size
        if (item.isVideo) {
            VideoOverlay(item)
        }

        // File size badge for large files (>5MB)
        if (item.size > 5 * 1024 * 1024 && !item.isVideo) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    item.formattedSize,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = Color.White
                )
            }
        }

        // Resolution badge for high-res images (>3000px)
        if (item.width > 3000 && !item.isVideo) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
            ) {
                Text(
                    item.formattedResolution,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
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
                        .padding(5.dp)
                        .size(24.dp),
                    shape = CircleShape,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    },
                    border = if (!isSelected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                )
            )
    ) {
        // Play icon - subtle circle background
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.25f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White.copy(alpha = 0.95f)
                )
            }
        }

        // Duration badge
        if (item.duration > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    item.formattedDuration,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White
                )
            }
        }

        // File size badge
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Text(
                item.formattedSize,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = Color.White.copy(alpha = 0.8f)
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
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(20.dp)
                        .size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
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
