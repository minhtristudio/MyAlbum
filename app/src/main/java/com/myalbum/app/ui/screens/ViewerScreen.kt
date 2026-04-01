package com.myalbum.app.ui.screens

import android.content.ContentResolver
import android.content.Intent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.myalbum.app.data.MediaItem
import com.myalbum.app.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    items: List<MediaItem>,
    initialIndex: Int,
    onBack: () -> Unit,
    onItemDeleted: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val scope = rememberCoroutineScope()

    var isSystemUiVisible by remember { mutableStateOf(true) }
    var currentPage by remember { mutableIntStateOf(initialIndex) }
    var isSlideShowActive by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(isSystemUiVisible) {
        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isSystemUiVisible) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    BackHandler {
        if (showInfoSheet) {
            showInfoSheet = false
        } else if (isSlideShowActive) {
            isSlideShowActive = false
        } else {
            onBack()
        }
    }

    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Không có media", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    val safeIndex = initialIndex.coerceIn(0, items.size - 1)
    val pagerState = rememberPagerState(
        initialPage = safeIndex,
        pageCount = { items.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        currentPage = pagerState.currentPage
    }

    // Slideshow auto-advance
    LaunchedEffect(isSlideShowActive) {
        if (isSlideShowActive) {
            while (true) {
                delay(3000)
                val nextPage = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    // Info bottom sheet
    if (showInfoSheet) {
        val currentItem = items.getOrNull(currentPage)
        if (currentItem != null) {
            InfoBottomSheet(
                item = currentItem,
                onDismiss = { showInfoSheet = false }
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa media") },
            text = { Text("Bạn có chắc muốn xóa \"${items.getOrNull(currentPage)?.name}\"? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val itemToDelete = items.getOrNull(currentPage) ?: return@TextButton
                        val contentUri = itemToDelete.contentUri
                        try {
                            val rowsDeleted = context.contentResolver.delete(
                                contentUri,
                                null,
                                null
                            )
                            if (rowsDeleted > 0) {
                                Toast.makeText(context, "Đã xóa thành công", Toast.LENGTH_SHORT).show()
                                onItemDeleted?.invoke(currentPage)
                                if (items.size <= 1) {
                                    onBack()
                                }
                            } else {
                                Toast.makeText(context, "Không thể xóa", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Xóa", color = AppColors.DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> items[index].id }
        ) { page ->
            val item = items[page]
            ViewerPage(
                item = item,
                onTap = { isSystemUiVisible = !isSystemUiVisible }
            )
        }

        // Top bar
        AnimatedVisibility(
            visible = isSystemUiVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        val currentItem = items.getOrNull(currentPage)
                        if (currentItem != null) {
                            Text(
                                currentItem.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${currentPage + 1} / ${items.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Slideshow button
                    IconButton(onClick = {
                        isSlideShowActive = !isSlideShowActive
                        isSystemUiVisible = false
                    }) {
                        Icon(
                            if (isSlideShowActive) Icons.Default.Close else Icons.Default.Repeat,
                            contentDescription = "Trình chiếu",
                            tint = if (isSlideShowActive) AppColors.SlideShowActive else Color.White
                        )
                    }

                    IconButton(onClick = { /* Toggle favorite */ }) {
                        val isFav = items.getOrNull(currentPage)?.isFavorite == true
                        Icon(
                            if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Yêu thích",
                            tint = if (isFav) AppColors.FavoriteActive else Color.White
                        )
                    }

                    IconButton(onClick = {
                        val shareItem = items.getOrNull(currentPage) ?: return@IconButton
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = shareItem.mimeType
                            putExtra(Intent.EXTRA_STREAM, shareItem.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ"))
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Chia sẻ",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Xóa",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = { showInfoSheet = true }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Thông tin",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Page indicator dots
        AnimatedVisibility(
            visible = isSystemUiVisible && !isSlideShowActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PageIndicator(
                currentPage = currentPage,
                totalPages = items.size,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        // Bottom info bar for videos
        AnimatedVisibility(
            visible = isSystemUiVisible && items.getOrNull(currentPage)?.isVideo == true,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                val currentItem = items.getOrNull(currentPage)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentItem != null) {
                        Text(
                            currentItem.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            currentItem.formattedDuration,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Slideshow indicator
        AnimatedVisibility(
            visible = isSlideShowActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Surface(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = AppColors.SlideShowActive.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Trình chiếu",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(
    item: MediaItem,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Title
            Text(
                "Thông tin chi tiết",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // File name
            InfoRow(label = "Tên file", value = item.name)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Resolution
            if (item.width > 0 && item.height > 0) {
                InfoRow(
                    label = "Độ phân giải",
                    value = "${item.width} × ${item.height} px"
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // File size
            InfoRow(label = "Kích thước", value = item.formattedSize)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Date added
            val dateStr = try {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                sdf.format(Date(item.dateAdded * 1000L))
            } catch (e: Exception) {
                "${item.dateAdded}"
            }
            InfoRow(label = "Ngày thêm", value = dateStr)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Duration for videos
            if (item.isVideo && item.duration > 0) {
                InfoRow(label = "Thời lượng", value = item.formattedDuration)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // MIME type
            InfoRow(label = "Loại file", value = item.mimeType)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Media type
            InfoRow(
                label = "Loại media",
                value = if (item.isVideo) "Video" else "Ảnh"
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Album
            if (item.bucketName.isNotEmpty()) {
                InfoRow(label = "Album", value = item.bucketName)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show dots based on total pages
        val maxDots = 7
        val dotsToShow = if (totalPages <= maxDots) {
            (0 until totalPages).toList()
        } else {
            val start = (currentPage - 3).coerceAtLeast(0)
            val end = (currentPage + 3).coerceAtMost(totalPages - 1)
            (start..end).toList()
        }

        for (i in dotsToShow) {
            val isSelected = i == currentPage
            val dotSize by animateFloatAsState(
                targetValue = if (isSelected) 8f else 5f,
                animationSpec = androidx.compose.animation.core.tween(200)
            )
            Box(
                modifier = Modifier
                    .size(dotSize.dp)
                    .background(
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            )
        }

        if (totalPages > maxDots) {
            Text(
                "${currentPage + 1}/${totalPages}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ViewerPage(
    item: MediaItem,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val state = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offset += panChange
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                    onTap = { onTap() }
                )
            }
            .transformable(state = state),
        contentAlignment = Alignment.Center
    ) {
        if (item.isVideo) {
            AsyncImage(
                model = item.uri,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.High
            )

            if (scale == 1f) {
                Surface(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, item.uri).apply {
                            setDataAndType(item.uri, item.mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Phát video",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Black
                        )
                    }
                }

                if (item.duration > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.VideoOverlay
                    ) {
                        Text(
                            item.formattedDuration,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            AsyncImage(
                model = item.uri,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.High
            )
        }
    }
}
